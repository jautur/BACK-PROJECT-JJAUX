package com.avisa.integration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import java.util.UUID;

/**
 * Connexió compartida dels tests d'integració sobre PostgreSQL real.
 *
 * Els tests *IT no s'executen amb "./mvnw test": cal demanar-los amb "-Dtest='*IT'" i indicar la base
 * amb les variables IT_DB_URL, IT_DB_USER i IT_DB_PASSWORD. Per protegir les dades, es neguen a
 * funcionar si el nom de la base no acaba en "_it" o "_test".
 */
final class PostgresItSupport {

	/** Esquema compartit pels tests amb context Spring. S'esborra en acabar la JVM. */
	static final String SHARED_SCHEMA = newSchemaName();

	static {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				dropSchema(SHARED_SCHEMA);
			} catch (SQLException | IllegalStateException ignored) {
				// La base és d'un sol ús: un esquema sobrant no afecta cap test.
			}
		}));
	}

	private PostgresItSupport() {}

	static String url() {
		String url = requiredEnv("IT_DB_URL");
		String database = databaseName(url);
		if (!database.endsWith("_it") && !database.endsWith("_test")) {
			throw new IllegalStateException(
				"Els tests d'integració només poden usar una base d'un sol ús (nom acabat en _it o _test), no '" + database + "'.");
		}
		return url;
	}

	static String user() {
		return requiredEnv("IT_DB_USER");
	}

	static String password() {
		String password = System.getenv("IT_DB_PASSWORD");
		return password != null ? password : "";
	}

	/** Cada test usa un esquema propi per començar sempre des de zero. */
	static String newSchemaName() {
		return "it_" + UUID.randomUUID().toString().replace("-", "").toLowerCase(Locale.ROOT);
	}

	static void dropSchema(String schema) throws SQLException {
		try (Connection connection = DriverManager.getConnection(url(), user(), password());
				Statement statement = connection.createStatement()) {
			statement.execute("DROP SCHEMA IF EXISTS \"" + schema + "\" CASCADE");
		}
	}

	private static String databaseName(String url) {
		String withoutParameters = url.split("\\?", 2)[0];
		return withoutParameters.substring(withoutParameters.lastIndexOf('/') + 1).toLowerCase(Locale.ROOT);
	}

	private static String requiredEnv(String name) {
		String value = System.getenv(name);
		if (value == null || value.isBlank()) {
			throw new IllegalStateException("Falta la variable d'entorn " + name + " per als tests d'integració.");
		}
		return value;
	}
}
