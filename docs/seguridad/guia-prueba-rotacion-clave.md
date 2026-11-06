# Guía de prueba: rotar la clave de cifrado sin cortar el servicio

**Objetivo:** comprobar en tu entorno (Mac con Docker Desktop) que se puede pasar de la clave de cifrado actual a una nueva, re-cifrar los datos y volver atrás si algo falla, sin perder información.

**Alcance:** clientes-ms, compras-proveedores-ms y usuarios-accesos-ms. No cubre los índices HMAC (`rfc_hash`, `correo_hash`, `rfc_indice`, `correo_indice`): esas claves no cambian en esta prueba.

**Riesgo:** esta prueba modifica datos cifrados. Haz el respaldo del paso 1 antes de cualquier otro paso.

## Antes de empezar

- Docker Desktop abierto y el repositorio en `main`, con los PRs #88 a #93 fusionados (los de versión de clave y re-cifrado).
- Los respaldos de claves del [inventario de claves](inventario-de-claves.md) hechos y guardados en el gestor de secretos.
- Ventana de prueba de al menos 1 hora. No hagas la prueba con datos reales de clientes: usa una base de desarrollo.

> **No pegues ninguna clave en el chat, en un commit ni en un archivo del repositorio.** Las claves se escriben solo en el `.env` de la raíz, que está en `.gitignore`.

## Variables que vas a usar

| Variable | Qué hace | Valor en esta prueba |
|---|---|---|
| `CRYPTO_CLAVE_CIFRADO` | Clave actual (versión 1) de clientes-ms | La que ya tienes; no la borres |
| `CRYPTO_CLAVES_CIFRADO` | Lista de versiones `N:base64,N:base64` | `1:<clave actual>,2:<clave nueva>` |
| `CRYPTO_VERSION_ACTIVA` | Versión con la que se cifra lo nuevo | `1` al inicio, luego `2` |
| `CRYPTO_RECIFRADO_ACTIVO` | Re-cifra al arrancar clientes-ms | `false` salvo en el paso 6 |
| `COMPRAS_CIFRADO_CLAVES`, `COMPRAS_CIFRADO_VERSION_ACTIVA`, `COMPRAS_RECIFRADO_ACTIVO` | Lo mismo para compras | Igual que arriba |
| `USUARIOS_CIFRADO_CLAVES`, `USUARIOS_CIFRADO_VERSION_ACTIVA`, `USUARIOS_RECIFRADO_ACTIVO` | Lo mismo para usuarios | Igual que arriba |

Cada vez que cambies código o variables de clave, reconstruye (`docker compose build`) y reinicia el servicio afectado. Cada servicio necesita que la **misma** clave de la versión 1 aparezca en su lista. En clientes, `1:` debe ser igual a `CRYPTO_CLAVE_CIFRADO`; en compras, `1:` debe ser igual a `COMPRAS_CIFRADO_CLAVE`; en usuarios, `1:` debe ser igual a `USUARIOS_CIFRADO_CLAVE`.

## Pasos

### 1. Respaldo

Respalda las tres bases y las claves antes de empezar:

```bash
docker compose exec -T postgres pg_dump -U postgres clientes > respaldo-clientes-$(date +%F).sql
docker compose exec -T postgres pg_dump -U postgres compras > respaldo-compras-$(date +%F).sql
docker compose exec -T postgres pg_dump -U postgres usuarios_accesos > respaldo-usuarios-$(date +%F).sql
```

Confirma que los archivos no están vacíos (`ls -lh respaldo-*.sql`). Guarda las claves actuales en el gestor de secretos si no lo has hecho.

### 2. Línea base

Levanta el ecosistema con la configuración actual y comprueba que todo funciona antes de cambiar nada:

```bash
docker compose up -d
```

Entra al gateway con un usuario de prueba, consulta un cliente existente y anota su respuesta. Si algo falla aquí, no sigas: resuélvelo primero.

### 3. Generar la clave nueva

Genera una clave de 32 bytes y cópiala directamente al `.env`, sin mostrarla en pantalla:

