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

- Alrededor de 100 filas por tabla de negocio. Las tablas que dependen de otras (pagos, reportes) tienen menos.
- Catálogos completos: todos los estatus, tipos, roles y canales. Las combinaciones se recorren hasta cubrirlas todas.
- 12 periodos contables y 12 reportes (uno por mes, porque el periodo es único).
- Usuarios demo: uno por rol, por ejemplo `admin@empresa.mx`, con contraseña **Demo2026!**.

## Notas

- `--reset` y `--cargar-datos` borran los datos de negocio de tus bases **locales**. No corras esto contra datos reales.
- Los datos personales se cifran con las claves de cada servicio en tu `.env`: `CRYPTO_CLAVE_CIFRADO` y `CRYPTO_CLAVE_INDICE` (clientes), `USUARIOS_CIFRADO_CLAVE` y `USUARIOS_INDICE_CLAVE` (usuarios), `COMPRAS_CIFRADO_CLAVE` y `COMPRAS_INDICE_CLAVE` (compras). Si cambias alguna, vuelve a correr el script con `--reset`.
- Los datos no se publican a Kafka, así que el tablero de reportes no cambia hasta que se generen eventos.
