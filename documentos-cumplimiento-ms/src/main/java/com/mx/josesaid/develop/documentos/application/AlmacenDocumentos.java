package com.mx.josesaid.develop.documentos.application;

import java.util.UUID;

/** Puerto de almacenamiento de archivos. La implementación los guarda cifrados. */
public interface AlmacenDocumentos {
    String referencia(UUID documentoId, int numero);
    void guardar(String referencia, byte[] contenido);
    byte[] leer(String referencia);
    /** Borra el archivo físico. Idempotente: un archivo que ya no existe no es error. */
    void borrar(String referencia);
}
