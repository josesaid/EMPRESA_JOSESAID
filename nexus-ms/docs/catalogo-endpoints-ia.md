# Catálogo de endpoints para consumo desde IA

Inventario de los endpoints REST de los servicios de EMPRESA_JOSESAID, con el rol que exige cada uno, la herramienta de Nexus que lo envuelve (si existe) y los huecos.

Estado al 2026-11-07. Fuente: anotaciones `@PreAuthorize`, `@*Mapping` y clases `Herramientas*.java` de `nexus-ms`, leídas directamente del código.

## Cómo consume la IA estos endpoints

Nexus expone las capacidades a la IA con Spring AI. Cada capacidad es un método anotado con `@Tool` (nombre, descripción) y sus parámetros con `@ToolParam`. Las clases están en `nexus-ms/src/main/java/.../nexus/infrastructure/` (`HerramientasClientes`, `HerramientasVentas`, etc.).

- **Lecturas**: la herramienta llama al servicio a través del gateway (`gateway.get().uri(...)`) reutilizando el JWT del usuario. Por eso la IA ve solo lo que el rol del usuario permite.
- **Escrituras**: no escriben directo. Una herramienta `*_borrador`, `confirmar_*`, `cancelar_*` o `sugerir_poliza` prepara una acción pendiente con `accion_id` y expira. Se ejecuta solo si el usuario confirma con `confirmar_accion` y el código de 8 dígitos. La ejecución la hace `EjecutorAccionesGateway`, que llama al endpoint real.
- **Límite**: `NEXUS_MAX_HERRAMIENTAS` (por defecto `4`) fija el máximo de herramientas por pregunta (`max-total-tool-calls` en `application.yml`).

Para conectar un cliente MCP, ver [conexion-mcp.md](conexion-mcp.md).

## Resumen de cobertura

| Concepto | Cantidad |
|---|---|
| Endpoints REST totales | 101 |
| Excluidos por diseño (autenticación, JWKS y el propio endpoint de chat) | 7 |
| Endpoints aplicables a IA | 94 |
| Con herramienta de Nexus | 44 (47 %) |
| Sin herramienta por flujo propio (alta de usuario de Nexus) | 3 |
| Huecos (sin herramienta) | 47 (50 %) |

Los 44 cubiertos incluyen `POST /v1/polizas` de contabilidad, que se registra solo a través de la acción `POLIZA_REGISTRAR` (`sugerir_poliza` → `confirmar_accion`).

Los 7 excluidos: `POST /v1/auth/login`, `POST /v1/auth/refresh`, `POST /v1/auth/cambiar-password-inicial`, `POST /v1/auth/logout`, `POST /v1/auth/logout-todas`, `GET /.well-known/jwks.json` y `POST /nexus/consultas` (es el endpoint del chat, no una herramienta).

## Endpoints por servicio

Convención: la columna **Ruta** es la del servicio. El prefijo del gateway es el nombre del servicio (por ejemplo `/clientes-ms/v1/clientes`). Las acciones indican el tipo entre paréntesis.

### clientes-ms (15 endpoints, 6 cubiertos)

| Método | Ruta | Rol requerido | Herramienta Nexus | 
|---|---|---|---|
| POST | `/v1/clientes` | ADMIN | **Hueco** |
| GET | `/v1/clientes` | VENTAS, ADMIN | `buscar_clientes` |
| GET | `/v1/clientes/{id}` | VENTAS, ADMIN | `obtener_cliente` |
| PATCH | `/v1/clientes/{id}` | ADMIN | **Hueco** |
| PATCH | `/v1/clientes/{id}/estatus` | ADMIN | **Hueco** |
| POST | `/v1/clientes/{id}/contactos` | ADMIN | **Hueco** |
| POST | `/v1/contactos/{contactoId}/consentimientos` | LEGAL | **Hueco** |
| POST | `/v1/consentimientos/{id}/revocacion` | LEGAL | **Hueco** |
| POST | `/v1/clientes/{clienteId}/interacciones` | VENTAS | `registrar_interaccion_cliente` (INTERACCION_REGISTRAR) |
| GET | `/v1/clientes/{clienteId}/interacciones` | VENTAS | **Hueco** |
| GET | `/v1/metricas/conversion` | VENTAS | **Hueco** |
| POST | `/v1/prospectos` | VENTAS | `crear_prospecto_borrador` (PROSPECTO_BORRADOR) |
| GET | `/v1/prospectos/{id}` | VENTAS | **Hueco** |
| PATCH | `/v1/prospectos/{id}/estatus` | VENTAS | `cambiar_estatus_prospecto` (PROSPECTO_ESTATUS) |
| POST | `/v1/prospectos/{id}/conversion` | VENTAS, ADMIN | `convertir_prospecto_en_cliente` (PROSPECTO_CONVERTIR) |

