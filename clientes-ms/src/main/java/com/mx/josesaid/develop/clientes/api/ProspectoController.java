package com.mx.josesaid.develop.clientes.api;

import com.mx.josesaid.develop.clientes.api.ApiDtos.CambioEstatusProspectoRequest;
import com.mx.josesaid.develop.clientes.api.ApiDtos.ClienteResponse;
import com.mx.josesaid.develop.clientes.api.ApiDtos.ConvertirProspectoRequest;
import com.mx.josesaid.develop.clientes.api.ApiDtos.NuevoProspectoRequest;
import com.mx.josesaid.develop.clientes.api.ApiDtos.ProspectoResponse;
import com.mx.josesaid.develop.clientes.application.Auditoria;
import com.mx.josesaid.develop.clientes.application.Comandos;
import com.mx.josesaid.develop.clientes.application.ProspectoService;
import com.mx.josesaid.develop.clientes.domain.Prospecto;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.UUID;

@RestController
@RequestMapping("/v1/prospectos")
public class ProspectoController {

    private final ProspectoService servicio;
    private final Auditoria auditoria;
    private final Presentacion presentacion;

    public ProspectoController(ProspectoService servicio, Auditoria auditoria, Presentacion presentacion) {
        this.servicio = servicio;
        this.auditoria = auditoria;
        this.presentacion = presentacion;
    }

    @PostMapping
    @PreAuthorize("hasRole('VENDEDOR')")
    public ResponseEntity<ProspectoResponse> crear(@Valid @RequestBody NuevoProspectoRequest req,
                                                   @RequestParam(defaultValue = "false") boolean confirmarDuplicado) {
        Prospecto creado = servicio.crear(new Comandos.NuevoProspecto(
                req.nombre(), req.correo(), req.rfc(), req.fuente(), confirmarDuplicado));
        return ResponseEntity.created(URI.create("/v1/prospectos/" + creado.getId()))
                .body(ProspectoResponse.de(creado, presentacion));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('VENDEDOR', 'GERENTE_VENTAS')")
    public ProspectoResponse obtener(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        Prospecto prospecto = servicio.obtener(id);
        auditoria.registrar("consulta_prospecto", id, Actor.de(jwt));
        return ProspectoResponse.de(prospecto, presentacion);
    }

    @PatchMapping("/{id}/estatus")
    @PreAuthorize("hasRole('VENDEDOR')")
    public ProspectoResponse cambiarEstatus(@PathVariable UUID id,
                                            @Valid @RequestBody CambioEstatusProspectoRequest req) {
        return ProspectoResponse.de(servicio.cambiarEstatus(id, req.estatus()), presentacion);
    }

    @PostMapping("/{id}/conversion")
    @PreAuthorize("hasAnyRole('VENDEDOR', 'ADMIN_CLIENTES')")
    public ResponseEntity<ClienteResponse> convertir(@PathVariable UUID id,
                                                     @Valid @RequestBody ConvertirProspectoRequest req) {
        var contacto = req.contactoPrincipal();
        var cliente = servicio.convertir(id, new Comandos.ConvertirProspecto(req.razonSocial(), req.rfc(),
                new Comandos.DatosContacto(contacto.nombre(), contacto.correo(), contacto.rol())));
        return ResponseEntity.created(URI.create("/v1/clientes/" + cliente.getId()))
                .body(ClienteResponse.de(cliente));
    }
}
