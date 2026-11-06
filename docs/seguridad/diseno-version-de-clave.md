# Diseño propuesto: versión de clave en los datos cifrados

**Estado:** diseño aprobado en sus decisiones (ver al final). No hay código todavía. Resuelve el bloqueo de la sección «Por qué las claves de cifrado no se pueden rotar todavía» de [inventario-de-claves.md](inventario-de-claves.md).

## Problema

Hoy el valor cifrado es `Base64(IV de 12 bytes || texto cifrado + tag)` y no dice con qué clave se cifró. Para rotar una clave hay que descifrar y volver a cifrar todo con una sola clave en un solo paso, y mientras eso corre, los valores viejos y los nuevos no se distinguen. Por eso hoy la rotación obliga a un corte.

## Objetivo

1. Cada valor cifrado declara su versión de clave.
2. El servicio puede leer valores con dos versiones a la vez durante la transición.
3. La rotación se hace por lotes, sin corte, y se puede pausar o revertir.
4. Los índices HMAC (búsqueda por RFC o correo) siguen funcionando durante la transición.

## Formato propuesto

```
v<N>:Base64(IV de 12 bytes || texto cifrado + tag)
```

- `N` es un entero que indica la versión de `*_CIFRADO_CLAVE` con la que se cifró.
- Los valores **sin prefijo** se tratan como versión 1, para no tener que reescribir de golpe lo que ya existe.
- Las columnas son `TEXT` (en clientes-ms, compras-proveedores-ms y usuarios-accesos-ms), así que el prefijo cabe sin cambiar el esquema.

Configuración: una lista de claves por versión, por ejemplo `CRYPTO_CLAVES_CIFRADO=1:<base64>,2:<base64>`, y `CRYPTO_VERSION_ACTIVA=2`. Cifrar siempre con la activa; descifrar con la que indique el prefijo.

## Índices HMAC

Los índices (`correo_hash`, `rfc_hash`, `rfc_indice`) son `VARCHAR(64)`. Cambiar la clave HMAC cambia todos los valores, así que no se puede hacer en un solo paso sin romper las búsquedas.

Propuesta en tres fases:

1. Agregar una columna de índice nueva (por ejemplo `rfc_indice_v2`), sin borrar la anterior.
2. Un proceso de recálculo por lotes llena la columna nueva.
3. Las consultas usan la nueva; cuando todos los registros estén llenos, se borra la anterior en una migración aparte.

En compras, `rfc_indice` es `UNIQUE`: la columna nueva también debe serlo, y el recálculo debe detectar colisiones antes de activarla.

## Procedimiento de rotación (propuesto)

1. Generar la clave nueva y agregarla a la lista con un número mayor (sin cambiar la activa).
2. Reiniciar el servicio: ya puede **leer** la nueva, y sigue **cifrando** con la anterior.
3. Cambiar `CRYPTO_VERSION_ACTIVA` a la nueva y reiniciar: los datos nuevos se cifran con ella.
4. Ejecutar el re-cifrado por lotes: lee cada valor, lo descifra con su versión y lo cifra con la activa. Registra progreso y puede reanudarse.
5. Verificar que no queden valores con versiones antiguas (consulta por prefijo).
6. Retirar la clave antigua de la lista, reiniciando.

Para los índices HMAC, el paso 4 incluye también recalcular las columnas `_v2` (fase 2 de arriba).

## Riesgos

- **Pérdida de una clave** deja sin acceso a los datos de su versión. El respaldo de claves (inventario) es requisito previo.
- **Re-cifrado a medio camino:** si el proceso se corta, quedan dos versiones. Por eso debe poder reanudarse y la lectura debe aceptar ambas.
- **Rendimiento:** el re-cifrado de tablas grandes debe hacerse por lotes pequeños, fuera de horas pico.
- **Archivos (documentos-cumplimiento-ms):** los archivos cifrados en disco necesitan el mismo prefijo en su encabezado, no en una columna. Hay que revisar `ProtectorArchivos` antes de diseñar esa parte.

## Por servicio

| Servicio | Campos | Índices | Notas |
|---|---|---|---|
| clientes-ms | `correo_cifrado`, `rfc_cifrado`, contactos | `correo_hash`, `rfc_hash` | `CRYPTO_CLAVE_*` |
| compras-proveedores-ms | `rfc_cifrado` | `rfc_indice` (UNIQUE) | `COMPRAS_*_CLAVE` |
| usuarios-accesos-ms | `correo_cifrado`, `nombre_completo` | índice de correo | `USUARIOS_*_CLAVE` |
| documentos-cumplimiento-ms | archivos en disco | — | Prefijo en el encabezado del archivo |

## Pruebas propuestas

- Leer un valor sin prefijo (versión 1) y uno con `v2:`, con las dos claves configuradas.
- Re-cifrado por lotes: después de correrlo, ningún valor queda con la versión anterior.
- Interrupción a la mitad del re-cifrado: al reanudar, termina sin duplicar ni perder datos.
- Índices: después de recalcular, las búsquedas devuelven el mismo resultado que antes.
- Fallo al descifrar con una versión no configurada: error explícito, sin devolver datos.

## Decisiones tomadas

1. **Formato propio, sin KMS por ahora.** Las claves se mantienen dentro del sistema, con el prefijo `v<N>:`. El despliegue actual es Docker Compose en un solo entorno; un KMS agregaría una dependencia externa y un costo mensual sin resolver un problema actual. El formato no impide migrar a un KMS después: solo cambia el origen de cada clave.
2. **Rotación una vez al año**, o de inmediato si hay sospecha de exposición.
3. **Autoriza solo el responsable del proyecto** (Said Olano).
