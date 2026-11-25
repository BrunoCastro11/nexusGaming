package nexus.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import nexus.nexus.dto.CategoriaRequest;
import nexus.nexus.dto.CategoriaResponse;
import nexus.nexus.service.CategoriaService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Tag(name = "Categorías", description = "Gestión de categorías de productos")
@RequestMapping("/api/v1/categorias")
@RequiredArgsConstructor
public class CategoriaController {

    private final CategoriaService categoriaService;

    @SecurityRequirements
    @Operation(summary = "Listar categorías", description = "Público.")
    @GetMapping
    public List<CategoriaResponse> listar() {
        return categoriaService.listar();
    }

    @Operation(summary = "Crear categoría", description = "Solo ADMIN. 409 si el nombre ya existe.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoriaResponse crear(@Valid @RequestBody CategoriaRequest req) {
        return categoriaService.crear(req);
    }

    @Operation(summary = "Renombrar categoría", description = "Solo ADMIN. Las consolas de la categoría quedan con el nuevo nombre.")
    @PutMapping("/{id}")
    public CategoriaResponse actualizar(@PathVariable Long id, @Valid @RequestBody CategoriaRequest req) {
        return categoriaService.actualizar(id, req);
    }

    @Operation(summary = "Eliminar categoría", description = "Solo ADMIN. 409 si todavía tiene productos.")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        categoriaService.eliminar(id);
    }
}
