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
| `USUARIOS_CIFRADO_CLAVE` (+ `USUARIOS_CIFRADO_CLAVES`, `USUARIOS_CIFRADO_VERSION_ACTIVA`) | usuarios-accesos-ms | AES-256-GCM de correo y nombre | Con versiones: lista `N:base64,N:base64` y versión activa | **Versionada** (PR #90) y re-cifrado por lotes (PR #93). Probada en desarrollo, ver abajo |
| `USUARIOS_INDICE_CLAVE` | usuarios-accesos-ms | HMAC del índice de búsqueda por correo | No definida | **Pendiente de diseño**: rotarla obliga a recalcular el índice |
| `COMPRAS_CIFRADO_CLAVE` (+ `COMPRAS_CIFRADO_CLAVES`, `COMPRAS_CIFRADO_VERSION_ACTIVA`) | compras-proveedores-ms | AES-256-GCM de RFC y correo de proveedor | Con versiones, igual que usuarios | **Versionada** (PR #89) y re-cifrado por lotes (PR #92). Probada en desarrollo |
| `COMPRAS_INDICE_CLAVE` | compras-proveedores-ms | HMAC del índice (RFC) | No definida | **Pendiente de diseño** |
| `DOCUMENTOS_CIFRADO_CLAVE` | documentos-cumplimiento-ms | AES-256 de archivos | No definida | **Pendiente de diseño** |
| `CRYPTO_CLAVE_CIFRADO` (+ `CRYPTO_CLAVES_CIFRADO`, `CRYPTO_VERSION_ACTIVA`) / `CRYPTO_CLAVE_INDICE` | clientes-ms | AES-256-GCM de datos de cliente; índice HMAC sin cambios | Con versiones, igual que usuarios | **Versionada** (PR #88) y re-cifrado por lotes (PR #91). Probada en desarrollo. El índice HMAC sigue **pendiente** |
| `GATEWAY_TLS_PASSWORD` | gateway-ms, ui-ms, nexus-ms | Contraseña del keystore y truststore TLS | Regenerar con `generar-certificados.sh` y redistribuir | Procedimiento: [infra/tls](../../infra/tls/README.md) |
| `*_DB_PASSWORD`, `POSTGRES_PASSWORD` | PostgreSQL | Usuarios de cada base | Cambiar con `ALTER ROLE` y actualizar el `.env` | Sin automatizar |
| `EUREKA_PASSWORD` | discovery-ms | Registro de servicios | Cambiar en `discovery-ms` y en `.env` a la vez | Sin automatizar |
| `ANTHROPIC_API_KEY`, `VOYAGE_API_KEY` | nexus-ms | Proveedores de IA | En la consola del proveedor | Externo |
| `GRAFANA_ADMIN_PASSWORD` | monitoreo | Acceso a Grafana | Cambiar en Grafana y en `.env` | Sin automatizar |

## Rotación de claves de cifrado de campos personales

Los campos cifrados de clientes, compras y usuarios ya llevan prefijo de versión: `v<N>:Base64(IV || cifrado + tag)`. Los valores anteriores, sin prefijo, se leen como versión 1.

Para rotar: agregar la nueva clave a la lista `N:base64,N:base64`, cambiar la versión activa, y correr el re-cifrado por lotes (`*_RECIFRADO_ACTIVO=true`, que se apaga después). El diseño está en [diseno-version-de-clave.md](diseno-version-de-clave.md) y el procedimiento de prueba en [guia-prueba-rotacion-clave.md](guia-prueba-rotacion-clave.md).

**Dónde viven las claves:** `CRYPTO_*` en `clientes-ms/.env`; `USUARIOS_CIFRADO_*` en el `.env` de la raíz y `usuarios-accesos-ms/.env`; `COMPRAS_CIFRADO_*` en el `.env` de la raíz. Cuando cambies una clave, revisa los dos archivos.

**Resultado de la prueba en el entorno de desarrollo del responsable (2026-11-05):**
- Línea base antes del cambio: consulta de un cliente con `HTTP 200`.
- Versión 2 agregada sin activarla: la lectura no cambió.
- Versión activa 2: un contacto nuevo quedó con prefijo `v2:`.
- Re-cifrado: conteos por campo en clientes, compras y usuarios; después, `0` filas en versión anterior en los cinco campos verificados.
- Lectura después del re-cifrado: igual a la línea base.

Esto es una prueba en desarrollo, no en producción. Quedan pendientes el retiro de la versión 1 y la prueba de corte a mitad.

**Incidencia encontrada:** después de reconstruir contenedores, el gateway mantuvo conexiones a las direcciones anteriores y respondió `500` y `404` a rutas válidas. Reiniciar `gateway-ms` lo resolvió. Por eso, tras reconstruir o recrear un servicio, reinicia también el gateway.

## Índices HMAC

Rotar `*_INDICE_CLAVE` (`rfc_hash`, `correo_hash`, `rfc_indice`, `correo_indice`) cambia todos los valores de búsqueda. Sigue **pendiente**: requiere columnas nuevas y recálculo, según el diseño. Hasta entonces, esas claves no se rotan.

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

- [x] Diseñar el formato con versión de clave para los campos cifrados.
- [x] Implementar la versión de clave en clientes, compras y usuarios.
- [x] Implementar el re-cifrado por lotes en clientes, compras y usuarios.
- [x] Probar la rotación en un entorno de desarrollo.
- [x] Retirar la versión 1 de las listas `*_CIFRADO_CLAVES`, con la clave 1 guardada fuera de la máquina (2026-11-05).
- [x] Sustituir el valor de las variables de clave única (`CRYPTO_CLAVE_CIFRADO`, `COMPRAS_CIFRADO_CLAVE`, `USUARIOS_CIFRADO_CLAVE`) por la clave 2 (verificado el 2026-11-05). Hace falta porque `docker-compose.yml` exige esas variables para arrancar. Mientras la lista exista, se ignoran; si algún día se borra la lista, la variable quedaría como versión 1 con la clave 2, y los datos nuevos no se leerían.
- [ ] Probar el corte a mitad del re-cifrado.
- [ ] Diseñar el recálculo de índices HMAC.
- [ ] Rotar `DOCUMENTOS_CIFRADO_CLAVE` (archivos cifrados en disco; aún sin diseño).
- [ ] Automatizar la rotación de contraseñas de PostgreSQL.
- [ ] Hacer la primera prueba de restauración y registrar el resultado.
