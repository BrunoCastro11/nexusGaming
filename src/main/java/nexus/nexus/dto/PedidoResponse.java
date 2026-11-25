package nexus.nexus.dto;

import nexus.nexus.model.Boleta;

import java.util.List;

/** Formato del contrato: estado en minúscula y fecha ISO-8601. */
public record PedidoResponse(Long nro, String fecha, String estado, ClientePedido cliente,
                             EntregaPedido entrega, List<ItemPedidoResponse> items, Integer total) {

    public static PedidoResponse de(Boleta b) {
        return new PedidoResponse(
                b.getNro(),
                b.getFecha().toString(),
                b.getEstado().name().toLowerCase(),
                new ClientePedido(b.getNombre(), b.getApellidos(), b.getCorreo()),
                new EntregaPedido(b.getCalle(), b.getDepartamento(), b.getRegion(), b.getComuna(), b.getIndicaciones()),
                b.getDetalles().stream().map(ItemPedidoResponse::de).toList(),
                b.getTotal());
    }
}
