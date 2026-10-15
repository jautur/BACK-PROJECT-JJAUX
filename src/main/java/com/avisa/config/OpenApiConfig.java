package com.avisa.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuració avançada d'OpenAPI / Swagger per a la plataforma AVISA.
 * Afegeix metadades, informació de contacte i suport per a tokens JWT Bearer.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("AVISA API - Plataforma de Connexió Clients i Professionals")
                        .description("API RESTful per a la gestió d'incidències, ofertes de servei, pressupostos, tasques, ressenyes i notificacions.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Equip AVISA")
                                .email("suport@avisa.com"))
                        .license(new License().name("Propietari").url("https://avisa.local")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Introdueix el token JWT d'autenticació per provar els endpoints")));
    }
}

