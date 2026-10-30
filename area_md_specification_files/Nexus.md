# Nexus (asistente de consulta con IA)

**Área:** Dirección general (consumidor transversal)
**Servicio propuesto:** nexus-ms

## Rol
Eres el asistente de consulta de EMPRESA_JOSESAID. Respondes preguntas en lenguaje natural usando los datos que ya expone cada área, con los permisos del usuario que pregunta.

## Contexto
**Incluye:** consultas en lenguaje natural sobre reportes y clientes (fase 1), registro de auditoría de cada consulta y medición de tokens y costo.
**Excluye:** escritura en cualquier servicio de negocio, cálculos contables (Contabilidad) y captura de datos. Las acciones que proponga el modelo llegan como borrador y una persona las confirma.

## Tarea
Recibir una pregunta, consultar las herramientas de solo lectura de los servicios autorizados y devolver una respuesta con la fuente de los datos.

## Restricciones
- Solo lectura en esta fase.
- Las llamadas a otros servicios van siempre a través de gateway-ms y con el token del usuario que pregunta. Nexus no usa credenciales propias para leer datos de negocio.
- Se pueden enviar al modelo datos redactados por el filtro de nexus-comun (RFC, correos, teléfonos, cuentas y tarjetas). Los datos sin redactar no salen de la empresa.
- Máximo de llamadas al modelo por pregunta y timeout configurables.

## Formato de salida
JSON para API. Texto en español con la fuente de los datos al final.

## Escenarios de entrada
1. Pregunta de un usuario sobre reportes → herramientas de reportes → respuesta con fuente.
2. Pregunta sobre clientes → herramientas de clientes → respuesta con fuente.
3. Pregunta fuera de alcance o sin permiso → respuesta que lo indica, sin datos.

## Reglas de negocio
- Si el usuario no tiene permiso sobre un servicio, la herramienta de ese servicio no se ofrece al modelo.
- Cada consulta queda registrada en auditoría con usuario, pregunta redactada, herramientas usadas y tokens.
- Una pregunta que supera el límite de llamadas devuelve un aviso, no una respuesta parcial.

## Catálogos
- Herramientas de fase 1: consultar_indicadores (reportes-ejecutivos-ms), consultar_clientes (clientes-ms).
- Proveedor de modelo: Claude, con la cuenta propia de la empresa.

## Entidades
- **Consulta:** id (UUID), usuario, pregunta_redactada, respuesta_resumen, herramientas (lista), tokens_entrada, tokens_salida, creado_utc (Instant).

## Endpoints
| Método | Ruta | Rol | Éxito | Errores |
|---|---|---|---|---|
| POST | /nexus/consultas | Cualquier rol con acceso a reportes o clientes | 200 | 400, 401, 403, 429, 504 |

## Eventos
Ninguno en esta fase.

## Requisitos funcionales
- Responder preguntas usando solo herramientas de solo lectura.
- Propagar el token del usuario en cada llamada a otro servicio.
- Registrar cada consulta en auditoría.

## Requisitos no funcionales
- Latencia p95 menor a 15 segundos por pregunta (propuesta).
- Costo medido por consulta con nexus-comun.

## Seguridad y acceso
- Autenticación: JWT RS256 emitido por usuarios-accesos-ms, validado como resource server.
- Autorización: la herramienta de cada servicio se habilita solo si el token tiene el rol correspondiente.

## Observabilidad
- Métricas: nexus.tokens, nexus.costo.estimado, nexus.consultas_total, nexus.consultas_rechazadas_total.

## Criterios de aceptación
- Un usuario sin rol de dirección no obtiene indicadores consolidados.
- Ninguna respuesta contiene RFC, correos, teléfonos, cuentas ni tarjetas.
- La línea base de casos de evaluación se registra antes del piloto.

## Pruebas
- Unitarias: AsistenteIA con ChatModel simulado.
- Integración: permisos por rol y auditoría con Testcontainers.
- Evaluación: casos de evaluación de nexus-comun contra el modelo real.

## Despliegue y rollback
Contenedor propio detrás de gateway-ms. Rollback por contenedor.

## Pendientes por integración futura
- Herramientas de escritura con confirmación humana (fase 2 en adelante).
- Servidor MCP (fase 4).

## Comentarios
- Depende de nexus-comun para filtro, tokens y prompts versionados.

## Roles en el área
- **DIRECCION:** consulta indicadores y clientes.
- **VENTAS:** consulta clientes.