```bash
openssl rand -base64 32
```

Pégala en el `.env` como versión 2 (paso siguiente). Repite el comando para cada servicio: **las tres claves nuevas deben ser distintas**.

### 4. Agregar la versión 2 sin cambiar la activa

En el `.env`, para cada servicio, agrega la lista. **Deja también la variable de clave única:** la lista tiene prioridad, y la clave 1 de la lista debe ser la misma que la clave única. Ejemplo para clientes (los valores reales van en tu `.env`, no aquí):

```
CRYPTO_CLAVES_CIFRADO=1:<clave_actual>,2:<clave_nueva>
CRYPTO_VERSION_ACTIVA=1
```

Haz lo mismo con `COMPRAS_CIFRADO_CLAVES` y `USUARIOS_CIFRADO_CLAVES`. Reconstruye las imágenes y reinicia los tres servicios. **Reconstruir es obligatorio:** `docker compose up -d` solo reinicia con la imagen que ya existe, y esa imagen puede no tener el código de versiones.

```bash
docker compose build clientes-ms compras-proveedores-ms usuarios-accesos-ms
docker compose up -d clientes-ms compras-proveedores-ms usuarios-accesos-ms
```

Repite la consulta del paso 2. Debe responder igual: la versión 2 solo se **lee**, todavía no se escribe.

### 5. Cambiar la versión activa a 2

En el `.env`, cambia `CRYPTO_VERSION_ACTIVA=2` (y las de compras y usuarios). Reinicia los servicios.

Crea un contacto de prueba desde la aplicación y luego revisa que existan valores nuevos con la versión 2 (las filas anteriores siguen sin cambiar hasta el paso 6):

```bash
docker compose exec -T postgres psql -U postgres -d clientes \
  -c "SELECT count(*) AS en_v2 FROM contacto WHERE correo_cifrado LIKE 'v2:%';"
```

Resultado esperado: `en_v2` es mayor que cero. Los datos anteriores siguen en `v1:` o sin prefijo, y deben seguir leyéndose igual.

### 6. Re-cifrar los datos anteriores

En el `.env`, cambia a `true` las tres variables de re-cifrado (`CRYPTO_RECIFRADO_ACTIVO`, `COMPRAS_RECIFRADO_ACTIVO`, `USUARIOS_RECIFRADO_ACTIVO`). Reinicia los servicios y revisa los logs:

```bash
docker compose logs clientes-ms | grep "Campos re-cifrados"
docker compose logs compras-proveedores-ms | grep "Campos re-cifrados"
docker compose logs usuarios-accesos-ms | grep "Campos re-cifrados"
```

Cada línea muestra el conteo por campo, por ejemplo `Campos re-cifrados en prospecto.correo_cifrado: 12`. Son solo números; no aparecen valores personales.

Cuando termine, **vuelve a poner las tres variables en `false`** y reinicia. El re-cifrado solo debe correr una vez por cada cambio de versión.

### 7. Verificar que no quedan valores antiguos

Cada consulta debe devolver `0`:

```bash
docker compose exec -T postgres psql -U postgres -d clientes -c "
SELECT count(*) FROM prospecto WHERE correo_cifrado IS NOT NULL AND correo_cifrado NOT LIKE 'v2:%';
SELECT count(*) FROM contacto  WHERE correo_cifrado IS NOT NULL AND correo_cifrado NOT LIKE 'v2:%';
SELECT count(*) FROM cliente   WHERE rfc_cifrado    IS NOT NULL AND rfc_cifrado    NOT LIKE 'v2:%';"

docker compose exec -T postgres psql -U postgres -d compras -c "
SELECT count(*) FROM proveedores WHERE rfc_cifrado IS NOT NULL AND rfc_cifrado NOT LIKE 'v2:%';"

docker compose exec -T postgres psql -U postgres -d usuarios_accesos -c "
SELECT count(*) FROM usuarios WHERE correo NOT LIKE 'v2:%';"
```

