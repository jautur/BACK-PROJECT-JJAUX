package com.avisa.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Les contrasenyes es guarden amb BCrypt i mai en text pla.
 * La base ho imposa amb la restricció usuaris_password_bcrypt (migració V2).
 */
@Configuration
public class ContrasenyesConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
