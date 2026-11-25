package nexus.nexus.dto;

import nexus.nexus.model.Producto;

/** Formato que consume el frontend: la categoría viaja como nombre. */
public record ConsolaResponse(Long id, String nombre, Integer precio, String categoria,
                              Boolean oferta, Integer descuento, Integer stock, String imagen) {

    public static ConsolaResponse de(Producto p) {
        return new ConsolaResponse(p.getId(), p.getNombre(), p.getPrecio(), p.getCategoria().getNombre(),
                p.getOferta(), p.getDescuento(), p.getStock(), p.getImagen());
    }
}
