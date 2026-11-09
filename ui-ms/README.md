# ui-ms

Interfaz web de EMPRESA_JOSESAID. Renderiza las pantallas del sistema (cuentas, pólizas, acciones pendientes y cambio de contraseña) y consume las APIs a través del gateway.

## Pantallas

La interfaz tiene una barra lateral con una sección por módulo. Cada sección solo consulta; las escrituras siguen en sus pantallas propias o en Nexus.

| Sección | Rutas | Qué muestra |
|---|---|---|
| Inicio | `/` | Panel con acceso a cada módulo |
| Comercial | `/clientes`, `/clientes/{id}`, `/ventas/pedidos`, `/ventas/pedidos/{id}`, `/ventas/metricas` | Clientes con sus interacciones, pedidos y métricas de ventas por mes |
| Compras | `/compras/proveedores`, `/compras/proveedores/{id}`, `/compras/ordenes`, `/compras/ordenes/{id}` | Proveedores (búsqueda por razón social) y órdenes de compra |
| Inventario | `/inventario/existencias`, `/inventario/bajo-minimo`, `/inventario/movimientos`, `/inventario/trazabilidad` | Existencias y movimientos por artículo, bajo mínimo y trazabilidad por lote |
| Finanzas | `/cuentas`, `/polizas`, `/polizas/nueva`, `/contabilidad/estados-financieros`, `/facturacion/comprobantes`, `/facturacion/pendientes-cobro`, `/facturacion/pagos-proveedor` | Cuentas, pólizas, propuesta de póliza con IA, estados financieros, comprobantes y cobranza |
| Cumplimiento | `/documentos`, `/documentos/{id}`, `/documentos/por-vencer` | Documentos por título y vencimientos |
| Dirección | `/reportes/indicadores`, `/reportes/reporte` | Indicadores y reporte por periodo (`AAAA`, `AAAA-Qn` o `AAAA-MM`) |
| Accesos | `/usuarios`, `/usuarios/{id}` | Usuarios y sus roles |
| Acciones | `/acciones/{id}/confirmacion`, `/acciones/{id}/cancelacion` | Confirmar o cancelar una acción pendiente |

Lo que ve cada persona depende de su rol: si un módulo no le corresponde, la pantalla muestra «Tu rol no tiene acceso a esta sección».

## Probar la interfaz

1. Levanta el ecosistema según la [guía 1](../docs/guias/01-levantar-ecosistema.html). Incluye PostgreSQL, Kafka, Redis, los servicios, el gateway y `ui-ms`.
2. Abre `http://127.0.0.1:8098/login` e inicia sesión con el administrador inicial (`USUARIOS_ADMIN_INICIAL_CORREO` y `USUARIOS_ADMIN_INICIAL_PASSWORD` del `.env`).
3. Si es la primera vez, la contraseña inicial debe cambiarse antes de entrar.
4. Recorre las secciones desde la barra lateral. Los filtros marcados como obligatorios (por ejemplo, razón social o artículo) muestran la tabla solo cuando los capturas.

Para ver datos, primero hay que crearlos: clientes, proveedores, artículos, etc. Se hace con la API del gateway o con Nexus.

## Configuración

| Variable | Valor por defecto | Descripción |
|---|---|---|
| `SERVER_PORT` | `8080` | Puerto interno del servicio |
| `GATEWAY_URL` | `http://localhost:8080` | Dirección del gateway |
| `REDIS_HOST` / `REDIS_PORT` | `localhost` / `6379` | Caché de sesiones de la UI |
| `UI_PROXIES_CONFIABLES` | vacío | Proxies de confianza para `X-Forwarded-For` del navegador. Vacío usa la IP directa |
| `GATEWAY_TLS_BUNDLE` | vacío | Bundle de certificados si el gateway usa TLS interno |

## Ejecución

En desarrollo, con Docker Compose desde esta carpeta:

```bash
docker compose up -d --build
```

La imagen se construye con el contexto de la raíz del repositorio. Por eso el Compose usa `context: ..`.

La UI se publica solo en `127.0.0.1:8098`, así que no está expuesta en la red local.

## Seguridad

- Las sesiones se guardan en Redis.
- El detalle del flujo de inicio de sesión está en [`docs/seguridad-login.md`](docs/seguridad-login.md).
- El actuator de la UI no se publica. Queda pendiente de definir su exposición.

## Guías

- [Levantar el ecosistema completo](../docs/guias/01-levantar-ecosistema.html)
- [Ejecutar las pruebas de un servicio](../docs/guias/02-ejecutar-pruebas.html)
- [Índice de todas las guías](../docs/guias/README.html)
