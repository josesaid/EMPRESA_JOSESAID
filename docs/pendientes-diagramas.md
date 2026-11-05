# Pendientes de diagramas

Inventario al 2026-11-05. Un ítem por cada área y uno por cada microservicio. Cada diagrama pendiente se entrega como fuente `.mmd` y `.png`, igual que los existentes; los de arquitectura solo llevan `.png`.

## Lo que existe

| Tipo | Cantidad | Dónde |
|---|---|---|
| Diagramas de flujo y entidades (`.mmd` + `.png`) | 22 | clientes-ms (18), usuarios-accesos-ms (4) |
| Arquitectura (`.png`) | 12 | clientes, compras, contabilidad, discovery, documentos, facturación, gateway, inventario, pedidos, reportes, usuarios y la general en `docs/diagramas` |
| **Total de diagramas** | **34** | 56 archivos (22 `.mmd` y 34 `.png`) |

Hoy no hay diagramas de reglas de negocio ni de flujos en 11 de 13 microservicios (todos excepto clientes-ms y usuarios-accesos-ms), ni diagramas entre servicios en ninguna de las 9 áreas.

El conteo sale de las secciones documentadas de cada servicio: máquinas o reglas de estado, reglas de duplicados, idempotencia, seguridad y flujos entre servicios. Puede crecer al revisar cada flujo.

## Pendientes por área (9)

### 1. Clientes y prospectos
- [ ] Validación de `clienteId` al crear o cotizar un pedido (pedidos-ventas-ms → clientes-ms). **1 diagrama**

### 2. Compras y proveedores
- [ ] Flujo de compra: orden, aprobación con límite, recepción y eventos hacia inventario y contabilidad. **1 diagrama**

### 3. Contabilidad
- [ ] Generación de pólizas a partir de eventos de compras, facturación e inventario. **1 diagrama**

### 4. Documentos y cumplimiento
- Sin pendiente entre servicios. El ciclo de vida interno está en el pendiente de su microservicio. **0 diagramas**

### 5. Facturación y pagos
- [ ] Flujo de orden confirmada → comprobante → cobro, con el consumo del evento de pedidos. Los pagos a proveedores quedan fuera de este diagrama. **1 diagrama**

### 6. Inventario y operaciones
- [ ] Flujo de existencias: recepciones de compra y movimientos por venta. **1 diagrama**

### 7. Pedidos y ventas
- [ ] Flujo de cotización → pedido → eventos hacia facturación e inventario. **1 diagrama**

### 8. Reportes ejecutivos
- [ ] Flujo de indicadores: cada área publica, reportes guarda por upsert y JEFE_AREA consulta. **1 diagrama**

### 9. Usuarios y accesos
- [ ] Flujo de alta de usuario, asignación de roles y área, y propagación a los servicios. **1 diagrama**

**Subtotal por área: 8 diagramas.**

## Pendientes por microservicio (13)

### clientes-ms (1 pendiente; 18 existentes)
- [ ] Roles y acceso a datos personales (sección 8 del README). **1**

### compras-proveedores-ms (5)
- [ ] Máquina de estados de la orden de compra
- [ ] Reglas de duplicados de RFC
- [ ] Aprobación con monto límite (sección 10)
- [ ] Recepciones y eventos publicados
- [ ] Flujo de `Idempotency-Key` en altas

### contabilidad-ms (3)
- [ ] Máquina de estados de la póliza
- [ ] Reglas de duplicados
- [ ] Flujo de `Idempotency-Key` en altas

### documentos-cumplimiento-ms (3)
- [ ] Reglas de estado del documento
- [ ] Reglas de duplicados
- [ ] Retención y purga física

### facturacion-pagos-ms (5)
- [ ] Máquinas de estado de comprobante y cobro
- [ ] Reglas de duplicados
- [ ] Tasa de IVA por fecha de creación
- [ ] Registro de pagos contra el saldo
- [ ] Flujo de `Idempotency-Key` en altas

### inventario-operaciones-ms (3)
- [ ] Máquinas de estado
- [ ] Reglas de duplicados
- [ ] Flujo de `Idempotency-Key` en altas

### pedidos-ventas-ms (4)
- [ ] Máquina de estados del pedido
- [ ] Reglas de duplicados
- [ ] Validación de cliente
- [ ] Flujo de `Idempotency-Key` en altas

### reportes-ejecutivos-ms (3)
- [ ] Reglas de estado
- [ ] Reglas de duplicados
- [ ] Lectura por área (JEFE_AREA)

### usuarios-accesos-ms (7; 4 existentes)
- [ ] Máquina de estados del usuario
- [ ] Reglas de duplicados
- [ ] Flujo de `Idempotency-Key` en altas
- [ ] Rotación de la clave RS256
- [ ] Área por usuario y rol JEFE_AREA
- [ ] Contraseña temporal
- [x] Bloqueo de 5 minutos tras 5 intentos fallidos (decisión de Said Olano, 2026-11-05)

### gateway-ms (1)
- [ ] Validación de JWT y enrutamiento

### discovery-ms (1)
- [ ] Registro de servicios y acceso protegido

### ui-ms (4; sin README)
- [ ] Arquitectura
- [ ] Login y sesión
- [ ] Renovación de sesión con lock
- [ ] Reintento tras 401 y cierre de sesión

### nexus-ms (3)
- [ ] Arquitectura
- [ ] Acciones con confirmación
- [ ] Acciones por MCP

**Subtotal por microservicio: 43 diagramas.**

## Resumen

| Concepto | Cantidad |
|---|---|
| Diagramas existentes | 34 (56 archivos) |
| Diagramas pendientes por área | 8 |
| Diagramas pendientes por microservicio | 43 |
| **Diagramas pendientes en total** | **51** |
| Imágenes `.png` pendientes | 51 |
| Fuentes `.mmd` pendientes | 49 (las 2 de arquitectura de ui-ms y nexus-ms solo llevan `.png`) |
