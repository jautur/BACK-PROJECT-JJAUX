package com.avisa.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Seeder de dades inicials per a la plataforma AVISA / JJAUX.
 * Manté únicament els usuaris necessaris per a proves des de zero:
 * - admin: Administrador General (admin@admin.es / adminadmin)
 * - cli: Client de proves (cli / clicli)
 * - pro: Professional de proves (pro / propro)
 */
@Component
public class DemoDataSeeder implements ApplicationRunner {

	private final JdbcTemplate jdbcTemplate;

	public DemoDataSeeder(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public void run(ApplicationArguments args) {
		seedUsers();
	}

	private void seedUsers() {
		// Assegurar usuari administrador
		upsertUser("u-admin", "Administrador General", "admin@admin.es", "adminadmin", "admin", null, "Administrador de la plataforma AVISA");

		// Usuari Client de proves
		upsertUser("u-cli", "cli", "cli", "clicli", "client", "600111222", "Compte client de proves per a creació d'incidències.");

		// Usuari Professional de proves
		upsertUser("u-pro", "pro", "pro", "propro", "professional", "000444555", "Compte professional de proves per a pressupostos i ofertes.");
	}

	private void upsertUser(String id, String nom, String email, String password, String rol, String telefon, String bio) {
		try {
			Integer exists = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM usuaris WHERE LOWER(email) = LOWER(?) OR LOWER(nom) = LOWER(?) OR id = ?",
				Integer.class, email, nom, id
			);
			if (exists != null && exists > 0) {
				jdbcTemplate.update(
					"UPDATE usuaris SET nom = ?, email = ?, password = ?, rol = ?, telefon = ?, bio = ? WHERE LOWER(email) = LOWER(?) OR LOWER(nom) = LOWER(?) OR id = ?",
					nom, email, password, rol, telefon, bio, email, nom, id
				);
			} else {
				jdbcTemplate.update(
					"INSERT INTO usuaris (id, nom, email, password, rol, telefon, bio) VALUES (?, ?, ?, ?, ?, ?, ?)",
					id, nom, email, password, rol, telefon, bio
				);
			}
		} catch (Exception ignored) {}
	}
}