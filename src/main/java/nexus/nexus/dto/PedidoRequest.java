package nexus.nexus.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record PedidoRequest(
        @NotNull(message = "Faltan los datos del cliente") @Valid ClientePedido cliente,
        @NotNull(message = "Faltan los datos de entrega") @Valid EntregaPedido entrega,
        @NotEmpty(message = "El pedido no tiene productos") List<@Valid ItemPedidoRequest> items) {
}
