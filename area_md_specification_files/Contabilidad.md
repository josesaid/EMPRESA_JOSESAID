# Contabilidad

**Área:** Finanzas y contabilidad
**Servicio propuesto:** contabilidad-ms

## Rol
Eres el microservicio de contabilidad. Llevas el libro contable, registras entradas y salidas, y generas estados financieros.

## Contexto
**Incluye:** catálogo de cuentas, pólizas (asientos), movimientos de entradas y salidas, cierres de periodo y generación de estados financieros (balanza, estado de resultados, balance general).
**Excluye:** emisión de comprobantes (Facturación y pagos), gestión de inventarios y reportes ejecutivos consolidados (Reportes ejecutivos).

## Tarea
Registrar pólizas balanceadas, mantener el catálogo de cuentas, cerrar periodos y generar estados financieros por periodo.

## Restricciones
- Una póliza debe cuadrar: suma de cargos igual a suma de abonos.
- Un periodo cerrado no admite movimientos nuevos.
- Montos con `BigDecimal`.

## Formato de salida
JSON para API; PDF y XLSX para estados financieros (propuesto).

## Escenarios de entrada
1. Evento de pago registrado → póliza automática.
2. Captura manual de póliza → validación de cuadre.
3. Cierre de periodo → bloqueo de movimientos.
4. Solicitud de estados financieros del periodo.

## Reglas de negocio (propuestas)
- Una póliza sin cuadre se rechaza con 422.
- Una cuenta con movimientos no se elimina.
- Las correcciones se hacen con póliza de reversa, nunca con borrado.
- Las reglas fiscales (IVA, retenciones) viven en Facturación y pagos, no aquí.

## Catálogos
- Catálogo de cuentas (código agrupador SAT, propuesto).
- Tipos de póliza: ingreso, egreso, diario.

## Entidades
- **CuentaContable:** id (UUID), codigo, nombre, naturaleza (DEUDORA, ACREEDORA), activa.
- **Poliza:** id (UUID), periodo (YearMonth), tipo, fecha (LocalDate), estatus (BORRADOR, APLICADA, REVERSADA), version.
- **Movimiento:** id (UUID), poliza_id (UUID), cuenta_id (UUID), cargo (BigDecimal), abono (BigDecimal).
- **Periodo:** anio_mes (YearMonth), estatus (ABIERTO, CERRADO).

## Endpoints
| Método | Ruta | Rol | Éxito | Errores |
|---|---|---|---|---|
| POST | /polizas | CONTADOR | 201 | 400, 409, 422 |
| POST | /polizas/{id}/reversa | CONTADOR | 201 | 404, 409 |
| GET | /polizas/{id} | CONTADOR, DIRECCION | 200 | 404 |
| POST | /periodos/{anio_mes}/cierre | CONTADOR | 200 | 404, 409 |
| GET | /estados-financieros?periodo= | CONTADOR, DIRECCION | 200 | 400, 404 |
| POST | /cuentas | CONTADOR | 201 | 400, 409 |

## Eventos
| Evento | Disparador | Payload |
|---|---|---|
| poliza-aplicada | Aplicación de póliza | id, periodo, total |
| periodo-cerrado | Cierre | anio_mes |

## Requisitos funcionales
- Registrar pólizas con validación de cuadre.
- Reversar pólizas.
- Cerrar periodos.
- Generar estados financieros.

## Requisitos no funcionales
- Cierre mensual de hasta 100,000 movimientos en menos de 5 minutos (propuesta).

## Seguridad y acceso
- CONTADOR: lectura y escritura.
- DIRECCION: solo lectura de estados financieros.
- Autenticación vía Usuarios y accesos.

## Observabilidad
- Métricas: polizas_aplicadas_total, cierres_periodo_total, errores 5xx.
- Logs sin montos individuales ni datos de contacto.

## Criterios de aceptación
- Una póliza descuadrada devuelve 422.
- Un movimiento en periodo cerrado devuelve 409.

## Pruebas
- Unitarias: cuadre, naturaleza de cuentas, cierre.
- Integración: PostgreSQL y Kafka con Testcontainers.
- Contrato: endpoints y eventos.

## Despliegue y rollback
Canary con criterio del anexo. Rollback por contenedor. Correcciones por póliza de reversa.

## Pendientes por integración futura
- Consumo de eventos de pagos y compras.
- Exportación a sistema fiscal (SAT).

## Comentarios
- El stack no está confirmado. Por defecto se usa el de crea-area-empresa.

## Roles en el área
- **CONTADOR:** registra, reversa y cierra.
- **DIRECCION:** consulta estados financieros.
