package com.avisa.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.ResourcePropertySource;

/**
 * Cap credencial té un valor per defecte fora del perfil dev.
 * Es llegeixen els fitxers de src/main/resources tal com els carrega Spring, sense variables d'entorn.
 */
class ConfiguracioTest {

	private static final String[] CREDENCIALS = {"spring.datasource.password", "avisa.jwt.secret", "avisa.admin.password"};

	private StandardEnvironment entorn(String... fitxers) throws IOException {
		StandardEnvironment entorn = new StandardEnvironment();
		MutablePropertySources fonts = entorn.getPropertySources();
		fonts.remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
		fonts.remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
		for (String fitxer : fitxers) {
			// Els de src/main i no els de test, que al classpath tenen prioritat. El perfil va primer: té prioritat sobre application.properties, com a Spring Boot
			fonts.addLast(new ResourcePropertySource(new FileSystemResource("src/main/resources/" + fitxer)));
		}
		return entorn;
	}

	@Test
	@DisplayName("Sense perfil, les credencials no tenen cap valor per defecte")
	void produccioSenseValorsPerDefecte() throws IOException {
		StandardEnvironment produccio = entorn("application.properties");
		for (String credencial : CREDENCIALS) {
			assertThat(produccio.getProperty(credencial)).as(credencial).isEmpty();
		}
	}

	@Test
	@DisplayName("En dev, la base local manté la seua contrasenya de sempre")
	void devManteLaBaseLocal() throws IOException {
		StandardEnvironment dev = entorn("application-dev.properties", "application.properties");
		assertThat(dev.getProperty("spring.datasource.password")).isEqualTo("postgres");
		assertThat(dev.getProperty("avisa.jwt.secret")).isEmpty();
	}
}
