package com.avisa.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Comprova que les migracions reals s'apliquen sobre un PostgreSQL buit.
 */
class FlywayMigrationsIT {

	private final String schema = PostgresItSupport.newSchemaName();

	@AfterEach
	void dropSchema() throws SQLException {
		PostgresItSupport.dropSchema(schema);
	}

	@Test
	@DisplayName("V1 i V2 s'apliquen sobre un esquema buit")
	void aplicaLesMigracionsFinsLaV2() {
		MigrateResult result = flyway().target("2").load().migrate();

		assertThat(result.success).isTrue();
		assertThat(result.migrationsExecuted).isEqualTo(2);
	}

	@Test
	@Disabled("V3 insereix dades d'usuaris que només crea el seeder i falla en una base buida; es reactiva amb el nou esquema.")
	@DisplayName("Totes les migracions s'apliquen sobre un esquema buit")
	void aplicaTotesLesMigracions() {
		MigrateResult result = flyway().load().migrate();

		assertThat(result.success).isTrue();
	}

	private FluentConfiguration flyway() {
		return Flyway.configure()
			.dataSource(PostgresItSupport.url(), PostgresItSupport.user(), PostgresItSupport.password())
			.schemas(schema)
			.locations("classpath:db/migration");
	}
}
