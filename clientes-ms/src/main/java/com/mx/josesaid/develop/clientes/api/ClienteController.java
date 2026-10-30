package com.mx.josesaid.develop.clientes.api;

import com.mx.josesaid.develop.clientes.api.ApiDtos.ActualizarClienteRequest;
import com.mx.josesaid.develop.clientes.api.ApiDtos.CambioEstatusClienteRequest;
import com.mx.josesaid.develop.clientes.api.ApiDtos.ClienteResponse;
import com.mx.josesaid.develop.clientes.api.ApiDtos.ClienteResumenResponse;
import com.mx.josesaid.develop.clientes.api.ApiDtos.ContactoRequest;
import com.mx.josesaid.develop.clientes.api.ApiDtos.ContactoResponse;
import com.mx.josesaid.develop.clientes.api.ApiDtos.InteraccionResponse;
import com.mx.josesaid.develop.clientes.api.ApiDtos.NuevoClienteRequest;
import com.mx.josesaid.develop.clientes.application.Auditoria;
import com.mx.josesaid.develop.clientes.application.ClienteService;
import com.mx.josesaid.develop.clientes.application.Comandos;
import com.mx.josesaid.develop.clientes.domain.Cliente;
import com.mx.josesaid.develop.clientes.domain.Contacto;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/clientes")
public class ClienteController {

    private final ClienteService servicio;
    private final Auditoria auditoria;
    private final Presentacion presentacion;

    public ClienteController(ClienteService servicio, Auditoria auditoria, Presentacion presentacion) {
        this.servicio = servicio;
        this.auditoria = auditoria;
        this.presentacion = presentacion;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN_CLIENTES')")
    public ResponseEntity<ClienteResponse> crear(@Valid @RequestBody NuevoClienteRequest req,
                                                 @RequestParam(defaultValue = "false") boolean confirmarDuplicado) {
        var contacto = req.contactoPrincipal();
        Cliente creado = servicio.crear(new Comandos.NuevoCliente(req.razonSocial(), req.rfc(),
                new Comandos.DatosContacto(contacto.nombre(), contacto.correo(), contacto.rol()), confirmarDuplicado));
        return ResponseEntity.created(URI.create("/v1/clientes/" + creado.getId()))
                .body(ClienteResponse.de(creado));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('VENDEDOR', 'GERENTE_VENTAS', 'ADMIN_CLIENTES')")
    public PagedModel<ClienteResumenResponse> buscar(
            @RequestParam(required = false) String rfc,
            @RequestParam(required = false) String nombre,
            @PageableDefault(size = 20, sort = "razonSocial", direction = Sort.Direction.ASC) Pageable pageable,
            @AuthenticationPrincipal Jwt jwt) {
        Page<Cliente> pagina = servicio.buscar(rfc, nombre, pageable);
        auditoria.registrar("busqueda_clientes", null, Actor.de(jwt));
        return new PagedModel<>(pagina.map(c -> new ClienteResumenResponse(
                ClienteResponse.de(c),
                servicio.ultimaInteraccion(c.getId())
                        .map(i -> InteraccionResponse.de(i, presentacion))
                        .orElse(null))));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('VENDEDOR', 'GERENTE_VENTAS', 'ADMIN_CLIENTES')")
    public ClienteResponse obtener(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        Cliente cliente = servicio.obtener(id);
        auditoria.registrar("consulta_cliente", id, Actor.de(jwt));
        return ClienteResponse.de(cliente);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_CLIENTES')")
    public ClienteResponse actualizar(@PathVariable UUID id, @Valid @RequestBody ActualizarClienteRequest req) {
        return ClienteResponse.de(servicio.actualizarRazonSocial(id, req.razonSocial()));
    }

    @PatchMapping("/{id}/estatus")
    @PreAuthorize("hasRole('ADMIN_CLIENTES')")
    public ClienteResponse cambiarEstatus(@PathVariable UUID id, @Valid @RequestBody CambioEstatusClienteRequest req) {
        return ClienteResponse.de(servicio.cambiarEstatus(id, req.estatus()));
    }

    @PostMapping("/{id}/contactos")
    @PreAuthorize("hasRole('ADMIN_CLIENTES')")
    public ResponseEntity<ContactoResponse> agregarContacto(@PathVariable UUID id,
                                                            @Valid @RequestBody ContactoRequest req) {
        Contacto contacto = servicio.agregarContacto(id, new Comandos.NuevoContacto(
                req.nombre(), req.correo(), req.rol(), req.principal()));
        return ResponseEntity.status(201).body(ContactoResponse.de(contacto));
    }
}
