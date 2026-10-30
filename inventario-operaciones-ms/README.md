# inventario-operaciones-ms

Microservicio del área **Operaciones** de EMPRESA_JOSESAID. Controla existencias por almacén, reservas para ventas, lotes con trazabilidad y órdenes de producción o de servicio.

## 1. Identidad

- **Servicio:** inventario-operaciones-ms · **Versión:** 0.1.0 · **Lenguaje:** Java 21 · **Framework:** Spring Boot 4.1.2
- **Puerto en tu Mac:** 8105 (contenedor: 8081) · **Base de datos:** `inventario`
- **Responsable:** pendiente de asignar · **Estado:** funcional; producción vs. servicio pendiente de confirmar (sección 20)

## 2. Propósito y alcance

Registra entradas, salidas, ajustes y reservas de inventario; controla órdenes de producción o de servicio; y permite consultar la trazabilidad de cada lote.

**Dentro del alcance:** artículos, almacenes, existencias, reservas, lotes, órdenes y eventos de bajo mínimo.

**Fuera del alcance:** compras a proveedores (compras-proveedores-ms), facturación (facturacion-pagos-ms) y reportes consolidados (reportes-ejecutivos-ms).

## 3. Estructura del repositorio

```
inventario-operaciones-ms/
├── pom.xml
├── Dockerfile
├── docker-compose.yml          Servicio, con comentarios
├── .env.example                Variables requeridas (sin valores reales)
├── docs/diseno.md              Diseño técnico
├── src/main/java/.../inventario/
│   ├── domain/                 Existencia, MovimientoInventario, Articulo, OrdenOperacion y puertos
│   ├── application/            InventarioService y publicador de eventos
│   ├── infrastructure/
│   │   ├── persistence/        Entidades JPA y adaptadores
│   │   └── outbox/             Outbox y retransmisor a Kafka
│   ├── api/                    Controlador REST y manejo de errores
│   └── config/                 Seguridad JWT, reloj y propiedades
└── src/test/java/...           Pruebas unitarias e integración
```

## 4. Requisitos

- JDK 21 y Maven 3.9 (solo para compilar fuera de Docker)
- Docker Desktop con Compose v2
- Red `empresa-net`: `docker network create empresa-net` (una sola vez)

## 5. Configuración

| Variable | Obligatoria | Descripción |
|---|---|---|
| `INVENTARIO_DB_PASSWORD` | Sí (Compose) | Contraseña del usuario `inventario`. En el `.env` de la raíz o de esta carpeta. |
| `JWT_JWKS_URI` | No | URL del JWKS de usuarios-accesos-ms (clave pública). Ya viene configurada en el Compose |
| `KAFKA_BOOTSTRAP` | No | Por defecto `localhost:9092`. En Compose: `kafka:29092`. |
| `OUTBOX_INTERVALO_MS` | No | Intervalo del retransmisor. Por defecto 5000. |

Las credenciales viven solo en `.env`, que está en `.gitignore`.

## 6. Ejecución

**Orden de arranque (obligatorio):**

1. Red: `docker network create empresa-net` (una sola vez)
2. Raíz: `docker compose up -d postgres kafka`
3. **usuarios-accesos-ms** primero, porque emite los tokens que este servicio valida
4. **inventario-operaciones-ms** (este servicio). Puede arrancar en cualquier orden respecto de los demás servicios de negocio.

```bash
cd inventario-operaciones-ms
docker compose up -d --build
curl -s http://localhost:8105/actuator/health
```

## 7. Modelo de datos

- **articulos:** id, sku (único), nombre, unidad, lote_obligatorio, minimo.
- **almacenes:** id, nombre, ubicacion.
- **existencias:** id, articulo_id, almacen_id (único por par), cantidad, reservado, version. `disponible = cantidad - reservado`.
- **movimientos_inventario:** id, articulo_id, almacen_id, tipo, cantidad, lote, referencia, creado_utc. Inmutables.
- **ordenes_operacion:** id, tipo (PRODUCCION, SERVICIO), estatus (ABIERTA, CERRADA), cantidad_objetivo, cantidad_real, version.
- **outbox_eventos:** id, tipo, agregado_id, payload, creado_utc, publicado_utc, intentos.

Las cantidades son `NUMERIC(14,3)` para admitir fracciones.

## 8. Migraciones

