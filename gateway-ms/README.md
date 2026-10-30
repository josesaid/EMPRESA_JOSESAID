# gateway-ms

## 1. Identidad

- **Nombre:** gateway-ms
- **Puerto en tu Mac:** 8080
- **Versión:** 0.1.0 · Java 21 · Spring Boot 4.1.2 · Spring Cloud 2025.1.2

## 2. Propósito y alcance

Punto de entrada único de EMPRESA_JOSESAID. Las rutas se generan desde Eureka: cada servicio registrado queda en `/{nombre}/**` y se reenvía sin tocar el código de los servicios.

## 3. Estructura

```
gateway-ms/
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

Requiere `EUREKA_USUARIO` y `EUREKA_PASSWORD` en el `.env` de la raíz. El Compose arma `EUREKA_URL` con esas credenciales.

## 6. Ejecución

Desde la raíz: `docker compose up -d --build` o, dentro de `gateway-ms/`, `docker compose up -d --build`.
Salud: `curl -s http://localhost:8080/actuator/health`.

## 7. Modelo de datos

No aplica. Este servicio no guarda datos de negocio.

## 8. Migraciones

No aplica.

## 9. Reglas de estado

No aplica.

## 10. Seguridad

El gateway no valida tokens (lo hace cada servicio). Bloquea hacia afuera `/actuator/**` y `/discovery-ms/**` con 404, para que el panel de Eureka y las métricas no sean accesibles desde la entrada única. Prometheus y Eureka se comunican dentro de `empresa-net`.

## 11. API

| Ruta de entrada | Destino |
|---|---|
| `/{nombre-del-servicio}/**` | Servicio registrado en `discovery-ms`, con el prefijo retirado. Ejemplo: `/usuarios-accesos-ms/v1/auth/login` llega a `/v1/auth/login`. |
| `/actuator/health` | Salud del gateway |

## 12. Reglas de duplicados

No aplica.

## 13. Eventos

No aplica.

## 14. Outbox

No aplica.

## 15. Observabilidad

Salud y métricas Prometheus en `/actuator/health` y `/actuator/prometheus` dentro de la red interna. Desde `localhost:8080`, `/actuator/**` responde 404 por diseño.

## 16. Pruebas

```bash
mvn -B test   # prueba de arranque del contexto (sin Eureka)
```
Rutas públicas (sin token): `/*/v1/auth/login`, `/*/v1/auth/refresh`, `/*/v1/auth/logout` y `/*/v1/auth/cambiar-password-inicial`. `/*/v1/auth/logout-todas` exige token. Todo `/actuator` responde 404.

La prueba con servicios reales se hace desde la raíz: `curl` a `http://localhost:8080/usuarios-accesos-ms/v1/auth/login`.

## 17. Despliegue

Imagen `gateway-ms:0.1.0`. Se levanta antes que los servicios de área.

## 18. Rollback

Detenga el contenedor y regrese a la imagen anterior. No hay datos que revertir.

## 19. Solución de problemas

| Síntoma | Causa probable | Acción |
|---|---|---|
| 404 en `/{servicio}/...` | El servicio no está registrado | Verifique en http://localhost:8761 que aparezca (con usuario y contraseña de Eureka). |
| 404 en `/actuator/...` o `/discovery-ms/...` | Ruta bloqueada a propósito | Use la red interna o el panel de Eureka en `localhost:8761`. |
| 503 | Ninguna instancia disponible | Levante el servicio y espere su registro. |
| 401 | Token inválido o ausente | El gateway lo reenvía; revise el token con el servicio de usuarios. |

## 20. Decisiones y pendientes

- **Sin validación de JWT en el gateway:** cada servicio valida su token. Centralizarlo es una decisión pendiente.
- **Sin límites de tasa ni CORS:** pendientes.
- **Puerto 8080 en tu Mac:** si lo ocupa otra aplicación, cambie el mapeo del compose.

## 21. Referencias

- Diseño: `docs/diseno.md`.
- Orden de arranque: sección 10 del README de la raíz.
- Gateway y monitoreo: sección 11 del README de la raíz.
