# Diseño: usuarios-accesos-ms

## Propósito
Centraliza autenticación, roles y permisos para todo el sistema. Emite los tokens JWT que consumen el resto de los servicios.

## Enfoque DDD
El código se organiza en capas, y la regla de negocio no depende de la infraestructura:

- **domain/**: agregado `Usuario`, enumeraciones (`Rol`, `EstatusUsuario`), excepciones de negocio y el puerto `UsuarioRepositorio`. No usa Spring, JPA ni Kafka.
- **application/**: casos de uso (`UsuarioService`) y el puerto `PublicadorEventos`. Coordina transacciones y eventos.
- **infrastructure/**: adaptadores. JPA (`UsuarioRepositorioJpa`), outbox y publicación a Kafka, y emisión de JWT.
- **api/**: controladores REST, DTOs, manejo de errores RFC 9457 y enmascarado de datos personales.
- **config/**: seguridad, reloj en UTC y propiedades.

Se separa el modelo de dominio (`Usuario`) del modelo de persistencia (`UsuarioEntity`) para que las reglas de bloqueo y baja no dependan del ORM.

## Reglas de negocio clave
- Cinco contraseñas incorrectas bloquean la cuenta 15 minutos (propuesta, por validar).
- La baja es definitiva: no se reactiva; se crea un usuario nuevo.
- Todo cambio de roles exige motivo y genera el evento `rol-asignado`.
- Credenciales incorrectas, usuario inexistente o dado de baja devuelven el mismo mensaje, para no revelar qué usuarios existen.

## Endpoints
| Método | Ruta | Rol | Éxito | Errores |
|---|---|---|---|---|
| POST | /v1/auth/login | Público | 200 | 400, 401, 423 |
| POST | /v1/usuarios | ADMIN | 201 | 400, 409, 422 |
| GET | /v1/usuarios/{id} | ADMIN | 200 | 404 |
| PUT | /v1/usuarios/{id}/roles | ADMIN | 200 | 400, 404, 422 |
| POST | /v1/usuarios/{id}/baja | ADMIN | 200 | 404, 422 |

Ejemplo de petición de login:
```json
{ "correo": "admin@empresa.mx", "password": "clave-segura" }
```
Ejemplo de respuesta:
```json
{ "accessToken": "eyJhbGciOi...", "tokenType": "Bearer", "expiresIn": 900 }
```

## Eventos (patrón outbox)
| Evento | Disparador | Datos |
|---|---|---|
| usuario-bloqueado | Quinto intento fallido | usuarioId, intentosFallidos |
| rol-asignado | Cambio de roles | usuarioId, rolesAnteriores, rolesNuevos, motivo |

El evento se guarda en `outbox_eventos` en la misma transacción que el cambio. Un proceso programado lo envía a Kafka en el tópico `usuarios-accesos.eventos`, con la clave del usuario. Si Kafka no responde, el evento queda pendiente y se reintenta.

## Observabilidad
- Health: `/actuator/health`. Métricas: `/actuator/prometheus`.
- Logs JSON en formato ECS.
- Nunca se registran correos, nombres, contraseñas ni tokens.

## Pruebas
- Unitarias: reglas del dominio y casos de uso (17 pruebas).
- Integración: `UsuariosAccesosIT` con PostgreSQL y Kafka reales (Testcontainers). Requiere Docker.

## Despliegue y rollback
Canary con criterio del anexo de la empresa. Rollback por contenedor. Las migraciones de Flyway son compatibles hacia atrás.

## Pendientes en esta versión
- Refresh de token y cierre de sesión (`/v1/auth/refresh`, `/v1/auth/logout`).
- Cifrado de columna AES-256-GCM y HMAC para búsquedas de correo.
- Idempotency-Key en escrituras.
- Argon2 para contraseñas (hoy BCrypt).
