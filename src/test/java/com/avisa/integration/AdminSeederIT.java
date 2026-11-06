package com.avisa.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.avisa.config.AdminSeeder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * La contrasenya de l'administrador surt de AVISA_ADMIN_PASSWORD; sense ella, fora de dev, no s'arrenca.
 */
class AdminSeederIT extends PostgresIntegrationTest {

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@BeforeEach
	void senseUsuaris() {
		jdbc.execute("TRUNCATE usuaris, blacklist_telefons CASCADE");
	}

	private MockEnvironment perfil(String... perfils) {
		MockEnvironment entorn = new MockEnvironment();
		entorn.setActiveProfiles(perfils);
		return entorn;
	}

	private AdminSeeder seeder(String contrasenya, String... perfils) {
		return new AdminSeeder(jdbc, passwordEncoder, perfil(perfils), contrasenya);
	}

	private String hashAdmin() {
		return jdbc.queryForObject("SELECT password FROM usuaris WHERE email = 'admin@admin.es'", String.class);
	}

	@Test
	@DisplayName("Fora de dev, sense la variable o amb una contrasenya massa curta, no s'arrenca")
	void exigeixLaVariable() {
		assertThatThrownBy(() -> seeder("")).isInstanceOf(IllegalStateException.class).hasMessageContaining("AVISA_ADMIN_PASSWORD");
		assertThatThrownBy(() -> seeder(null, "demo")).isInstanceOf(IllegalStateException.class).hasMessageContaining("AVISA_ADMIN_PASSWORD");
		assertThatThrownBy(() -> seeder("curta", "dev")).isInstanceOf(IllegalStateException.class).hasMessageContaining("massa curta");
	}

	@Test
	@DisplayName("Amb la variable, l'admin es crea amb aquesta contrasenya, guardada amb hash")
	void creaAmbLaVariable() {
		seeder("una-clau-de-producció").run(new DefaultApplicationArguments());

		assertThat(hashAdmin()).startsWith("$2");
		assertThat(passwordEncoder.matches("una-clau-de-producció", hashAdmin())).isTrue();
		assertThat(passwordEncoder.matches("adminadmin", hashAdmin())).isFalse();
	}

	@Test
	@DisplayName("Si la variable canvia, la contrasenya de l'admin existent passa a ser la nova")
	void laVariableMana() {
		seeder("primera-clau-123").run(new DefaultApplicationArguments());
		seeder("segona-clau-456").run(new DefaultApplicationArguments());

		assertThat(passwordEncoder.matches("segona-clau-456", hashAdmin())).isTrue();
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM usuaris WHERE rol = 'ADMIN'", Integer.class)).isEqualTo(1);
	}

	@Test
	@DisplayName("En dev sense variable es fa servir la contrasenya de proves i no es toca un admin existent")
	void devSenseVariable() {
		seeder("", "dev").run(new DefaultApplicationArguments());
		assertThat(passwordEncoder.matches("adminadmin", hashAdmin())).isTrue();

		jdbc.update("UPDATE usuaris SET password = ? WHERE email = 'admin@admin.es'", ContrasenyaDeProva.HASH);
		seeder("", "dev").run(new DefaultApplicationArguments());
		assertThat(hashAdmin()).isEqualTo(ContrasenyaDeProva.HASH);
	}
}
