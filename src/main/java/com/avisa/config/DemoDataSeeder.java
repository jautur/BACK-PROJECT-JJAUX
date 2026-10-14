package com.avisa.config;

import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DemoDataSeeder implements ApplicationRunner {

	private final JdbcTemplate jdbcTemplate;

	public DemoDataSeeder(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public void run(ApplicationArguments args) {
		seedUsers();
		seedTasks();
		seedOffers();
	}

	private void seedUsers() {
		if (countRows("usuaris") > 0) {
			return;
		}

		jdbcTemplate.update(
			"INSERT INTO usuaris (id, nom, email, password, rol, telefon, bio) VALUES (?, ?, ?, ?, ?, ?, ?)",
			"u-demo-client-1", "Marta Client", "marta.client@demo.local", "123456", "Client", "600111222", "Client que necessita suport puntual en projectes digitals"
		);
		jdbcTemplate.update(
			"INSERT INTO usuaris (id, nom, email, password, rol, telefon, bio) VALUES (?, ?, ?, ?, ?, ?, ?)",
			"u-demo-prof-1", "Pau Disseny", "pau.disseny@demo.local", "123456", "Professional", "600222333", "Dissenyador orientat a web i identitat visual"
		);
		jdbcTemplate.update(
			"INSERT INTO usuaris (id, nom, email, password, rol, telefon, bio) VALUES (?, ?, ?, ?, ?, ?, ?)",
			"u-demo-prof-2", "Laia Web", "laia.web@demo.local", "123456", "Professional", "600333444", "Desenvolupadora especialitzada en interfaces i landing pages"
		);
	}

	private void seedTasks() {
		if (countRows("tasques") > 0) {
			return;
		}

		jdbcTemplate.update(
			"INSERT INTO tasques (id, titol, descripcio, categoria, client_nom, budget, estat) VALUES (?, ?, ?, ?, ?, ?, ?)",
			"T-901", "Web corporativa per a un estudi creatiu", "Necessitem una web moderna amb portada, serveis i contacte.", "Web", "Marta Vila", "€1.200", "Oberta"
		);
		jdbcTemplate.update(
			"INSERT INTO tasques (id, titol, descripcio, categoria, client_nom, budget, estat) VALUES (?, ?, ?, ?, ?, ?, ?)",
			"T-902", "Reforma integral de bany principal", "Canvi de banyera per plat de dutxa de resina, enrajolat de parets i canvi de sanitaris.", "Reformes i Obres", "Marta Vila", "€2.400", "Oberta"
		);
		jdbcTemplate.update(
			"INSERT INTO tasques (id, titol, descripcio, categoria, client_nom, budget, estat) VALUES (?, ?, ?, ?, ?, ?, ?)",
			"T-903", "Instal·lació de quadre elèctric i revisió de línia", "Revisió de potència, canvi de diferencials i butlletí elèctric per a habitatge de 90m2.", "Electricitat", "Marta Vila", "€450", "Oberta"
		);
		jdbcTemplate.update(
			"INSERT INTO tasques (id, titol, descripcio, categoria, client_nom, budget, estat) VALUES (?, ?, ?, ?, ?, ?, ?)",
			"T-904", "Substitució de baixant i reparació de fuita d’aigua", "Reparació d’humitat a la cuina i canvi de tram de canonada general de desguàs.", "Lampisteria", "Marta Vila", "€380", "Oberta"
		);
		jdbcTemplate.update(
			"INSERT INTO tasques (id, titol, descripcio, categoria, client_nom, budget, estat) VALUES (?, ?, ?, ?, ?, ?, ?)",
			"T-905", "Instal·lació d’aire condicionat split inverter", "Muntatge d’unitat exterior a façana i unitat interior al menjador amb 4m de línia frigorífica.", "Climatització", "Marta Vila", "€600", "Oberta"
		);
		jdbcTemplate.update(
			"INSERT INTO tasques (id, titol, descripcio, categoria, client_nom, budget, estat) VALUES (?, ?, ?, ?, ?, ?, ?)",
			"T-906", "Pintura plàstica llisa per a pis de 3 habitacions", "Pintar sostres i parets en blanc mat, massillat de petites esquerdes i protecció integral de sòls.", "Pintura", "Marta Vila", "€850", "Oberta"
		);
		jdbcTemplate.update(
			"INSERT INTO tasques (id, titol, descripcio, categoria, client_nom, budget, estat) VALUES (?, ?, ?, ?, ?, ?, ?)",
			"T-907", "Instal·lació de 5 portes de pas lacades en blanc", "Substitució de portes interiors amb manetes d’acer inoxidable i tapajuntes nous.", "Fusteria", "Marta Vila", "€1.100", "Oberta"
		);
		jdbcTemplate.update(
			"INSERT INTO tasques (id, titol, descripcio, categoria, client_nom, budget, estat) VALUES (?, ?, ?, ?, ?, ?, ?)",
			"T-908", "Neteja a fons de final d’obra", "Neteja integral de vidres, marcs, persianes, sòls i sanitaris després de reforma.", "Neteja", "Marta Vila", "€280", "Oberta"
		);
	}

	private void seedOffers() {
		if (countRows("ofertes") > 0) {
			return;
		}

		List<String> taskIds = jdbcTemplate.queryForList("SELECT id FROM tasques ORDER BY created_at ASC", String.class);
		if (taskIds.isEmpty()) {
			return;
		}

		insertOffer("O-901", taskIds.get(0), "Jordi Romero", "€1.050", "Proposta amb entrega en 3 fases i revisió visual completa.", "Pendent");
		if (taskIds.size() > 1) {
			insertOffer("O-902", taskIds.get(1), "Jordi Romero", "€2.250", "Demolició, fontaneria completa, plat antilliscant i desescombro.", "Pendent");
			insertOffer("O-903", taskIds.get(1), "Clara Sanz", "€2.100", "Pressupost tancat amb materials de primera qualitat i garantia.", "Pendent");
		}
		if (taskIds.size() > 2) {
			insertOffer("O-904", taskIds.get(2), "Jordi Romero", "€420", "Revisió reglamentària REBT, diferencials i butlletí oficial.", "Pendent");
		}
		if (taskIds.size() > 3) {
			insertOffer("O-905", taskIds.get(3), "Clara Sanz", "€350", "Inspecció amb càmera tèrmica i canonada PVC insonoritzada.", "Pendent");
		}
		if (taskIds.size() > 4) {
			insertOffer("O-906", taskIds.get(4), "Jordi Romero", "€560", "Instal·lador autoritzat RITE amb buit i suport antivibratori.", "Pendent");
		}
		if (taskIds.size() > 5) {
			insertOffer("O-907", taskIds.get(5), "Clara Sanz", "€790", "Pintura rentable transpirable a 2 capes i protecció integral.", "Pendent");
		}
		if (taskIds.size() > 6) {
			insertOffer("O-908", taskIds.get(6), "Jordi Romero", "€1.050", "Portes massisses lacades de fàbrica en blanc i ajust ràpid.", "Pendent");
		}
		if (taskIds.size() > 7) {
			insertOffer("O-909", taskIds.get(7), "Clara Sanz", "€260", "Desinfecció profunda amb màquines de vapor i productes neutres.", "Pendent");
		}
	}

	private void insertOffer(String id, String tascaId, String professional, String price, String note, String estat) {
		jdbcTemplate.update(
			"INSERT INTO ofertes (id, tasca_id, professional, price, note, estat) VALUES (?, ?, ?, ?, ?, ?)",
			id, tascaId, professional, price, note, estat
		);
	}

	private int countRows(String tableName) {
		Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Integer.class);
		return count != null ? count : 0;
	}
}