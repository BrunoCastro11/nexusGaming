package nexus.nexus.model;

import jakarta.persistence.*;
import lombok.*;

/** Consola del catálogo (la API la expone como /consolas). */
@Entity
@Table(name = "producto")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false)
    private Integer precio;

    @Column(nullable = false)
    private Integer stock;

    @Column(nullable = false)
    @Builder.Default
    private Boolean oferta = false;

    /** Porcentaje de descuento (1 a 90); null si no tiene. */
    private Integer descuento;

    /** Nombre de archivo del frontend (ej. ps5ozi.webp) o URL completa. */
    @Column(length = 300)
    private String imagen;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;
}
