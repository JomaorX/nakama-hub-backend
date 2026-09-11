package com.nakamahub.backend.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limita los intentos fallidos por clave dentro de una ventana de tiempo.
 *
 * Sin esto, el login está abierto a fuerza bruta: con contraseñas de seis
 * caracteres y sin límite, probar millones de combinaciones es cuestión de horas.
 *
 * El recuento vive en memoria. Es suficiente mientras haya una sola instancia,
 * que es el caso ahora; el día que haya varias detrás de un balanceador cada una
 * llevaría su propia cuenta y habría que mover esto a Redis.
 */
@Component
public class AttemptLimiter {

    private static final Logger log = LoggerFactory.getLogger(AttemptLimiter.class);

    /**
     * Tope de claves distintas en memoria. Sin él, un atacante que varíe el usuario
     * en cada intento haría crecer el mapa sin límite hasta agotar la memoria, que
     * es cambiar un problema por otro peor.
     */
    private static final int MAX_TRACKED_KEYS = 50_000;

    private final Map<String, Deque<Instant>> failures = new ConcurrentHashMap<>();

    /**
     * Comprueba si la clave puede seguir intentándolo.
     *
     * @param key      identificador del cubo, por ejemplo "login:ip:1.2.3.4"
     * @param maxTries intentos fallidos permitidos dentro de la ventana
     * @param window   duración de la ventana
     */
    public void check(String key, int maxTries, Duration window) {
        Deque<Instant> attempts = failures.get(key);
        if (attempts == null) {
            return;
        }

        Instant now = Instant.now();
        Instant oldestAllowed = now.minus(window);

        synchronized (attempts) {
            purge(attempts, oldestAllowed);

            if (attempts.size() < maxTries) {
                return;
            }

            Instant retryAt = attempts.peekFirst().plus(window);
            throw new TooManyAttemptsException(
                    Duration.between(now, retryAt),
                    "Demasiados intentos fallidos. Espera un poco antes de volver a probar.");
        }
    }

    public void recordFailure(String key, Duration window) {
        if (failures.size() >= MAX_TRACKED_KEYS && !failures.containsKey(key)) {
            log.warn("Límite de claves vigiladas alcanzado, se descarta el recuento de {}", key);
            return;
        }

        Deque<Instant> attempts = failures.computeIfAbsent(key, ignored -> new ArrayDeque<>());

        synchronized (attempts) {
            purge(attempts, Instant.now().minus(window));
            attempts.addLast(Instant.now());
        }
    }

    /** Un acierto limpia el historial: quien conoce su contraseña no es un atacante. */
    public void reset(String key) {
        failures.remove(key);
    }

    private void purge(Deque<Instant> attempts, Instant oldestAllowed) {
        while (!attempts.isEmpty() && attempts.peekFirst().isBefore(oldestAllowed)) {
            attempts.removeFirst();
        }
    }
}
