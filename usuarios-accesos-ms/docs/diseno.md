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
- Cifrado de columna AES-256-GCM y HMAC para búsquedas de correo.
- Idempotency-Key en escrituras.
- Argon2 para contraseñas (hoy BCrypt).

## Indicadores para reportes

- **Indicador:** `usuarios_activos` (área `ADMINISTRACION_LEGAL`). Usuarios activos: usuarios en estatus ACTIVO en este momento. Foto del mes actual.
- **Publicación:** clase `PublicadorIndicadoresUsuarios`, cada 60 minutos (solo mes actual), a `indicadores-areas`. Clave `ADMINISTRACION_LEGAL|usuarios_activos|AAAA-MM`.
- **Decisión:** foto del periodo, no evento. No usa el outbox; un envío fallido se corrige en el siguiente ciclo.
- **Prueba:** `PublicadorIndicadores*IT` con PostgreSQL y Kafka reales (Testcontainers).


## Rotación de la clave RS256

Los tokens se firman con una clave RS256 identificada por `kid` (`JWT_KID`, por defecto `empresa-usuarios-1`). `/.well-known/jwks.json` publica la clave actual y, durante una rotación, también la anterior (`JWT_CLAVE_PUBLICA_ANTERIOR` y `JWT_KID_ANTERIOR`). Los servicios validan por `kid`, así que la rotación no requiere cambiar código ni reiniciar los demás servicios.

**Procedimiento:** el paso a paso está en el README de usuarios-accesos-ms, sección «Rotación de la clave RS256». Resumen: publicar la clave anterior junto con la nueva (dos reinicios de usuarios-accesos-ms), esperar `JWT_DURACION` (15 minutos) y quitar la anterior.

**Efecto:** durante el traslape se validan tokens anteriores y nuevos, así que las sesiones no se cortan. Los refresh tokens son opacos y no cambian con la rotación.

**Si la clave se expone:** rotar y quitar la anterior de inmediato, sin esperar a que expiren los tokens.
## Área por usuario y rol JEFE_AREA

Cada usuario puede tener un área (`area`, texto de hasta 40 caracteres, por ejemplo `VENTAS_ATENCION_CLIENTES`). El área se asigna con `PUT /v1/usuarios/{id}/area`, body `{"area": "...", "motivo": "..."}`, solo con rol ADMIN. El motivo es obligatorio.

- El rol `JEFE_AREA` exige área. No se puede asignar sin ella (422), y no se puede crear un usuario con ese rol directamente: primero se crea, luego se asigna el área y después el rol.
- El token incluye el claim `area` cuando el usuario tiene una. reportes-ejecutivos-ms lo usa para limitar la consulta de indicadores.
- Un usuario que ya tenía sesión abierta antes de este cambio no trae el claim hasta volver a iniciar sesión.
- El valor de `area` debe coincidir con los nombres del enum `Area` de reportes-ejecutivos-ms. Este servicio no lo valida; si no coincide, reportes responde 403.
- Migración `V2__area_jefe.sql`: agrega la columna `area` y amplía la restricción de roles con `JEFE_AREA`.
- `GET /v1/usuarios/{id}` devuelve el área del usuario.
- El valor de `area` se valida contra la lista de áreas de reportes; un valor desconocido responde 422.
- Cada cambio de área publica el evento `area-asignada` (área anterior, área nueva y motivo), igual que el cambio de roles.

## Sesiones: refresh token y cierre de sesión

El login devuelve `accessToken` (15 minutos) y `refreshToken` (opaco, 7 días desde su último uso).

- `POST /v1/auth/refresh` con `{"refreshToken": "..."}` devuelve un access token y un refresh token nuevos. El anterior queda invalidado.
- Reusar un refresh token ya rotado revoca toda la sesión: el token nuevo también deja de servir. Es la señal de que el token se copió.
- `POST /v1/auth/logout` con `{"refreshToken": "..."}` cierra esa sesión. Es público porque basta con tener el token. Responde 204.
- `POST /v1/auth/logout-todas` requiere access token y cierra todas las sesiones del usuario. Responde 204.
- `POST /v1/auth/cambiar-password-inicial` con `{"correo", "passwordActual", "passwordNueva"}` cambia la contraseña temporal por una definitiva (mínimo 12 caracteres). Es público y responde 204. Mientras la contraseña sea temporal, el login responde 403 `cambio-password-requerido`.
- Solo se guarda el hash SHA-256 del refresh token, en la tabla `refresh_tokens` (migración `V3__refresh_tokens.sql`).
- Bloqueo o baja: el refresh falla porque se vuelve a verificar la cuenta. El access token ya emitido sigue válido hasta 15 minutos.
- Al cerrar sesión, el access token de esa sesión también se revoca en todos los servicios. Ver «Revocación de access tokens» más abajo.
- Revocación de access tokens: diseñada con eventos de logout y `sid` en el token. Diagramas y límites en `docs/diagramas.md`.
