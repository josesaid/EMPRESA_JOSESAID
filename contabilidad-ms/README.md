# contabilidad-ms

Microservicio del área **Finanzas y contabilidad** de EMPRESA_JOSESAID. Lleva el libro contable: catálogo de cuentas, pólizas balanceadas, reversas, cierres de periodo y balanza de comprobación.

## 1. Identidad

- **Servicio:** contabilidad-ms · **Versión:** 0.1.0 · **Lenguaje:** Java 21 · **Framework:** Spring Boot 4.1.2
- **Puerto en tu Mac:** 8104 (contenedor: 8081) · **Base de datos:** `contabilidad`
- **Responsable:** pendiente de asignar · **Estado:** libro contable funcional; estados financieros parciales (ver sección 20)

## 2. Propósito y alcance

Registra pólizas (asientos) que deben cuadrar, las compensa con reversas, cierra periodos mensuales y calcula la balanza de comprobación.

**Dentro del alcance:** catálogo de cuentas, pólizas de ingreso, egreso y diario, reversas, cierre de periodos y balanza.

**Fuera del alcance:** emisión de comprobantes (facturacion-pagos-ms), inventarios (inventario-operaciones-ms), reportes consolidados (reportes-ejecutivos-ms) y reglas fiscales de IVA o retenciones (facturacion-pagos-ms).

## 3. Estructura del repositorio

