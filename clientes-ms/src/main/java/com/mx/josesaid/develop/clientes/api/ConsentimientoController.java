package com.mx.josesaid.develop.clientes.api;

import com.mx.josesaid.develop.clientes.api.ApiDtos.ConsentimientoResponse;
import com.mx.josesaid.develop.clientes.api.ApiDtos.NuevoConsentimientoRequest;
import com.mx.josesaid.develop.clientes.application.Comandos;
import com.mx.josesaid.develop.clientes.application.ConsentimientoService;
import com.mx.josesaid.develop.clientes.domain.Consentimiento;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ConsentimientoController {

    private final ConsentimientoService servicio;
    private final Presentacion presentacion;

    public ConsentimientoController(ConsentimientoService servicio, Presentacion presentacion) {
        this.servicio = servicio;
        this.presentacion = presentacion;
    }

    @PostMapping("/v1/contactos/{contactoId}/consentimientos")
    @PreAuthorize("hasRole('LEGAL')")
    public ResponseEntity<ConsentimientoResponse> otorgar(@PathVariable UUID contactoId,
                                                          @Valid @RequestBody NuevoConsentimientoRequest req) {
        Consentimiento otorgado = servicio.otorgar(contactoId, new Comandos.NuevoConsentimiento(
                req.finalidad(), req.fechaOtorgado(), req.medio(), req.vigencia()));
        return ResponseEntity.status(201).body(ConsentimientoResponse.de(otorgado, presentacion));
    }

    @PostMapping("/v1/consentimientos/{id}/revocacion")
    @PreAuthorize("hasRole('LEGAL')")
    public ConsentimientoResponse revocar(@PathVariable UUID id) {
        return ConsentimientoResponse.de(servicio.revocar(id), presentacion);
    }
}
