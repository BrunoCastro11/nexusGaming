package nexus.nexus.service;

import nexus.nexus.dto.ConsolaRequest;
import nexus.nexus.dto.ConsolaResponse;
import nexus.nexus.exception.ApiException;
import nexus.nexus.model.Categoria;
import nexus.nexus.model.Producto;
import nexus.nexus.repository.CategoriaRepository;
import nexus.nexus.repository.DetalleBoletaRepository;
import nexus.nexus.repository.ProductoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    @Mock ProductoRepository productoRepository;
    @Mock CategoriaRepository categoriaRepository;
    @Mock DetalleBoletaRepository detalleBoletaRepository;
    @InjectMocks ProductoService service;

    private Categoria sony() {
        return Categoria.builder().id(1L).nombre("Sony").build();
    }

    private Producto ps5() {
        return Producto.builder().id(1L).nombre("Playstation 5").precio(659990).stock(10)
                .oferta(false).imagen("ps5.webp").categoria(sony()).build();
    }

    @Test
    void crearConUnaCategoriaQueNoExisteDa400() {
        when(categoriaRepository.findByNombreIgnoreCase("Sega")).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class,
                () -> service.crear(new ConsolaRequest("Dreamcast", 100000, "Sega", 3, false, null, null)));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verify(productoRepository, never()).save(any(Producto.class));
    }

    @Test
    void crearGuardaLaConsolaConSuCategoria() {
        when(categoriaRepository.findByNombreIgnoreCase("Sony")).thenReturn(Optional.of(sony()));
        when(productoRepository.save(any(Producto.class))).thenAnswer(i -> i.getArgument(0));

        ConsolaResponse resp = service.crear(
                new ConsolaRequest("  Playstation 3 ", 150000, "Sony", 4, null, null, null));

        assertEquals("Playstation 3", resp.nombre());
        assertEquals("Sony", resp.categoria());
        assertFalse(resp.oferta());
        assertEquals(4, resp.stock());
    }

    @Test
    void crearConNombreCortoDa400() {
        ApiException ex = assertThrows(ApiException.class,
                () -> service.crear(new ConsolaRequest("ab", 100000, "Sony", 3, false, null, null)));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void actualizarSinImagenConservaLaActual() {
        when(productoRepository.findById(1L)).thenReturn(Optional.of(ps5()));
        when(categoriaRepository.findByNombreIgnoreCase("Sony")).thenReturn(Optional.of(sony()));
        when(productoRepository.save(any(Producto.class))).thenAnswer(i -> i.getArgument(0));

        ConsolaResponse resp = service.actualizar(1L,
                new ConsolaRequest("Playstation 5", 600000, "Sony", 8, true, 10, null));

        assertEquals("ps5.webp", resp.imagen());
        assertEquals(600000, resp.precio());
        assertTrue(resp.oferta());
    }

    @Test
    void noSePuedeEliminarUnaConsolaQueYaFueVendida() {
        when(productoRepository.findById(1L)).thenReturn(Optional.of(ps5()));
        when(detalleBoletaRepository.existsByProductoId(1L)).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class, () -> service.eliminar(1L));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        verify(productoRepository, never()).delete(any(Producto.class));
    }

    @Test
    void seEliminaUnaConsolaSinVentas() {
        Producto producto = ps5();
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        when(detalleBoletaRepository.existsByProductoId(1L)).thenReturn(false);

        service.eliminar(1L);

        verify(productoRepository).delete(producto);
    }

    @Test
    void obtenerUnaConsolaInexistenteDa404() {
        when(productoRepository.findById(99L)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> service.obtener(99L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }
}
