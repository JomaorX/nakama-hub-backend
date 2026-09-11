# Compilación
FROM eclipse-temurin:25-jdk AS build
WORKDIR /build

# Primero solo el descriptor: así la capa de dependencias se reutiliza entre
# builds mientras no cambie el pom, que es lo que más tarda.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -B -q dependency:go-offline

COPY src/ src/
RUN ./mvnw -B -q package -DskipTests

# Ejecución
FROM eclipse-temurin:25-jre
WORKDIR /app

# curl lo usa la comprobación de salud del compose. Las imágenes jre no lo traen,
# y sin él el contenedor se quedaría para siempre en estado "starting".
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# Usuario sin privilegios: si alguien logra ejecutar algo dentro, que sea como nadie.
RUN useradd --system --uid 1001 nakama
USER nakama

COPY --from=build --chown=nakama:nakama /build/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=70", "-jar", "app.jar"]
