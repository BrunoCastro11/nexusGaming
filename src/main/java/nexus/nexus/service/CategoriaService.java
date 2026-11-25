package nexus.nexus.service;

import lombok.RequiredArgsConstructor;
import nexus.nexus.dto.CategoriaRequest;
import nexus.nexus.dto.CategoriaResponse;
import nexus.nexus.exception.ApiException;
import nexus.nexus.model.Categoria;
import nexus.nexus.repository.CategoriaRepository;
import nexus.nexus.repository.ProductoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final ProductoRepository productoRepository;

    @Transactional(readOnly = true)
    public List<CategoriaResponse> listar() {
        return categoriaRepository.findAll().stream().map(CategoriaResponse::de).toList();
    }

    @Transactional
    public CategoriaResponse crear(CategoriaRequest req) {
        String nombre = nombreValido(req.nombre());
        if (categoriaRepository.existsByNombreIgnoreCase(nombre)) {
            throw new ApiException(HttpStatus.CONFLICT, "Esa categoría ya existe");
        }
        return CategoriaResponse.de(categoriaRepository.save(Categoria.builder().nombre(nombre).build()));
    }

    /** Renombrar la categoría actualiza también a sus consolas, porque comparten la misma fila. */
    @Transactional
    public CategoriaResponse actualizar(Long id, CategoriaRequest req) {
        Categoria categoria = buscar(id);
        String nombre = nombreValido(req.nombre());
        categoriaRepository.findByNombreIgnoreCase(nombre)
                .filter(otra -> !otra.getId().equals(id))
                .ifPresent(otra -> {
                    throw new ApiException(HttpStatus.CONFLICT, "Esa categoría ya existe");
                });
        categoria.setNombre(nombre);
        return CategoriaResponse.de(categoriaRepository.save(categoria));
    }

    @Transactional
    public void eliminar(Long id) {
        Categoria categoria = buscar(id);
        if (productoRepository.existsByCategoriaId(id)) {
            throw new ApiException(HttpStatus.CONFLICT, "No se puede eliminar: hay productos en esta categoría");
        }
        categoriaRepository.delete(categoria);
    }

    private Categoria buscar(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Categoría no encontrada"));
    }

    private String nombreValido(String nombre) {
        String limpio = nombre == null ? "" : nombre.trim();
        if (limpio.length() < 2) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El nombre debe tener al menos 2 caracteres");
        }
        return limpio;
    }
}
