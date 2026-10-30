package com.mx.josesaid.develop.pedidos.infrastructure.clientes;

import com.mx.josesaid.develop.pedidos.application.ValidadorCliente;
import com.mx.josesaid.develop.pedidos.domain.ReglaDeNegocioException;
import com.mx.josesaid.develop.pedidos.domain.ServicioNoDisponibleException;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.ObjectMapper;

/**
 * Consulta {@code GET /v1/clientes/{id}} en clientes-ms con el mismo token del usuario que hizo
 * la petición. clientes-ms exige rol VENTAS o ADMIN, igual que la operación de pedidos.
 */
@Component
public class ClientesRestValidador implements ValidadorCliente {

    private final RestClient http;
    private final ObjectMapper mapper;

    public ClientesRestValidador(RestClient.Builder builder,
                                 @Value("${clientes.url:http://clientes-ms:8080}") String url,
                                 ObjectMapper mapper) {
        this.http = builder.baseUrl(url).build();
        this.mapper = mapper;
    }

    @Override
    public void exigirActivo(UUID clienteId) {
        String cuerpo;
        try {
            cuerpo = http.get()
                    .uri("/v1/clientes/{id}", clienteId)
                    .header("Authorization", "Bearer " + tokenActual())
                    .retrieve()
                    .body(String.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ReglaDeNegocioException("El cliente no existe");
        } catch (RestClientResponseException e) {
            throw new ServicioNoDisponibleException(
                    "clientes-ms respondió " + e.getStatusCode().value() + "; intente de nuevo");
        } catch (RestClientException e) {
            throw new ServicioNoDisponibleException("No se pudo consultar clientes-ms; intente de nuevo");
        }
        Map<?, ?> respuesta;
        try {
            respuesta = mapper.readValue(cuerpo, Map.class);
        } catch (RuntimeException e) {
            throw new ServicioNoDisponibleException("Respuesta inválida de clientes-ms; intente de nuevo");
        }
        if (respuesta == null) {
            throw new ServicioNoDisponibleException("Respuesta vacía de clientes-ms; intente de nuevo");
        }
        if (!"ACTIVO".equals(respuesta.get("estatus"))) {
            throw new ReglaDeNegocioException("El cliente no está activo");
        }
    }

    private static String tokenActual() {
        var autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion instanceof JwtAuthenticationToken jwt) {
            return jwt.getToken().getTokenValue();
        }
        throw new ServicioNoDisponibleException("No hay token para consultar clientes-ms");
    }
}
