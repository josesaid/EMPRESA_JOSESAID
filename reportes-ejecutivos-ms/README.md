# reportes-ejecutivos-ms

## 1. Identidad

- **Nombre:** reportes-ejecutivos-ms
- **Área de negocio:** Dirección general
- **Puerto en tu Mac:** 8109 (dentro del contenedor escucha en 8081)
- **Base de datos:** `reportes`, usuario `reportes` (creados por `infra/postgres/init-databases.sh`)
- **Versión:** 0.1.0 · Java 21 · Spring Boot 4.1.2 · PostgreSQL 17 · Kafka 4.0.0

## 2. Propósito y alcance

Consolida indicadores de todas las áreas por periodo, para que Dirección general los consulte, cierre reportes y los exporte.

Incluye: recepción de indicadores por Kafka, consolidado por periodo con marca de desactualizado, cierre de reportes con foto inmutable y exportación en CSV, PDF y XLSX.

No incluye: la captura de datos (cada área es dueña de los suyos), cálculos contables (Contabilidad), tableros interactivos ni alertas por umbral (pendientes).

## 3. Estructura del repositorio

```
reportes-ejecutivos-ms/
├── Dockerfile                  Build multi-etapa, usuario no root
├── docker-compose.yml          Servicio con comentarios; se incluye desde la raíz
├── .env.example                Variables del servicio
├── pom.xml
├── docs/diseno.md              Diseño y decisiones
└── src/
    ├── main/java/.../reportes/
    │   ├── domain/             Indicador, Reporte, Periodo, Area y reglas
    │   ├── application/        ReportesService, puerto ExportadorReporte, FormatoExportacion
    │   ├── infrastructure/
    │   │   ├── exportacion/    ExportadorCsv, ExportadorPdf (OpenPDF), ExportadorXlsx (Apache POI)
    │   │   ├── mensajeria/     ListenerIndicadores (Kafka, entrada de indicadores)
    │   │   ├── persistence/    Entidades JPA y adaptadores
    │   │   └── outbox/         Outbox y retransmisor a Kafka
    │   ├── api/                ReportesControlador y ManejadorErrores (RFC 9457)
    │   └── config/             Seguridad JWT, reloj y propiedades
    ├── main/resources/
    │   ├── application.yml
    │   └── db/migration/V1__esquema_inicial.sql
    └── test/java/.../reportes/
        ├── domain/             PeriodoTest, ReporteTest
        └── api/ReportesIT.java Integración con Testcontainers
```

## 4. Requisitos

- macOS con Docker Desktop en ejecución.
- JDK 21 y Maven 3.9+ (solo para correr fuera de Docker).
- Red `empresa-net`, postgres y kafka levantados desde la raíz.
- Secreto JWT igual al de usuarios-accesos-ms.

## 5. Configuración

| Variable | Obligatoria | Descripción |
|---|---|---|
| `REPORTES_DB_PASSWORD` | Sí (compose) | Contraseña del usuario `reportes`. |
| `USUARIOS_JWT_SECRET` | Sí | Clave HMAC de al menos 32 bytes; igual a usuarios-accesos-ms. |
| `REPORTES_FRESCURA_DIAS` | No | Días sin actualización para marcar un indicador como desactualizado. Por omisión 7. |
| `REPORTES_TOPICO_INDICADORES` | No | Tópico Kafka de entrada. Por omisión `indicadores-areas`. |

## 6. Ejecución

Desde la raíz:

```bash
docker compose up -d postgres kafka
cd reportes-ejecutivos-ms
docker compose up -d --build
curl -s http://localhost:8109/actuator/health
```

## 7. Modelo de datos

| Tabla | Contenido |
|---|---|
| `indicadores` | Valor vigente por área, nombre y periodo. Clave única por esas tres columnas. |
| `reportes` | Un reporte por periodo: BORRADOR o CERRADO. |
| `reporte_indicadores` | Foto de cada indicador al cerrar el reporte. |
| `outbox_eventos` | Eventos pendientes de enviar a Kafka. |

## 8. Migraciones

Flyway ejecuta `V1__esquema_inicial.sql`. Hibernate solo valida. Los cambios futuros serán migraciones nuevas.

## 9. Reglas de estado

**Periodos.** Formato `AAAA` (anual), `AAAA-Qn` (trimestral) o `AAAA-MM` (mensual). Otro formato responde 400.

**Indicadores.** Un indicador se identifica por área, nombre y periodo. Un nuevo valor reemplaza al anterior.

**Desactualizado.** Un indicador cuya fuente no se actualizó dentro de `REPORTES_FRESCURA_DIAS` aparece marcado. El cálculo se hace al momento de consultar.

**Reporte.** Se abre automáticamente con el primer indicador de su periodo. Mientras es BORRADOR, el consolidado se calcula en vivo.

**Cierre.** Congela una foto del consolidado. Un reporte cerrado no cambia: cerrar de nuevo responde 409, y registrar indicadores de ese periodo responde 409. Un periodo sin indicadores no se puede cerrar (422).

## 10. Seguridad

- API sin estado con JWT HS256. Solo `/actuator/health` es público.
- Toda la API es de rol DIRECCION (lectura completa, carga manual y cierre).
- Cada área solo ve sus indicadores en el consolidado: **propuesta, pendiente**.

