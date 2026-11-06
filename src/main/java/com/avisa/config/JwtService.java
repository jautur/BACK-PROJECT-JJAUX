package com.avisa.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Servei de generació i descodificació de tokens JWT per a OAuth2 Resource Server.
 * Empra NimbusJwtEncoder i NimbusJwtDecoder amb signatura HMAC-SHA256.
 *
 * El secret ve de la variable d'entorn AVISA_JWT_SECRET (propietat avisa.jwt.secret) i mai del codi:
 * qui el coneix pot signar tokens de qualsevol usuari, també d'administrador.
 * Si falta o és massa curt, l'aplicació no arrenca.
 */
@Configuration
public class JwtService {

    /** HS256 necessita una clau d'almenys 256 bits. */
    static final int LONGITUD_MINIMA = 32;

    private final SecretKey secretKey;

    public JwtService(@Value("${avisa.jwt.secret:}") String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("Falta el secret dels tokens: defineix la variable d'entorn AVISA_JWT_SECRET (vegeu docs/configuracio.md).");
        }
        byte[] bytes = secret.trim().getBytes(StandardCharsets.UTF_8);
        if (bytes.length < LONGITUD_MINIMA) {
            throw new IllegalStateException("AVISA_JWT_SECRET és massa curt: ha de tenir com a mínim " + LONGITUD_MINIMA + " caràcters.");
        }
        this.secretKey = new SecretKeySpec(bytes, "HmacSHA256");
    }

    /**
     * Bean JwtDecoder requerit per Spring Security OAuth2 Resource Server.
     * Descodifica i valida la signatura i expiració dels Bearer tokens entrants.
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withSecretKey(this.secretKey).build();
    }

    /**
     * Bean JwtEncoder per a la creació de tokens signats.
     */
    @Bean
    public JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(this.secretKey));
    }

    /**
     * Genera un token JWT signat amb validesa de 24 hores per a un usuari autenticat.
     * @param userId Identificador de l'usuari
     * @param email Correu de l'usuari
     * @param rol Rol de l'usuari ('client', 'professional', 'admin')
     * @return Token JWT serialitzat
     */
    public String generateToken(String userId, String email, String rol) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("AVISA-JJAUX")
                .issuedAt(now)
                .expiresAt(now.plus(24, ChronoUnit.HOURS))
                .subject(userId)
                .claim("email", email)
                .claim("rol", rol != null ? rol.toUpperCase() : "CLIENT")
                .build();

        JwsHeader header = JwsHeader.with(() -> "HS256").build();
        return jwtEncoder().encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
