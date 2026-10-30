# documentos-cumplimiento-ms

## 1. Identidad

- **Nombre:** documentos-cumplimiento-ms
- **Área de negocio:** Administración y legal
- **Puerto en tu Mac:** 8108 (dentro del contenedor escucha en 8081)
- **Base de datos:** `documentos`, usuario `documentos` (creados por `infra/postgres/init-databases.sh`)
- **Versión:** 0.1.0 · Java 21 · Spring Boot 4.1.2 · PostgreSQL 17 · Kafka 4.0.0

## 2. Propósito y alcance

Guarda contratos, permisos y registros legales con control de versiones, cifrado en reposo, alertas de vencimiento y reglas de retención.

Incluye: carga de documentos, nuevas versiones, descarga auditada, alertas de permisos por vencer y marcado de retenciones vencidas para revisión.

No incluye (pendiente): firma electrónica y almacenamiento en la nube (proveedor por definir). Tampoco incluye la lógica de negocio de cada área.

## 3. Estructura del repositorio

```
documentos-cumplimiento-ms/
├── Dockerfile                  Build multi-etapa; crea /data/documentos con permisos del usuario app
├── docker-compose.yml          Servicio con comentarios y volumen propio de archivos
├── .env.example                Variables del servicio
├── pom.xml
├── docs/diseno.md              Diseño de dominio y decisiones
└── src/
    ├── main/java/.../documentos/
    │   ├── domain/             Documento, VersionDocumento, TipoDocumento, reglas y excepciones
    │   ├── application/        DocumentosService, puerto AlmacenDocumentos, eventos
    │   ├── infrastructure/
    │   │   ├── seguridad/      ProtectorArchivos (AES-256-GCM)
    │   │   ├── almacen/        AlmacenArchivosLocal (archivos cifrados en el volumen)
    │   │   ├── persistence/    Entidades JPA y adaptadores
    │   │   ├── programacion/   TareasDocumentales (alertas diarias)
    │   │   └── outbox/         Outbox y retransmisor a Kafka
    │   ├── api/                DocumentosControlador y ManejadorErrores (RFC 9457)
    │   └── config/             Seguridad JWT, reloj y propiedades
    ├── main/resources/
    │   ├── application.yml
    │   └── db/migration/V1__esquema_inicial.sql
    └── test/java/.../documentos/
        ├── domain/DocumentoTest.java
        ├── infrastructure/ProtectorArchivosTest.java
        └── api/DocumentosIT.java
```

## 4. Requisitos

- macOS con Docker Desktop en ejecución.
- JDK 21 y Maven 3.9+ (solo para correr fuera de Docker).
- Red `empresa-net` creada por la raíz.
- postgres y kafka levantados desde la raíz.
- Clave de cifrado de archivos. Valida tokens con la clave pública de usuarios-accesos-ms (JWKS), sin secreto compartido.

## 5. Configuración

| Variable | Obligatoria | Descripción |
|---|---|---|
| `DOCUMENTOS_DB_PASSWORD` | Sí (compose) | Contraseña del usuario `documentos`. |
| `DOCUMENTOS_CIFRADO_CLAVE` | Sí | Clave AES-256 en base64 (32 bytes). Respáldela: sin ella no se leen los archivos. |
| `JWT_JWKS_URI` | No | URL del JWKS de usuarios-accesos-ms (clave pública). Ya viene configurada en el Compose |
| `DOCUMENTOS_ALMACEN_DIR` | No | Carpeta de archivos dentro del contenedor. Por omisión `/data/documentos`. |
| `DOCUMENTOS_ALERTAS_DIAS` | No | Días de anticipación para alertar permisos. Por omisión 30. |
| `DOCUMENTOS_ALERTAS_CRON` | No | Cuándo corre la tarea diaria. Por omisión 07:00. |
| `DOCUMENTOS_MAX_ARCHIVO` | No | Tamaño máximo por archivo. Por omisión 20MB. |

El servicio no arranca sin `DOCUMENTOS_CIFRADO_CLAVE` válida ni sin clave pública JWKS de 32 bytes o más.

## 6. Ejecución

Desde la raíz:

```bash
docker compose up -d postgres kafka
cd documentos-cumplimiento-ms
docker compose up -d --build
curl -s http://localhost:8108/actuator/health
```

## 7. Modelo de datos

