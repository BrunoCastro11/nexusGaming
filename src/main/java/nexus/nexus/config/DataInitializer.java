package nexus.nexus.config;

import lombok.RequiredArgsConstructor;
import nexus.nexus.model.Categoria;
import nexus.nexus.model.Producto;
import nexus.nexus.model.Rol;
import nexus.nexus.model.Usuario;
import nexus.nexus.repository.CategoriaRepository;
import nexus.nexus.repository.ProductoRepository;
import nexus.nexus.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final CategoriaRepository categoriaRepository;
    private final ProductoRepository productoRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        cargarUsuarios();
        cargarCatalogo();
    }

    private void cargarUsuarios() {
        if (usuarioRepository.count() > 0) {
            return;
        }
        usuarioRepository.saveAll(List.of(
                usuario("admin", "admin", "admin@gmail.com", "admin123", Rol.ADMIN),
                usuario("vendedor", "nexus", "vendedor@gmail.com", "vendedor123", Rol.VENDEDOR),
                usuario("vicente", "vasquez", "vicente6@gmail.com", "vicente6", Rol.CLIENTE)
        ));
    }

    private Usuario usuario(String nombre, String apellidos, String correo, String password, Rol rol) {
        return Usuario.builder()
                .nombre(nombre)
                .apellidos(apellidos)
                .correo(correo)
                .password(passwordEncoder.encode(password))
                .rol(rol)
                .build();
    }

    private void cargarCatalogo() {
        if (categoriaRepository.count() > 0 || productoRepository.count() > 0) {
            return;
        }
        Map<String, Categoria> categorias = new HashMap<>();
        for (String nombre : List.of("Sony", "Microsoft", "Nintendo")) {
            categorias.put(nombre, categoriaRepository.save(Categoria.builder().nombre(nombre).build()));
        }

        productoRepository.saveAll(List.of(
                producto("Playstation 5", 659990, categorias.get("Sony"), false, 10, 10, "ps5ozi.webp"),
                producto("Xbox series x", 659990, categorias.get("Microsoft"), false, null, 8, "xbox.webp"),
                producto("Nintendo switch 2", 549990, categorias.get("Nintendo"), false, 20, 15, "switch2.webp"),
                producto("Playstation 4", 249990, categorias.get("Sony"), true, 25, 12, "images.webp"),
                producto("Nintendo switch 1", 299990, categorias.get("Nintendo"), true, 12, 9, "switch-1.webp"),
                producto("Xbox 360", 89990, categorias.get("Microsoft"), true, 30, 6, "xbox360.webp"),
                producto("PSP", 79990, categorias.get("Sony"), true, 35, 5, "images-_1_.webp"),
                producto("Nintendo 2DS", 99990, categorias.get("Nintendo"), true, 5, 7, "2ds.webp")
        ));
    }

    private Producto producto(String nombre, int precio, Categoria categoria,
                              boolean oferta, Integer descuento, int stock, String imagen) {
        return Producto.builder()
                .nombre(nombre)
                .precio(precio)
                .categoria(categoria)
                .oferta(oferta)
                .descuento(descuento)
                .stock(stock)
                .imagen(imagen)
                .build();
    }
}
