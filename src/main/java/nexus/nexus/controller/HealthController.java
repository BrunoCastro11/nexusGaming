package nexus.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Tag(name = "Salud", description = "Verificación de que la API está en línea")
@RequestMapping("/api/v1/health")
public class HealthController {

    @SecurityRequirements
    @Operation(summary = "Estado de la API", description = "Público. No consulta la base de datos.")
    @GetMapping
    public Map<String, String> estado() {
        return Map.of("estado", "ok");
    }
}