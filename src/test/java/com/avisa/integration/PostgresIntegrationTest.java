package com.avisa.integration;

import com.avisa.AvisaApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Base dels tests que arrenquen l'aplicació sencera amb Flyway activat sobre PostgreSQL.
 */
@SpringBootTest(classes = AvisaApplication.class)
abstract class PostgresIntegrationTest {

	@DynamicPropertySource
	static void postgres(DynamicPropertyRegistry registry) {
		String url = PostgresItSupport.url();
		String schema = PostgresItSupport.SHARED_SCHEMA;
		registry.add("spring.datasource.url", () -> url + (url.contains("?") ? "&" : "?") + "currentSchema=" + schema);
		registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
		registry.add("spring.datasource.username", PostgresItSupport::user);
		registry.add("spring.datasource.password", PostgresItSupport::password);
		registry.add("spring.flyway.enabled", () -> "true");
		registry.add("spring.flyway.schemas", () -> schema);
	}
}
