# Diagramas de contabilidad-ms

Todos los diagramas son imágenes PNG. Cada imagen tiene su fuente en Mermaid (`.mmd`) en `docs/diagramas/`; para regenerarla:

```bash
cd docs/diagramas
mmdc -i nombre.mmd -o nombre.png -b white -w 1400 -s 2
```

## Arquitectura

![Arquitectura de contabilidad-ms](diagramas/arquitectura.png)

## Estados, duplicados e idempotencia

### Máquina de estados de la póliza
![Póliza aplicada, reversada y periodos](diagramas/01-maquina-de-estados-poliza.png)

Una póliza se aplica al registrarse y solo una `APLICADA` puede reversarse; la reversa nace `APLICADA` y apunta a la original. Un periodo cerrado no admite pólizas nuevas ni reversas, y no se reabre. `BORRADOR` está reservado. Fuente: [01-maquina-de-estados-poliza.mmd](diagramas/01-maquina-de-estados-poliza.mmd).

### Reglas de duplicados
![Reglas de duplicados en cuentas y pólizas](diagramas/02-reglas-de-duplicados.png)

El código de cuenta es único: un duplicado responde 409. Las pólizas no se deduplican por contenido; el control contra reintentos es la `Idempotency-Key`. Fuente: [02-reglas-de-duplicados.mmd](diagramas/02-reglas-de-duplicados.mmd).

### Idempotency-Key en pólizas
![Flujo de Idempotency-Key en POST /v1/polizas](diagramas/03-idempotency-key-en-polizas.png)

Con la misma clave y el mismo cuerpo se devuelve la respuesta original. Con otro cuerpo responde 422, y si la petición original sigue en proceso responde 409 hasta que pasen 2 minutos. Fuente: [03-idempotency-key-en-polizas.mmd](diagramas/03-idempotency-key-en-polizas.mmd).
