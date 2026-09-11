#!/usr/bin/env bash
#
# Despliegue de Nakama Hub en un servidor con Docker.
#
#   ./deploy.sh              levanta o actualiza la pila entera
#   ./deploy.sh estado       muestra qué está corriendo y si está sano
#   ./deploy.sh logs         sigue los registros de todos los servicios
#   ./deploy.sh parar        detiene la pila sin borrar los datos
#   ./deploy.sh copia        vuelca la base de datos a ./copias
#
# La primera vez crea el fichero .env, genera los secretos y pregunta el dominio.
# Las siguientes no toca nada de lo que ya hay: se puede ejecutar tantas veces
# como haga falta para actualizar la versión desplegada.

set -Eeuo pipefail

readonly RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly ENV_FILE="$RAIZ/.env"
readonly ENV_EJEMPLO="$RAIZ/.env.example"
readonly DIR_COPIAS="$RAIZ/copias"

# Margen para el primer arranque, que compila imágenes y espera a MySQL.
readonly ESPERA_MAXIMA=300

# --- salida ------------------------------------------------------------------

if [[ -t 1 ]]; then
  readonly C_OK=$'\e[32m' C_AVISO=$'\e[33m' C_ERR=$'\e[31m' C_TENUE=$'\e[2m' C_FIN=$'\e[0m'
else
  readonly C_OK='' C_AVISO='' C_ERR='' C_TENUE='' C_FIN=''
fi

paso()  { printf '\n%s==>%s %s\n' "$C_OK" "$C_FIN" "$1"; }
info()  { printf '    %s\n' "$1"; }
tenue() { printf '    %s%s%s\n' "$C_TENUE" "$1" "$C_FIN"; }
aviso() { printf '%s aviso %s %s\n' "$C_AVISO" "$C_FIN" "$1" >&2; }
error() { printf '%s error %s %s\n' "$C_ERR" "$C_FIN" "$1" >&2; }

