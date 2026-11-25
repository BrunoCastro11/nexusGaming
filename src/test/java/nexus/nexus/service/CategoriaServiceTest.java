package nexus.nexus.service;

import nexus.nexus.dto.CategoriaRequest;
import nexus.nexus.dto.CategoriaResponse;
import nexus.nexus.exception.ApiException;
import nexus.nexus.model.Categoria;
import nexus.nexus.repository.CategoriaRepository;
import nexus.nexus.repository.ProductoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoriaServiceTest {

    @Mock CategoriaRepository categoriaRepository;
    @Mock ProductoRepository productoRepository;
    @InjectMocks CategoriaService service;

    private Categoria sony() {
        return Categoria.builder().id(1L).nombre("Sony").build();
    }

    @Test
    void noSePuedeEliminarUnaCategoriaConProductos() {
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(sony()));
        when(productoRepository.existsByCategoriaId(1L)).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class, () -> service.eliminar(1L));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        verify(categoriaRepository, never()).delete(any(Categoria.class));
    }

    @Test
    void seEliminaUnaCategoriaSinProductos() {
        Categoria categoria = sony();
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(productoRepository.existsByCategoriaId(1L)).thenReturn(false);

        service.eliminar(1L);

        verify(categoriaRepository).delete(categoria);
    }

    @Test
    void eliminarUnaCategoriaInexistenteDa404() {
        when(categoriaRepository.findById(9L)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> service.eliminar(9L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void noSePuedeCrearUnaCategoriaDuplicada() {
        when(categoriaRepository.existsByNombreIgnoreCase("Sony")).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class, () -> service.crear(new CategoriaRequest("Sony")));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        verify(categoriaRepository, never()).save(any(Categoria.class));
    }

    @Test
    void noSePuedeCrearUnaCategoriaConNombreCorto() {
        ApiException ex = assertThrows(ApiException.class, () -> service.crear(new CategoriaRequest(" a ")));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void noSePuedeRenombrarConElNombreDeOtraCategoria() {
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(sony()));
        when(categoriaRepository.findByNombreIgnoreCase("Nintendo"))
                .thenReturn(Optional.of(Categoria.builder().id(2L).nombre("Nintendo").build()));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.actualizar(1L, new CategoriaRequest("Nintendo")));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    void seRenombraUnaCategoria() {
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(sony()));
        when(categoriaRepository.findByNombreIgnoreCase("PlayStation")).thenReturn(Optional.empty());
        when(categoriaRepository.save(any(Categoria.class))).thenAnswer(i -> i.getArgument(0));

        CategoriaResponse resp = service.actualizar(1L, new CategoriaRequest("  PlayStation "));

        assertEquals("PlayStation", resp.nombre());
    }
}
