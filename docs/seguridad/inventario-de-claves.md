# Inventario de claves y procedimientos de rotación y respaldo

Revisión al 2026-11-05. Todas las claves viven en el `.env` de la raíz (o en el de cada servicio). **Ninguna se versiona en Git.** El `.env` debe guardarse en el gestor de secretos de la empresa, nunca en un archivo compartido.

Para verificar un `.env` antes de levantar el ecosistema:

```bash
bash infra/claves/verificar-claves.sh            # revisa .env de la raíz
bash infra/claves/prueba-verificar-claves.sh     # prueba del propio verificador
```

## Inventario

| Clave | Servicio | Uso | Rotación | Estado |
|---|---|---|---|---|
| `USUARIOS_JWT_CLAVE_PRIVADA` (+ `KID`) | usuarios-accesos-ms | Firma RS256 de los tokens | Sin cortar sesiones, dos reinicios | **Procedimiento listo**: [guía 3](../guias/03-rotar-clave-rs256.md) |
| `USUARIOS_CIFRADO_CLAVE` | usuarios-accesos-ms | AES-256-GCM de correo y nombre | No definida | **Pendiente de diseño**: el cifrado no lleva versión de clave |
| `USUARIOS_INDICE_CLAVE` | usuarios-accesos-ms | HMAC del índice de búsqueda por correo | No definida | **Pendiente de diseño**: rotarla obliga a recalcular el índice |
| `COMPRAS_CIFRADO_CLAVE` | compras-proveedores-ms | AES-256-GCM de datos de proveedor | No definida | **Pendiente de diseño** (ver abajo) |
| `COMPRAS_INDICE_CLAVE` | compras-proveedores-ms | HMAC del índice (RFC) | No definida | **Pendiente de diseño** |
| `DOCUMENTOS_CIFRADO_CLAVE` | documentos-cumplimiento-ms | AES-256 de archivos | No definida | **Pendiente de diseño** |
| `CRYPTO_CLAVE_CIFRADO` / `CRYPTO_CLAVE_INDICE` | clientes-ms | AES-256-GCM e índice HMAC de datos de cliente | No definida | **Pendiente de diseño** |
| `GATEWAY_TLS_PASSWORD` | gateway-ms, ui-ms, nexus-ms | Contraseña del keystore y truststore TLS | Regenerar con `generar-certificados.sh` y redistribuir | Procedimiento: [infra/tls](../../infra/tls/README.md) |
| `*_DB_PASSWORD`, `POSTGRES_PASSWORD` | PostgreSQL | Usuarios de cada base | Cambiar con `ALTER ROLE` y actualizar el `.env` | Sin automatizar |
| `EUREKA_PASSWORD` | discovery-ms | Registro de servicios | Cambiar en `discovery-ms` y en `.env` a la vez | Sin automatizar |
| `ANTHROPIC_API_KEY`, `VOYAGE_API_KEY` | nexus-ms | Proveedores de IA | En la consola del proveedor | Externo |
| `GRAFANA_ADMIN_PASSWORD` | monitoreo | Acceso a Grafana | Cambiar en Grafana y en `.env` | Sin automatizar |

## Por qué las claves de cifrado no se pueden rotar todavía

El formato de la columna cifrada es `Base64(IV de 12 bytes || texto cifrado + tag)`. **No incluye un identificador de la clave**, así que el servicio no sabe con cuál descifrar un valor. Eso impide convivir dos claves durante la transición, que es lo que hace posible la rotación sin cortes.

Para rotarlas hace falta primero un diseño:

1. Agregar un identificador de clave al formato (por ejemplo, un prefijo `v2:`), manteniendo la lectura de los valores antiguos.
2. Un proceso de re-cifrado por lotes que lea con la clave anterior y escriba con la nueva.
3. Para las claves HMAC de índice, un proceso de recálculo de los índices, con el mismo esquema de versiones.

Hasta que eso exista, **rotar `*_CIFRADO_CLAVE` o `*_INDICE_CLAVE` dejaría sin acceso a los datos cifrados.** Esa es la razón de que no haya procedimiento.

## Respaldo

Un respaldo completo tiene **dos partes que deben guardarse por separado**:

1. **Base de datos:** volcado de cada base con `pg_dump` (o el respaldo de la infraestructura). Contiene los datos cifrados, pero sin las claves no se leen.
2. **Claves:** el contenido del `.env` (o las claves de la tabla) en el gestor de secretos. Sin ellas, la base es ilegible para los campos cifrados.

No guardes el volcado y las claves en el mismo lugar ni en el mismo medio.

## Prueba de restauración

Hazla al menos una vez al trimestre, en un entorno separado:

1. Restaura el volcado de PostgreSQL en una instancia nueva.
2. Restaura el `.env` desde el gestor de secretos.
3. Ejecuta `bash infra/claves/verificar-claves.sh` sobre ese `.env`. Debe terminar con 0 fallos.
4. Levanta `usuarios-accesos-ms` y `clientes-ms` y consulta un usuario y un cliente conocidos. Si el correo se muestra en claro, las claves y la base corresponden.
5. Registra la fecha, quién la hizo y el resultado.

## Pendiente

- [ ] Diseñar el formato con versión de clave para los campos cifrados (requisito de cualquier rotación de `*_CIFRADO_CLAVE`).
- [ ] Diseñar el recálculo de índices HMAC.
- [ ] Automatizar la rotación de contraseñas de PostgreSQL.
- [ ] Hacer la primera prueba de restauración y registrar el resultado.
