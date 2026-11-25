package nexus.nexus.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Pedido de un cliente (la API lo expone como /pedidos). */
@Entity
@Table(name = "boleta")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Boleta {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "boleta_nro_gen")
    @SequenceGenerator(name = "boleta_nro_gen", sequenceName = "boleta_nro_seq",
            initialValue = 20260001, allocationSize = 1)
    private Long nro;

    @Column(nullable = false)
    private Instant fecha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EstadoBoleta estado = EstadoBoleta.PENDIENTE;

    @Column(nullable = false)
    private Integer total;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    // Datos del cliente
    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(nullable = false, length = 80)
    private String apellidos;

    @Column(nullable = false, length = 120)
    private String correo;

    // Datos de entrega
    @Column(nullable = false, length = 150)
    private String calle;

    @Column(length = 20)
    private String departamento;

    @Column(nullable = false, length = 100)
    private String region;

    @Column(nullable = false, length = 100)
    private String comuna;

    @Column(length = 300)
    private String indicaciones;

    @OneToMany(mappedBy = "boleta", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DetalleBoleta> detalles = new ArrayList<>();

    public void agregarDetalle(DetalleBoleta detalle) {
        detalle.setBoleta(this);
        detalles.add(detalle);
    }
}
