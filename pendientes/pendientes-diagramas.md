# Pendientes de diagramas

Inventario al 2026-11-05. Estado al 2026-11-09: todos los ítems marcados, con los PR #172 a #186 fusionados. Un ítem por cada área y uno por cada microservicio. Cada diagrama se entrega como fuente `.mmd` y `.png`. Los de arquitectura ya tienen su `.mmd` (PR #191).

## Lo que existe

| Tipo | Cantidad | Dónde |
|---|---|---|
| Diagramas de flujo y entidades (`.mmd` + `.png`) | 22 | clientes-ms (18), usuarios-accesos-ms (4) |
| Arquitectura (`.mmd` + `.png`) | 12 | clientes, compras, contabilidad, discovery, documentos, facturación, gateway, inventario, pedidos, reportes, usuarios y la general en `docs/diagramas` |
| **Total de diagramas** | **34** | 56 archivos (22 `.mmd` y 34 `.png`) |

Hoy no hay diagramas de reglas de negocio ni de flujos en 11 de 13 microservicios (todos excepto clientes-ms y usuarios-accesos-ms), ni diagramas entre servicios en ninguna de las 9 áreas.

El conteo sale de las secciones documentadas de cada servicio: máquinas o reglas de estado, reglas de duplicados, idempotencia, seguridad y flujos entre servicios. Puede crecer al revisar cada flujo.

## Pendientes por área (9)

### 1. Clientes y prospectos
- [x] Validación de `clienteId` al crear o cotizar un pedido (pedidos-ventas-ms → clientes-ms). **1 diagrama**

### 2. Compras y proveedores
- [x] Flujo de compra: orden, aprobación con límite, recepción y eventos hacia inventario y contabilidad. **1 diagrama**

### 3. Contabilidad
- [x] Generación de pólizas a partir de eventos de compras, facturación e inventario. **1 diagrama**

### 4. Documentos y cumplimiento
- Sin pendiente entre servicios. El ciclo de vida interno está en el pendiente de su microservicio. **0 diagramas**

### 5. Facturación y pagos
- [x] Flujo de orden confirmada → comprobante → cobro, con el consumo del evento de pedidos. Los pagos a proveedores quedan fuera de este diagrama. **1 diagrama**

### 6. Inventario y operaciones
- [x] Flujo de existencias: recepciones de compra y movimientos por venta. **1 diagrama**

### 7. Pedidos y ventas
- [x] Flujo de cotización → pedido → eventos hacia facturación e inventario. **1 diagrama**

### 8. Reportes ejecutivos
- [x] Flujo de indicadores: cada área publica, reportes guarda por upsert y JEFE_AREA consulta. **1 diagrama**

### 9. Usuarios y accesos
- [x] Flujo de alta de usuario, asignación de roles y área, y propagación a los servicios. **1 diagrama**

**Subtotal por área: 8 diagramas.**

## Pendientes por microservicio (13)

### clientes-ms (0 pendientes; 19 existentes)
- [x] Roles y acceso a datos personales (sección 8 del README). **1**

### compras-proveedores-ms (5)
- [x] Máquina de estados de la orden de compra
- [x] Reglas de duplicados de RFC
- [x] Aprobación con monto límite (sección 10)
- [x] Recepciones y eventos publicados
- [x] Flujo de `Idempotency-Key` en altas

### contabilidad-ms (3)
- [x] Máquina de estados de la póliza
- [x] Reglas de duplicados
- [x] Flujo de `Idempotency-Key` en altas

### documentos-cumplimiento-ms (3)
- [x] Reglas de estado del documento
- [x] Reglas de duplicados
- [x] Retención y purga física

### facturacion-pagos-ms (5)
- [x] Máquinas de estado de comprobante y cobro
- [x] Reglas de duplicados
- [x] Tasa de IVA por fecha de creación
- [x] Registro de pagos contra el saldo
- [x] Flujo de `Idempotency-Key` en altas

### inventario-operaciones-ms (4)
- [x] Máquinas de estado
- [x] Reglas de duplicados
- [x] Flujo de `Idempotency-Key` en altas

### pedidos-ventas-ms (4)
- [x] Máquina de estados del pedido
- [x] Reglas de duplicados
- [x] Validación de cliente
- [x] Flujo de `Idempotency-Key` en altas

### reportes-ejecutivos-ms (3)
- [x] Reglas de estado
- [x] Reglas de duplicados
- [x] Lectura por área (JEFE_AREA)

### usuarios-accesos-ms (7; 4 existentes)
- [x] Máquina de estados del usuario
- [x] Reglas de duplicados
- [x] Flujo de `Idempotency-Key` en altas
- [x] Rotación de la clave RS256
- [x] Área por usuario y rol JEFE_AREA
- [x] Contraseña temporal
- [x] Bloqueo de 5 minutos tras 5 intentos fallidos (decisión de Said Olano, 2026-11-05)

### gateway-ms (1)
- [x] Validación de JWT y enrutamiento

### discovery-ms (1)
- [x] Registro de servicios y acceso protegido

### ui-ms (4)
- [x] Arquitectura
- [x] Login y sesión
- [x] Renovación de sesión con lock
- [x] Reintento tras 401 y cierre de sesión

### nexus-ms (3)
- [x] Arquitectura
- [x] Acciones con confirmación
- [x] Acciones por MCP

**Subtotal por microservicio: 43 diagramas.**

## Resumen

| Concepto | Cantidad |
|---|---|
| Diagramas existentes | 34 (56 archivos) |
| Diagramas pendientes por área | 8 |
| Diagramas pendientes por microservicio | 43 |
| **Diagramas pendientes en total** | **51** |
| Imágenes `.png` pendientes | 51 |
| Fuentes `.mmd` pendientes | 49 (las 2 de arquitectura de ui-ms y nexus-ms ya tienen `.mmd` desde PR #191 y #188-#190) |
