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

## Indicadores para reportes

- **Indicador:** `usuarios_activos` (área `ADMINISTRACION_LEGAL`). Usuarios activos: usuarios en estatus ACTIVO en este momento. Foto del mes actual.
- **Publicación:** clase `PublicadorIndicadoresUsuarios`, cada 60 minutos (solo mes actual), a `indicadores-areas`. Clave `ADMINISTRACION_LEGAL|usuarios_activos|AAAA-MM`.
- **Decisión:** foto del periodo, no evento. No usa el outbox; un envío fallido se corrige en el siguiente ciclo.
- **Prueba:** `PublicadorIndicadores*IT` con PostgreSQL y Kafka reales (Testcontainers).


## Rotación de la clave RS256

usuarios-accesos-ms firma los tokens con una sola clave RS256. Su identificador (`kid`) es `empresa-usuarios-1`. La clave pública se publica en `/.well-known/jwks.json` y los servicios la cachean.

**Procedimiento (corte en ventana de baja actividad):**

1. Generar la nueva clave privada en PKCS#8 DER en Base64:
   `openssl genrsa 2048 | openssl pkcs8 -topk8 -nocrypt -outform DER | base64`
2. Guardarla en el gestor de secretos de producción, nunca en el repositorio.
3. Actualizar `USUARIOS_JWT_CLAVE_PRIVADA` en el `.env` de la raíz (se entrega al contenedor como `JWT_CLAVE_PRIVADA`). Cambiar también la constante `KID` en `ClavesJwt.java` (valor actual `empresa-usuarios-1`) a un valor nuevo; el `kid` es código, no configuración, así que requiere recompilar y desplegar.
4. Desplegar usuarios-accesos-ms. El JWKS publica solo la clave nueva.
5. Los servicios que validan tokens obtienen la nueva clave cuando su proveedor de JWKS la recarga. Revisar en la práctica cuánto tarda; no está medido.

**Efecto:** los tokens emitidos con la clave anterior dejan de validarse en cuanto los servicios refrescan el JWKS. Todas las sesiones deben iniciar sesión de nuevo; el costo es como máximo los 15 minutos de vida del token.

**Limitación:** no hay periodo de traslape. Para rotar sin cortar sesiones, el JWKS debe publicar ambas claves durante al menos la duración del token y firmar solo con la nueva. Eso requiere cambiar `ClavesJwt` y `SeguridadConfig` para aceptar varias claves; hoy no está implementado.

**Calendario:** por definir. Se recomienda rotar al menos una vez al año y de inmediato si la clave se expone.

## Área por usuario y rol JEFE_AREA

Cada usuario puede tener un área (`area`, texto de hasta 40 caracteres, por ejemplo `VENTAS_ATENCION_CLIENTES`). El área se asigna con `PUT /v1/usuarios/{id}/area`, body `{"area": "...", "motivo": "..."}`, solo con rol ADMIN. El motivo es obligatorio.

- El rol `JEFE_AREA` exige área. No se puede asignar sin ella (422), y no se puede crear un usuario con ese rol directamente: primero se crea, luego se asigna el área y después el rol.
- El token incluye el claim `area` cuando el usuario tiene una. reportes-ejecutivos-ms lo usa para limitar la consulta de indicadores.
- Un usuario que ya tenía sesión abierta antes de este cambio no trae el claim hasta volver a iniciar sesión.
- El valor de `area` debe coincidir con los nombres del enum `Area` de reportes-ejecutivos-ms. Este servicio no lo valida; si no coincide, reportes responde 403.
- Migración `V2__area_jefe.sql`: agrega la columna `area` y amplía la restricción de roles con `JEFE_AREA`.

## Sesiones: refresh token y cierre de sesión

El login devuelve `accessToken` (15 minutos) y `refreshToken` (opaco, 7 días desde su último uso).

- `POST /v1/auth/refresh` con `{"refreshToken": "..."}` devuelve un access token y un refresh token nuevos. El anterior queda invalidado.
- Reusar un refresh token ya rotado revoca toda la sesión: el token nuevo también deja de servir. Es la señal de que el token se copió.
- `POST /v1/auth/logout` con `{"refreshToken": "..."}` cierra esa sesión. Es público porque basta con tener el token. Responde 204.
- `POST /v1/auth/logout-todas` requiere access token y cierra todas las sesiones del usuario. Responde 204.
- Solo se guarda el hash SHA-256 del refresh token, en la tabla `refresh_tokens` (migración `V3__refresh_tokens.sql`).
- Bloqueo o baja: el refresh falla porque se vuelve a verificar la cuenta. El access token ya emitido sigue válido hasta 15 minutos.
- Al cerrar sesión, el access token que ya existe no se revoca: sigue válido hasta vencer. Si eso no es aceptable, falta una lista de revocación de access tokens.
