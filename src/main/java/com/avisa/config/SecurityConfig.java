package com.avisa.config;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

import org.springframework.security.oauth2.jwt.JwtDecoder;

/**
 * Configuració de seguretat per al backend d'AVISA.
 * Deshabilita CSRF per a endpoints REST, permet l'accés des d'Angular,
 * i habilita OAuth2 Resource Server per a la validació de tokens JWT Bearer.
 *
 * Tot demana sessió llevat del que es llista ací: una ruta nova naix privada.
 * Els permisos per rol i propietari es comproven als controladors i al catàleg.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** Entrar, registrar-se i recuperar l'accés es fa, per definició, sense sessió. */
    private static final String[] AUTH_PUBLICA = {
        "/api/auth/login",
        "/api/auth/register",
        "/api/auth/logout",
        "/api/auth/demanar-recuperacio",
        "/api/auth/restablir-contrasenya"
    };

    /** El mercat: el que es mostra a qui encara no ha iniciat sessió. */
    private static final String[] MERCAT_PUBLIC = {
        "/api/dashboard",
        "/api/offers",
        "/api/offers/*",
        "/api/offers/professional/*",
        "/api/incidencies",
        "/api/incidencies/*",
        "/api/incidencies/client/*",
        "/api/tasks/disponibles",
        "/api/resenyes",
        "/api/resenyes/treballador/*",
        "/api/resenyes/client/*",
        "/api/professionals",
        "/api/professionals/*"
    };

    private final JwtDecoder jwtDecoder;

    public SecurityConfig(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> {})
            .authorizeHttpRequests(auth -> auth
                // Sense això, un error com un 400 d'una ruta pública arribaria al visitant com a 401
                .dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.FORWARD).permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                .requestMatchers(HttpMethod.POST, AUTH_PUBLICA).permitAll()
                .requestMatchers(HttpMethod.GET, MERCAT_PUBLIC).permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(jwtDecoder)));
        return http.build();
    }
}