## 11. API

Base `/v1`.

| Método | Ruta | Descripción | Éxito |
|---|---|---|---|
| POST | /indicadores | Carga o corrige un indicador (manual) | 201 |
| GET | /indicadores?area=&periodo= | Indicadores del periodo, con marca de desactualizado | 200 |
| GET | /reportes/{periodo} | Reporte del periodo (BORRADOR en vivo o CERRADO con foto) | 200 |
| POST | /reportes/{periodo}/cierre | Cierra el reporte | 200 |
| GET | /reportes/{id}/exportacion?formato=CSV\|PDF\|XLSX | Descarga el reporte en el formato indicado (CSV por omisión) | 200 |

Ejemplo de carga:

```json
{"area":"OPERACIONES","nombre":"Órdenes cerradas","periodo":"2026-10",
 "valor":12.5,"fuenteActualizadaUtc":"2026-10-29T10:00:00Z"}
```

Formatos: `CSV` (UTF-8 con BOM), `PDF` (A4 horizontal con tabla) y `XLSX` (una hoja con encabezados). El nombre del formato no distingue mayúsculas.

Errores: 400 solicitud inválida o formato no soportado, 403 sin rol, 404 reporte inexistente, 409 reporte cerrado o concurrencia, 422 periodo sin indicadores. Todos en RFC 9457.

## 12. Reglas de duplicados

Un indicador con la misma área, nombre y periodo reemplaza al anterior; no se duplica. Las cargas simultáneas del mismo indicador se resuelven por control optimista.

## 13. Eventos

| Evento | Cuándo | Datos |
|---|---|---|
| reporte-cerrado | Al cerrar un reporte | id, periodo |

Entrada: mensajes JSON en el tópico `indicadores-areas` (confirmado) con `area`, `nombre`, `periodo`, `valor` y `fuenteActualizadaUtc`. Un mensaje inválido o que viola una regla se registra en el log y se descarta.

## 14. Outbox

Los eventos se escriben en `outbox_eventos` dentro de la misma transacción y `RetransmisorOutbox` los envía a Kafka.

## 15. Observabilidad

- Salud: `GET /actuator/health`.
- Métricas Prometheus: `GET /actuator/prometheus`. Contadores `indicadores_recibidos_total` y `reportes_cerrados_total`.
- Logs JSON ECS. Indicadores descartados quedan en el log como `WARN`.

## 16. Pruebas

```bash
mvn -B test     # unitarias: periodos, reglas del reporte y exportación (10)
mvn -B verify   # además ReportesIT con PostgreSQL y Kafka reales (requiere Docker)
```

Desde la raíz, en tu Mac: `bash ejecutar-pruebas-reportes.sh`.

## 17. Despliegue

Imagen `reportes-ejecutivos-ms:0.1.0`. Requiere red `empresa-net`, base `reportes` y secreto JWT. Canary según el anexo. Se despliega después de los servicios de área, porque depende de sus eventos.

## 18. Rollback

1. `docker compose stop reportes-ejecutivos-ms`.
2. Regrese a la imagen anterior.
3. Los reportes cerrados se conservan en la base de datos; no se borran al hacer rollback.

## 19. Solución de problemas

| Síntoma | Causa probable | Acción |
|---|---|---|
| 404 en `/reportes/{periodo}` | No hay indicadores para ese periodo | Cargue un indicador primero. |
| Indicador no aparece tras el evento | Mensaje inválido o área mal escrita | Revise el log `Indicador descartado`. |
| 409 al cargar indicador | Periodo ya cerrado | Un cierre no se reabre; cargue en el periodo siguiente. |
| 400 al exportar | Formato distinto de CSV, PDF o XLSX | Use `formato=CSV`, `PDF` o `XLSX`. |

## 20. Decisiones y pendientes

- **Permisos por área:** pendiente. Hoy toda la API es de DIRECCION.
- **Alertas por umbral y tableros interactivos:** pendientes, según la especificación.
- **Descarte de mensajes inválidos:** hoy se registran y se descartan. Falta una cola de mensajes fallidos.
- **Tópico de indicadores:** `indicadores-areas`, confirmado.
- **Frescura:** 7 días por omisión; confirmar con Dirección.

## 21. Referencias

- Diseño detallado: `docs/diseno.md`.
- Orden de arranque: sección del README de la raíz.
- Especificación del área: `area_md_specification_files/Reportes_Ejecutivos.md`.

## Descubrimiento, gateway y monitoreo

- Al arrancar, `reportes-ejecutivos-ms` se registra en `discovery-ms` (Eureka). Después queda accesible por el gateway en `http://localhost:8080/reportes-ejecutivos-ms/...`.
- **Orden de arranque:** `discovery-ms` primero. Si este servicio arranca antes, se registra solo cuando el registro esté disponible.
- Métricas Prometheus en `/actuator/prometheus`, sin token, solo lectura.
- Variables `EUREKA_USUARIO` y `EUREKA_PASSWORD`: obligatorias en Docker; el Compose arma la URL de registro `http://USUARIO:CLAVE@discovery-ms:8761/eureka/`. Fuera de Docker use `EUREKA_URL=http://USUARIO:CLAVE@localhost:8761/eureka/` y `EUREKA_INSTANCIA_HOST=localhost`.
