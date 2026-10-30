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

    public AlmacenArchivosLocal(@Value("${documentos.almacen.directorio}") String directorio,
                                ProtectorArchivos protector) {
        this.raiz = Path.of(directorio).toAbsolutePath().normalize();
        this.protector = protector;
    }

    @Override
    public String referencia(UUID documentoId, int numero) {
        return documentoId + "/v" + numero + ".bin";
    }

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

    @Override
    public byte[] leer(String referencia) {
        try {
            return protector.descifrar(Files.readAllBytes(resolver(referencia)));
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer el archivo", e);
        }
    }

    private Path resolver(String referencia) {
        Path p = raiz.resolve(referencia).normalize();
        if (!p.startsWith(raiz)) {
            throw new IllegalArgumentException("Referencia fuera del almacén");
        }
        return p;
    }
}