### compras-proveedores-ms (9 endpoints, 4 cubiertos)

| Método | Ruta | Rol requerido | Herramienta Nexus |
|---|---|---|---|
| POST | `/v1/proveedores` | COMPRAS | **Hueco** |
| GET | `/v1/proveedores` | COMPRAS, FINANZAS | `buscar_proveedores` |
| GET | `/v1/proveedores/{id}` | COMPRAS, FINANZAS | `consultar_proveedor` |
| POST | `/v1/ordenes-compra` | COMPRAS | **Hueco** |
| GET | `/v1/ordenes-compra` | COMPRAS, ALMACEN, DIRECCION | `listar_ordenes_compra` |
| GET | `/v1/ordenes-compra/{id}` | COMPRAS, ALMACEN, DIRECCION | `consultar_orden_compra` |
| POST | `/v1/ordenes-compra/{id}/aprobacion` | COMPRAS, DIRECCION | **Hueco** |
| POST | `/v1/ordenes-compra/{id}/recepciones` | ALMACEN | **Hueco** |
| POST | `/v1/ordenes-compra/{id}/cancelacion` | COMPRAS | **Hueco** |

### contabilidad-ms (8 endpoints, 5 cubiertos)

| Método | Ruta | Rol requerido | Herramienta Nexus |
|---|---|---|---|
| POST | `/v1/cuentas` | CONTADOR | **Hueco** |
| GET | `/v1/cuentas` | CONTADOR, DIRECCION | `consultar_catalogo_cuentas` |
| POST | `/v1/polizas` | CONTADOR | `sugerir_poliza` + `confirmar_accion` (POLIZA_REGISTRAR) |
| POST | `/v1/polizas/{id}/reversa` | CONTADOR | **Hueco** |
| GET | `/v1/polizas` | CONTADOR, DIRECCION | `listar_polizas` |
| GET | `/v1/polizas/{id}` | CONTADOR, DIRECCION | `consultar_poliza` |
| POST | `/v1/periodos/{anioMes}/cierre` | CONTADOR | **Hueco** |
| GET | `/v1/estados-financieros` | CONTADOR, DIRECCION | `consultar_estados_financieros` |

### documentos-cumplimiento-ms (9 endpoints, 3 cubiertos)

| Método | Ruta | Rol requerido | Herramienta Nexus |
|---|---|---|---|
| POST | `/v1/documentos` | LEGAL | **Hueco** |
| POST | `/v1/documentos/{id}/versiones` | LEGAL | **Hueco** |
| GET | `/v1/documentos/{id}` | LEGAL, DIRECCION | `consultar_documento` |
| GET | `/v1/documentos/{id}/versiones/{n}/descarga` | LEGAL | **Hueco** |
| GET | `/v1/documentos?titulo=` | LEGAL, DIRECCION | `buscar_documentos` |
| GET | `/v1/documentos/por-vencer` | LEGAL, DIRECCION | `consultar_documentos_por_vencer` |
| POST | `/v1/documentos/{id}/eliminacion` | LEGAL | **Hueco** |
| POST | `/v1/documentos/{id}/purga` | LEGAL | **Hueco** |
| GET | `/v1/catalogos/tipos-documento` | Autenticado | **Hueco** |

### facturacion-pagos-ms (10 endpoints, 4 cubiertos)

| Método | Ruta | Rol requerido | Herramienta Nexus |
|---|---|---|---|
| POST | `/v1/comprobantes` | FINANZAS | **Hueco** |
| GET | `/v1/comprobantes` | FINANZAS, VENTAS | `buscar_comprobante_por_folio` |
| GET | `/v1/comprobantes/pendientes-cobro` | FINANZAS, VENTAS | `listar_comprobantes_pendientes_cobro` |
| GET | `/v1/comprobantes/{id}` | FINANZAS, VENTAS | `consultar_comprobante` |
| POST | `/v1/comprobantes/{id}/emision` | FINANZAS | **Hueco** |
| POST | `/v1/comprobantes/{id}/cancelacion` | FINANZAS | **Hueco** |
| POST | `/v1/comprobantes/{id}/pagos` | FINANZAS | **Hueco** |
| GET | `/v1/cuentas-por-pagar/pagos` | FINANZAS | `listar_pagos_proveedor` |
| GET | `/v1/cuentas-por-pagar/{id}` | FINANZAS | **Hueco** |
| POST | `/v1/cuentas-por-pagar/{id}/pagos` | FINANZAS | **Hueco** |

