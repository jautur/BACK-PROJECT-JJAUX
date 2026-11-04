package com.avisa.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Crea els tres usuaris de prova en els perfils de desenvolupament i demostració.
 * Mai sobreescriu: si l'usuari ja existeix, no el toca.
 */
@Component
@Profile({"dev", "demo"})
@Order(1)
public class DevUsersSeeder implements ApplicationRunner {

	private final JdbcTemplate jdbcTemplate;

	public DevUsersSeeder(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public void run(ApplicationArguments args) {
		crearSiNoExisteix("Administrador General", "admin@admin.es", "adminadmin", "ADMIN", "960000000", true);
		crearSiNoExisteix("cli", "cli", "clicli", "CLIENT", "600111222", false);
		crearSiNoExisteix("pro", "pro", "propro", "PROFESSIONAL", "600333444", true);
	}

	private void crearSiNoExisteix(String nom, String email, String password, String rol, String telefon, boolean verificat) {
		jdbcTemplate.update(
			"INSERT INTO usuaris (nom, email, password, rol, telefon, verificat) VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT DO NOTHING",
			nom, email, password, rol, telefon, verificat
		);
	}
}
