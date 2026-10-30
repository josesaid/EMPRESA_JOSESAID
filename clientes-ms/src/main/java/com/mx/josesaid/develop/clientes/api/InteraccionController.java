package com.mx.josesaid.develop.clientes.api;

import com.mx.josesaid.develop.clientes.api.ApiDtos.InteraccionResponse;
import com.mx.josesaid.develop.clientes.api.ApiDtos.NuevaInteraccionRequest;
import com.mx.josesaid.develop.clientes.application.Auditoria;
import com.mx.josesaid.develop.clientes.application.Comandos;
import com.mx.josesaid.develop.clientes.application.InteraccionService;
import com.mx.josesaid.develop.clientes.domain.Interaccion;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/clientes/{clienteId}/interacciones")
public class InteraccionController {

    private final InteraccionService servicio;
    private final Auditoria auditoria;
    private final Presentacion presentacion;

    public InteraccionController(InteraccionService servicio, Auditoria auditoria, Presentacion presentacion) {
        this.servicio = servicio;
        this.auditoria = auditoria;
        this.presentacion = presentacion;
    }

    @PostMapping
    @PreAuthorize("hasRole('VENTAS')")
    public ResponseEntity<InteraccionResponse> registrar(@PathVariable UUID clienteId,
                                                         @Valid @RequestBody NuevaInteraccionRequest req,
                                                         @AuthenticationPrincipal Jwt jwt) {
        Interaccion creada = servicio.registrar(clienteId,
                new Comandos.NuevaInteraccion(req.tipo(), req.fecha(), req.resumen()), Actor.de(jwt));
        return ResponseEntity.created(URI.create("/v1/clientes/" + clienteId + "/interacciones/" + creada.getId()))
                .body(InteraccionResponse.de(creada, presentacion));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('VENTAS')")
    public PagedModel<InteraccionResponse> listar(
            @PathVariable UUID clienteId,
            @PageableDefault(size = 20, sort = "fecha", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal Jwt jwt) {
        Page<Interaccion> pagina = servicio.listar(clienteId, pageable);
        auditoria.registrar("consulta_interacciones", clienteId, Actor.de(jwt));
        return new PagedModel<>(pagina.map(i -> InteraccionResponse.de(i, presentacion)));
    }
}
