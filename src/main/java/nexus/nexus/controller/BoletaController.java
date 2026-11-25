package nexus.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import nexus.nexus.dto.PedidoRequest;
import nexus.nexus.dto.PedidoResponse;
import nexus.nexus.service.BoletaService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Tag(name = "Pedidos", description = "Pedidos (boletas) y pagos")
@RequestMapping("/api/v1/pedidos")
@RequiredArgsConstructor
public class BoletaController {

    private final BoletaService boletaService;

    @Operation(summary = "Crear pedido",
            description = "CLIENTE o ADMIN. Calcula precios y total en el servidor y valida el stock. Queda en estado pendiente.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PedidoResponse crear(@Valid @RequestBody PedidoRequest req, Authentication auth) {
        return boletaService.crear(req, auth.getName(), tieneRol(auth, "ADMIN"));
    }

    @Operation(summary = "Pagar pedido",
            description = "Dueño del pedido o ADMIN. Pago simulado: si se aprueba descuenta el stock; si no, queda rechazado.")
    @PostMapping("/{nro}/pagar")
    public PedidoResponse pagar(@PathVariable Long nro, Authentication auth) {
        return boletaService.pagar(nro, auth.getName(), tieneRol(auth, "ADMIN"));
    }

    @Operation(summary = "Mis pedidos", description = "Pedidos del usuario autenticado.")
    @GetMapping("/mios")
    public List<PedidoResponse> mios(Authentication auth) {
        return boletaService.mios(auth.getName());
    }

    @Operation(summary = "Listar todos los pedidos", description = "ADMIN y VENDEDOR.")
    @GetMapping
    public List<PedidoResponse> todos() {
        return boletaService.todos();
    }

    @Operation(summary = "Detalle de un pedido",
            description = "Su dueño, ADMIN y VENDEDOR. 404 si no existe o no tienes acceso.")
    @GetMapping("/{nro}")
    public PedidoResponse obtener(@PathVariable Long nro, Authentication auth) {
        boolean puedeVerTodos = tieneRol(auth, "ADMIN") || tieneRol(auth, "VENDEDOR");
        return boletaService.obtener(nro, auth.getName(), puedeVerTodos);
    }

    private boolean tieneRol(Authentication auth, String rol) {
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_" + rol));
    }
}
