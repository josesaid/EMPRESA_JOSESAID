# nexus-ms

## 1. Identidad

- **Nombre:** nexus-ms
- **Área de negocio:** Asistente de IA sobre el resto de la plataforma (consulta y, con confirmación del usuario, escritura)
- **Acceso:** vía gateway (`/nexus/**`). Dentro del contenedor escucha en 8080; en el host, 8097.
- **Base de datos:** `nexus`, usuario `nexus`. El esquema lo maneja Flyway.
- **Versión:** 0.1.0 · Java 21 · Spring Boot 4.1.2 · Spring AI · PostgreSQL 17

## 2. Propósito y alcance

Responde preguntas en lenguaje natural sobre la empresa usando herramientas que consultan los microservicios con el token del propio usuario. Nunca actúa con permisos propios: cada servicio vuelve a validar el rol.

Incluye:
- Consultas de solo lectura: pedidos, resumen de ventas, clientes, reportes, proveedores, órdenes de compra, existencias, trazabilidad, documentos, contabilidad y usuarios, según el rol.
- Escritura con confirmación: Nexus prepara la acción, muestra un resumen y el usuario la confirma. Ver sección 9.
- Auditoría de cada consulta, con datos sensibles redactados.
- Tope de tokens por usuario y por servicio. Ver sección 12.

No incluye: ejecutar acciones sin confirmación, guardar datos sensibles en claro, ni decidir por el usuario.

## 3. Estructura del repositorio

```
nexus-ms/
├── Dockerfile                  Build multi-etapa (JDK 21 → JRE 21), usuario no root
├── docker-compose.yml          Servicio; se incluye desde la raíz
├── pom.xml                     Dependencias heredadas del padre de la empresa
└── src/
    ├── main/java/.../nexus/
    │   ├── application/        AsistenteIA, AccionesPendientes, PresupuestoTokens, puertos
    │   ├── infrastructure/     Herramientas por área, ClaudeAsistenteIA, EjecutorAccionesGateway,
    │   │                       AlcanceHerramientas y persistencia JPA
    │   ├── api/                NexusControlador, ManejoErrores (RFC 9457), DTOs
    │   └── config/             Seguridad JWT, propagación del token, beans y Clock
    ├── main/resources/
    │   ├── application.yml
    │   └── db/migration/       V1 auditoría · V2 acciones pendientes · V3 estatus CANCELADA
    └── test/java/.../nexus/
        ├── application/        AccionesPendientesTest, PresupuestoTokensTest
        ├── infrastructure/     Pruebas unitarias de herramientas y del ejecutor (MockRestServiceServer)
        ├── api/                NexusIT (Testcontainers) y pruebas de identidad
        └── evaluacion/         LineaBaseIT: casos de evaluación del piloto
```

## 4. Requisitos

- macOS con Docker Desktop en ejecución.
- JDK 21 y Maven 3.9+ (solo si se corre fuera de Docker).
- Red `empresa-net` creada por la raíz.
- postgres, usuarios-accesos-ms (JWKS), gateway-ms y discovery-ms levantados.
- Clave de API de Anthropic (`ANTHROPIC_API_KEY`).

## 5. Configuración

Los secretos se leen de variables de entorno o del `.env` de la raíz. Nunca se escriben en el repositorio.

| Variable | Obligatoria | Descripción |
|---|---|---|
| `ANTHROPIC_API_KEY` | Sí | Clave del modelo. |
| `NEXUS_DB_PASSWORD` | Sí | Contraseña del usuario `nexus`. |
| `EUREKA_USUARIO`, `EUREKA_PASSWORD` | Sí (en compose) | Credenciales del registro en Eureka. |
| `NEXUS_MODELO` | No | Modelo de Claude. Por defecto `claude-sonnet-5-5`. |
| `NEXUS_MODELO_TIMEOUT` | No | Tiempo máximo por llamada al modelo. Por defecto `20s`. |
| `NEXUS_MODELO_REINTENTOS` | No | Reintentos por llamada. Por defecto `1`. |
| `NEXUS_MAX_HERRAMIENTAS` | No | Máximo de herramientas por pregunta. Por defecto `4`. |
| `NEXUS_PRESUPUESTO_DIARIO` | No | Tope diario por usuario, en tokens. Por defecto `15000`. |
| `NEXUS_PRESUPUESTO_MENSUAL` | No | Tope mensual por usuario, en tokens. Por defecto `150000`. |
| `NEXUS_PRESUPUESTO_SERVICIO_MENSUAL` | No | Tope mensual de todo el servicio, en tokens. Por defecto `1000000`. |
| `JWT_JWKS_URI` | Sí (en compose) | Llave pública de usuarios-accesos-ms. |
| `GATEWAY_URL` | Sí (en compose) | URL del gateway para llamar a los servicios. |

## 6. Ejecución

Desde la raíz del repositorio, con el `.env` configurado:

```
docker compose up -d --build nexus-ms
```

Verificación rápida:

```
curl -s http://localhost:8097/actuator/health
```

