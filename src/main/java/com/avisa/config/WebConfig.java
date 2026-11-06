package com.avisa.config;

import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Política CORS del backend AVISA: l'única per a tota l'API (els controladors no en declaren cap).
 *
 * El front de producció es serveix pel mateix origen que l'API (proxy /api), així que CORS només cal quan
 * el navegador crida el port 8080 directament, com fa el front des d'un mòbil de la xarxa local.
 * Els orígens permesos surten de avisa.cors.origens (variable AVISA_CORS_ORIGENS, separats per comes).
 * No s'envien credencials: la sessió va a la capçalera Authorization, no en galetes.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final String[] origens;

    public WebConfig(@Value("${avisa.cors.origens}") String origens) {
        this.origens = Arrays.stream(origens.split(","))
            .map(String::trim)
            .filter(origen -> !origen.isEmpty())
            .toArray(String[]::new);
        if (Arrays.asList(this.origens).contains("*")) {
            throw new IllegalStateException("avisa.cors.origens no pot contenir '*': qualsevol web podria cridar l'API.");
        }
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(origens)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("Authorization", "Content-Type")
                .allowCredentials(false);
    }
}