Repite la consulta de lectura del paso 2 y comprueba que el cliente y el proveedor se siguen leyendo con su nombre, RFC y correo correctos.

### 8. Prueba de reanudación (opcional, recomendada)

Para probar que un corte no pierde datos, repite el paso 6 pero detén el servicio a mitad:

```bash
docker compose stop clientes-ms     # mientras corre el re-cifrado
docker compose start clientes-ms    # debe continuar donde quedó
```

Al final, el paso 7 debe seguir dando `0`, y el conteo de re-cifrados de la segunda corrida debe ser menor que el total.

### 9. Retirar la versión 1 (solo cuando todo esté bien)

Cuando el paso 7 dé `0` en todo, y hayas verificado la lectura, quita la versión 1 de la lista (`CRYPTO_CLAVES_CIFRADO=2:<clave_nueva>`) y reinicia.

> **No borres la clave 1 de tu gestor de secretos.** Si restauras el respaldo del paso 1, esos datos siguen cifrados con la clave 1.

> **Variables de clave única.** `CRYPTO_CLAVE_CIFRADO`, `COMPRAS_CIFRADO_CLAVE` y `USUARIOS_CIFRADO_CLAVE` siguen con la clave 1 después de este paso. El `docker-compose.yml` de la raíz las exige para arrancar, así que no se borran. Mientras la lista exista, se ignoran. Para sacar la clave 1 también de esas variables, pon en ellas el valor de la clave 2 (el mismo que aparece en la lista), y deja documentado que una lista borrada las convertiría en versión 1 con la clave 2.

> **Reinicio del gateway.** Después de recrear contenedores, reinicia también `gateway-ms`. Sin ese reinicio, el gateway puede responder `500` o `404` a rutas válidas.

## Cómo volver atrás

Si algo falla en cualquier paso:

1. En el `.env`, pon `CRYPTO_VERSION_ACTIVA=1` (y las de compras y usuarios). Las nuevas escrituras vuelven a usar la versión 1 y la lectura de la 2 sigue funcionando mientras la tengas en la lista.
2. Pon en `false` las variables de re-cifrado.
3. Reinicia los servicios.
4. Si los datos quedaron dañados, restaura el respaldo del paso 1 con `psql` y la clave 1 original.

## Resultados esperados

| Paso | Qué revisar | Resultado esperado |
|---|---|---|
| 2 | Consulta de cliente | Igual que antes |
| 4 | Consulta después de agregar la versión 2 | Igual que antes |
| 5 | Valor de un contacto nuevo | Empieza por `v2:` |
| 6 | Logs de re-cifrado | Conteos por campo, sin valores |
| 7 | Consultas de conteo | `0` en todas |
| 8 | Corte a mitad | Termina sin duplicar ni perder |
| 9 | Consulta después de quitar la 1 | Datos legibles |

## Problemas comunes

| Síntoma | Causa probable | Qué hacer |
|---|---|---|
| `Versión de clave de cifrado no configurada: 2` al arrancar | `CRYPTO_CLAVES_CIFRADO` no tiene la versión 2 | Revisa el `.env`; no tiene que haber espacios ni saltos de línea |
| `La versión activa de clave 2 no está configurada` | La activa no está en la lista | Agrega `2:` a la lista o baja la activa a 1 |
| `No se actualizó ... el valor cambió durante el proceso` | Otra escritura tocó la fila | Vuelve a correr el re-cifrado; es reanudable |
| El re-cifrado no hace nada | La variable no llegó al contenedor | Revisa con `docker compose config` que aparezca el valor `true` |
| El conteo de `NOT LIKE 'v2:%'` no baja a 0 | Quedan filas con `NULL` o el re-cifrado no terminó | Revisa los logs y vuelve a correrlo |

## Lo que esta guía no prueba

- Los índices HMAC: siguen con sus claves originales.
- La producción: esta prueba es solo en tu Mac, con datos de desarrollo.
- Rotaciones en varias versiones a la vez. La guía cubre un solo cambio, de 1 a 2.
