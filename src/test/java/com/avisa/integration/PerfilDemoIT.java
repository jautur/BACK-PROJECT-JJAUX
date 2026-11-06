package com.avisa.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.avisa.config.DemoDataSeeder;
import com.avisa.config.DevUsersSeeder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

/**
 * El perfil demo tot sol compta com a producció: cap usuari amb contrasenya de proves.
 */
@ActiveProfiles("demo")
class PerfilDemoIT extends PostgresIntegrationTest {

	@Autowired
	private ApplicationContext context;

	@Test
	@DisplayName("Amb el perfil demo sense dev no es creen ni els usuaris de prova ni les dades de demo")
	void demoSenseDevNoCreaUsuarisDeProva() {
		assertThat(context.getBeansOfType(DevUsersSeeder.class)).isEmpty();
		assertThat(context.getBeansOfType(DemoDataSeeder.class)).isEmpty();
	}
}
