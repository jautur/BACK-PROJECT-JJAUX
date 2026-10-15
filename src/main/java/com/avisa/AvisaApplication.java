package com.avisa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Classe principal d'arrencada de l'aplicació backend Spring Boot AVISA (JJAUX).
 * Configura i aixeca el servidor REST sobre el port per defecte (8080)
 * connectant-se a PostgreSQL per a la gestió de tasques, ofertes, usuaris i avisos.
 */
@SpringBootApplication
public class AvisaApplication {

    public static void main(String[] args) {
        SpringApplication.run(AvisaApplication.class, args);
    }
}