### inventario-operaciones-ms (9 endpoints, 4 cubiertos)

| Método | Ruta | Rol requerido | Herramienta Nexus |
|---|---|---|---|
| POST | `/v1/articulos` | OPERACIONES | **Hueco** |
| POST | `/v1/almacenes` | ALMACEN | **Hueco** |
| POST | `/v1/movimientos` | ALMACEN | **Hueco** |
| GET | `/v1/articulos/bajo-minimo` | ALMACEN, COMPRAS | `listar_articulos_bajo_minimo` |
| GET | `/v1/movimientos` | ALMACEN, COMPRAS | `listar_movimientos_articulo` |
| GET | `/v1/existencias` | ALMACEN, COMPRAS, VENTAS | `consultar_existencias` |
| POST | `/v1/ordenes-operacion` | OPERACIONES | **Hueco** |
| POST | `/v1/ordenes-operacion/{id}/cierre` | OPERACIONES | **Hueco** |
| GET | `/v1/lotes/{lote}/trazabilidad` | OPERACIONES, CALIDAD | `consultar_trazabilidad_lote` |

### nexus-ms (8 endpoints, 3 cubiertos)

Rutas en `/nexus`. Son endpoints propios de Nexus, no de un servicio de negocio.

| Método | Ruta | Rol requerido | Herramienta Nexus |
|---|---|---|---|
| POST | `/nexus/acciones/{id}/confirmacion` | VENTAS, CONTADOR | `confirmar_accion` (envoltorio) |
| GET | `/nexus/acciones/{id}/alta` | ADMIN | Sin herramienta (flujo de alta) |
| POST | `/nexus/acciones/{id}/alta/verificacion` | ADMIN | Sin herramienta (flujo de alta) |
| POST | `/nexus/acciones/{id}/alta/completacion` | ADMIN | Sin herramienta (flujo de alta, completa `preparar_alta_usuario`) |
| GET | `/nexus/acciones/{id}` | VENTAS, CONTADOR, ADMIN | **Hueco** |
| POST | `/nexus/acciones/{id}/cancelacion` | VENTAS, CONTADOR | `cancelar_accion` (envoltorio) |
| POST | `/nexus/consultas` | Autenticado | No aplica (es el endpoint del chat) |
| POST | `/nexus/polizas/sugerencias` | CONTADOR | `sugerir_poliza` |

### pedidos-ventas-ms (16 endpoints, 11 cubiertos)

| Método | Ruta | Rol requerido | Herramienta Nexus |
|---|---|---|---|
| POST | `/v1/cotizaciones` | VENTAS, DIRECCION | `crear_cotizacion_borrador` (COTIZACION_CREAR) |
| GET | `/v1/cotizaciones/{id}` | VENTAS, DIRECCION, FINANZAS | **Hueco** |
| POST | `/v1/cotizaciones/{id}/envio` | VENTAS, DIRECCION | `enviar_cotizacion` (COTIZACION_ENVIAR) |
| POST | `/v1/cotizaciones/{id}/aceptacion` | VENTAS, DIRECCION | `aceptar_cotizacion` (COTIZACION_ACEPTAR) |
| POST | `/v1/cotizaciones/{id}/pedido` | VENTAS, DIRECCION | `convertir_cotizacion_en_pedido` (COTIZACION_A_PEDIDO) |
| POST | `/v1/pedidos` | VENTAS, DIRECCION | `preparar_pedido_borrador` (PEDIDO_BORRADOR) |
| GET | `/v1/pedidos` | VENTAS, DIRECCION, FINANZAS | `consultar_pedidos` |
| GET | `/v1/pedidos/{id}` | VENTAS, DIRECCION, FINANZAS, ALMACEN | **Hueco** |
| PUT | `/v1/pedidos/{id}/lineas` | VENTAS, DIRECCION | `reemplazar_lineas_pedido` (PEDIDO_REEMPLAZAR_LINEAS) |
| POST | `/v1/pedidos/{id}/confirmacion` | VENTAS, DIRECCION | `confirmar_pedido` (PEDIDO_CONFIRMAR) |
| POST | `/v1/pedidos/{id}/entrega` | ALMACEN, DIRECCION | **Hueco** |
| POST | `/v1/pedidos/{id}/cancelacion` | VENTAS, DIRECCION | `cancelar_pedido` (PEDIDO_CANCELAR) |
| POST | `/v1/pedidos/{id}/devolucion` | VENTAS, DIRECCION | `registrar_devolucion_pedido` (PEDIDO_DEVOLVER) |
| GET | `/v1/metricas/ventas` | DIRECCION, FINANZAS | `resumen_ventas` |
| GET | `/v1/catalogos/tasas-iva` | Autenticado | **Hueco** |
| GET | `/v1/catalogos/monedas` | Autenticado | **Hueco** |

