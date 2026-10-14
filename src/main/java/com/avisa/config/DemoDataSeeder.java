package com.avisa.config;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Seeder de dades inicials per a la plataforma AVISA / JJAUX.
 * Inicialitza els usuaris (clients i professionals reals), tasques, ofertes i ressenyes.
 */
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
		seedReviews();
	}

	private void seedUsers() {
		// Assegurar usuari administrador
		upsertUser("u-admin", "Administrador General", "admin@admin.es", "adminadmin", "admin", null, "Administrador de la plataforma AVISA");

		// Clients
		upsertUser("u-1", "Marta Vila", "marta@jjaux.cat", "martamarta", "client", "600111222", "Clienta habitual. Propietària particular interessada en reformes de qualitat i manteniment de la llar.");
		upsertUser("u-client-2", "Albert Rovira", "albert@client.es", "albertalbert", "client", "610203040", "Propietari d'immobles de lloguer i client habitual d'AVISA per a serveis d'obra, reparacions i manteniment.");
		upsertUser("u-client-3", "Núria Puig", "nuria@client.es", "nurianuria", "client", "620304050", "Clienta particular que busca serveis ràpids i de màxima confiança per a disseny i pintura.");
		upsertUser("u-client-4", "Josep Casals", "josep@client.es", "josepjosep", "client", "630405060", "President de comunitat de veïns enfocat al manteniment periòdic i eficiència energètica de l'edifici.");

		// Professionals amb format nom@prof.es i contrasenya nomnom
		upsertUser("u-2", "Jordi Romero", "jordi@prof.es", "jordijordi", "professional", "612345678", "Constructor i paleta amb més de 15 anys d'experiència en reformes integrals, rehabilitació d'interiors i estructures.");
		upsertUser("u-3", "Clara Sanz", "clara@prof.es", "claraclara", "professional", "623456789", "Dissenyadora d'espais i restauradora d'interiors, especialitzada en disseny contemporani i acabats d'alta qualitat.");
		upsertUser("u-prof-pere", "Pere Font", "pere@prof.es", "perepere", "professional", "634567890", "Lampista autoritzat i instal·lador de gas. Especialista en calderes, calefacció, fuites d'aigua urgents, desbussos i tractament d'aigua per osmosi.");
		upsertUser("u-prof-marc", "Marc Soler", "marc@prof.es", "marcmarc", "professional", "645678901", "Instal·lador elèctric autoritzat en baixa tensió. Butlletins elèctrics oficials, renovació de quadres generals, instal·lacions LED i domòtica intel·ligent.");
		upsertUser("u-prof-laura", "Laura Ribas", "laura@prof.es", "lauralaura", "professional", "656789012", "Pintora professional i decoradora d'espais interiors. Alisat de parets, eliminació de gotelé, pintura plàstica ecològica i microciment decoratiu.");
		upsertUser("u-prof-joan", "Joan Valls", "joan@prof.es", "joanjoan", "professional", "667890123", "Fuster ebenista artesà. Fabricació i col·locació de portes de pas lacades, parquet laminat AC5, mobles de cuina a mida i armaris encastats.");
		upsertUser("u-prof-anna", "Anna Prat", "anna@prof.es", "annaanna", "professional", "678901234", "Tècnica frigorista i especialista en climatització RITE. Muntatge i manteniment d'aires condicionats inverter, bombes de calor i aerotèrmia.");
		upsertUser("u-prof-sergi", "Sergi Bosch", "sergi@prof.es", "sergisergi", "professional", "689012345", "Paleta i mestre d'obres. Reformes completes de banys i cuines, envans de pladur amb aïllament acústic, enrajolats de precisió i impermeabilitzacions.");
		upsertUser("u-prof-carme", "Carme Vidal", "carme@prof.es", "carmecarme", "professional", "690123456", "Especialista en neteja tècnica i desinfecció d'espais. Neteja integral de final d'obra, vidres en alçada, desgreixatge de cuines i poliment de terres.");
		upsertUser("u-prof-david", "David Morales", "david@prof.es", "daviddavid", "professional", "601234567", "Serraller professional d'urgències 24 hores. Obertura de portes sense danys, canvi de panys d'alta seguretat antibumping i escuts cuirassats.");
		upsertUser("u-prof-elena", "Elena Pons", "elena@prof.es", "elenaelena", "professional", "612987654", "Arquitecta tècnica i aparelladora col·legiada. Tramitació ràpida de cèdules d'habitabilitat, certificats d'eficiència energètica, ITE i llicències d'obres.");
	}

	private void upsertUser(String id, String nom, String email, String password, String rol, String telefon, String bio) {
		try {
			int updated = jdbcTemplate.update(
				"UPDATE usuaris SET nom = ?, email = ?, password = ?, rol = ?, telefon = ?, bio = ? WHERE id = ?",
				nom, email, password, rol, telefon, bio, id
			);
			if (updated == 0) {
				jdbcTemplate.update(
					"INSERT INTO usuaris (id, nom, email, password, rol, telefon, bio) VALUES (?, ?, ?, ?, ?, ?, ?)",
					id, nom, email, password, rol, telefon, bio
				);
			}
		} catch (Exception ignored) {}
	}

	private void seedTasks() {
		if (countRows("tasques") > 0) {
			return;
		}

		jdbcTemplate.update(
			"INSERT INTO tasques (id, titol, descripcio, categoria, client_nom, budget, estat) VALUES (?, ?, ?, ?, ?, ?, ?)",
			"T-101", "Disseny d’avatar i marca personal", "Necessitem una imatge gràfica i branding professional.", "Branding", "Marta Vila", "€350", "Oberta"
		);
		jdbcTemplate.update(
			"INSERT INTO tasques (id, titol, descripcio, categoria, client_nom, budget, estat) VALUES (?, ?, ?, ?, ?, ?, ?)",
			"T-102", "Desenvolupament d’una landing page", "Pàgina web ràpida amb formulari de contacte i adaptació mòbil.", "Web", "Marta Vila", "€800", "Oberta"
		);
		jdbcTemplate.update(
			"INSERT INTO tasques (id, titol, descripcio, categoria, client_nom, budget, estat) VALUES (?, ?, ?, ?, ?, ?, ?)",
			"T-103", "Edició de vídeo per promoció", "Muntatge de vídeo de 60 segons per a campanya publicitària.", "Multimèdia", "Marta Vila", "€450", "Oberta"
		);
		jdbcTemplate.update(
			"INSERT INTO tasques (id, titol, descripcio, categoria, client_nom, budget, estat) VALUES (?, ?, ?, ?, ?, ?, ?)",
			"T-104", "Reforma integral de bany principal", "Canvi de banyera per plat de dutxa de resina, enrajolat de parets i canvi de sanitaris.", "Reformes i Obres", "Marta Vila", "€2.400", "Oberta"
		);
		jdbcTemplate.update(
			"INSERT INTO tasques (id, titol, descripcio, categoria, client_nom, budget, estat) VALUES (?, ?, ?, ?, ?, ?, ?)",
			"T-105", "Instal·lació de quadre elèctric i revisió de línia", "Revisió de potència, canvi de diferencials i butlletí elèctric per a habitatge de 90m2.", "Electricitat", "Marta Vila", "€450", "Oberta"
		);
		jdbcTemplate.update(
			"INSERT INTO tasques (id, titol, descripcio, categoria, client_nom, budget, estat) VALUES (?, ?, ?, ?, ?, ?, ?)",
			"T-106", "Substitució de baixant i reparació de fuita d’aigua", "Reparació d’humitat a la cuina i canvi de tram de canonada general de desguàs.", "Lampisteria", "Marta Vila", "€380", "Oberta"
		);
		jdbcTemplate.update(
			"INSERT INTO tasques (id, titol, descripcio, categoria, client_nom, budget, estat) VALUES (?, ?, ?, ?, ?, ?, ?)",
			"T-107", "Instal·lació d’aire condicionat split inverter", "Muntatge d’unitat exterior a façana i unitat interior al menjador amb 4m de línia frigorífica.", "Climatització", "Marta Vila", "€600", "Oberta"
		);
		jdbcTemplate.update(
			"INSERT INTO tasques (id, titol, descripcio, categoria, client_nom, budget, estat) VALUES (?, ?, ?, ?, ?, ?, ?)",
			"T-108", "Pintura plàstica llisa per a pis de 3 habitacions", "Pintar sostres i parets en blanc mat, massillat de petites esquerdes i protecció integral de sòls.", "Pintura", "Marta Vila", "€850", "Oberta"
		);
		jdbcTemplate.update(
			"INSERT INTO tasques (id, titol, descripcio, categoria, client_nom, budget, estat) VALUES (?, ?, ?, ?, ?, ?, ?)",
			"T-109", "Instal·lació de 5 portes de pas lacades en blanc", "Substitució de portes interiors amb manetes d’acer inoxidable i tapajuntes nous.", "Fusteria", "Marta Vila", "€1.100", "Oberta"
		);
		jdbcTemplate.update(
			"INSERT INTO tasques (id, titol, descripcio, categoria, client_nom, budget, estat) VALUES (?, ?, ?, ?, ?, ?, ?)",
			"T-110", "Neteja a fons de final d’obra", "Neteja integral de vidres, marcs, persianes, sòls i sanitaris després de reforma.", "Neteja", "Marta Vila", "€280", "Oberta"
		);
	}

	private void seedOffers() {
		insertOfferIfAbsent("O-201", "T-102", "Jordi Romero", "€780", "Inclou maquetació responsive i SEO bàsic.");
		insertOfferIfAbsent("O-202", "T-101", "Clara Sanz", "€320", "Proposta amb moodboard i identitat visual.");
		insertOfferIfAbsent("O-204", "T-104", "Jordi Romero", "€2.250", "Inclou demolició, fontaneria completa, plat antilliscant i desescombro.");
		insertOfferIfAbsent("O-205", "T-104", "Clara Sanz", "€2.100", "Pressupost tancat amb materials de primera qualitat i garantia de 2 anys.");
		insertOfferIfAbsent("O-206", "T-105", "Jordi Romero", "€420", "Revisió reglamentària REBT, instal·lació de diferencials superimmunitzats i butlletí oficial.");
		insertOfferIfAbsent("O-207", "T-106", "Clara Sanz", "€350", "Localització amb càmera d’inspecció, substitució amb canonada de PVC insonoritzada i prova d’estanquitat.");
		insertOfferIfAbsent("O-208", "T-107", "Jordi Romero", "€560", "Instal·lador autoritzat RITE. Inclou suport antivibratori per a la unitat exterior i buit del circuit.");
		insertOfferIfAbsent("O-209", "T-108", "Clara Sanz", "€790", "Pintura plàstica rentable transpirable a 2 capes, masillat d’esquerdes i neteja posterior.");
		insertOfferIfAbsent("O-210", "T-109", "Jordi Romero", "€1.050", "Portes massisses lacades de fàbrica en blanc, tancaments magnètics silenciosos i col·locació ràpida.");
		insertOfferIfAbsent("O-211", "T-110", "Clara Sanz", "€260", "Desinfecció profunda de final d’obra amb màquines de vapor i productes ecològics neutres.");

		// Ofertes presentades pels nous professionals especialistes
		insertOfferIfAbsent("O-212", "T-106", "Pere Font", "€340", "Inspecció immediata de la fuita, reparació de canonada i garantia de 2 anys per escrit.");
		insertOfferIfAbsent("O-213", "T-105", "Marc Soler", "€410", "Canvi complet de proteccions amb diferencials superimmunitzats i butlletí tramitat en 24h.");
		insertOfferIfAbsent("O-214", "T-108", "Laura Ribas", "€750", "Alisat d'imperfeccions, dues capes de pintura rentadora ecològica i neteja completa.");
		insertOfferIfAbsent("O-215", "T-109", "Joan Valls", "€980", "Portes lacades massisses amb manetes d'acer inoxidable i ajust ràpid.");
		insertOfferIfAbsent("O-216", "T-107", "Anna Prat", "€550", "Equip split A+++ amb buit certificat de línies frigorífiques i suport exterior antivibratori.");
		insertOfferIfAbsent("O-217", "T-104", "Sergi Bosch", "€2.180", "Demolició ràpida, enrajolat de primera qualitat i retirada de runes homologada en contenidor.");
		insertOfferIfAbsent("O-218", "T-110", "Carme Vidal", "€250", "Neteja profunda amb màquines d'aigua a pressió i vapor a vidres, terres i rajoles.");
	}

	private void insertOfferIfAbsent(String id, String tascaId, String professional, String price, String note) {
		try {
			int exists = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ofertes WHERE id = ?", Integer.class, id);
			if (exists == 0) {
				jdbcTemplate.update(
					"INSERT INTO ofertes (id, tasca_id, professional, price, note, estat) VALUES (?, ?, ?, ?, ?, 'Pendent')",
					id, tascaId, professional, price, note
				);
			}
		} catch (Exception ignored) {}
	}

	private void seedReviews() {
		if (countRows("resenyes") >= 30) {
			return;
		}

		insertReviewIfAbsent("R-001", "u-1", "u-2", "T-104", 5, "Molt bon professional. Va realitzar la reforma integral del bany amb màxima netedat i acabats de primera qualitat.");
		insertReviewIfAbsent("R-002", "u-client-2", "u-2", "T-104", 5, "Puntual, serietat absoluta amb el pressupost pactat i sense retards en el termini d'entrega.");
		insertReviewIfAbsent("R-003", "u-client-4", "u-2", null, 4, "Molt bona feina de paleteria al replà de la comunitat. Bon tracte i comunicació constant durant tota l'obra.");

		insertReviewIfAbsent("R-004", "u-1", "u-3", "T-101", 5, "Té un gust excel·lent i va aportar solucions creatives que van transformar completament l'espai. Molt recomanable!");
		insertReviewIfAbsent("R-005", "u-client-3", "u-3", null, 5, "Molt professional, atenta al detall i sempre disposada a adaptar-se als canvis demanats amb amabilitat.");
		insertReviewIfAbsent("R-006", "u-client-2", "u-3", null, 4, "Assessorament de color i distribució molt encertat. Va complir les dates acordades.");

		insertReviewIfAbsent("R-007", "u-1", "u-prof-pere", "T-106", 5, "Va venir el mateix dia d'urgència per una fuita d'aigua a la cuina. Ràpid, net i molt eficaç. Preu molt just.");
		insertReviewIfAbsent("R-008", "u-client-2", "u-prof-pere", null, 5, "Ens va instal·lar la caldera nova de condensació i va deixar tot el circuit funcionant a la perfecció. Gran professional!");
		insertReviewIfAbsent("R-009", "u-client-4", "u-prof-pere", null, 4, "Molt bon lampista. Va canviar els desguassos i aixetes generals sense embrutar res. Totalment recomanable.");

		insertReviewIfAbsent("R-010", "u-client-3", "u-prof-marc", "T-105", 5, "Va actualitzar tot el quadre elèctric i ens va tramitar el butlletí blanc sense cap complicació. Feina impecable.");
		insertReviewIfAbsent("R-011", "u-client-4", "u-prof-marc", null, 5, "Molt coneixedor de la normativa vigent. Va detectar i solucionar una derivació oculta que ningú sabia trobar.");
		insertReviewIfAbsent("R-012", "u-1", "u-prof-marc", null, 4, "Instal·lació de lluminàries LED a tot el pis en un sol matí. Feina polida, ràpida i preu ajustat.");

		insertReviewIfAbsent("R-013", "u-1", "u-prof-laura", "T-108", 5, "Va allisar totes les parets d'un pis antic eliminant un gotelé molt gruixut. El resultat final sembla d'obra nova!");
		insertReviewIfAbsent("R-014", "u-client-3", "u-prof-laura", null, 5, "Pintura impecable, talls de sostre perfectes i protecció exhaustiva de tots els mobles i sòls. Una sort trobar-la.");
		insertReviewIfAbsent("R-015", "u-client-2", "u-prof-laura", null, 5, "Molt ràpida i perfeccionista. Pintura blanca satinada rentadora de qualitat aplicada a dues capes uniformes.");

		insertReviewIfAbsent("R-016", "u-1", "u-prof-joan", "T-109", 5, "Va col·locar el parquet sintètic de 90m2 i 5 portes blanques en temps rècord. Ajustaments mil·limètrics.");
		insertReviewIfAbsent("R-017", "u-client-2", "u-prof-joan", null, 4, "Molt bona feina construint un armari encastat a mida aprofitant un racó difícil sota l'escala.");
		insertReviewIfAbsent("R-018", "u-client-4", "u-prof-joan", null, 5, "Fuster de màxima solvència i materials resistents. Les manetes i tancaments magnètics funcionen amb gran suavitat.");

		insertReviewIfAbsent("R-019", "u-1", "u-prof-anna", "T-107", 5, "Instal·lació d'un split inverter impecable. La canaleta exterior ha quedat molt discreta i no fa gens de soroll.");
		insertReviewIfAbsent("R-020", "u-client-3", "u-prof-anna", null, 5, "Càrrega de gas, buit de circuit i neteja antibacteriana dels filtres. Molt puntual, neta i explicativa.");
		insertReviewIfAbsent("R-021", "u-client-2", "u-prof-anna", null, 4, "Gran professional de la climatització. Ens va assessorar sobre el consum eficient i la potència necessària.");

		insertReviewIfAbsent("R-022", "u-client-4", "u-prof-sergi", null, 5, "Va construir un envà insonoritzat amb llana de roca i pladur doble. L'aïllament del soroll és absolutament sorprenent.");
		insertReviewIfAbsent("R-023", "u-1", "u-prof-sergi", null, 4, "Molt bon paleta per a enrajolar la cuina. Rajoles rectificades de gran format anivellades a la perfecció.");
		insertReviewIfAbsent("R-024", "u-client-2", "u-prof-sergi", null, 5, "Molt complidor amb els terminis. Treball dur, serietat i neteja de runa diària en contenidor homologat.");

		insertReviewIfAbsent("R-025", "u-1", "u-prof-carme", "T-110", 5, "Neteja a fons després de setmanes de pols d'obra. Van deixar el pis lluent i a punt per entrar a viure sense tocar res.");
		insertReviewIfAbsent("R-026", "u-client-2", "u-prof-carme", null, 5, "Productes no abrasius de qualitat i resultat brillant als vidres exteriors i perfils d'alumini.");
		insertReviewIfAbsent("R-027", "u-client-3", "u-prof-carme", null, 5, "Molt bona coordinació d'equip. En un matí van deixar el local comercial completament higienitzat.");

		insertReviewIfAbsent("R-028", "u-client-3", "u-prof-david", null, 5, "Em vaig deixar les claus posades per dins un diumenge a la nit. Va arribar en 20 minuts i va obrir sense danyar la porta!");
		insertReviewIfAbsent("R-029", "u-client-4", "u-prof-david", null, 5, "Canvi de bombí vell per un Kaba antibumping amb 5 claus de seguretat. Molta serietat i rapidesa.");
		insertReviewIfAbsent("R-030", "u-1", "u-prof-david", null, 4, "Preu clar per avançat sense cap sorpresa desagradable a la factura. Treball impecable.");

		insertReviewIfAbsent("R-031", "u-client-2", "u-prof-elena", null, 5, "Presa de mesures, cèdula d'habitabilitat i certificat energètic tramitats i visats en només 48 hores.");
		insertReviewIfAbsent("R-032", "u-client-4", "u-prof-elena", null, 5, "Gran rigor en la inspecció tècnica de l'edifici (ITE). Informe detallat i assessorament clar per a la comunitat.");
		insertReviewIfAbsent("R-033", "u-1", "u-prof-elena", null, 5, "Gestió excel·lent de la llicència d'obres a l'ajuntament. Ens va evitar tràmits feixucs i esperes.");
	}

	private void insertReviewIfAbsent(String id, String clientId, String treballadorId, String tascaId, int puntuacio, String descripcio) {
		try {
			int exists = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM resenyes WHERE id = ?", Integer.class, id);
			if (exists == 0) {
				jdbcTemplate.update(
					"INSERT INTO resenyes (id, client_id, treballador_id, tasca_id, puntuacio, descripcio, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
					id, clientId, treballadorId, tascaId, puntuacio, descripcio, LocalDateTime.now().minusDays(1 + (int)(Math.random() * 30))
				);
			}
		} catch (Exception ignored) {}
	}

	private int countRows(String tableName) {
		Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Integer.class);
		return count != null ? count : 0;
	}
}