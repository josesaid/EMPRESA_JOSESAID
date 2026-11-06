# Etapa 1: compilación con Maven y JDK 21. El contexto de build es la raíz del repo.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY ui-ms/pom.xml ui-ms/pom.xml
RUN mvn -B -q -f ui-ms/pom.xml dependency:go-offline
COPY ui-ms/src ui-ms/src
RUN mvn -B -q -f ui-ms/pom.xml package -DskipTests

# Etapa 2: ejecución con JRE 21 y usuario no root
FROM eclipse-temurin:21-jre@sha256:0b99b3376fbb50dbf9c0af8535fdd5b8eb0d3fdfc8b1e4967f383b6919f6901a
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/* \
 && groupadd --system app \
 && useradd --system --gid app --home /app app
WORKDIR /app
COPY --from=build /build/ui-ms/target/ui-ms-0.1.0.jar app.jar
USER app
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -fsS http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
