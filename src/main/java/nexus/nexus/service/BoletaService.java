package nexus.nexus.service;

import lombok.RequiredArgsConstructor;
import nexus.nexus.dto.ItemPedidoRequest;
import nexus.nexus.dto.PedidoRequest;
import nexus.nexus.dto.PedidoResponse;
import nexus.nexus.exception.ApiException;
import nexus.nexus.model.*;
import nexus.nexus.repository.BoletaRepository;
import nexus.nexus.repository.ProductoRepository;
import nexus.nexus.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class BoletaService {

    private static final int DESCUENTO_OFERTA_POR_DEFECTO = 15;

    private final BoletaRepository boletaRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;

    /** Probabilidad de que el pago simulado sea rechazado (0 = nunca, útil para pruebas). */
    @Value("${pago.probabilidad-rechazo:0.25}")
    private double probabilidadRechazo;

    /**
     * Crea el pedido en estado PENDIENTE. El precio y el total se calculan aquí,
     * nunca se confía en los valores que envía el cliente. El stock se descuenta al pagar.
     */
    @Transactional
    public PedidoResponse crear(PedidoRequest req, String correoUsuario, boolean esAdmin) {
        Usuario usuario = usuarioPorCorreo(correoUsuario);

        // Si el mismo producto viene repetido, se suman las cantidades
        Map<Long, Integer> cantidades = new LinkedHashMap<>();
        for (ItemPedidoRequest item : req.items()) {
            cantidades.merge(item.consolaId(), item.cantidad(), Integer::sum);
        }

        Boleta boleta = Boleta.builder()
                .fecha(Instant.now())
                .estado(EstadoBoleta.PENDIENTE)
                .usuario(usuario)
                .nombre(req.cliente().nombre().trim())
                .apellidos(req.cliente().apellidos().trim())
                .correo(correoDelCliente(req, usuario, esAdmin))
                .calle(req.entrega().calle().trim())
                .departamento(req.entrega().departamento())
                .region(req.entrega().region().trim())
                .comuna(req.entrega().comuna().trim())
                .indicaciones(req.entrega().indicaciones())
                .total(0)
                .build();

        int total = 0;
        for (Map.Entry<Long, Integer> e : cantidades.entrySet()) {
            Producto producto = productoRepository.findById(e.getKey())
                    .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Producto " + e.getKey() + " no existe"));
            if (e.getValue() > producto.getStock()) {
                throw new ApiException(HttpStatus.CONFLICT, "Stock insuficiente para " + producto.getNombre());
            }
            int precio = precioFinal(producto);
            int subtotal = precio * e.getValue();
            boleta.agregarDetalle(DetalleBoleta.builder()
                    .producto(producto)
                    .nombre(producto.getNombre())
                    .precio(precio)
                    .cantidad(e.getValue())
                    .subtotal(subtotal)
                    .build());
            total += subtotal;
        }
        boleta.setTotal(total);
        return PedidoResponse.de(boletaRepository.save(boleta));
    }

    /**
     * Pago simulado. Si se aprueba, descuenta el stock; si algún producto ya no alcanza,
     * el pago falla con 409 y no se modifica nada. Si ya estaba pagado, lo devuelve igual.
     */
    @Transactional
    public PedidoResponse pagar(Long nro, String correoUsuario, boolean esAdmin) {
        Boleta boleta = boletaRepository.findById(nro)
                .filter(b -> esAdmin || esDueno(b, correoUsuario))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Pedido no encontrado"));

        if (boleta.getEstado() == EstadoBoleta.PAGADO) {
            return PedidoResponse.de(boleta);
        }

        boolean aprobado = ThreadLocalRandom.current().nextDouble() >= probabilidadRechazo;
        if (aprobado) {
            for (DetalleBoleta d : boleta.getDetalles()) {
                if (productoRepository.descontarStock(d.getProducto().getId(), d.getCantidad()) == 0) {
                    throw new ApiException(HttpStatus.CONFLICT, "Stock insuficiente para " + d.getNombre());
                }
            }
            boleta.setEstado(EstadoBoleta.PAGADO);
        } else {
            boleta.setEstado(EstadoBoleta.RECHAZADO);
        }
        return PedidoResponse.de(boleta);
    }

    /** Detalle: lo ve su dueño, ADMIN y VENDEDOR. */
    @Transactional(readOnly = true)
    public PedidoResponse obtener(Long nro, String correoUsuario, boolean puedeVerTodos) {
        return boletaRepository.findById(nro)
                .filter(b -> puedeVerTodos || esDueno(b, correoUsuario))
                .map(PedidoResponse::de)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Pedido no encontrado"));
    }

    @Transactional(readOnly = true)
    public List<PedidoResponse> mios(String correoUsuario) {
        Usuario usuario = usuarioPorCorreo(correoUsuario);
        return boletaRepository.findByUsuarioIdOrderByFechaDesc(usuario.getId())
                .stream().map(PedidoResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public List<PedidoResponse> todos() {
        return boletaRepository.findAllByOrderByFechaDesc().stream().map(PedidoResponse::de).toList();
    }

    // --- utilidades ---

    private Usuario usuarioPorCorreo(String correo) {
        return usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "No autenticado o sesión expirada"));
    }

    private boolean esDueno(Boleta boleta, String correoUsuario) {
        return boleta.getUsuario().getCorreo().equalsIgnoreCase(correoUsuario);
    }

    /** Un cliente siempre compra a su nombre; solo un ADMIN puede indicar otro correo. */
    private String correoDelCliente(PedidoRequest req, Usuario usuario, boolean esAdmin) {
        String correo = req.cliente().correo();
        if (esAdmin && correo != null && !correo.isBlank()) {
            return correo.trim().toLowerCase();
        }
        return usuario.getCorreo();
    }

    /** Precio con descuento: solo si el producto está en oferta (15 % si no trae descuento). */
    private int precioFinal(Producto p) {
        int descuento = 0;
        if (Boolean.TRUE.equals(p.getOferta())) {
            descuento = p.getDescuento() != null ? p.getDescuento() : DESCUENTO_OFERTA_POR_DEFECTO;
        }
        return (int) Math.round(p.getPrecio() * (100 - descuento) / 100.0);
    }
}
