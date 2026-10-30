package com.mx.josesaid.develop.compras.infrastructure.idempotencia;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mx.josesaid.develop.compras.domain.ConflictoException;
import com.mx.josesaid.develop.compras.domain.ReglaDeNegocioException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.function.Supplier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * Hace seguras de reintentar las altas que reciben la cabecera Idempotency-Key.
 * Misma clave y misma petición: devuelve la respuesta original, sin crear nada nuevo.
 * Misma clave con otra petición: 422. Clave todavía en proceso: 409.
 * Sin cabecera: la operación se ejecuta como siempre.
 */
@Component
public class Idempotencia {

    public static final String CABECERA = "Idempotency-Key";

    private final RegistroIdempotencia registro;
    private final ObjectMapper mapper;

    public Idempotencia(RegistroIdempotencia registro, ObjectMapper mapper) {
        this.registro = registro;
        this.mapper = mapper;
    }

    public <T> ResponseEntity<?> ejecutar(String clave, String ruta, Object peticion,
                                          HttpStatus estado, Supplier<T> operacion) {
        if (clave == null || clave.isBlank()) {
            return ResponseEntity.status(estado).body(operacion.get());
        }
        String huella = huella(ruta, peticion);
        if (!registro.reservar(clave, huella)) {
            RegistroIdempotencia.Registro previo = registro.buscar(clave)
                    .orElseThrow(() -> new ConflictoException("La Idempotency-Key ya no está disponible. Intente de nuevo."));
            if (!previo.huella().equals(huella)) {
                throw new ReglaDeNegocioException("La Idempotency-Key ya se usó con una petición distinta.");
            }
            if (!previo.completada()) {
                throw new ConflictoException("La petición con esta Idempotency-Key aún se está procesando.");
            }
            return ResponseEntity.status(previo.estatus())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(previo.cuerpo());
        }
        T cuerpo;
        try {
            cuerpo = operacion.get();
        } catch (RuntimeException e) {
            registro.liberar(clave);
            throw e;
        }
        registro.completar(clave, estado.value(), serializar(cuerpo));
        return ResponseEntity.status(estado).body(cuerpo);
    }

    private String huella(String ruta, Object peticion) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest((ruta + "\n" + mapper.writeValueAsString(peticion)).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (JsonProcessingException | NoSuchAlgorithmException e) {
            throw new IllegalStateException("No se pudo calcular la huella de la petición", e);
        }
    }

    private String serializar(Object cuerpo) {
        try {
            return mapper.writeValueAsString(cuerpo);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo guardar la respuesta idempotente", e);
        }
    }
}