```
contabilidad-ms/
├── pom.xml
├── Dockerfile
├── docker-compose.yml          Servicio, con comentarios
├── .env.example                Variables requeridas (sin valores reales)
├── docs/diseno.md              Diseño técnico
├── src/main/java/.../contabilidad/
│   ├── domain/                 Póliza, Movimiento, Periodo, CuentaContable, Balanza y puertos
│   ├── application/            ContabilidadService y publicador de eventos
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
| `CONTABILIDAD_DB_PASSWORD` | Sí (Compose) | Contraseña del usuario `contabilidad`. En el `.env` de la raíz o de esta carpeta. |
| `USUARIOS_JWT_SECRET` | Sí (Compose) | Mismo secreto que usuarios-accesos-ms. Mínimo 32 caracteres. |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | No | Conexión. Compose ya las fija. |
| `KAFKA_BOOTSTRAP` | No | Por defecto `localhost:9092`. En Compose: `kafka:29092`. |
| `OUTBOX_INTERVALO_MS` | No | Cada cuántos milisegundos se envían eventos pendientes. Por defecto 5000. |

Ningún valor real está en el código. Las credenciales viven en `.env`, que está en `.gitignore`.

## 6. Ejecución

**Orden de arranque (obligatorio):**

1. Red: `docker network create empresa-net` (una sola vez)
2. Raíz: `docker compose up -d postgres kafka`
3. **usuarios-accesos-ms** primero, porque emite los tokens que este servicio valida
4. **contabilidad-ms** (este servicio). Puede arrancar después de facturacion-pagos-ms, pero no depende de él en tiempo de ejecución.

```bash
cd contabilidad-ms
docker compose up -d --build
curl -s http://localhost:8104/actuator/health
```

Para desarrollo sin contenedor: `mvn spring-boot:run` con las variables de la sección 5.

## 7. Modelo de datos

- **cuentas_contables:** id, codigo (único), nombre, naturaleza (DEUDORA o ACREEDORA), activa.
- **periodos:** anio_mes (AAAA-MM), estatus (ABIERTO o CERRADO), version.
- **polizas:** id, anio_mes, tipo (INGRESO, EGRESO, DIARIO), fecha, estatus, reversa_de, version.
- **movimientos:** id, poliza_id, cuenta_id, cargo, abono. Cada fila tiene un cargo o un abono, nunca ambos.
- **outbox_eventos:** id, tipo, agregado_id, payload, creado_utc, publicado_utc, intentos.

## 8. Migraciones

- Flyway ejecuta `V1__esquema_inicial.sql` al arrancar. Hibernate solo valida (`ddl-auto: validate`).
- Las migraciones son hacia adelante: cambios en `V2`, `V3`… Nunca se editan las ya aplicadas.

## 9. Máquinas de estado

**Póliza:** `APLICADA → REVERSADA` (la original). La reversa nace como `APLICADA` con `reversa_de` apuntando a la original. `BORRADOR` está reservado para captura diferida (pendiente).

**Periodo:** `ABIERTO → CERRADO`. No hay reapertura.

Un periodo cerrado rechaza pólizas nuevas y reversas (409). Solo se reversa una póliza `APLICADA` (409 en otro caso).

## 10. Seguridad

- JWT HS256 emitido por usuarios-accesos-ms. Solo `/actuator/health` es público.
- **CONTADOR:** crea cuentas, registra, reversa y cierra periodos. Además consulta.
- **DIRECCION:** solo consulta pólizas y estados financieros.
- Sin token responde 401; sin el rol responde 403.

## 11. API

Base: `/v1`

| Método | Ruta | Rol | Éxito | Errores |
|---|---|---|---|---|
| POST | `/cuentas` | CONTADOR | 201 | 400, 409 |
| POST | `/polizas` | CONTADOR | 201 | 400, 409 (periodo cerrado), 422 (no cuadra, cuenta inexistente o inactiva) |
| GET | `/polizas/{id}` | CONTADOR, DIRECCION | 200 | 404 |
| POST | `/polizas/{id}/reversa` | CONTADOR | 201 | 404, 409 |
| POST | `/periodos/{anioMes}/cierre` | CONTADOR | 200 | 400, 404, 409 |
| GET | `/estados-financieros?periodo=AAAA-MM` | CONTADOR, DIRECCION | 200 | 400, 404 |

Ejemplo de póliza (las cantidades van en pesos con dos decimales):

```json
{
  "tipo": "INGRESO",
  "fecha": "2026-10-15",
  "movimientos": [
    { "cuentaId": "<UUID de 1101 Caja>", "cargo": 1160.00 },
    { "cuentaId": "<UUID de 4101 Ventas>", "abono": 1000.00 },
    { "cuentaId": "<UUID de 2108 IVA por pagar>", "abono": 160.00 }
  ]
}
```

Errores RFC 9457 con `type` `urn:josesaid:contabilidad:error:<slug>`.

## 12. Reglas de duplicados

- Código de cuenta único: un duplicado responde 409.
- Pólizas: no hay deduplicación. Pendiente `Idempotency-Key` (sección 20).
- Un conflicto de versión por edición concurrente responde 409 (`conflicto-concurrencia`).

## 13. Eventos

Tópico `contabilidad.eventos`, clave = id de la póliza o del periodo.

| Tipo | Cuándo | Datos |
|---|---|---|
| `poliza-aplicada` | Al registrar o reversar una póliza | id, periodo, total |
| `periodo-cerrado` | Al cerrar un periodo | anioMes |

## 14. Outbox

El evento se guarda en `outbox_eventos` en la misma transacción que la póliza o el cierre. El retransmisor lo envía a Kafka cada `OUTBOX_INTERVALO_MS`, en orden, y se detiene en el primer fallo.

## 15. Observabilidad

- Salud: `GET /actuator/health`. Métricas: `GET /actuator/prometheus`.
- Logs JSON en formato ECS. Los logs no incluyen montos individuales ni datos de contacto.
- Pendiente: métricas de negocio (`polizas_aplicadas_total`, `cierres_periodo_total`) y trazas.

## 16. Pruebas

- Unitarias (10): cuadre, reversa, periodos, balanza por naturaleza y reglas del servicio con repositorios en memoria.
- Integración (1): PostgreSQL 17 y Kafka 4.0.0 con Testcontainers. Cubre roles, póliza descuadrada (422), cierre (409) y balanza.

```bash
mvn -B verify              # unitarias e integración, requiere Docker
mvn -B package -DskipITs   # solo unitarias
```

## 17. Despliegue

Imagen multi-etapa (Maven y JRE 21), usuario sin privilegios (uid 10001), `HEALTHCHECK` sobre `/actuator/health`. Se despliega con `docker compose up -d --build`.

## 18. Rollback

- Las migraciones no revierten solas. Para volver atrás: desplegar la imagen anterior, compatible con el esquema actual.
- Las correcciones contables nunca borran: se hacen con una póliza de reversa.

## 19. Solución de problemas

- **"Defina CONTABILIDAD_DB_PASSWORD":** falta la variable. Compose se detiene a propósito.
- **422 "La póliza no cuadra":** la suma de cargos y abonos no coincide. Revisa cada renglón.
- **409 "El periodo está cerrado":** el periodo ya se cerró. Las correcciones van en el periodo abierto actual.
- **400 "Formato de periodo inválido":** usa AAAA-MM, por ejemplo `2026-10`.
- **401 en todas las rutas:** el secreto JWT no coincide con usuarios-accesos-ms.

## 20. Decisiones y pendientes

- **Decisión:** las pólizas se aplican al registrarlas. `BORRADOR` queda reservado.
- **Decisión:** la reversa se fecha con la fecha de hoy y cae en el periodo abierto actual.
- **Pendiente:** estado de resultados y balance general. Hoy solo existe la balanza de comprobación.
- **Pendiente:** consumo de eventos de pagos y compras para generar pólizas automáticas (escenario 1 de la especificación).
- **Pendiente:** catálogo con código agrupador SAT y exportación fiscal.
- **Pendiente:** PDF y XLSX de estados financieros.
- **Pendiente:** `Idempotency-Key` y métricas de negocio.
- **Pendiente:** diagramas en PNG.

## 21. Referencias

- Especificación del área: `area_md_specification_files/Contabilidad.md`
- Diseño técnico: `docs/diseno.md`
- Orden de arranque general: `README.md` de la raíz, sección 10

## Descubrimiento, gateway y monitoreo

- Al arrancar, `contabilidad-ms` se registra en `discovery-ms` (Eureka). Después queda accesible por el gateway en `http://localhost:8080/contabilidad-ms/...`.
- **Orden de arranque:** `discovery-ms` primero. Si este servicio arranca antes, se registra solo cuando el registro esté disponible.
- Métricas Prometheus en `/actuator/prometheus`, sin token, solo lectura.
- Variables `EUREKA_USUARIO` y `EUREKA_PASSWORD`: obligatorias en Docker; el Compose arma la URL de registro `http://USUARIO:CLAVE@discovery-ms:8761/eureka/`. Fuera de Docker use `EUREKA_URL=http://USUARIO:CLAVE@localhost:8761/eureka/` y `EUREKA_INSTANCIA_HOST=localhost`.
