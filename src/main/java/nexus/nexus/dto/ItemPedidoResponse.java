package nexus.nexus.dto;

import nexus.nexus.model.DetalleBoleta;

public record ItemPedidoResponse(Long consolaId, String nombre, Integer precio, Integer cantidad,
                                 Integer subtotal, String imagen) {

    public static ItemPedidoResponse de(DetalleBoleta d) {
        return new ItemPedidoResponse(d.getProducto().getId(), d.getNombre(), d.getPrecio(),
                d.getCantidad(), d.getSubtotal(), d.getProducto().getImagen());
    }
}
