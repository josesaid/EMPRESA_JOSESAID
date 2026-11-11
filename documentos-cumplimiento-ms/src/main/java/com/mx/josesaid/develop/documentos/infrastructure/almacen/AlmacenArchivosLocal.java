package com.mx.josesaid.develop.documentos.infrastructure.almacen;

import com.mx.josesaid.develop.documentos.application.AlmacenDocumentos;
import com.mx.josesaid.develop.documentos.infrastructure.seguridad.ProtectorArchivos;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Almacén en el volumen del contenedor. Cada archivo se cifra antes de escribirse. */
@Component
public class AlmacenArchivosLocal implements AlmacenDocumentos {

    private final Path raiz;
    private final ProtectorArchivos protector;

    /** Recibe el directorio base donde se guardan los archivos. Ahí se crean las carpetas de cada documento. */
    public AlmacenArchivosLocal(@Value("${documentos.almacen.directorio}") String directorio,
                                ProtectorArchivos protector) {
        this.raiz = Path.of(directorio).toAbsolutePath().normalize();
        this.protector = protector;
    }

    /** Construye la referencia de almacenamiento de una versión a partir de su documento y número. La referencia nunca sale de la API. */
    @Override
    public String referencia(UUID documentoId, int numero) {
        return documentoId + "/v" + numero + ".bin";
    }

    /** Guarda el contenido en la referencia indicada, cifrado si hay clave configurada. */
    @Override
    public void guardar(String referencia, byte[] contenido) {
        Path destino = resolver(referencia);
        try {
            Files.createDirectories(destino.getParent());
            Files.write(destino, protector.cifrar(contenido),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo guardar el archivo", e);
        }
    }

    /** Lee el contenido de una referencia y lo descifra si corresponde. */
    @Override
    public byte[] leer(String referencia) {
        try {
            return protector.descifrar(Files.readAllBytes(resolver(referencia)));
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer el archivo", e);
        }
    }

    /** Borra el archivo de una referencia. Si no existe, no hace nada. */
    @Override
    public void borrar(String referencia) {
        try {
            Files.deleteIfExists(resolver(referencia));
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo borrar el archivo", e);
        }
    }

    /** Convierte una referencia en una ruta del disco, impidiendo salir del directorio base. */
    private Path resolver(String referencia) {
        Path p = raiz.resolve(referencia).normalize();
        if (!p.startsWith(raiz)) {
            throw new IllegalArgumentException("Referencia fuera del almacén");
        }
        return p;
    }
}