### reportes-ejecutivos-ms (5 endpoints, 2 cubiertos)

| Método | Ruta | Rol requerido | Herramienta Nexus |
|---|---|---|---|
| POST | `/v1/indicadores` | DIRECCION | **Hueco** |
| GET | `/v1/indicadores` | DIRECCION, JEFE_AREA | `consultar_indicadores` |
| GET | `/v1/reportes/{periodo}` | DIRECCION, JEFE_AREA | `consultar_reporte` |
| POST | `/v1/reportes/{periodo}/cierre` | DIRECCION | **Hueco** |
| GET | `/v1/reportes/{id}/exportacion` | DIRECCION | **Hueco** |

### usuarios-accesos-ms (12 endpoints, 2 cubiertos)

`UsuarioController` tiene `@PreAuthorize("hasRole('ADMIN')")` a nivel de clase, así que todos sus endpoints son solo ADMIN.

| Método | Ruta | Rol requerido | Herramienta Nexus |
|---|---|---|---|
| POST | `/v1/auth/login` | Público | No aplica |
| POST | `/v1/auth/refresh` | Sin `@PreAuthorize` | No aplica |
| POST | `/v1/auth/cambiar-password-inicial` | Sin `@PreAuthorize` | No aplica |
| POST | `/v1/auth/logout` | Sin `@PreAuthorize` | No aplica |
| POST | `/v1/auth/logout-todas` | Sin `@PreAuthorize` | No aplica |
| GET | `/.well-known/jwks.json` | Público | No aplica |
| POST | `/v1/usuarios` | ADMIN | **Hueco** (el alta por IA usa `preparar_alta_usuario` + flujo de alta de Nexus) |
| GET | `/v1/usuarios` | ADMIN | `listar_usuarios` |
| GET | `/v1/usuarios/{id}` | ADMIN | `consultar_usuario` |
| PUT | `/v1/usuarios/{id}/roles` | ADMIN | **Hueco** |
| PUT | `/v1/usuarios/{id}/area` | ADMIN | **Hueco** |
| POST | `/v1/usuarios/{id}/baja` | ADMIN | **Hueco** |

## Herramientas de Nexus sin endpoint de negocio

Estas herramientas no llaman a un endpoint REST de un servicio:

| Herramienta | Qué hace |
|---|---|
| `consultar_politicas` | Búsqueda sobre las políticas internas (recuperador interno, no un endpoint) |
| `confirmar_accion` / `cancelar_accion` | Operan sobre las acciones pendientes de Nexus |
| `sugerir_poliza` | Prepara una póliza (se registra al confirmar) |
| `preparar_alta_usuario` | Prepara un alta de usuario (se completa en el flujo de alta de Nexus) |

## Observaciones

- **Huecos sin decidir**: las operaciones de administración de usuarios (roles, área, baja), de contabilidad (cuentas, reversa, cierre de periodo) y de documentos legales (eliminación, purga) no tienen herramienta. No hay decisión documentada sobre si deben tenerla; conviene decidirlo antes de añadirlas.
- **Acciones con tipo y ejecutor**: los tipos de acción que pasan por `EjecutorAccionesGateway` son PEDIDO_BORRADOR, PROSPECTO_BORRADOR, COTIZACION_CREAR, PEDIDO_REEMPLAZAR_LINEAS, PEDIDO_DEVOLVER, PROSPECTO_ESTATUS, PROSPECTO_CONVERTIR, INTERACCION_REGISTRAR, PEDIDO_CONFIRMAR, PEDIDO_CANCELAR, COTIZACION_ENVIAR, COTIZACION_ACEPTAR, COTIZACION_A_PEDIDO y POLIZA_REGISTRAR. `USUARIO_ALTA` no está en ese ejecutor: lo ejecuta el flujo de alta.

## Cómo se generó

Lectura directa de los controladores (`@*Mapping`, `@PreAuthorize`, incluida la anotación de clase) y de las clases `Herramientas*.java` y `EjecutorAccionesGateway.java`. Para regenerarlo tras un cambio, repetir la extracción y actualizar las tablas. Este documento no se genera automáticamente.