Flyway ejecuta `V1__esquema_inicial.sql`. Hibernate solo valida. Los cambios futuros van en `V2`, `V3`…, nunca editando las migraciones ya aplicadas.

## 9. Máquinas de estado

**Orden de operación:** `ABIERTA → CERRADA`. El cierre exige cantidad real mayor a cero. Una orden cerrada no se vuelve a cerrar.

**Existencia:** no tiene estado, pero aplica estas reglas en cada movimiento:
- La existencia nunca es negativa.
- Lo reservado nunca supera lo existente.
- Una salida o reserva no puede exceder lo disponible.
- Un ajuste no puede dejar la existencia negativa ni por debajo de lo reservado.

## 10. Seguridad

- JWT RS256 emitido por usuarios-accesos-ms. Solo `/actuator/health` es público.
- **ALMACEN:** registra movimientos y da de alta almacenes.
- **OPERACIONES:** da de alta artículos, abre y cierra órdenes, y consulta trazabilidad.
- **CALIDAD:** consulta trazabilidad.
- **COMPRAS** y **VENTAS:** solo consultan existencias.

## 11. API

Base: `/v1`

| Método | Ruta | Rol | Éxito | Errores |
|---|---|---|---|---|
| POST | `/articulos` (añadido) | OPERACIONES | 201 | 400, 409 |
| POST | `/almacenes` (añadido) | ALMACEN | 201 | 400 |
| POST | `/movimientos` | ALMACEN | 201 | 400 (lote faltante), 404, 409 (existencia insuficiente), 422 |
| GET | `/existencias?articulo=` | ALMACEN, COMPRAS, VENTAS | 200 | 404 |
| POST | `/ordenes-operacion` | OPERACIONES | 201 | 400, 422 |
| POST | `/ordenes-operacion/{id}/cierre` | OPERACIONES | 200 | 404, 409, 422 |
| GET | `/lotes/{lote}/trazabilidad` | OPERACIONES, CALIDAD | 200 | 404 |

Ejemplo de entrada con lote:

```json
{ "articuloId": "<UUID>", "almacenId": "<UUID>", "tipo": "ENTRADA", "cantidad": 20, "lote": "LOTE-A" }
```

Errores RFC 9457 con `type` `urn:josesaid:inventario-operaciones:error:<slug>`.

## 12. Reglas de duplicados

- SKU único: un duplicado responde 409.
- Un par artículo-almacén tiene una sola fila de existencia.
- Dos movimientos iguales registran dos movimientos. Pendiente `Idempotency-Key`.
- Edición concurrente de una existencia responde 409 (`conflicto-concurrencia`).

## 13. Eventos

Tópico `inventario.eventos`, clave = id del artículo o de la orden.

| Tipo | Cuándo | Datos |
|---|---|---|
| `existencia-bajo-minimo` | Una salida o ajuste deja la existencia por debajo del mínimo del artículo | articulo_id, almacen_id, cantidad |
| `orden-operacion-cerrada` | Al cerrar una orden | id, cantidad_real |

## 14. Outbox

El evento se guarda en `outbox_eventos` dentro de la misma transacción que el movimiento o el cierre. El retransmisor lo envía a Kafka en orden y se detiene en el primer fallo.

## 15. Observabilidad

- Salud: `GET /actuator/health`. Métricas: `GET /actuator/prometheus`.
- Logs JSON en formato ECS.
- Pendiente: métricas de negocio (`movimientos_total`, `existencia_bajo_minimo_total`) y trazas.

## 16. Pruebas

- Unitarias (11): reglas de existencia y reserva, cierre de órdenes y reglas del servicio con repositorios en memoria.
- Integración (1): PostgreSQL 17 y Kafka 4.0.0 con Testcontainers. Cubre lote obligatorio (400), existencia insuficiente (409), reserva, trazabilidad y cierre de orden.

```bash
mvn -B verify              # requiere Docker
mvn -B package -DskipITs   # solo unitarias
```

## 17. Despliegue

Imagen multi-etapa, usuario sin privilegios (uid 10001), `HEALTHCHECK` sobre `/actuator/health`. Despliegue con `docker compose up -d --build`.

## 18. Rollback

Las migraciones no revierten solas. Para volver atrás, despliega la imagen anterior compatible con el esquema actual. Las correcciones de inventario se hacen con movimientos de compensación, nunca borrando.

