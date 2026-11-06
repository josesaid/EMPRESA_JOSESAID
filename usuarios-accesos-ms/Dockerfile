# syntax=docker/dockerfile:1
# Build multietapa: Maven + Temurin 21 para compilar, Temurin 21 JRE para ejecutar.

# ---- Etapa 1: compilación ----
FROM maven:3.9-eclipse-temurin-21@sha256:99e61abcff91a9b1333463bd8451fb18495d6eba9250ac66a338b518f8278320 AS build
WORKDIR /app
# Primero las dependencias, para aprovechar la caché de capas
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package

# ---- Etapa 2: ejecución ----
FROM eclipse-temurin:21-jre@sha256:0b99b3376fbb50dbf9c0af8535fdd5b8eb0d3fdfc8b1e4967f383b6919f6901a
# curl solo sirve para el HEALTHCHECK; usuario sin privilegios
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/* \
 && useradd --system --uid 10001 --user-group app
WORKDIR /app
COPY --from=build /app/target/usuarios-accesos-ms-*.jar app.jar
USER app
EXPOSE 8081
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -fsS http://localhost:8081/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
