package com.avisa.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
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
	private final PasswordEncoder passwordEncoder;

	public DevUsersSeeder(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder) {
		this.jdbcTemplate = jdbcTemplate;
		this.passwordEncoder = passwordEncoder;
	}

	/** Només es calcula el hash si l'usuari no existeix: BCrypt és lent a propòsit. */
	private boolean existeix(String email) {
		Integer total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM usuaris WHERE LOWER(email) = LOWER(?)", Integer.class, email);
		return total != null && total > 0;
	}

	@Override
	public void run(ApplicationArguments args) {
		crearSiNoExisteix("Administrador General", "admin@admin.es", "adminadmin", "ADMIN", "960000000", true);
		crearSiNoExisteix("cli", "cli", "clicli", "CLIENT", "600111222", false);
		crearSiNoExisteix("pro", "pro", "propro", "PROFESSIONAL", "600333444", true);
	}

	private void crearSiNoExisteix(String nom, String email, String password, String rol, String telefon, boolean verificat) {
		if (existeix(email)) {
			return;
		}
		jdbcTemplate.update(
			"INSERT INTO usuaris (nom, email, password, rol, telefon, verificat) VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT DO NOTHING",
			nom, email, passwordEncoder.encode(password), rol, telefon, verificat
		);
	}
}
