# Datos de demo

Llena las bases de EMPRESA_JOSESAID con información falsa para demos rápidas.

## Cómo usarlo

Opción fácil (levanta todo y carga los datos):

```bash
bash configuration_scripts/start.sh --cargar-datos
```

Opción manual (con el stack ya levantado):

```bash
pip3 install cryptography          # solo la primera vez
python3 seed/cargar_datos.py       # carga; se niega si ya hay datos
python3 seed/cargar_datos.py --reset   # borra los datos de negocio y vuelve a cargar
```

## Qué carga

- 100 filas por tabla de negocio (clientes, contactos, pedidos, comprobantes, pólizas, documentos, etc.).
- Catálogos completos: todos los estatus, tipos, roles y canales. Las combinaciones se recorren hasta cubrirlas todas.
- 12 periodos contables y 12 reportes (uno por mes, porque el periodo es único).
- Usuarios demo: uno por rol, por ejemplo `admin@demo.empresa.mx`, con contraseña **Demo2026!**.

## Notas

- `--reset` y `--cargar-datos` borran los datos de negocio de tus bases **locales**. No corras esto contra datos reales.
- Los datos personales se cifran con las claves de tu `.env`. Si cambias esas claves, vuelve a correr el script.
- Los datos no se publican a Kafka, así que el tablero de reportes no cambia hasta que se generen eventos.
