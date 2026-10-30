# Diseño técnico: contabilidad-ms

## 1. Contexto

Finanzas necesita un libro contable que garantice que cada asiento cuadra, que un periodo cerrado no cambia y que toda corrección deja rastro. La especificación funcional está en `area_md_specification_files/Contabilidad.md`.

## 2. Objetivos

- Ningún asiento descuadrado entra al libro.
- Un periodo cerrado es inmutable.
- Ninguna póliza se borra ni se edita: las correcciones son reversas.
- Balanza de comprobación reproducible para cualquier periodo.

## 3. Arquitectura (DDD)

| Capa | Contenido | Depende de |
|---|---|---|
| Dominio | `Poliza`, `Movimiento`, `Periodo`, `CuentaContable`, `Balanza` y puertos | Nada del framework |
| Aplicación | `ContabilidadService`, `EventoDominio`, `PublicadorEventos` | Dominio |
| Infraestructura | Entidades JPA, adaptadores, outbox y Kafka | Aplicación y dominio |
| API | Controlador REST, DTOs y `ProblemDetail` | Aplicación |

## 4. Modelo

- **Póliza (raíz)** con movimientos inmutables. El cuadre se valida al crearla: suma de cargos igual a suma de abonos.
- **Movimiento** es un valor: exactamente un cargo o un abono, ambos con dos decimales y redondeo bancario.
- **Periodo** es un agregado pequeño que controla si se aceptan movimientos. Se crea al registrar la primera póliza del mes.
- **Saldo** derivado: en cuentas deudoras, cargos menos abonos; en acreedoras, abonos menos cargos. No se guarda.

## 5. Invariantes y orden de validación

En `registrarPoliza` el orden es fijo y tiene sentido de negocio:

1. Cuadre y estructura de la póliza (422). Se valida antes de tocar la base.
2. Cuentas existentes y activas (422).
3. Periodo abierto (409).

Así un error de cuadre nunca depende del estado del periodo.

## 6. Persistencia

- PostgreSQL 17 con Flyway. Hibernate solo valida.
- `Poliza` y `Periodo` usan control optimista (`@Version`). Dos cierres simultáneos no pueden confirmarse a la vez; el perdedor recibe 409.
- Los movimientos se insertan solo con la póliza nueva. Una póliza existente solo cambia su estatus.
- La balanza se calcula en memoria a partir de las pólizas del periodo. Para volúmenes grandes se migrará a una consulta agregada (pendiente).

## 7. Eventos y outbox

Igual que en facturación: el evento se guarda en la misma transacción y el retransmisor lo envía en orden. Los consumidores deben tolerar entrega al menos una vez.

## 8. Seguridad

JWT emitido por usuarios-accesos-ms. CONTADOR escribe; DIRECCION solo consulta. El servicio no arranca sin un secreto de al menos 32 bytes.

## 9. Operación

Arranque después de PostgreSQL, Kafka y usuarios-accesos-ms. Salud y métricas en `/actuator`. Logs JSON ECS sin montos individuales.

## 10. Riesgos

| Riesgo | Mitigación |
|---|---|
| Balanza lenta con muchos movimientos | Medir con el volumen de la especificación (100,000 movimientos en menos de 5 minutos). Consulta agregada si no alcanza. |
| Pólizas automáticas duplicadas al consumir pagos | Cuando llegue el consumidor, usar el id del evento como clave de idempotencia. |
| Cierre sin estados financieros completos | Documentado como pendiente. La balanza ya cuadra y sirve para revisión. |

## 11. Decisiones pendientes

1. Estado de resultados y balance general.
2. Consumidor de eventos de pagos y compras.
3. Catálogo con código agrupador SAT y exportación fiscal.
4. PDF y XLSX de estados financieros.
