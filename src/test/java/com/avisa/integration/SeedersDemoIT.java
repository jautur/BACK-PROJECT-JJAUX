package com.avisa.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.avisa.config.DemoDataSeeder;
import com.avisa.config.DevUsersSeeder;
import com.avisa.dto.LoginRequest;
import com.avisa.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("demo")
class SeedersDemoIT extends PostgresIntegrationTest {

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private com.avisa.config.AdminSeeder adminSeeder;

	@Autowired
	private DevUsersSeeder devSeeder;

	@Autowired
	private DemoDataSeeder demoSeeder;

	@Autowired
	private AuthService authService;

	@BeforeEach
	void baseDeDemo() {
		jdbc.execute("TRUNCATE usuaris, blacklist_telefons CASCADE");
		adminSeeder.run(new DefaultApplicationArguments());
		devSeeder.run(new DefaultApplicationArguments());
		demoSeeder.run(new DefaultApplicationArguments());
	}

	@Test
	@DisplayName("Afig les empreses i els clients de demo als tres usuaris de prova")
	void afigLesDadesDeDemo() {
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM usuaris WHERE rol = 'PROFESSIONAL' AND email <> 'pro'", Integer.class)).isEqualTo(10);
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM usuaris WHERE rol = 'CLIENT' AND email <> 'cli'", Integer.class)).isEqualTo(10);
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM usuaris WHERE rol = 'ADMIN'", Integer.class)).isEqualTo(1);
	}

	@Test
	@DisplayName("Les dades de demo no trenquen l'accés de l'usuari pro")
	void noTrencaElLoginDePro() {
		assertThat(authService.login(new LoginRequest("pro", "propro"))).isPresent();
		assertThat(authService.login(new LoginRequest("cli", "clicli"))).isPresent();
	}

	@Test
	@DisplayName("Tornar a executar el seeder no duplica ni sobreescriu")
	void noDuplicaNiSobreescriu() {
		Integer abans = jdbc.queryForObject("SELECT COUNT(*) FROM usuaris", Integer.class);
		jdbc.update("UPDATE usuaris SET bio = 'editada' WHERE email = 'contacte@fontanals.es'");

		demoSeeder.run(new DefaultApplicationArguments());

		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM usuaris", Integer.class)).isEqualTo(abans);
		assertThat(jdbc.queryForObject("SELECT bio FROM usuaris WHERE email = 'contacte@fontanals.es'", String.class)).isEqualTo("editada");
	}
}
