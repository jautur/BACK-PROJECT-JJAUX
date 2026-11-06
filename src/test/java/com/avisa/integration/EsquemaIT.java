package com.avisa.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Comprova que la base de dades imposa per si sola les regles del disseny.
 */
class EsquemaIT extends PostgresIntegrationTest {

	@Autowired
	private JdbcTemplate jdbc;

	private UUID client;
	private UUID professional;
	private UUID admin;
	private UUID incidencia;
	private UUID oferta;

	@BeforeEach
	void dadesBase() {
		jdbc.execute("TRUNCATE usuaris, blacklist_telefons CASCADE");
		client = usuari("CLIENT");
		professional = usuari("PROFESSIONAL");
		admin = usuari("ADMIN");
		incidencia = publicacio("INCIDENCIA", "OBERTA", client);
		oferta = publicacio("OFERTA", "ACTIVA", professional);
	}

	@Test
	@DisplayName("Rebutja un rol fora del disseny i un correu repetit sense distingir majúscules")
	void validaUsuaris() {
		assertRebutjat("INSERT INTO usuaris (nom, email, password, rol) VALUES ('x', 'x@x.es', '$2a$10$n5f0jmVZUnF5mzfnxuBFUOaUGBZ.MDhQug1rbtTUoeCyj/3rTUE8K', 'superhacker')");

		jdbc.update("INSERT INTO usuaris (nom, email, password, rol) VALUES ('a', 'Mateix@x.es', '$2a$10$n5f0jmVZUnF5mzfnxuBFUOaUGBZ.MDhQug1rbtTUoeCyj/3rTUE8K', 'CLIENT')");
		assertRebutjat("INSERT INTO usuaris (nom, email, password, rol) VALUES ('b', 'mateix@x.es', '$2a$10$n5f0jmVZUnF5mzfnxuBFUOaUGBZ.MDhQug1rbtTUoeCyj/3rTUE8K', 'CLIENT')");
	}