| Tabla | Contenido |
|---|---|
| `documentos` | Metadatos, tipo, vencimiento, retención, estado de eliminación y banderas de alerta/revisión. |
| `versiones_documento` | Versiones con número, hash SHA-256 y referencia al archivo cifrado. Inmutables. |
| `outbox_eventos` | Eventos pendientes de enviar a Kafka. |

Un trigger en PostgreSQL impide `UPDATE` y `DELETE` sobre `versiones_documento`.

## 8. Migraciones

Flyway ejecuta `V1__esquema_inicial.sql` al arrancar. Hibernate solo valida. Los cambios futuros serán migraciones `V2`, `V3`, etc.

## 9. Reglas de estado

**Versiones.** Cada cambio de contenido crea la versión siguiente. Si el contenido es idéntico al de la versión actual, responde 409.

**Documento vencido.** Si `vence_en` ya pasó, no recibe versiones (409). Se registra un documento nuevo.

**Eliminación.** Solo después de que venza la `retencion_hasta` y con motivo (409 dentro de retención, 400 sin motivo). La eliminación es lógica: el registro queda marcado con motivo y fecha.

**Retención.** Se calcula al crear: CONTRATO 10 años, PERMISO 5 años, REGISTRO_LEGAL 10 años. **Pendiente de confirmación legal.**

**Alertas.** La tarea diaria emite `permiso-por-vencer` una sola vez por permiso, cuando su vencimiento cae dentro del umbral. La retención vencida se marca para revisión.

## 10. Seguridad

- API sin estado con JWT RS256. Solo `/actuator/health` es público.
- Autorización por rol con `@PreAuthorize`.
- Los archivos se cifran con AES-256-GCM antes de escribirse en disco. Cada archivo tiene su propio IV aleatorio.
- Al descargar se verifica el hash SHA-256. Si no coincide, responde 500 y no entrega el archivo.
- Cada descarga y cada eliminación se registra en el log de auditoría (`AUDITORIA`), sin el contenido del documento.

| Operación | Roles |
|---|---|
| Crear documento, nueva versión, descargar, eliminar | LEGAL |
| Consultar documento y vencimientos | LEGAL, DIRECCION |
| Catálogo de tipos | Cualquier usuario autenticado |

## 11. API

Base `/v1`. Las cargas usan `multipart/form-data`: la parte `datos` es JSON y la parte `archivo` es el documento.

| Método | Ruta | Descripción | Éxito |
|---|---|---|---|
| POST | /documentos | Carga documento y versión 1 (`datos` + `archivo`) | 201 |
| POST | /documentos/{id}/versiones | Carga nueva versión (`archivo`) | 201 |
| GET | /documentos/{id} | Consulta metadatos y versiones | 200 |
| GET | /documentos/{id}/versiones/{n}/descarga | Descarga el archivo | 200 |
| GET | /documentos/por-vencer?dias= | Documentos que vencen en N días (0–3650) | 200 |
| POST | /documentos/{id}/eliminacion | Elimina con motivo, solo después de retención | 200 |
| GET | /catalogos/tipos-documento | Tipos y años de retención | 200 |

Ejemplo de `datos`:

```json
{"tipo":"PERMISO","titulo":"Permiso ambiental","area":"Operaciones","venceEn":"2027-03-31"}
```

Errores: 400 solicitud inválida, 404 no encontrado, 409 conflicto (versión repetida, documento vencido, dentro de retención, concurrencia), 413 archivo demasiado grande, 422 regla de negocio, 403 sin rol. Todos en RFC 9457.

## 12. Reglas de duplicados

- El mismo contenido consecutivo no crea versión (409).
- Dos cargas simultáneas de la misma versión: una gana y la otra recibe 409 por control optimista.

## 13. Eventos

| Evento | Cuándo | Datos |
|---|---|---|
| documento-version-creada | Versión 1 o nueva versión | documento_id, numero |
| permiso-por-vencer | Permiso dentro del umbral (una vez) | documento_id, vence_en |

## 14. Outbox

Los eventos se escriben en `outbox_eventos` en la misma transacción. `RetransmisorOutbox` los envía a Kafka cada `OUTBOX_INTERVALO_MS`.

## 15. Observabilidad

- Salud: `GET /actuator/health`.
- Métricas Prometheus: `GET /actuator/prometheus`. Contadores `documentos_creados_total` y `versiones_creadas_total`.
- Logs JSON ECS. Nunca incluyen contenido de documentos.
- Auditoría de descargas y eliminaciones en el log con etiqueta `AUDITORIA`.

