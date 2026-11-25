package nexus.nexus.service;

import nexus.nexus.dto.*;
import nexus.nexus.exception.ApiException;
import nexus.nexus.model.*;
import nexus.nexus.repository.BoletaRepository;
import nexus.nexus.repository.ProductoRepository;
import nexus.nexus.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BoletaServiceTest {

    private static final Long NRO = 20260001L;

    @Mock BoletaRepository boletaRepository;
    @Mock ProductoRepository productoRepository;
    @Mock UsuarioRepository usuarioRepository;
    @InjectMocks BoletaService service;

    private Usuario cliente;

    @BeforeEach
    void setUp() {
        // 0 = el pago simulado siempre se aprueba (cada test cambia esto si lo necesita)
        ReflectionTestUtils.setField(service, "probabilidadRechazo", 0.0);
        cliente = Usuario.builder().id(1L).nombre("Vicente").apellidos("Vasquez")
                .correo("vicente6@gmail.com").rol(Rol.CLIENTE).build();
    }

    // --- crear pedido ---

    @Test
    void crearCalculaElPrecioConDescuentoSoloSiElProductoEstaEnOferta() {
        when(usuarioRepository.findByCorreo(cliente.getCorreo())).thenReturn(Optional.of(cliente));
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto(1L, "En oferta", 100000, 10, true, 20)));
        when(productoRepository.findById(2L)).thenReturn(Optional.of(producto(2L, "Sin oferta", 50000, 10, false, 10)));
        when(boletaRepository.save(any(Boleta.class))).thenAnswer(i -> i.getArgument(0));

        PedidoResponse resp = service.crear(pedido(item(1L, 2), item(2L, 1)), cliente.getCorreo(), false);

        assertEquals(80000, resp.items().get(0).precio());      // 100000 con 20 % de descuento
        assertEquals(160000, resp.items().get(0).subtotal());
        assertEquals(50000, resp.items().get(1).precio());      // sin oferta, el descuento no aplica
        assertEquals(210000, resp.total());
        assertEquals("pendiente", resp.estado());
    }

    @Test
    void crearAplica15PorCientoSiEstaEnOfertaSinDescuento() {
        when(usuarioRepository.findByCorreo(cliente.getCorreo())).thenReturn(Optional.of(cliente));
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto(1L, "Oferta", 100000, 10, true, null)));
        when(boletaRepository.save(any(Boleta.class))).thenAnswer(i -> i.getArgument(0));

        PedidoResponse resp = service.crear(pedido(item(1L, 1)), cliente.getCorreo(), false);

        assertEquals(85000, resp.total());
    }

    @Test
    void crearNoDescuentaElStock() {
        Producto p = producto(1L, "Consola", 100000, 10, false, null);
        when(usuarioRepository.findByCorreo(cliente.getCorreo())).thenReturn(Optional.of(cliente));
        when(productoRepository.findById(1L)).thenReturn(Optional.of(p));
        when(boletaRepository.save(any(Boleta.class))).thenAnswer(i -> i.getArgument(0));

        service.crear(pedido(item(1L, 3)), cliente.getCorreo(), false);

        verify(productoRepository, never()).descontarStock(anyLong(), anyInt());
        assertEquals(10, p.getStock());
    }

    @Test
    void crearConStockInsuficienteDa409() {
        when(usuarioRepository.findByCorreo(cliente.getCorreo())).thenReturn(Optional.of(cliente));
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto(1L, "Consola", 100000, 1, false, null)));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.crear(pedido(item(1L, 2)), cliente.getCorreo(), false));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        verify(boletaRepository, never()).save(any(Boleta.class));
    }

    @Test
    void crearSumaLasCantidadesDeUnProductoRepetido() {
        when(usuarioRepository.findByCorreo(cliente.getCorreo())).thenReturn(Optional.of(cliente));
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto(1L, "Consola", 100000, 5, false, null)));

        // 3 + 4 = 7 unidades y solo hay 5
        ApiException ex = assertThrows(ApiException.class,
                () -> service.crear(pedido(item(1L, 3), item(1L, 4)), cliente.getCorreo(), false));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    void crearConProductoInexistenteDa400() {
        when(usuarioRepository.findByCorreo(cliente.getCorreo())).thenReturn(Optional.of(cliente));
        when(productoRepository.findById(99L)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class,
                () -> service.crear(pedido(item(99L, 1)), cliente.getCorreo(), false));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void unClienteSiempreCompraConSuPropioCorreo() {
        when(usuarioRepository.findByCorreo(cliente.getCorreo())).thenReturn(Optional.of(cliente));
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto(1L, "Consola", 100000, 5, false, null)));
        when(boletaRepository.save(any(Boleta.class))).thenAnswer(i -> i.getArgument(0));

        service.crear(pedido(item(1L, 1)), cliente.getCorreo(), false);

        ArgumentCaptor<Boleta> guardada = ArgumentCaptor.forClass(Boleta.class);
        verify(boletaRepository).save(guardada.capture());
        assertEquals("vicente6@gmail.com", guardada.getValue().getCorreo());
        assertEquals(cliente, guardada.getValue().getUsuario());
    }

    @Test
    void unAdminPuedeIndicarElCorreoDelCliente() {
        when(usuarioRepository.findByCorreo(cliente.getCorreo())).thenReturn(Optional.of(cliente));
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto(1L, "Consola", 100000, 5, false, null)));
        when(boletaRepository.save(any(Boleta.class))).thenAnswer(i -> i.getArgument(0));

        service.crear(pedido(item(1L, 1)), cliente.getCorreo(), true);

        ArgumentCaptor<Boleta> guardada = ArgumentCaptor.forClass(Boleta.class);
        verify(boletaRepository).save(guardada.capture());
        assertEquals("otro@correo.cl", guardada.getValue().getCorreo());
    }

    // --- pagar ---

    @Test
    void pagarAprobadoDescuentaElStockYQuedaPagado() {
        Boleta boleta = boletaPendiente(cliente, producto(1L, "Consola", 100000, 10, false, null), 2);
        when(boletaRepository.findById(NRO)).thenReturn(Optional.of(boleta));
        when(productoRepository.descontarStock(1L, 2)).thenReturn(1);

        PedidoResponse resp = service.pagar(NRO, cliente.getCorreo(), false);

        assertEquals("pagado", resp.estado());
        verify(productoRepository).descontarStock(1L, 2);
    }

    @Test
    void pagarRechazadoNoTocaElStock() {
        ReflectionTestUtils.setField(service, "probabilidadRechazo", 1.0);
        Boleta boleta = boletaPendiente(cliente, producto(1L, "Consola", 100000, 10, false, null), 2);
        when(boletaRepository.findById(NRO)).thenReturn(Optional.of(boleta));

        PedidoResponse resp = service.pagar(NRO, cliente.getCorreo(), false);

        assertEquals("rechazado", resp.estado());
        verify(productoRepository, never()).descontarStock(anyLong(), anyInt());
    }

    @Test
    void pagarSinStockSuficienteDa409YElPedidoSigueEnPendiente() {
        Boleta boleta = boletaPendiente(cliente, producto(1L, "Consola", 100000, 1, false, null), 2);
        when(boletaRepository.findById(NRO)).thenReturn(Optional.of(boleta));
        when(productoRepository.descontarStock(1L, 2)).thenReturn(0);

        ApiException ex = assertThrows(ApiException.class, () -> service.pagar(NRO, cliente.getCorreo(), false));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals(EstadoBoleta.PENDIENTE, boleta.getEstado());
    }

    @Test
    void pagarUnPedidoYaPagadoNoVuelveACobrar() {
        Boleta boleta = boletaPendiente(cliente, producto(1L, "Consola", 100000, 10, false, null), 2);
        boleta.setEstado(EstadoBoleta.PAGADO);
        when(boletaRepository.findById(NRO)).thenReturn(Optional.of(boleta));

        PedidoResponse resp = service.pagar(NRO, cliente.getCorreo(), false);

        assertEquals("pagado", resp.estado());
        verify(productoRepository, never()).descontarStock(anyLong(), anyInt());
    }

    @Test
    void pagarElPedidoDeOtroUsuarioDa404() {
        Boleta boleta = boletaPendiente(cliente, producto(1L, "Consola", 100000, 10, false, null), 1);
        when(boletaRepository.findById(NRO)).thenReturn(Optional.of(boleta));

        ApiException ex = assertThrows(ApiException.class, () -> service.pagar(NRO, "otro@correo.cl", false));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void unAdminPuedePagarElPedidoDeCualquierUsuario() {
        Boleta boleta = boletaPendiente(cliente, producto(1L, "Consola", 100000, 10, false, null), 1);
        when(boletaRepository.findById(NRO)).thenReturn(Optional.of(boleta));
        when(productoRepository.descontarStock(1L, 1)).thenReturn(1);

        PedidoResponse resp = service.pagar(NRO, "admin@gmail.com", true);

        assertEquals("pagado", resp.estado());
    }

    // --- consultar ---

    @Test
    void elVendedorPuedeVerElDetalleDeCualquierPedido() {
        Boleta boleta = boletaPendiente(cliente, producto(1L, "Consola", 100000, 10, false, null), 1);
        when(boletaRepository.findById(NRO)).thenReturn(Optional.of(boleta));

        PedidoResponse resp = service.obtener(NRO, "vendedor@gmail.com", true);

        assertEquals(NRO, resp.nro());
    }

    @Test
    void unClienteNoPuedeVerElPedidoDeOtro() {
        Boleta boleta = boletaPendiente(cliente, producto(1L, "Consola", 100000, 10, false, null), 1);
        when(boletaRepository.findById(NRO)).thenReturn(Optional.of(boleta));

        ApiException ex = assertThrows(ApiException.class, () -> service.obtener(NRO, "otro@correo.cl", false));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void misPedidosSoloTraeLosDelUsuario() {
        Boleta boleta = boletaPendiente(cliente, producto(1L, "Consola", 100000, 10, false, null), 1);
        when(usuarioRepository.findByCorreo(cliente.getCorreo())).thenReturn(Optional.of(cliente));
        when(boletaRepository.findByUsuarioIdOrderByFechaDesc(1L)).thenReturn(List.of(boleta));

        List<PedidoResponse> lista = service.mios(cliente.getCorreo());

        assertEquals(1, lista.size());
        verify(boletaRepository, never()).findAllByOrderByFechaDesc();
    }

    // --- datos de prueba ---

    private Producto producto(Long id, String nombre, int precio, int stock, boolean oferta, Integer descuento) {
        return Producto.builder().id(id).nombre(nombre).precio(precio).stock(stock)
                .oferta(oferta).descuento(descuento).imagen("consola.webp")
                .categoria(Categoria.builder().id(1L).nombre("Sony").build()).build();
    }

    private ItemPedidoRequest item(Long consolaId, int cantidad) {
        return new ItemPedidoRequest(consolaId, cantidad);
    }

    private PedidoRequest pedido(ItemPedidoRequest... items) {
        return new PedidoRequest(
                new ClientePedido("Vicente", "Vasquez", "otro@correo.cl"),
                new EntregaPedido("Av. Siempre Viva 123", null, "Metropolitana", "Santiago", null),
                List.of(items));
    }

    private Boleta boletaPendiente(Usuario dueno, Producto producto, int cantidad) {
        Boleta boleta = Boleta.builder().nro(NRO).fecha(Instant.now()).estado(EstadoBoleta.PENDIENTE)
                .total(producto.getPrecio() * cantidad).usuario(dueno)
                .nombre("Vicente").apellidos("Vasquez").correo(dueno.getCorreo())
                .calle("Av. Siempre Viva 123").region("Metropolitana").comuna("Santiago").build();
        boleta.agregarDetalle(DetalleBoleta.builder().producto(producto).nombre(producto.getNombre())
                .precio(producto.getPrecio()).cantidad(cantidad)
                .subtotal(producto.getPrecio() * cantidad).build());
        return boleta;
    }
}
