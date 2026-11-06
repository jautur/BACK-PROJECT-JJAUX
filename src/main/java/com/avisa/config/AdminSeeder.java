package com.avisa.config;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Assegura el compte d'administrador (admin@admin.es) en tots els perfils.
 *
 * La contrasenya surt de la variable d'entorn AVISA_ADMIN_PASSWORD (propietat avisa.admin.password):
 * - Si està definida, mana ella: es crea l'admin amb aquesta contrasenya o se li posa si en tenia una altra.
 * - Si falta, només el perfil dev arrenca, amb la contrasenya de proves de sempre i sense tocar un admin existent.
 *   En qualsevol altre perfil l'aplicació no arrenca.
 */
@Component
@Order(0)
public class AdminSeeder implements ApplicationRunner {

	static final String EMAIL = "admin@admin.es";
	static final String CONTRASENYA_DEV = "adminadmin";
	static final int LONGITUD_MINIMA = 10;

	private final JdbcTemplate jdbcTemplate;
	private final PasswordEncoder passwordEncoder;
	private final String contrasenya;

	public AdminSeeder(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder, Environment environment,
			@Value("${avisa.admin.password:}") String contrasenya) {
		this.jdbcTemplate = jdbcTemplate;
		this.passwordEncoder = passwordEncoder;
		this.contrasenya = contrasenya == null ? "" : contrasenya.trim();
		boolean dev = environment.acceptsProfiles(Profiles.of("dev"));
		if (this.contrasenya.isEmpty() && !dev) {
			throw new IllegalStateException("Falta la contrasenya de l'administrador: defineix la variable d'entorn AVISA_ADMIN_PASSWORD (vegeu docs/configuracio.md).");
		}
		if (!this.contrasenya.isEmpty() && this.contrasenya.length() < LONGITUD_MINIMA) {
			throw new IllegalStateException("AVISA_ADMIN_PASSWORD és massa curta: ha de tenir com a mínim " + LONGITUD_MINIMA + " caràcters.");
		}
	}

	@Override
	public void run(ApplicationArguments args) {
		List<String> actual = jdbcTemplate.queryForList("SELECT password FROM usuaris WHERE LOWER(email) = LOWER(?)", String.class, EMAIL);
		if (actual.isEmpty()) {
			String clar = contrasenya.isEmpty() ? CONTRASENYA_DEV : contrasenya;
			jdbcTemplate.update(
				"INSERT INTO usuaris (nom, email, password, rol, telefon, verificat) VALUES ('Administrador General', ?, ?, 'ADMIN', '960000000', TRUE)",
				EMAIL, passwordEncoder.encode(clar));
			return;
		}
		if (!contrasenya.isEmpty() && !passwordEncoder.matches(contrasenya, actual.get(0))) {
			jdbcTemplate.update("UPDATE usuaris SET password = ?, rol = 'ADMIN' WHERE LOWER(email) = LOWER(?)", passwordEncoder.encode(contrasenya), EMAIL);
		}
	}
}