## 16. Pruebas

```bash
mvn -B test     # unitarias: dominio (11) y cifrado (4)
mvn -B verify   # además DocumentosIT con PostgreSQL y Kafka reales (requiere Docker)
```

Desde la raíz, en tu Mac: `bash ejecutar-pruebas-documentos.sh`.

## 17. Despliegue

Imagen `documentos-cumplimiento-ms:0.1.0`. Requiere red `empresa-net`, base `documentos`, clave pública JWKS, clave de cifrado y el volumen `documentos-archivos`. Canary según el anexo de la empresa.

## 18. Rollback

1. `docker compose stop documentos-cumplimiento-ms`.
2. Regrese a la imagen anterior.
3. No borre el volumen `documentos-archivos`: contiene los archivos legales.
4. Las migraciones no se revierten automáticamente; una corrección es una migración nueva.

## 19. Solución de problemas

| Síntoma | Causa probable | Acción |
|---|---|---|
| Arranque: "debe ser AES-256" | `DOCUMENTOS_CIFRADO_CLAVE` mal copiada | Revise el `.env` de la raíz (32 bytes en base64). |
| Descarga responde 500 con "verificación de integridad" | Archivo alterado en disco | Escale a Legal; no sobrescriba el archivo. |
| 409 al subir un archivo | Contenido igual a la versión actual | Suba un contenido distinto. |
| 413 al subir | Archivo mayor al límite | Reduzca el archivo o ajuste `DOCUMENTOS_MAX_ARCHIVO`. |

## 20. Decisiones y pendientes

- **Periodos de retención:** propuesta (10/5/10 años). Pendiente de confirmación legal.
- **Eliminación lógica:** el archivo se conserva. La purga física requiere política de Legal y queda pendiente.
- **Firma electrónica:** pendiente.
- **Almacenamiento en la nube:** hoy es un volumen local cifrado. El proveedor está por definir.
- **Rol LEGAL:** existe en el catálogo de usuarios-accesos.

## 21. Referencias

- Diseño detallado: `docs/diseno.md`.
- Orden de arranque: sección del README de la raíz.
- Especificación del área: `area_md_specification_files/Documentos_Cumplimiento.md`.

## Descubrimiento, gateway y monitoreo

- Al arrancar, `documentos-cumplimiento-ms` se registra en `discovery-ms` (Eureka). Después queda accesible por el gateway en `http://localhost:8080/documentos-cumplimiento-ms/...`.
- **Orden de arranque:** `discovery-ms` primero. Si este servicio arranca antes, se registra solo cuando el registro esté disponible.
- Métricas Prometheus en `/actuator/prometheus`, sin token, solo lectura.
- Variables `EUREKA_USUARIO` y `EUREKA_PASSWORD`: obligatorias en Docker; el Compose arma la URL de registro `http://USUARIO:CLAVE@discovery-ms:8761/eureka/`. Fuera de Docker use `EUREKA_URL=http://USUARIO:CLAVE@localhost:8761/eureka/` y `EUREKA_INSTANCIA_HOST=localhost`.


## Indicadores para reportes ejecutivos

Publica un indicador mensual al tópico `indicadores-areas`, que consume `reportes-ejecutivos-ms`.

- **Indicador:** `documentos_por_vencer_30d` (área `ADMINISTRACION_LEGAL`).
- **Definición:** Número de documentos no eliminados que vencen entre hoy y dentro de 30 días. Depende de la fecha de hoy: solo se publica el mes actual.
- **Clave del mensaje:** `ADMINISTRACION_LEGAL|documentos_por_vencer_30d|AAAA-MM`. Reportes actualiza el valor por área, nombre y periodo (upsert).
- **Ciclo:** recalcula el mes actual y el anterior cada 60 minutos; el primer envío ocurre a los 30 segundos.
- **Configuración:** `INDICADORES_TOPICO`, `INDICADORES_INTERVALO_MS`, `INDICADORES_INICIO_MS`.
- **Entrega:** no pasa por el outbox. Es una foto del periodo; si un envío falla, el siguiente ciclo lo corrige.
- **Clase:** `PublicadorIndicadoresVencimientos`. Prueba: `mvn verify -Dit.test='PublicadorIndicadores*IT'`.
