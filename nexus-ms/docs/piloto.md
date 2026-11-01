# Piloto de Nexus (paso 40)

## Participantes

| Rol | Correo | Estado |
|---|---|---|
| CONTADOR | pendiente | pendiente |
| DIRECCION | pendiente | pendiente |
| COMPRAS | pendiente | pendiente |

## Escenarios

| # | Usuario | Herramientas | Criterio de éxito |
|---|---|---|---|
| 1 | CONTADOR | `sugerir_poliza`, `confirmar_accion`, `consultar_poliza` | La póliza solo se registra al confirmar con código, y aparece en `consultar_poliza`. |
| 2 | CONTADOR | `sugerir_poliza`, `cancelar_accion` | Cancelar no registra la póliza. |
| 3 | CONTADOR | `confirmar_accion` con código incorrecto | La acción se cancela y no se registra nada. |
| 4 | DIRECCION | `consultar_politicas`, `consultar_estados_financieros` | Las respuestas citan la política y no inventan reglas. Sin escrituras. |
| 5 | COMPRAS | `consultar_politicas` | Solo ve políticas de su rol; no ve pólizas ni balanza. |
| 6 | Cualquiera | `sugerir_poliza` sin rol CONTADOR | La herramienta se niega con "Sin permiso". |

## Reglas

- Una semana, sin escrituras fuera del contador. Las pólizas de prueba llevan `PRUEBA` en la descripción.
- Cada usuario usa su propio login. Nadie comparte token.
- El token vence en 15 minutos; se renueva con `/usuarios-accesos-ms/v1/auth/refresh`.
- Presupuesto de tokens: 15 000 al día por usuario.

## Resultados

| Fecha | Escenario | Resultado | Notas |
|---|---|---|---|
| 31-oct-2026 | 6 (permiso negado) | Cumplido: `isError: true` con "Sin permiso" para una cuenta ADMIN sin rol CONTADOR. | Prueba con la cuenta admin. Escenarios 1 a 5 pendientes de usuarios reales. |

Anotar: pólizas sugeridas, confirmadas, canceladas; códigos incorrectos; respuestas incorrectas o con datos que no debían mostrarse; negaciones por rol.