morir() {
  error "$1"
  [[ $# -gt 1 ]] && printf '        %s\n' "${@:2}" >&2
  exit 1
}

# Una traza de bash no le dice nada a nadie; al menos que señale la línea.
trap 'error "El despliegue se ha interrumpido en la línea $LINENO."' ERR

# --- comprobaciones previas --------------------------------------------------

comprobar_requisitos() {
  paso "Comprobando requisitos"

  command -v docker >/dev/null 2>&1 || morir \
    "Docker no está instalado." \
    "En Debian o Ubuntu:  curl -fsSL https://get.docker.com | sh"

  docker compose version >/dev/null 2>&1 || morir \
    "Falta el plugin 'docker compose'." \
    "La versión antigua 'docker-compose' con guion no sirve aquí."

  docker info >/dev/null 2>&1 || morir \
    "Docker está instalado pero no responde." \
    "Arranca el servicio:  sudo systemctl start docker" \
    "Si da permiso denegado, añade tu usuario:  sudo usermod -aG docker \$USER" \
    "y vuelve a entrar en la sesión."

  command -v openssl >/dev/null 2>&1 || morir \
    "Falta openssl, que hace falta para generar los secretos."

  info "Docker $(docker version --format '{{.Server.Version}}' 2>/dev/null || echo '?') disponible"
}

# --- fichero .env ------------------------------------------------------------

# Un valor aleatorio apto para URL: sin barras ni signos que rompan el .env.
secreto() {
  openssl rand -base64 "${1:-48}" | tr -d '\n=+/' | cut -c1-"${2:-44}"
}

leer_valor() {
  [[ -f "$ENV_FILE" ]] || return 0
  sed -n "s/^$1=//p" "$ENV_FILE" | head -1
}

# Escribe la clave, exista ya o no. Se usa awk y no sed porque los valores llevan
# caracteres que sed interpretaría como parte de la expresión.
fijar_valor() {
  local clave="$1" valor="$2" tmp

  if grep -q "^$clave=" "$ENV_FILE" 2>/dev/null; then
    tmp="$(mktemp)"
    awk -v c="$clave" -v v="$valor" \
      '$0 ~ "^" c "=" { print c "=" v; next } { print }' "$ENV_FILE" > "$tmp"
    cat "$tmp" > "$ENV_FILE"
    rm -f "$tmp"
  else
    printf '%s=%s\n' "$clave" "$valor" >> "$ENV_FILE"
  fi
}

# Igual, pero sin pisar lo que ya tiene valor. El .env de un servidor en marcha
# puede tener secretos que no conviene regenerar.
fijar_si_falta() {
  local clave="$1" valor="$2"
  grep -q "^$clave=." "$ENV_FILE" 2>/dev/null && return 0
  fijar_valor "$clave" "$valor"
}

preguntar_dominio() {
  local dominio="${DOMAIN:-}"

  if [[ -z "$dominio" ]]; then
    if [[ ! -t 0 ]]; then
      morir "Falta el dominio y no hay terminal para preguntarlo." \
            "Ejecuta:  DOMAIN=tudominio.com ./deploy.sh"
    fi
    printf '\n'
    read -rp "    Dominio público (ej. nakamahub.com): " dominio
  fi

  dominio="${dominio#http://}"
  dominio="${dominio#https://}"
  dominio="${dominio%%/*}"

  [[ -n "$dominio" ]] || morir "El dominio no puede quedar vacío."
  printf '%s' "$dominio"
}

preparar_env() {
  paso "Preparando la configuración"

  local primera_vez=false
  if [[ ! -f "$ENV_FILE" ]]; then
    primera_vez=true
    [[ -f "$ENV_EJEMPLO" ]] || morir "No encuentro .env.example junto a este script."

    # Se parte del ejemplo vacío para no perder los comentarios que explican cada
    # variable, y a continuación se rellenan las que el despliegue necesita.
    cp "$ENV_EJEMPLO" "$ENV_FILE"
    info "Creado .env a partir de .env.example"
  fi

  chmod 600 "$ENV_FILE"

  local dominio
  dominio="$(leer_valor DOMAIN)"
  if [[ -z "$dominio" || "$dominio" == "nakamahub.example" ]]; then
    dominio="$(preguntar_dominio)"
    fijar_valor DOMAIN "$dominio"
    fijar_valor PUBLIC_ORIGIN "https://$dominio"
    info "Dominio fijado en $dominio"
  else
    info "Dominio ya configurado: $dominio"
  fi

  # El ejemplo trae el origen de desarrollo. Dejarlo puesto en producción haría
  # que el navegador rechazase todas las peticiones de la web al API por CORS,
  # con la web en blanco y un error que no dice de dónde viene.
  local cors
  cors="$(leer_valor CORS_ALLOWED_ORIGINS)"
  if [[ -z "$cors" || "$cors" == *localhost* ]]; then
    fijar_valor CORS_ALLOWED_ORIGINS "https://$dominio"
    info "CORS apuntando a https://$dominio"
  fi

  if [[ "$(leer_valor MAIL_FROM)" == *nakamahub.example* ]]; then
    fijar_valor MAIL_FROM "no-responder@$dominio"
  fi

  # Los secretos se generan una sola vez. Regenerar JWT_SECRET en cada despliegue
  # cerraría la sesión de todo el mundo, y cambiar la contraseña de MySQL dejaría
  # la API sin poder abrir su propia base de datos.
  fijar_si_falta DB_USERNAME nakamahub
  fijar_si_falta DB_PASSWORD "$(secreto 32 32)"
  fijar_si_falta DB_ROOT_PASSWORD "$(secreto 32 32)"
  fijar_si_falta JWT_SECRET "$(secreto 48 44)"
  fijar_si_falta JWT_EXPIRATION 3600000
  fijar_si_falta JWT_REFRESH_EXPIRATION 2592000000

  if [[ "$primera_vez" == true ]]; then
    info "Secretos generados y guardados en .env"
    tenue "No se muestran por pantalla. Haz una copia del fichero en sitio seguro."
  fi

  validar_env
}

validar_env() {
  local faltan=()
  local clave
  for clave in DOMAIN PUBLIC_ORIGIN DB_USERNAME DB_PASSWORD DB_ROOT_PASSWORD JWT_SECRET; do
    [[ -n "$(leer_valor "$clave")" ]] || faltan+=("$clave")
  done

  if [[ ${#faltan[@]} -gt 0 ]]; then
    morir "Faltan valores en .env: ${faltan[*]}" "Edítalo y vuelve a ejecutar el script."
  fi

  # La API se niega a arrancar con un secreto corto, y el error aparecería mucho
  # más tarde y mucho peor explicado que aquí.
  local secreto_jwt
  secreto_jwt="$(leer_valor JWT_SECRET)"
  if [[ ${#secreto_jwt} -lt 32 ]]; then
    morir "JWT_SECRET tiene ${#secreto_jwt} caracteres y hacen falta al menos 32." \
          "Genera uno nuevo con:  openssl rand -base64 48"
  fi

  if [[ -z "$(leer_valor SMTP_HOST)" ]]; then
    aviso "Sin SMTP configurado: los correos de verificación y de recuperación"
    aviso "de contraseña se escribirán en el log en lugar de enviarse."
  fi
}

comprobar_dns() {
  local dominio ip_dominio ip_servidor
  dominio="$(leer_valor DOMAIN)"

  command -v getent >/dev/null 2>&1 || return 0
  ip_dominio="$(getent ahostsv4 "$dominio" 2>/dev/null | awk 'NR==1 {print $1}')" || true

  if [[ -z "$ip_dominio" ]]; then
    aviso "$dominio todavía no resuelve. Caddy no podrá obtener el certificado"
    aviso "hasta que el registro A apunte a este servidor."
    return 0
  fi

  ip_servidor="$(curl -fsS --max-time 5 https://api.ipify.org 2>/dev/null || true)"
  if [[ -n "$ip_servidor" && "$ip_dominio" != "$ip_servidor" ]]; then
    aviso "$dominio resuelve a $ip_dominio pero este servidor es $ip_servidor."
    aviso "Revisa el registro A o el certificado fallará."
  fi
}

# --- despliegue --------------------------------------------------------------

compose() {
  docker compose --project-directory "$RAIZ" "$@"
}

levantar() {
  paso "Construyendo y levantando los servicios"
  tenue "La primera vez tarda varios minutos: compila el backend y el frontend."
  compose up -d --build --remove-orphans
}

esperar_salud() {
  paso "Esperando a que la API responda"

  local transcurrido=0 estado
  while [[ $transcurrido -lt $ESPERA_MAXIMA ]]; do
    estado="$(docker inspect --format '{{.State.Health.Status}}' \
      "$(compose ps -q backend 2>/dev/null)" 2>/dev/null || echo desconocido)"

    case "$estado" in
      healthy)
        info "La API está lista (${transcurrido}s)"
        return 0
        ;;
      unhealthy)
        error "La API ha arrancado pero no está sana."
        compose logs --tail 40 backend
        return 1
        ;;
    esac

    sleep 5
    transcurrido=$((transcurrido + 5))
    [[ $((transcurrido % 30)) -eq 0 ]] && tenue "${transcurrido}s…"
  done

  error "La API no ha llegado a estar lista en ${ESPERA_MAXIMA}s."
  compose logs --tail 40 backend
  return 1
}

resumen() {
  local dominio
  dominio="$(leer_valor DOMAIN)"

  paso "Listo"
  info "Comunidad      https://$dominio"
  info "API            https://$dominio/api"
  info "Mapa del sitio https://$dominio/sitemap.xml"
  printf '\n'
  tenue "Ver el estado      ./deploy.sh estado"
  tenue "Seguir los logs    ./deploy.sh logs"
  tenue "Copia de seguridad ./deploy.sh copia"
  printf '\n'
  if [[ -z "$(leer_valor SMTP_HOST)" ]]; then
    tenue "Para activar el correo, rellena SMTP_HOST y sus credenciales en .env"
    tenue "y vuelve a ejecutar ./deploy.sh"
  fi
}

desplegar() {
  comprobar_requisitos
  preparar_env
  comprobar_dns
  levantar
  esperar_salud
  resumen
}

# --- operaciones auxiliares --------------------------------------------------

estado() {
  compose ps
}

logs() {
  compose logs -f --tail 100
}

parar() {
  paso "Deteniendo los servicios"
  tenue "Los datos se conservan. Para arrancar de nuevo:  ./deploy.sh"
  compose down
}

copia() {
  [[ -f "$ENV_FILE" ]] || morir "No hay .env todavía. Ejecuta ./deploy.sh primero."

  mkdir -p "$DIR_COPIAS"
  local destino="$DIR_COPIAS/nakamahub-$(date +%Y%m%d-%H%M%S).sql.gz"

  paso "Volcando la base de datos"
  # --single-transaction evita bloquear las tablas mientras la comunidad funciona.
  compose exec -T db sh -c \
    'exec mysqldump --single-transaction --quick -u root -p"$MYSQL_ROOT_PASSWORD" nakamahub' \
    | gzip > "$destino"

  if [[ ! -s "$destino" ]]; then
    rm -f "$destino"
    morir "El volcado ha salido vacío. ¿Está la base de datos levantada?"
  fi

  info "Guardada en $destino ($(du -h "$destino" | cut -f1))"
  tenue "Cópiala fuera del servidor. Una copia que vive en la misma máquina"
  tenue "no protege de perder la máquina."
}

ayuda() {
  sed -n '3,14p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'
}

# --- entrada -----------------------------------------------------------------

case "${1:-desplegar}" in
  desplegar|deploy|'')   desplegar ;;
  estado|status|ps)      estado ;;
  logs|log)              logs ;;
  parar|stop|down)       parar ;;
  copia|backup)          copia ;;
  -h|--help|ayuda|help)  ayuda ;;
  *)
    error "No conozco la orden '$1'."
    printf '\n'
    ayuda
    exit 1
    ;;
esac