	@Test
	@DisplayName("L'estat de la publicació ha de correspondre al seu tipus")
	void estatSegonsTipus() {
		assertThatThrownBy(() -> publicacio("OFERTA", "OBERTA", professional))
			.isInstanceOf(DataIntegrityViolationException.class);
		assertThatThrownBy(() -> publicacio("INCIDENCIA", "ACTIVA", client))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	@DisplayName("Un sol pressupost per professional i un sol acceptat per incidència")
	void reglesDePressupostos() {
		UUID altre = usuari("PROFESSIONAL");
		pressupost(professional, "ACCEPTAT");

		assertThatThrownBy(() -> pressupost(professional, "PENDENT"))
			.isInstanceOf(DataIntegrityViolationException.class);
		assertThatThrownBy(() -> pressupost(altre, "ACCEPTAT"))
			.isInstanceOf(DataIntegrityViolationException.class);
		pressupost(altre, "REBUTJAT");
	}

	@Test
	@DisplayName("Només un pressupost rebutjat pot estar marcat com a rebuig automàtic")
	void rebuigAutomaticNomesSiRebutjat() {
		assertRebutjat("INSERT INTO pressupostos (publicacio_id, professional_id, preu, estat, rebutjat_automaticament) VALUES ('" + incidencia + "', '" + professional + "', '1', 'PENDENT', TRUE)");
	}

	@Test
	@DisplayName("Una sola sol·licitud activa per client i oferta, però es pot tornar a sol·licitar")
	void unaSollicitudActiva() {
		sollicitud("REBUTJADA");
		sollicitud("SOLLICITADA");

		assertThatThrownBy(() -> sollicitud("SOLLICITADA"))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	@DisplayName("Les dades de la tasca han de ser coherents amb el seu estat")
	void coherenciaDeTasques() {
		assertRebutjat(tascaSql("EN_CURS", "NULL", "NULL", "NULL", "NULL"));
		assertRebutjat(tascaSql("FINALITZADA", "now()", "NULL", "NULL", "NULL"));
		assertRebutjat(tascaSql("EN_CURS", "now()", "now()", "NULL", "NULL"));
		assertRebutjat(tascaSql("EN_CURS", "now()", "NULL", "5", "now()"));
		assertRebutjat(tascaSql("FINALITZADA", "now()", "now()", "6", "now()"));
		assertRebutjat(tascaSql("FINALITZADA", "now()", "now()", "4", "NULL"));

		jdbc.update(tascaSql("FINALITZADA", "now()", "now()", "4", "now()"));
	}

	@Test
	@DisplayName("El client i el professional d'una tasca han de ser persones diferents")
	void partsDiferents() {
		assertRebutjat("INSERT INTO tasques (publicacio_id, client_id, professional_id, preu_acordat, estat) VALUES ('"
			+ oferta + "', '" + client + "', '" + client + "', '10€', 'SOLLICITADA')");
	}

	@Test
	@DisplayName("Una tasca només té una cancel·lació pendent, i es pot tornar a sol·licitar si es rebutja")
	void unaCancellacioPendent() {
		UUID tasca = tascaEnCurs();
		cancellacio(tasca);

		assertThatThrownBy(() -> cancellacio(tasca)).isInstanceOf(DataIntegrityViolationException.class);

		jdbc.update("UPDATE sollicituds_cancellacio SET resolucio = 'REBUTJADA', admin_id = ?, data_resolucio = now()", admin);
		cancellacio(tasca);
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sollicituds_cancellacio", Integer.class)).isEqualTo(2);
	}

	@Test
	@DisplayName("La negligència exigeix responsable, i tota resolució exigeix admin i data")
	void resolucioDeCancellacions() {
		UUID tasca = tascaEnCurs();
		cancellacio(tasca);

		assertThatThrownBy(() -> jdbc.update(
			"UPDATE sollicituds_cancellacio SET resolucio = 'NEGLIGENCIA', admin_id = ?, data_resolucio = now()", admin))
			.isInstanceOf(DataIntegrityViolationException.class);
		assertThatThrownBy(() -> jdbc.update(
			"UPDATE sollicituds_cancellacio SET resolucio = 'SENSE_CONSEQUENCIES', data_resolucio = now()"))
			.isInstanceOf(DataIntegrityViolationException.class);
		assertThatThrownBy(() -> jdbc.update(
			"UPDATE sollicituds_cancellacio SET resolucio = 'SENSE_CONSEQUENCIES', admin_id = ?, responsable_id = ?, data_resolucio = now()",
			admin, professional))
			.isInstanceOf(DataIntegrityViolationException.class);

		jdbc.update("UPDATE sollicituds_cancellacio SET resolucio = 'NEGLIGENCIA', admin_id = ?, responsable_id = ?, data_resolucio = now()",
			admin, professional);
	}

	@Test
	@DisplayName("No es pot esborrar un usuari amb historial")
	void usuariAmbHistorial() {
		assertThatThrownBy(() -> jdbc.update("DELETE FROM usuaris WHERE id = ?", client))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	@DisplayName("Els avisos s'esborren amb el destinatari i conserven el missatge si s'esborra l'emissor")
	void avisos() {
		UUID emissor = usuari("ADMIN");
		UUID destinatari = usuari("CLIENT");
		jdbc.update("INSERT INTO avisos (destinatari_id, emissor_id, missatge) VALUES (?, ?, 'hola')", destinatari, emissor);

		jdbc.update("DELETE FROM usuaris WHERE id = ?", emissor);
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM avisos WHERE emissor_id IS NULL", Integer.class)).isEqualTo(1);

		jdbc.update("DELETE FROM usuaris WHERE id = ?", destinatari);
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM avisos", Integer.class)).isZero();
	}

	private UUID usuari(String rol) {
		UUID id = UUID.randomUUID();
		jdbc.update("INSERT INTO usuaris (id, nom, email, password, rol) VALUES (?, ?, ?, '$2a$10$n5f0jmVZUnF5mzfnxuBFUOaUGBZ.MDhQug1rbtTUoeCyj/3rTUE8K', ?)",
			id, "Usuari " + id, id + "@test.es", rol);
		return id;
	}

	private UUID publicacio(String tipus, String estat, UUID autor) {
		UUID id = UUID.randomUUID();
		jdbc.update("INSERT INTO publicacions (id, tipus, autor_id, titol, categoria, estat) VALUES (?, ?, ?, 'títol', 'Altres', ?)",
			id, tipus, autor, estat);
		return id;
	}

	private void pressupost(UUID autor, String estat) {
		jdbc.update("INSERT INTO pressupostos (publicacio_id, professional_id, preu, estat) VALUES (?, ?, '100€', ?)",
			incidencia, autor, estat);
	}

	private void sollicitud(String estat) {
		jdbc.update("INSERT INTO tasques (publicacio_id, client_id, professional_id, preu_acordat, estat) VALUES (?, ?, ?, '10€', ?)",
			oferta, client, professional, estat);
	}

	private String tascaSql(String estat, String inici, String finalitzacio, String valoracio, String dataValoracio) {
		return "INSERT INTO tasques (publicacio_id, client_id, professional_id, preu_acordat, estat, data_inici, data_finalitzacio, valoracio, data_valoracio) VALUES ('"
			+ oferta + "', '" + client + "', '" + professional + "', '10€', '" + estat + "', " + inici + ", " + finalitzacio + ", " + valoracio + ", " + dataValoracio + ")";
	}

	private UUID tascaEnCurs() {
		UUID id = UUID.randomUUID();
		jdbc.update("INSERT INTO tasques (id, publicacio_id, client_id, professional_id, preu_acordat, estat, data_inici) VALUES (?, ?, ?, ?, '10€', 'EN_CURS', now())",
			id, oferta, client, professional);
		return id;
	}

	private void cancellacio(UUID tasca) {
		jdbc.update("INSERT INTO sollicituds_cancellacio (tasca_id, sollicitant_id, motiu) VALUES (?, ?, 'motiu')", tasca, client);
	}

	private void assertRebutjat(String sql) {
		assertThatThrownBy(() -> jdbc.update(sql)).isInstanceOf(DataIntegrityViolationException.class);
	}
}
