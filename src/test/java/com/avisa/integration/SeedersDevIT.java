package com.avisa.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.avisa.config.DevUsersSeeder;
import com.avisa.dto.LoginRequest;
import com.avisa.service.AuthService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

// Sense AVISA_ADMIN_PASSWORD: en dev l'admin es crea amb la contrasenya de proves
@ActiveProfiles("dev")
@org.springframework.test.context.TestPropertySource(properties = "avisa.admin.password=")
class SeedersDevIT extends PostgresIntegrationTest {

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private DevUsersSeeder seeder;

	@Autowired
	private com.avisa.config.AdminSeeder adminSeeder;

	@Autowired
	private AuthService authService;

	@BeforeEach
	void baseAmbElsUsuarisDeProva() {
		jdbc.execute("TRUNCATE usuaris, blacklist_telefons CASCADE");
		adminSeeder.run(new DefaultApplicationArguments());
		seeder.run(new DefaultApplicationArguments());
	}

	@Test
	@DisplayName("Crea exactament els tres usuaris de prova amb el seu rol")
	void creaElsTresUsuaris() {
		List<String> usuaris = jdbc.queryForList("SELECT email || ':' || rol FROM usuaris", String.class);

		assertThat(usuaris).containsExactlyInAnyOrder("admin@admin.es:ADMIN", "cli:CLIENT", "pro:PROFESSIONAL");
	}

	@Test
	@DisplayName("Els tres usuaris poden iniciar sessió amb les seues credencials")
	void elsUsuarisPodenEntrar() {
		assertThat(authService.login(new LoginRequest("admin@admin.es", "adminadmin"))).isPresent();
		assertThat(authService.login(new LoginRequest("cli", "clicli"))).isPresent();
		assertThat(authService.login(new LoginRequest("pro", "propro"))).isPresent();
	}

	@Test
	@DisplayName("Tornar a executar el seeder no sobreescriu els canvis de l'usuari")
	void noSobreescriu() {
		jdbc.update("UPDATE usuaris SET password = ?, bio = 'la meua bio', nom = 'Nom editat', percentil = 50 WHERE email = 'pro'", ContrasenyaDeProva.HASH);

		seeder.run(new DefaultApplicationArguments());

		assertThat(jdbc.queryForObject("SELECT password || '|' || bio || '|' || nom || '|' || percentil FROM usuaris WHERE email = 'pro'", String.class))
			.isEqualTo(ContrasenyaDeProva.HASH + "|la meua bio|Nom editat|50");
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM usuaris", Integer.class)).isEqualTo(3);
	}
}
