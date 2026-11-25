package nexus.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import nexus.nexus.dto.ConsolaRequest;
import nexus.nexus.dto.ConsolaResponse;
import nexus.nexus.service.ProductoService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Tag(name = "Consolas", description = "Catálogo de productos (consolas)")
@RequestMapping("/api/v1/consolas")
@RequiredArgsConstructor
public class ProductoController {

    private final ProductoService productoService;

    @SecurityRequirements
    @Operation(summary = "Listar consolas", description = "Público.")
    @GetMapping
    public List<ConsolaResponse> listar() {
        return productoService.listar();
    }

    @SecurityRequirements
    @Operation(summary = "Detalle de una consola", description = "Público. 404 si no existe.")
    @GetMapping("/{id}")
    public ConsolaResponse obtener(@PathVariable Long id) {
        return productoService.obtener(id);
    }

    @Operation(summary = "Crear consola", description = "Solo ADMIN. La categoría se indica por su nombre.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConsolaResponse crear(@Valid @RequestBody ConsolaRequest req) {
        return productoService.crear(req);
    }

    @Operation(summary = "Actualizar consola", description = "Solo ADMIN. Reemplaza los campos de la consola.")
    @PutMapping("/{id}")
    public ConsolaResponse actualizar(@PathVariable Long id, @Valid @RequestBody ConsolaRequest req) {
        return productoService.actualizar(id, req);
    }

    @Operation(summary = "Eliminar consola", description = "Solo ADMIN. 409 si ya tiene pedidos asociados.")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        productoService.eliminar(id);
    }
}
