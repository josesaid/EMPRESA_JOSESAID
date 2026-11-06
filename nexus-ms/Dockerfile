# Etapa 1: compilación con Maven y JDK 21. El contexto de build es la raíz del repo,
# porque nexus-comun es una dependencia local que no está en Maven Central.
FROM maven:3.9-eclipse-temurin-21@sha256:99e61abcff91a9b1333463bd8451fb18495d6eba9250ac66a338b518f8278320 AS build
WORKDIR /build

# nexus-comun se instala en el repositorio Maven de la imagen antes de compilar nexus-ms
COPY nexus-comun/pom.xml nexus-comun/pom.xml
COPY nexus-comun/src nexus-comun/src
RUN mvn -B -q -f nexus-comun/pom.xml install -DskipTests

COPY nexus-ms/pom.xml nexus-ms/pom.xml
RUN mvn -B -q -f nexus-ms/pom.xml dependency:go-offline
COPY nexus-ms/src nexus-ms/src
RUN mvn -B -q -f nexus-ms/pom.xml package -DskipTests

# Etapa 2: ejecución con JRE 21 y usuario no root
FROM eclipse-temurin:21-jre@sha256:0b99b3376fbb50dbf9c0af8535fdd5b8eb0d3fdfc8b1e4967f383b6919f6901a
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/* \
 && groupadd --system app \
 && useradd --system --gid app --home /app app
WORKDIR /app
COPY --from=build /build/nexus-ms/target/nexus-ms-0.1.0.jar app.jar
USER app
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -fsS http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
