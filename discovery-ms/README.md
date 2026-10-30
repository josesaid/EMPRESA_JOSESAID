# discovery-ms

## 1. Identidad

- **Nombre:** discovery-ms
- **Puerto en tu Mac:** 8761
- **Versión:** 0.1.0 · Java 21 · Spring Boot 4.1.2 · Spring Cloud 2025.1.2

## 2. Propósito y alcance

Registro y descubrimiento de servicios con Eureka. Cada servicio se anuncia al arrancar y el gateway consulta este registro para enrutar.

## 3. Estructura

```
discovery-ms/
├── Dockerfile
├── docker-compose.yml
├── pom.xml
├── docs/diseno.md
└── src/
    ├── main/java/   aplicación
    ├── main/resources/application.yml
    └── test/java/   prueba de arranque
```

## 4. Requisitos

- Docker Desktop en ejecución.
- Red `empresa-net` creada (`docker network create empresa-net`).
- JDK 21 y Maven 3.9+ solo si corre fuera de Docker.

## 5. Configuración

Sin credenciales. Variables opcionales en el compose o en el entorno; ver `application.yml`.

## 6. Ejecución

Desde la raíz: `docker compose up -d --build` o, dentro de `discovery-ms/`, `docker compose up -d --build`.
Salud: `curl -s http://localhost:8761/actuator/health`.

## 7. Modelo de datos

No aplica. Este servicio no guarda datos de negocio.

## 8. Migraciones

No aplica.

## 9. Reglas de estado

No aplica.

## 10. Seguridad

Sin autenticación de usuarios. Solo `/actuator/health` y `/actuator/prometheus` son públicos dentro de la red.

## 11. API

| Método | Ruta | Descripción |
|---|---|---|
| GET | / | Panel de Eureka con las instancias registradas |
| GET | /eureka/apps | Instancias registradas (XML/JSON) |
| GET | /actuator/health | Salud del servicio |

## 12. Reglas de duplicados

No aplica.

## 13. Eventos

No aplica.

## 14. Outbox

No aplica.

## 15. Observabilidad

Salud y métricas Prometheus en `/actuator/health` y `/actuator/prometheus`.

## 16. Pruebas

```bash
mvn -B test   # prueba de arranque del contexto
```
La integración completa se verifica con los servicios desde la raíz.

## 17. Despliegue

Imagen `discovery-ms:0.1.0`. Se levanta antes que los servicios de área.

## 18. Rollback

Detenga el contenedor y regrese a la imagen anterior. No hay datos que revertir.

## 19. Solución de problemas

| Síntoma | Causa probable | Acción |
|---|---|---|
| Un servicio no aparece en el panel | El servicio arrancó antes que el registro, o no encuentra `discovery-ms` | Levante `discovery-ms` y espere un minuto. Revise `EUREKA_URL` del servicio. |
| Panel no abre en 8761 | Puerto ocupado en tu Mac | Libere el 8761 o cambie el mapeo en el compose. |

## 20. Decisiones y pendientes

- **Autoprotección desactivada:** en desarrollo, los servicios caídos desaparecen del registro. Evaluar en producción.
- **Panel sin autenticación:** pendiente de decisión si se expone fuera de la red local.
- **Registro en memoria:** si se reinicia, los servicios se vuelven a registrar en su siguiente latido.

## 21. Referencias

- Diseño: `docs/diseno.md`.
- Orden de arranque: sección 10 del README de la raíz.
- Gateway y monitoreo: sección 11 del README de la raíz.