## 7. Modelo de datos

- `nexus_consultas`: una fila por pregunta. Guarda usuario, pregunta redactada, resumen de la respuesta, herramientas usadas y tokens de entrada y salida.
- `acciones_pendientes`: acciones preparadas con su estatus (PENDIENTE, CONFIRMADA, EXPIRADA, CANCELADA), payload técnico y vigencia.

## 8. Migraciones

| Versión | Descripción |
|---|---|
| V1 | Tabla de auditoría de consultas. |
| V2 | Tabla de acciones pendientes. |
| V3 | Agrega CANCELADA al check de estatus de acciones. |

No se edita una migración ya aplicada: Flyway valida el checksum.

## 9. Acciones con confirmación

Nexus nunca ejecuta una escritura por sí solo. El flujo es:

1. **Preparar.** La herramienta valida con el token del usuario contra el servicio dueño y guarda la acción como PENDIENTE, con vigencia de 10 minutos. Devuelve un resumen y un `accion_id`.
2. **Consultar.** `GET /nexus/acciones/{id}` muestra el resumen. Si ya venció, la marca como EXPIRADA.
3. **Confirmar.** `POST /nexus/acciones/{id}/confirmacion` ejecuta la acción con el token del usuario. Si la ejecución falla, la acción sigue PENDIENTE.
4. **Cancelar.** `POST /nexus/acciones/{id}/cancelacion` la marca como CANCELADA sin ejecutarla.

Solo quien la preparó puede consultarla, confirmarla o cancelarla. Para otros usuarios responde 404.

### Acciones por MCP (con código de confirmación)

Por MCP (`/mcp`) la confirmación exige un código de una sola vez de 8 dígitos además del JWT:

1. `sugerir_poliza` prepara una póliza (rol CONTADOR). No la registra. Devuelve el `accion_id` y el código.
2. `confirmar_accion` con `accion_id` y código la registra. Solo quien la preparó puede confirmarla, dentro de 10 minutos.
3. `cancelar_accion` la cancela sin ejecutarla.

Un código incorrecto cancela la acción. El código se deriva con HMAC-SHA256 del id de la acción y del usuario, con el secreto `NEXUS_CONFIRMACION_SECRETO`. Si el secreto no está definido, cada instancia usa uno aleatorio y los códigos dejan de valer al reiniciar.

Acciones disponibles (solo rol VENTAS):

| Herramienta | Qué hace al confirmar |
|---|---|
| `preparar_pedido_borrador` | Crea un pedido en borrador. |
| `crear_prospecto_borrador` | Crea un prospecto. |
| `confirmar_pedido` | Confirma un pedido en borrador. |
| `cancelar_pedido` | Cancela un pedido en borrador o confirmado. |
| `enviar_cotizacion` | Envía una cotización en borrador. |
| `aceptar_cotizacion` | Acepta una cotización enviada. |
| `convertir_cotizacion_en_pedido` | Convierte una cotización aceptada en pedido. |
| `crear_cotizacion_borrador` | Crea una cotización con una línea. |
| `reemplazar_lineas_pedido` | Reemplaza las líneas de un pedido en borrador por una línea. |
| `registrar_devolucion_pedido` | Registra la devolución total de un pedido entregado. |
| `cambiar_estatus_prospecto` | Cambia el estatus de un prospecto a CONTACTADO, CALIFICADO o PERDIDO. |
| `convertir_prospecto_en_cliente` | Convierte un prospecto calificado en cliente con su contacto principal. |
| `registrar_interaccion_cliente` | Registra una llamada, correo o visita con un cliente. |

Las reglas de negocio se validan dos veces: en Nexus, para dar un mensaje claro antes de preparar, y en el servicio dueño, al confirmar.

## 10. Seguridad

- Valida el JWT contra la llave pública de usuarios-accesos-ms (JWKS). No comparte secretos con otros servicios.
- Cada herramienta usa el token del usuario, así que el servicio dueño aplica sus propios permisos.
- `AlcanceHerramientas` decide qué herramientas se ofrecen al modelo según el rol. Lo que no está en el mapa se niega.
- Las respuestas de los servicios y las preguntas pasan por `FiltroDatosSensibles` antes de volver al modelo o guardarse.
- La API de consultas requiere autenticación. Las de acciones requieren rol VENTAS.
- El endpoint MCP (`/mcp`) usa el mismo JWT. Cada llamada toma la identidad del cliente y aplica `AlcanceHerramientas`; una herramienta fuera del mapa de roles se niega aunque el cliente la pida. El gateway enruta `/mcp` a nexus-ms.

## 11. API

