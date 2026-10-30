# Diagramas de clientes-ms

Todos los diagramas son imágenes PNG. Cada imagen tiene su fuente en Mermaid (`.mmd`) en la misma carpeta; para regenerarla:

```bash
cd docs/diagramas
mmdc -i nombre.mmd -o nombre.png -b white -w 1400 -s 2
```

## Mapa del servicio

### Vista general
![Mapa del servicio: actores, clientes-ms, PostgreSQL, Kafka y observabilidad](diagramas/00-mapa-del-servicio.png)

Muestra quién usa el servicio, de dónde sale la autenticación, qué almacena y a quién publica eventos. Fuente: [00-mapa-del-servicio.mmd](diagramas/00-mapa-del-servicio.mmd).

## Entidades

### Prospecto
![Entidad Prospecto](diagramas/entidad-prospecto.png)

Campos, tipos y estados del prospecto. Nombre, correo y RFC se guardan cifrados. Fuente: [entidad-prospecto.mmd](diagramas/entidad-prospecto.mmd).

### Cliente
![Entidad Cliente](diagramas/entidad-cliente.png)

Campos del cliente y sus relaciones con contactos e interacciones. Fuente: [entidad-cliente.mmd](diagramas/entidad-cliente.mmd).

### Contacto
![Entidad Contacto](diagramas/entidad-contacto.png)

Roles del contacto y la regla de un único principal por cliente. Fuente: [entidad-contacto.mmd](diagramas/entidad-contacto.mmd).

### Interacción
![Entidad Interacción](diagramas/entidad-interaccion.png)

Registro de llamadas, correos y visitas con fecha en UTC. Fuente: [entidad-interaccion.mmd](diagramas/entidad-interaccion.mmd).

### Consentimiento
![Entidad Consentimiento](diagramas/entidad-consentimiento.png)

Finalidad, medio, vigencia y revocación. Fuente: [entidad-consentimiento.mmd](diagramas/entidad-consentimiento.mmd).

### Relaciones entre entidades
![Diagrama de relaciones entre entidades con cardinalidades y tipos de columna](diagramas/relaciones.png)

Cardinalidades y columnas de todas las tablas. Fuente: [relaciones.mmd](diagramas/relaciones.mmd).

## Procesos

### Alta de prospecto
![Proceso de alta de prospecto](diagramas/proceso-alta-prospecto.png)

Validación, detección de RFC y duplicados, y respuestas posibles. Fuente: [proceso-alta-prospecto.mmd](diagramas/proceso-alta-prospecto.mmd).

### Cambio de estatus de prospecto
![Proceso de cambio de estatus de prospecto](diagramas/proceso-estatus-prospecto.png)

Transiciones válidas, idempotencia y restricción de CONVERTIDO. Fuente: [proceso-estatus-prospecto.mmd](diagramas/proceso-estatus-prospecto.mmd).

### Conversión de prospecto a cliente
![Proceso de conversión de prospecto a cliente](diagramas/proceso-conversion.png)

Creación del cliente, contacto principal, marca de conversión y evento en la misma transacción. Fuente: [proceso-conversion.mmd](diagramas/proceso-conversion.mmd).

### Reglas de duplicados
![Proceso de detección de duplicados](diagramas/proceso-duplicados.png)

Qué bloquea siempre y qué se puede confirmar. Fuente: [proceso-duplicados.mmd](diagramas/proceso-duplicados.mmd).

### Cambio de estatus de cliente
![Proceso de cambio de estatus de cliente](diagramas/proceso-estatus-cliente.png)

ACTIVO, SUSPENDIDO y BAJA terminal. Fuente: [proceso-estatus-cliente.mmd](diagramas/proceso-estatus-cliente.mmd).

### Contactos del cliente
![Proceso para agregar un contacto](diagramas/proceso-contactos.png)

Desmarcado del principal anterior y alta del nuevo contacto. Fuente: [proceso-contactos.mmd](diagramas/proceso-contactos.mmd).

### Interacciones
![Proceso de registro y consulta de interacciones](diagramas/proceso-interacciones.png)

Alta y consulta paginada del historial. Fuente: [proceso-interacciones.mmd](diagramas/proceso-interacciones.mmd).

### Consentimientos
![Proceso de otorgamiento y revocación de consentimientos](diagramas/proceso-consentimiento.png)

Otorgamiento y revocación idempotente. Fuente: [proceso-consentimiento.mmd](diagramas/proceso-consentimiento.mmd).

## Eventos

### ClienteCreado
![Flujo del evento ClienteCreado](diagramas/evento-cliente-creado.png)

Disparador, outbox, Kafka y payload. Fuente: [evento-cliente-creado.mmd](diagramas/evento-cliente-creado.mmd).

### ClienteEstatusCambiado
![Flujo del evento ClienteEstatusCambiado](diagramas/evento-cliente-estatus-cambiado.png)

Disparador, outbox, Kafka y payload. Fuente: [evento-cliente-estatus-cambiado.mmd](diagramas/evento-cliente-estatus-cambiado.mmd).

### ConsentimientoRevocado
![Flujo del evento ConsentimientoRevocado](diagramas/evento-consentimiento-revocado.png)

Disparador, outbox, Kafka y payload. Fuente: [evento-consentimiento-revocado.mmd](diagramas/evento-consentimiento-revocado.mmd).
