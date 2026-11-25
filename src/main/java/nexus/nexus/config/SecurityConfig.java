package nexus.nexus.config;

import nexus.nexus.security.JwtAuthFilter;
import nexus.nexus.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class SecurityConfig {

    private static final String API = "/api/v1";

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtService jwtService) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(e -> e
                .authenticationEntryPoint((req, res, ex) ->
                    escribirError(res, 401, "No autenticado o sesión expirada"))
                .accessDeniedHandler((req, res, ex) ->
                    escribirError(res, 403, "No tienes permiso para realizar esta acción")))
            .authorizeHttpRequests(auth -> auth
                // Documentación
                .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                // Autenticación pública
                .requestMatchers(HttpMethod.POST, API + "/auth/login", API + "/auth/register", API + "/auth/refresh").permitAll()
                // Catálogo público (lectura)
                .requestMatchers(HttpMethod.GET, API + "/consolas/**", API + "/categorias/**").permitAll()
                // Administración del catálogo y usuarios
                .requestMatchers(API + "/usuarios/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, API + "/consolas/**", API + "/categorias/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, API + "/consolas/**", API + "/categorias/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, API + "/consolas/**", API + "/categorias/**").hasRole("ADMIN")
                // Pedidos: el cliente ve solo los suyos; ADMIN y VENDEDOR ven la lista completa
                .requestMatchers(HttpMethod.POST, API + "/pedidos", API + "/pedidos/*/pagar").hasAnyRole("CLIENTE", "ADMIN")
                .requestMatchers(HttpMethod.GET, API + "/pedidos/mios").authenticated()
                .requestMatchers(HttpMethod.GET, API + "/pedidos").hasAnyRole("ADMIN", "VENDEDOR")
                .anyRequest().authenticated())
            .addFilterBefore(new JwtAuthFilter(jwtService), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${cors.allowed-origins}") String origenes) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.stream(origenes.split(",")).map(String::trim).toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private static void escribirError(jakarta.servlet.http.HttpServletResponse res, int status, String mensaje)
            throws java.io.IOException {
        res.setStatus(status);
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");
        res.getWriter().write("{\"message\":\"" + mensaje + "\"}");
    }
}
