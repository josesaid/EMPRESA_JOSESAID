# Usuarios y accesos

**Área:** Administración y legal
**Servicio propuesto:** usuarios-accesos-ms

## Rol
Eres el microservicio central de identidad. Autenticas usuarios, administras roles y permisos, y emites los tokens que usan todos los demás servicios.

## Contexto
**Incluye:** usuarios internos, roles, permisos, emisión de tokens OAuth2/JWT, cierre de sesión y auditoría de accesos.
**Excluye:** la lógica de negocio de cada área y el acceso de clientes externos (pendiente).

## Tarea
Autenticar usuarios, emitir y revocar tokens, asignar roles y permisos, y registrar intentos de acceso.

## Restricciones
- Las contraseñas se guardan con hash fuerte (Argon2 o bcrypt, propuesto).
- Los tokens tienen vida corta; el refresh se revoca al cerrar sesión.
- Sin datos sensibles en logs.

## Formato de salida
JSON. Tokens JWT firmados.

## Escenarios de entrada
1. Inicio de sesión con usuario y contraseña → token.
2. Renovación de token.
3. Asignación de rol a usuario.
4. Revocación de acceso de un usuario.

## Reglas de negocio (propuestas)
- Tras 5 intentos fallidos, la cuenta se bloquea temporalmente (propuesta).
- Un usuario dado de baja no se reactiva; se crea uno nuevo.
- Todo cambio de rol requiere motivo y queda auditado.

## Catálogos
- Roles base: ADMIN, FINANZAS, CONTADOR, VENTAS, COMPRAS, ALMACEN, OPERACIONES, CALIDAD, DIRECCION, LEGAL.
- Permisos por recurso y acción.

## Entidades
- **Usuario:** id (UUID), correo (cifrado), nombre (cifrado), estatus (ACTIVO, BLOQUEADO, BAJA), version.
- **Rol:** id (UUID), nombre, descripcion.
- **Permiso:** id (UUID), recurso, accion.
- **AuditoriaAcceso:** id (UUID), usuario_id, evento, ip, creado_utc (Instant).

## Endpoints
| Método | Ruta | Rol | Éxito | Errores |
|---|---|---|---|---|
| POST | /auth/login | Público | 200 | 400, 401, 423 |
| POST | /auth/refresh | Autenticado | 200 | 401 |
| POST | /auth/logout | Autenticado | 204 | 401 |
| POST | /usuarios | ADMIN | 201 | 400, 409 |
| PUT | /usuarios/{id}/roles | ADMIN | 200 | 404, 422 |
| POST | /usuarios/{id}/baja | ADMIN | 200 | 404, 409 |

## Eventos
| Evento | Disparador | Payload |
|---|---|---|
| usuario-bloqueado | Bloqueo por intentos | usuario_id |
| rol-asignado | Cambio de rol | usuario_id, rol, motivo |

## Requisitos funcionales
- Autenticación con OAuth2/JWT.
- Gestión de roles y permisos.
- Auditoría de accesos.

## Requisitos no funcionales
- Disponibilidad 99.9% (propuesta), por ser dependencia de todos los servicios.

## Seguridad y acceso
- Este servicio es la fuente de tokens para el resto del sistema.
- Claves de firma rotables sin reiniciar el servicio.

## Observabilidad
- Métricas: logins_exitosos_total, logins_fallidos_total, errores 5xx.
- Logs sin correos ni nombres.

## Criterios de aceptación
- Un login con contraseña incorrecta devuelve 401.
- El quinto intento fallido devuelve 423.

## Pruebas
- Unitarias: bloqueo, hash, roles.
- Integración: PostgreSQL y Kafka con Testcontainers.
- Contrato: endpoints y tokens.

## Despliegue y rollback
Este servicio se despliega primero. Canary con criterio del anexo. Rollback por contenedor.

## Pendientes por integración futura
- Inicio de sesión federado (SSO corporativo).
- Autenticación de dos factores.

## Comentarios
- Este servicio debe generarse antes que los demás porque emite los tokens.

## Roles en el área
- **ADMIN:** gestiona usuarios y roles.
- **LEGAL:** consulta auditoría.
