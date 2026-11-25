package nexus.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import nexus.nexus.dto.UsuarioRequest;
import nexus.nexus.dto.UsuarioResponse;
import nexus.nexus.service.UsuarioService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Tag(name = "Usuarios", description = "Gestión de usuarios (solo ADMIN)")
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @Operation(summary = "Listar usuarios", description = "Solo ADMIN. Nunca incluye contraseñas.")
    @GetMapping
    public List<UsuarioResponse> listar() {
        return usuarioService.listar();
    }

    @Operation(summary = "Detalle de un usuario", description = "Solo ADMIN. 404 si no existe.")
    @GetMapping("/{id}")
    public UsuarioResponse obtener(@PathVariable Long id) {
        return usuarioService.obtener(id);
    }

    @Operation(summary = "Crear usuario", description = "Solo ADMIN. Roles: ADMIN, VENDEDOR o CLIENTE. 409 si el correo existe.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse crear(@Valid @RequestBody UsuarioRequest req) {
        return usuarioService.crear(req);
    }

    @Operation(summary = "Actualizar usuario",
            description = "Solo ADMIN. La contraseña es opcional (vacía = no cambia). No puedes cambiar tu propio rol.")
    @PutMapping("/{id}")
    public UsuarioResponse actualizar(@PathVariable Long id, @Valid @RequestBody UsuarioRequest req,
                                      Authentication auth) {
        return usuarioService.actualizar(id, req, auth.getName());
    }

    @Operation(summary = "Eliminar usuario",
            description = "Solo ADMIN. No puedes eliminar tu propia cuenta ni un usuario con pedidos (409).")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id, Authentication auth) {
        usuarioService.eliminar(id, auth.getName());
    }
}