## 19. Solución de problemas

- **400 "requiere lote":** el artículo tiene lote obligatorio. Envía el campo `lote`.
- **409 "Existencia insuficiente":** consulta `GET /existencias` para ver lo disponible.
- **409 "conflicto-concurrencia":** dos operaciones tocaron la misma existencia. Reintenta.
- **422 en el cierre:** la cantidad real debe ser mayor a cero.

## 20. Decisiones y pendientes

- **Añadido a la especificación:** `POST /articulos` y `POST /almacenes`, sin los cuales el servicio no puede usarse.
- **Añadido:** el campo `lote` en movimientos y el campo `minimo` en artículos, necesarios para trazabilidad y para el evento de bajo mínimo.
- **Decisión:** `AJUSTE` acepta cantidades negativas. Los demás tipos son siempre positivos.
- **Pendiente:** confirmar si el área maneja producción, servicio o ambos.
- **Pendiente:** entidad de lote con liberación; hoy un lote solo se identifica en los movimientos.
- **Pendiente:** liberar reservas. Hoy una reserva se mantiene hasta que la operación la consume.
- **Pendiente:** recepción automática desde compras y reserva automática desde ventas (eventos).
- **Pendiente:** conteo cíclico con dispositivos móviles e integración con sensores.
- **Pendiente:** `Idempotency-Key`, métricas de negocio y diagramas en PNG.

## 21. Referencias

- Especificación del área: `area_md_specification_files/Inventario_Operaciones.md`
- Diseño técnico: `docs/diseno.md`
- Orden de arranque general: `README.md` de la raíz, sección 10

## Descubrimiento, gateway y monitoreo

- Al arrancar, `inventario-operaciones-ms` se registra en `discovery-ms` (Eureka). Después queda accesible por el gateway en `http://localhost:8080/inventario-operaciones-ms/...`.
- **Orden de arranque:** `discovery-ms` primero. Si este servicio arranca antes, se registra solo cuando el registro esté disponible.
- Métricas Prometheus en `/actuator/prometheus`, sin token, solo lectura.
- Variables `EUREKA_USUARIO` y `EUREKA_PASSWORD`: obligatorias en Docker; el Compose arma la URL de registro `http://USUARIO:CLAVE@discovery-ms:8761/eureka/`. Fuera de Docker use `EUREKA_URL=http://USUARIO:CLAVE@localhost:8761/eureka/` y `EUREKA_INSTANCIA_HOST=localhost`.


## Idempotencia de altas (`Idempotency-Key`)

El alta de `POST /v1/movimientos` acepta la cabecera opcional `Idempotency-Key`. Úsela cuando un reintento no debe crear el registro dos veces (por ejemplo, si se cortó la conexión y no sabe si la petición llegó).

- **Misma clave y mismo cuerpo:** responde la misma respuesta original; no se crea nada nuevo.
- **Misma clave con otro cuerpo:** responde `422` (`regla-de-negocio`).
- **Misma clave mientras la primera petición sigue en proceso:** responde `409` (`conflicto-de-estado`).
- **Sin la cabecera:** la petición se procesa como siempre.

Las claves se guardan 24 horas en la tabla `idempotencia`. Si la operación falla, la clave se libera para que el cliente pueda reintentar.


## Indicadores para reportes ejecutivos

Publica un indicador mensual al tópico `indicadores-areas`, que consume `reportes-ejecutivos-ms`.

- **Indicador:** `movimientos_mes` (área `OPERACIONES`).
- **Definición:** Número de movimientos de inventario del mes, por hora de la Ciudad de México.
- **Clave del mensaje:** `OPERACIONES|movimientos_mes|AAAA-MM`. Reportes actualiza el valor por área, nombre y periodo (upsert).
- **Ciclo:** recalcula el mes actual y el anterior cada 60 minutos; el primer envío ocurre a los 30 segundos.
- **Configuración:** `INDICADORES_TOPICO`, `INDICADORES_INTERVALO_MS`, `INDICADORES_INICIO_MS`.
- **Entrega:** no pasa por el outbox. Es una foto del periodo; si un envío falla, el siguiente ciclo lo corrige.
- **Clase:** `PublicadorIndicadoresMovimientos`. Prueba: `mvn verify -Dit.test='PublicadorIndicadores*IT'`.
