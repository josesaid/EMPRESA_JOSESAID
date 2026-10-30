# Diseño técnico: compras-proveedores-ms

## 1. Contexto

Compras necesita alta de proveedores con datos fiscales protegidos, órdenes que la dirección aprueba y recepciones que alimentan al inventario. La especificación está en `area_md_specification_files/Compras_Proveedores.md`.

## 2. Objetivos

- Los datos fiscales nunca se guardan ni se registran en claro.
- Buscar un proveedor por RFC sin descifrar toda la tabla.
- Una orden aprobada no cambia; sus recepciones nunca exceden lo ordenado.
- Cada recepción queda registrada y publica un evento para inventario.

## 3. Arquitectura (DDD)

| Capa | Contenido | Depende de |
|---|---|---|
| Dominio | `Proveedor`, `OrdenCompra`, `LineaOrden`, `Recepcion` y puertos | Nada del framework |
| Aplicación | `ComprasService`, `EventoDominio`, `PublicadorEventos` | Dominio |
| Infraestructura | Entidades JPA, `ProtectorDatos`, outbox y Kafka | Aplicación y dominio |
| API | Controlador REST, enmascarado y `ProblemDetail` | Aplicación |

## 4. Protección de datos

- **Cifrado:** AES-256-GCM con IV aleatorio de 12 bytes por valor. Dos cifrados del mismo RFC son distintos, y GCM detecta cualquier alteración del texto cifrado.
- **Índice de búsqueda:** HMAC-SHA256 del RFC normalizado. Es determinista, así que sirve para la unicidad y la búsqueda por igualdad, pero no revela el RFC sin la clave.
- **Enmascarado:** la API devuelve el RFC y el correo parcialmente ocultos. Los logs y los eventos no los incluyen.
- **Claves:** llegan por variables de entorno. Si no hay clave válida, el servicio no arranca.

## 5. Órdenes y recepciones

- La orden se crea en `BORRADOR` con un total derivado de sus líneas.
- Cada recepción pasa por `OrdenCompra.recibir`, que valida lo pendiente antes de cambiar nada.
- El estatus pasa a `RECIBIDA` solo cuando todas las líneas están completas. Mientras tanto es `RECIBIDA_PARCIAL`.
- Un lote de recepciones es atómico: si una línea falla, ninguna se guarda.

## 6. Eventos y outbox

El evento de aprobación y el de recepción se guardan en la misma transacción que el cambio de orden. El retransmisor los envía en orden. Los consumidores deben tolerar entrega al menos una vez.

## 7. Seguridad

JWT emitido por usuarios-accesos-ms. Cada rol hace solo lo suyo: COMPRAS crea, DIRECCION aprueba, ALMACEN recibe. El servicio no arranca sin clave pública JWKS ni claves de cifrado válidas.

## 8. Riesgos

| Riesgo | Mitigación |
|---|---|
| Pérdida de la clave de cifrado | Respaldo fuera de la máquina y procedimiento de rotación pendiente. |
| Cambio de la clave HMAC rompe búsquedas | La clave HMAC se trata como la clave de cifrado: se respalda y no se cambia sin migrar. |
| Aprobación sin límite de monto | Pendiente de confirmar con Dirección. |

## 9. Decisiones pendientes

1. Quién aprueba y el monto límite.
2. Endpoints para inactivar o bloquear proveedores.
3. Rotación de claves con re-cifrado.
4. Pagos a proveedores desde facturación.


## Indicadores para reportes

- **Indicador:** `ordenes_mes` (área `COMPRAS_PROVEEDORES`). Órdenes del mes: conteo de órdenes creadas en el mes (todos los estatus). Requiere `creado_utc` (V3). Las órdenes previas reciben en V4 la fecha de aplicación de la migración.
- **Indicador adicional:** `ordenes_canceladas_mes`: las órdenes CANCELADA del mes, para medir la proporción de cancelaciones.
- **Publicación:** clase `PublicadorIndicadoresOrdenes`, cada 60 minutos, a `indicadores-areas`. Clave `COMPRAS_PROVEEDORES|ordenes_mes|AAAA-MM`.
- **Decisión:** es una foto del periodo, no un evento. No usa el outbox; un envío fallido se corrige en el siguiente ciclo.
- **Consumo:** `reportes-ejecutivos-ms` lo guarda con upsert por área, nombre y periodo.
- **Prueba:** `PublicadorIndicadores*IT` con PostgreSQL y Kafka reales (Testcontainers).
