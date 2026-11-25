package nexus.nexus.service;

import lombok.RequiredArgsConstructor;
import nexus.nexus.dto.ConsolaRequest;
import nexus.nexus.dto.ConsolaResponse;
import nexus.nexus.exception.ApiException;
import nexus.nexus.model.Categoria;
import nexus.nexus.model.Producto;
import nexus.nexus.repository.CategoriaRepository;
import nexus.nexus.repository.DetalleBoletaRepository;
import nexus.nexus.repository.ProductoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final DetalleBoletaRepository detalleBoletaRepository;

    @Transactional(readOnly = true)
    public List<ConsolaResponse> listar() {
        return productoRepository.findAll().stream().map(ConsolaResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public ConsolaResponse obtener(Long id) {
        return ConsolaResponse.de(buscar(id));
    }

    @Transactional
    public ConsolaResponse crear(ConsolaRequest req) {
        Producto producto = Producto.builder()
                .nombre(nombreValido(req.nombre()))
                .precio(req.precio())
                .stock(req.stock())
                .categoria(buscarCategoria(req.categoria()))
                .oferta(Boolean.TRUE.equals(req.oferta()))
                .descuento(req.descuento())
                .imagen(req.imagen() == null ? "" : req.imagen().trim())
                .build();
        return ConsolaResponse.de(productoRepository.save(producto));
    }

    /** Reemplaza los campos de la consola. Si no se envía imagen, se conserva la actual. */
    @Transactional
    public ConsolaResponse actualizar(Long id, ConsolaRequest req) {
        Producto producto = buscar(id);
        producto.setNombre(nombreValido(req.nombre()));
        producto.setPrecio(req.precio());
        producto.setStock(req.stock());
        producto.setCategoria(buscarCategoria(req.categoria()));
        producto.setOferta(Boolean.TRUE.equals(req.oferta()));
        producto.setDescuento(req.descuento());
        if (req.imagen() != null) {
            producto.setImagen(req.imagen().trim());
        }
        return ConsolaResponse.de(productoRepository.save(producto));
    }

    @Transactional
    public void eliminar(Long id) {
        Producto producto = buscar(id);
        if (detalleBoletaRepository.existsByProductoId(id)) {
            throw new ApiException(HttpStatus.CONFLICT, "No se puede eliminar: el producto tiene pedidos asociados");
        }
        productoRepository.delete(producto);
    }

    private Producto buscar(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Consola no encontrada"));
    }

    private Categoria buscarCategoria(String nombre) {
        return categoriaRepository.findByNombreIgnoreCase(nombre.trim())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "La categoría no existe"));
    }

    private String nombreValido(String nombre) {
        String limpio = nombre == null ? "" : nombre.trim();
        if (limpio.length() < 3) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El nombre debe tener al menos 3 caracteres");
        }
        return limpio;
    }
}
