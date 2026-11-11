# Guías paso a paso

Procedimientos completos, con pasos numerados, diagramas y verificación. Cada guía indica el rol que se necesita y el servicio que atiende.

| # | Guía | Cuándo usarla |
|---|---|---|
| 1 | [Levantar el ecosistema completo](01-levantar-ecosistema.md) | Primera vez en una máquina o después de un reinicio completo |
| 2 | [Ejecutar las pruebas de un servicio](02-ejecutar-pruebas.md) | Antes de integrar un cambio |
| 3 | [Rotar la clave RS256 sin cortar sesiones](03-rotar-clave-rs256.md) | Cada año, o de inmediato si la clave se expone |
| 4 | [Conectar un cliente MCP a Nexus](04-conectar-cliente-mcp.md) | Para usar Nexus desde un cliente MCP |
| 5 | [Dar de alta un usuario y asignarle rol y área](05-dar-de-alta-usuario.md) | Incorporar a alguien (rol ADMIN) |
| 6 | [Crear un proveedor, una orden, aprobarla y recibirla](06-orden-de-compra.md) | Ciclo de compra |
| 7 | [Registrar un pago a proveedor](07-pago-a-proveedor.md) | Pagar una cuenta por pagar (rol FINANZAS) |
| 8 | [Emitir un comprobante, registrar un cobro y cancelar](08-comprobantes-y-cobros.md) | Facturación y cobranza (rol FINANZAS) |
| 9 | [Consultar indicadores y generar reportes](09-indicadores-y-reportes.md) | Reporte ejecutivo mensual, trimestral o anual |
| 10 | [Rastrear un request en Jaeger](10-rastrear-request-jaeger.md) | Ver por dónde pasó un request y en qué paso se tardó o falló |

## Pendientes de diseño

Estos procedimientos todavía no tienen guía porque el procedimiento no está definido. No se escriben pasos que aún no existen.

- **Derechos ARCO** (acceso, rectificación, cancelación y oposición) de datos personales en `clientes-ms`: falta definir el proceso de anonimización.
- **Rotación de la clave de cifrado y de la clave HMAC de `compras-proveedores-ms`:** falta el diseño de la rotación y de su prueba.

## Java Docs

Javadoc de cada microservicio. Se genera con `configuration_scripts/build.sh` y se abre desde tu equipo; no está publicado en GitHub.

- [clientes-ms](../../clientes-ms/target/reports/apidocs/index.html)
- [compras-proveedores-ms](../../compras-proveedores-ms/target/reports/apidocs/index.html)
- [contabilidad-ms](../../contabilidad-ms/target/reports/apidocs/index.html)
- [discovery-ms](../../discovery-ms/target/reports/apidocs/index.html)
- [documentos-cumplimiento-ms](../../documentos-cumplimiento-ms/target/reports/apidocs/index.html)
- [facturacion-pagos-ms](../../facturacion-pagos-ms/target/reports/apidocs/index.html)
- [gateway-ms](../../gateway-ms/target/reports/apidocs/index.html)
- [inventario-operaciones-ms](../../inventario-operaciones-ms/target/reports/apidocs/index.html)
- [nexus-ms](../../nexus-ms/target/reports/apidocs/index.html)
- [pedidos-ventas-ms](../../pedidos-ventas-ms/target/reports/apidocs/index.html)
- [reportes-ejecutivos-ms](../../reportes-ejecutivos-ms/target/reports/apidocs/index.html)
- [ui-ms](../../ui-ms/target/reports/apidocs/index.html)
- [usuarios-accesos-ms](../../usuarios-accesos-ms/target/reports/apidocs/index.html)

## Convenciones

- `HOST` es la dirección del gateway (por ejemplo `localhost:8080`). Las rutas de API van bajo el prefijo del servicio, por ejemplo `/compras-proveedores-ms/v1`.
- `ACCESS_TOKEN` es el `accessToken` del login. Vence en 15 minutos.
- Nunca pegues contraseñas, claves o tokens en el chat ni en un commit.