| Método | Ruta | Rol | Descripción |
|---|---|---|---|
| POST | `/nexus/consultas` | Autenticado | Hace una pregunta al asistente. |
| GET | `/nexus/acciones/{id}` | VENTAS | Muestra una acción pendiente (sin payload). |
| POST | `/nexus/acciones/{id}/confirmacion` | VENTAS | Confirma y ejecuta la acción. Responde 201. |
| POST | `/nexus/acciones/{id}/cancelacion` | VENTAS | Cancela la acción. Responde 204. |
| POST | `/nexus/polizas/sugerencias` | CONTADOR | Prepara una póliza sugerida. No la registra. Responde 201 y 422 si no cuadra. |
| POST | `/mcp` | JWT | Endpoint MCP (streamable). Herramientas de lectura, `sugerir_poliza`, `confirmar_accion` y `cancelar_accion`. Ver `docs/conexion-mcp.md`. |

Errores (RFC 9457): 404 si la acción no existe o no es del usuario, 409 si ya no está pendiente, 429 si se alcanzó un tope de tokens, 504 si el modelo o un servicio no responde a tiempo.

## 12. Presupuesto de tokens

Antes de llamar al modelo, Nexus revisa tres topes en este orden:

1. Diario por usuario.
2. Mensual por usuario.
3. Mensual del servicio.

Si uno se alcanzó, responde 429 con el motivo y no gasta tokens. Los días y meses se cuentan en hora de Ciudad de México.

El tope se revisa antes de la consulta, así que una consulta que empieza debajo del límite puede pasarlo por unos miles de tokens. Ese exceso sí se cobra.

## 13. Observabilidad

Métricas en `/actuator/prometheus`:

| Métrica | Tipo | Uso |
|---|---|---|
| `nexus_presupuesto_bloqueos_total{ambito}` | Contador | Bloqueos por tope: `usuario-diario`, `usuario-mensual` o `servicio`. |
| `nexus_presupuesto_servicio_consumo` | Gauge | Tokens consumidos por el servicio en el mes. |
| `nexus_presupuesto_servicio_limite` | Gauge | Tope mensual del servicio. |
| `nexus_presupuesto_umbral_alcanzado_total` | Contador | Avisos del 80% del tope del servicio. |
| `nexus.tokens` y `nexus.costo.estimado` | Métricas | Tokens y costo estimado por llamada. El costo queda en cero hasta configurar los precios por millón de tokens. |

Logs: un aviso al cruzar el 80% del tope del servicio (una vez por mes) y un error en cada bloqueo.

Alarmas en `infra/monitoring/alertas-nexus.yml`:

- **Crítica:** bloqueo por tope del servicio.
- **Aviso:** consumo arriba del 80% durante 5 minutos.
- **Aviso:** un usuario choca con su tope más de 3 veces en 15 minutos.

## 14. Pruebas

```
cd nexus-ms
mvn test
mvn verify
```

- `mvn test`: pruebas unitarias. No requiere Docker.
- `mvn verify`: también corre las pruebas de integración (`*IT`), que levantan PostgreSQL con Testcontainers. Requiere Docker y el `.env` cargado.
- `LineaBaseIT`: corre los casos de evaluación del piloto contra el modelo. Requiere `ANTHROPIC_API_KEY`.

## 15. Despliegue

Se publica igual que el resto de los servicios: imagen construida desde el Dockerfile y registro en Eureka. Antes de desplegar, confirmar que las migraciones se aplican sin cambios sobre V1 a V3.

## 16. Rollback

Revertir la imagen a la versión anterior. Las migraciones no se revierten: son aditivas. Si una acción quedó PENDIENTE, expira sola en 10 minutos.

## 17. Solución de problemas

- **404 en `/nexus/acciones/{id}`:** la acción no existe o la preparó otro usuario.
- **409 al confirmar:** la acción ya se confirmó, se canceló o expiró.
- **429:** se alcanzó un tope. Revisar la métrica `nexus_presupuesto_bloqueos_total` para ver el ámbito.
- **Prometheus no ve `nexus-ms`:** el scrape apunta al puerto 8080 del contenedor. Revisar en `/targets` que el objetivo esté `up`.

## 18. Decisiones y pendientes

- Fase 5: no se crean agentes por dominio. La decisión y sus motivos están en `docs/decisiones/fase-5-agentes.md`.
- Fase 6: el piloto usa un cliente MCP conectado a `/mcp` con token manual. Guía en `docs/conexion-mcp.md`. Pendiente: OAuth para clientes MCP, porque hoy el token se pega a mano y vence en 15 minutos.
- Pendiente: el código de confirmación aparece en la respuesta de `sugerir_poliza`. Quien tenga acceso a esa conversación lo ve; es una barrera suave.
- Se reemplazan líneas de pedido de una en una. Varias líneas quedan para una iteración posterior.
- El tope de servicio es de 1 000 000 tokens al mes. Se revisa con el consumo real del piloto.
- Pendiente: decisión de proveedor de modelo y de datos redactados (fase 0).
- Pendiente: monto límite de aprobación en compras (fase 2).

## 19. Referencias

- Diseño del asistente: `Nexus-Diseno-Asistente-IA.pdf` en la raíz del repositorio.
- Fases y pasos: `Fases y pasos requeridos en la implementación de Nexus.pptx` en la raíz.
- Monitoreo: `infra/monitoring/`.
