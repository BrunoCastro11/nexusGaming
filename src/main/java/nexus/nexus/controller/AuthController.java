package nexus.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import nexus.nexus.dto.*;
import nexus.nexus.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "Autenticación", description = "Registro, login, refresh de token y usuario actual")
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @SecurityRequirements
    @Operation(summary = "Registrar un cliente nuevo", description = "Crea la cuenta con rol CLIENTE y devuelve la sesión.")
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> registrar(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrar(req));
    }

    @SecurityRequirements
    @Operation(summary = "Iniciar sesión", description = "Devuelve el token JWT, el refresh token y el usuario.")
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }

    @SecurityRequirements
    @Operation(summary = "Renovar el token", description = "Intercambia un refresh token por un token y un refresh token nuevos (rotación).")
    @PostMapping("/refresh")
    public TokenResponse refrescar(@Valid @RequestBody RefreshRequest req) {
        return authService.refrescar(req);
    }

    @Operation(summary = "Usuario autenticado", description = "Devuelve los datos del usuario dueño del token.")
    @GetMapping("/me")
    public UsuarioResponse me(Authentication authentication) {
        return authService.perfil(authentication.getName());
    }
}
