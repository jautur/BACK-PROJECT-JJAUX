package com.avisa.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Seeder de dades inicials per a la plataforma AVISA / JJAUX.
 * Introdueix usuaris (clients, professionals verificats i admin), ofertes,
 * incidències obertes, tasques en curs i valoracions de mostra perquè
 * la plataforma es veja viva, completa i plenament funcional en pantalles i vídeos.
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
		seedOfertes();
		seedIncidencies();
		seedTasques();
		seedResenyes();
	}

	// ==========================================
	// 1. USUARIS (CLIENTS I PROFESSIONALS VERIFICATS)
	// ==========================================
	private void seedUsers() {
		// Administrador General
		upsertUser("u-admin", "Administrador General", "admin@admin.es", "adminadmin", "admin", "960000000", "Administrador de la plataforma comunitària AVISA.", 3, true, false);

		// Usuaris base de proves
		upsertUser(
			"u-cli", "cli", "cli", "clicli", "client", "600111222",
			"Compte client de proves per a creació d'incidències.", 3, false, false
		);
		upsertUser(
			"u-pro", "Llevant Reformes & Obres Integrals S.L.", "pro", "propro", "professional", "963778899",
			"Empresa constructora homologada per a reformes integrals de vivendes i locals comercials. Projectes clau en mà amb direcció facultativa, paletes, enrajolats i coordinació de gremis.",
			4, true, false,
			"https://images.unsplash.com/photo-1541888946425-d0fbb186156f?w=800&auto=format&fit=crop&q=80",
			"https://images.unsplash.com/photo-1497366216548-37526070297c?w=300&auto=format&fit=crop&q=80",
			"https://images.unsplash.com/photo-1541888946425-d0fbb186156f?w=800&auto=format&fit=crop&q=80"
		);

		// Empreses professionals homologades amb distintiu oficial verificat, foto de perfil (isotip) i logo (fons targeta)
		upsertUser(
			"u-pro-fontanals",
			"Fontanals Lampisteria & Climatització S.L.",
			"contacte@fontanals.es",
			"123456",
			"professional",
			"963112233",
			"Empresa líder en lampisteria urgent 24h, reparació de canonades, descalcificadors, termos elèctrics i calderes a València i àrea metropolitana. Més de 15 anys de garantia i resposta immediata.",
			5,
			true,
			false,
			"https://images.unsplash.com/photo-1581244277943-fe4a9c777189?w=800&auto=format&fit=crop&q=80",
			"https://images.unsplash.com/photo-1507679799987-c73779587ccf?w=300&auto=format&fit=crop&q=80",
			"https://images.unsplash.com/photo-1581244277943-fe4a9c777189?w=800&auto=format&fit=crop&q=80"
		);

		upsertUser(
			"u-pro-electromartinez",
			"ElectroMartínez Instal·lacions Elèctriques S.L.",
			"info@electromartinez.es",
			"123456",
			"professional",
			"963223344",
			"Enginyeria i muntatges elèctrics autoritzats. Quadres industrials, domòtica domèstica, panells solars fotovoltaics, butlletins oficials i punts de recàrrega de vehicles elèctrics.",
			4,
			true,
			false,
			"https://images.unsplash.com/photo-1621905251189-08b45d6a269e?w=800&auto=format&fit=crop&q=80",
			"https://images.unsplash.com/photo-1560179707-f14e90ef3623?w=300&auto=format&fit=crop&q=80",
			"https://images.unsplash.com/photo-1621905251189-08b45d6a269e?w=800&auto=format&fit=crop&q=80"
		);

		upsertUser(
			"u-pro-solerpintures",
			"Soler Pintura & Alta Decoració S.L.",
			"info@solerpintura.es",
			"123456",
			"professional",
			"963334455",
			"Empresa familiar amb 25 anys d'experiència en alta decoració, pintura plàstica ecològica, allisats de parets, microciment continu i impermeabilització de façanes i terrasses.",
			4,
			true,
			false,
			"https://images.unsplash.com/photo-1589939705384-5185137a7f0f?w=800&auto=format&fit=crop&q=80",
			"https://images.unsplash.com/photo-1572021335469-31706a17aaef?w=300&auto=format&fit=crop&q=80",
			"https://images.unsplash.com/photo-1589939705384-5185137a7f0f?w=800&auto=format&fit=crop&q=80"
		);

		upsertUser(
			"u-pro-climabeneyto",
			"ClimaBeneyto Climatització & Aerotèrmia S.L.",
			"serveis@climabeneyto.es",
			"123456",
			"professional",
			"963556677",
			"Servei tècnic oficial multimarca. Instal·lació i manteniment d'aire condicionat, bombes de calor aerotèrmiques d'alta eficiència A+++, terra radiant i sistemes de ventilació mecànica.",
			5,
			true,
			false,
			"https://images.unsplash.com/photo-1621905252507-b35492cc74b4?w=800&auto=format&fit=crop&q=80",
			"https://images.unsplash.com/photo-1486406146926-c627a92ad1ab?w=300&auto=format&fit=crop&q=80",
			"https://images.unsplash.com/photo-1621905252507-b35492cc74b4?w=800&auto=format&fit=crop&q=80"
		);

		// Empresa professional amb sol·licitud de verificació pendent de trucada
		upsertUser(
			"u-pro-fusteriaribes",
			"Fusteria Artesana Ribes & Fills S.L.",
			"tallers@fusteriaribes.es",
			"123456",
			"professional",
			"963667788",
			"Fusteria a mida i ebenisteria arquitectònica. Fabricació pròpia de cuines de disseny, portes acústiques, vestidors integrats i restauració de fustes nobles per a vivendes i negocis.",
			3,
			false,
			true,
			"https://images.unsplash.com/photo-1540555700478-4be289fbecef?w=800&auto=format&fit=crop&q=80",
			"https://images.unsplash.com/photo-1513694203232-719a280e022f?w=300&auto=format&fit=crop&q=80",
			"https://images.unsplash.com/photo-1540555700478-4be289fbecef?w=800&auto=format&fit=crop&q=80"
		);

		upsertUser(
			"u-pro-llevant",
			"Llevant Reformes & Obres Integrals S.L.",
			"projectes@llevantreformes.es",
			"123456",
			"professional",
			"963778899",
			"Empresa constructora homologada per a reformes integrals de vivendes i locals comercials. Projectes clau en mà amb direcció facultativa, paletes, enrajolats i coordinació de gremis.",
			4,
			true,
			false,
			"https://images.unsplash.com/photo-1541888946425-d0fbb186156f?w=800&auto=format&fit=crop&q=80",
			"https://images.unsplash.com/photo-1497366216548-37526070297c?w=300&auto=format&fit=crop&q=80",
			"https://images.unsplash.com/photo-1541888946425-d0fbb186156f?w=800&auto=format&fit=crop&q=80"
		);

		// Clients particulars de la comunitat
		upsertUser("u-cli-joan", "Joan Navarro", "joan@client.es", "123456", "client", "666112233", "Client particular resident a l'Horta Sud.", 3, false, false);
		upsertUser("u-cli-marta", "Marta Albiol", "marta@client.es", "123456", "client", "677223344", "Clienta particular a Gandia.", 3, false, false);
		upsertUser("u-cli-pau", "Pau Ferrandis", "pau@client.es", "123456", "client", "688334455", "Client particular resident a València.", 3, false, false);
	}

	private void upsertUser(String id, String nom, String email, String password, String rol, String telefon, String bio, int percentil, boolean verificat, boolean sollicitudVerificacio) {
		upsertUser(id, nom, email, password, rol, telefon, bio, percentil, verificat, sollicitudVerificacio, null, null, null);
	}

	private void upsertUser(String id, String nom, String email, String password, String rol, String telefon, String bio, int percentil, boolean verificat, boolean sollicitudVerificacio, String foto) {
		upsertUser(id, nom, email, password, rol, telefon, bio, percentil, verificat, sollicitudVerificacio, foto, null, foto);
	}

	private void upsertUser(String id, String nom, String email, String password, String rol, String telefon, String bio, int percentil, boolean verificat, boolean sollicitudVerificacio, String foto, String fotoPerfil, String logo) {
		try {
			Integer exists = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM usuaris WHERE LOWER(email) = LOWER(?) OR LOWER(nom) = LOWER(?) OR id = ?",
				Integer.class, email, nom, id
			);
			if (exists != null && exists > 0) {
				jdbcTemplate.update(
					"UPDATE usuaris SET nom = ?, email = ?, password = ?, rol = ?, telefon = ?, bio = ?, percentil = ?, verificat = ?, sollicitud_verificacio = ?, foto = COALESCE(?, foto), foto_perfil = COALESCE(?, foto_perfil), logo = COALESCE(?, logo) WHERE LOWER(email) = LOWER(?) OR LOWER(nom) = LOWER(?) OR id = ?",
					nom, email, password, rol, telefon, bio, percentil, verificat, sollicitudVerificacio, foto, fotoPerfil, logo, email, nom, id
				);
			} else {
				jdbcTemplate.update(
					"INSERT INTO usuaris (id, nom, email, password, rol, telefon, bio, percentil, verificat, sollicitud_verificacio, foto, foto_perfil, logo) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
					id, nom, email, password, rol, telefon, bio, percentil, verificat, sollicitudVerificacio, foto, fotoPerfil, logo
				);
			}
		} catch (Exception ignored) {}
	}

	// ==========================================
	// 2. OFERTES DE PROFESSIONALS AL MERCAT
	// ==========================================
	private void seedOfertes() {
		upsertOferta(
			"O-101",
			"u-pro-fontanals",
			"Fontanals Lampisteria & Climatització S.L.",
			"lampisteria",
			"45€ / hora",
			"Reparació urgent de fuites d'aigua, desbussament d'aigüeres, canvi d'aixetes i manteniment de calderes. Arribada en menys de 45 minuts."
		);

		upsertOferta(
			"O-102",
			"u-pro-electromartinez",
			"ElectroMartínez Instal·lacions Elèctriques S.L.",
			"electricitat",
			"60€",
			"Diagnòstic de curtcircuits, canvi de diferencials i revisió reglamentària del quadre elèctric amb garantia oficial."
		);

		upsertOferta(
			"O-103",
			"u-pro-solerpintures",
			"Soler Pintura & Alta Decoració S.L.",
			"pintura",
			"350€",
			"Pintura completa de saló o habitació amb pintura rentada d'alta cobertura i protecció total de mobles i sòl."
		);

		upsertOferta(
			"O-104",
			"u-pro-fusteriaribes",
			"Fusteria Artesana Ribes & Fills S.L.",
			"fusteria",
			"120€",
			"Ajustament de portes que freguen, raspallat, canvi de panys de seguretat i reparació de calaixos encallats."
		);

		upsertOferta(
			"O-105",
			"u-pro-climabeneyto",
			"ClimaBeneyto Climatització & Aerotèrmia S.L.",
			"climatitzacio",
			"85€",
			"Neteja profunda de filtres, desinfecció antibacteriana i revisió de pressió de gas per a equips domèstics d'aire condicionat."
		);

		upsertOferta(
			"O-106",
			"u-pro-llevant",
			"Llevant Reformes & Obres Integrals S.L.",
			"reformes",
			"95€",
			"Treballs ràpids d'obra menor, enrajolat de zones humides i petites reparacions de paleta per a la llar."
		);
	}

	private void upsertOferta(String id, String proId, String proNom, String categoria, String price, String note) {
		try {
			Integer exists = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ofertes WHERE id = ?", Integer.class, id);
			if (exists != null && exists > 0) {
				jdbcTemplate.update(
					"UPDATE ofertes SET professional = ?, price = ?, note = ?, categoria = ?, estat = 'Pendent' WHERE id = ?",
					proNom, price, note, categoria, id
				);
			} else {
				jdbcTemplate.update(
					"INSERT INTO ofertes (id, professional, price, note, categoria, estat) VALUES (?, ?, ?, ?, ?, 'Pendent')",
					id, proNom, price, note, categoria
				);
			}

			// Sincronitza amb la taula publicacions perquè aparega en totes les cerques
			Integer pubExists = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM publicacions WHERE id = ?", Integer.class, id);
			if (pubExists != null && pubExists > 0) {
				jdbcTemplate.update(
					"UPDATE publicacions SET usuari_id = ?, usuari_nom = ?, tipus = 'oferta', titol = ?, descripcio = ?, categoria = ?, preu_o_pressupost = ? WHERE id = ?",
					proId, proNom, "Servei de " + categoria + ": " + proNom, note, categoria, price, id
				);
			} else {
				jdbcTemplate.update(
					"INSERT INTO publicacions (id, usuari_id, usuari_nom, tipus, titol, descripcio, categoria, preu_o_pressupost) VALUES (?, ?, ?, 'oferta', ?, ?, ?, ?)",
					id, proId, proNom, "Servei de " + categoria + ": " + proNom, note, categoria, price
				);
			}
		} catch (Exception ignored) {}
	}

	// ==========================================
	// 3. INCIDÈNCIES OBERTES DE CLIENTS
	// ==========================================
	private void seedIncidencies() {
		upsertIncidencia(
			"INC-101",
			"u-cli-joan",
			"Joan Navarro",
			"Goteig constant sota la pica de la cuina",
			"Ha començat a perdre aigua pel sifó des d'ahir de nit i necessite canviar la junta abans que danye el moble de fusta.",
			"lampisteria",
			"50€"
		);

		upsertIncidencia(
			"INC-102",
			"u-cli-marta",
			"Marta Albiol",
			"Salta el diferencial quan encenem el forn",
			"En posar en marxa el forn elèctric es queda tota la vivenda sense llum. Sembla una derivació interna o un fusible cremat.",
			"electricitat",
			"75€"
		);

		upsertIncidencia(
			"INC-103",
			"u-cli-pau",
			"Pau Ferrandis",
			"Pintar passadís i sanejar petita humitat",
			"Passadís d'uns 10 metres lineals amb una taca d'humitat ja eixuta. Cal raspar, sanejar i donar dues capes de pintura blanca.",
			"pintura",
			"160€"
		);

		upsertIncidencia(
			"INC-104",
			"u-cli",
			"cli",
			"Reparar persiana de fusta que s'ha encallat",
			"La corretja s'ha soltat de l'eix superior a la finestra del menjador i no es pot pujar ni baixar.",
			"fusteria",
			"45€"
		);
	}

	private void upsertIncidencia(String id, String clientId, String clientNom, String titol, String descripcio, String categoria, String budget) {
		try {
			Integer exists = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM incidencies WHERE id = ?", Integer.class, id);
			if (exists != null && exists > 0) {
				jdbcTemplate.update(
					"UPDATE incidencies SET client_id = ?, client_nom = ?, titol = ?, descripcio = ?, categoria = ?, budget = ?, estat = 'Oberta' WHERE id = ?",
					clientId, clientNom, titol, descripcio, categoria, budget, id
				);
			} else {
				jdbcTemplate.update(
					"INSERT INTO incidencies (id, client_id, client_nom, titol, descripcio, categoria, budget, estat) VALUES (?, ?, ?, ?, ?, ?, ?, 'Oberta')",
					id, clientId, clientNom, titol, descripcio, categoria, budget
				);
			}

			// Sincronitza amb la taula publicacions
			Integer pubExists = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM publicacions WHERE id = ?", Integer.class, id);
			if (pubExists != null && pubExists > 0) {
				jdbcTemplate.update(
					"UPDATE publicacions SET usuari_id = ?, usuari_nom = ?, tipus = 'incidencia', titol = ?, descripcio = ?, categoria = ?, preu_o_pressupost = ? WHERE id = ?",
					clientId, clientNom, titol, descripcio, categoria, budget, id
				);
			} else {
				jdbcTemplate.update(
					"INSERT INTO publicacions (id, usuari_id, usuari_nom, tipus, titol, descripcio, categoria, preu_o_pressupost) VALUES (?, ?, ?, 'incidencia', ?, ?, ?, ?)",
					id, clientId, clientNom, titol, descripcio, categoria, budget
				);
			}
		} catch (Exception ignored) {}
	}

	// ==========================================
	// 4. TASQUES (EN CURS I FINALITZADES AMB VALORACIONS)
	// ==========================================
	private void seedTasques() {
		upsertTasca(
			"T-101",
			"Substitució de termo elèctric de 80L",
			"Desmuntatge de l'aparell vell defectuós i connexió d'un nou termo amb vàlvula de seguretat.",
			"lampisteria",
			"u-cli-joan",
			"Joan Navarro",
			"u-pro-fontanals",
			"Fontanals Lampisteria & Climatització S.L.",
			"180€",
			"Finalitzada",
			5,
			"L'equip de Fontanals va ser puntualíssim i molt polit. Van desmuntar el termo vell i van deixar el nou funcionant impecable en dues hores."
		);

		upsertTasca(
			"T-102",
			"Instal·lació de línia independent per a inducció",
			"Tirada de cable de 6mm des del quadre fins a la cuina amb magnetotèrmic dedicat de 25A.",
			"electricitat",
			"u-cli-marta",
			"Marta Albiol",
			"u-pro-electromartinez",
			"ElectroMartínez Instal·lacions Elèctriques S.L.",
			"95€",
			"Finalitzada",
			5,
			"Treball impecable! ElectroMartínez és una gran empresa, ens van explicar tot el quadre i van deixar la cuina perfecta."
		);

		upsertTasca(
			"T-103",
			"Pintura del dormitori principal i sostre",
			"Dues capes de pintura plàstica color blanc trencat i tractament preventiu antihumitat.",
			"pintura",
			"u-cli-pau",
			"Pau Ferrandis",
			"u-pro-solerpintures",
			"Soler Pintura & Alta Decoració S.L.",
			"220€",
			"Finalitzada",
			4,
			"Molt bon acabat i pintura d'alta qualitat. Van protegir el terra a la perfecció i van acabar abans del previst."
		);

		upsertTasca(
			"T-104",
			"Manteniment i càrrega de gas aire condicionat",
			"Revisió de vàlvules, substitució de filtres i càrrega ecològica R32 a split domèstic.",
			"climatitzacio",
			"u-cli-joan",
			"Joan Navarro",
			"u-pro-climabeneyto",
			"ClimaBeneyto Climatització & Aerotèrmia S.L.",
			"85€",
			"Finalitzada",
			5,
			"Molt ràpids i eficients. Ara l'equip refreda de meravella i fa molt menys soroll que abans."
		);

		upsertTasca(
			"T-105",
			"Ajustament de porta d'entrada que frega el terra",
			"Desmuntatge de frontisses, raspallat inferior de 3mm i envernissat de cantells.",
			"fusteria",
			"u-cli",
			"cli",
			"u-pro",
			"Llevant Reformes & Obres Integrals S.L.",
			"60€",
			"En curs",
			null,
			null
		);
	}

	private void upsertTasca(String id, String titol, String descripcio, String categoria, String clientId, String clientNom, String proId, String proNom, String preu, String estat, Integer valoracio, String opinio) {
		try {
			Integer exists = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM tasques WHERE id = ?", Integer.class, id);
			if (exists != null && exists > 0) {
				jdbcTemplate.update(
					"UPDATE tasques SET titol = ?, descripcio = ?, categoria = ?, client_id = ?, client_nom = ?, professional_id = ?, professional_nom = ?, preu_acordat = ?, budget = ?, estat = ?, valoracio = ?, opinio = ? WHERE id = ?",
					titol, descripcio, categoria, clientId, clientNom, proId, proNom, preu, preu, estat, valoracio, opinio, id
				);
			} else {
				jdbcTemplate.update(
					"INSERT INTO tasques (id, titol, descripcio, categoria, client_id, client_nom, professional_id, professional_nom, preu_acordat, budget, estat, tipus, valoracio, opinio) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'tasca', ?, ?)",
					id, titol, descripcio, categoria, clientId, clientNom, proId, proNom, preu, preu, estat, valoracio, opinio
				);
			}
		} catch (Exception ignored) {}
	}

	// ==========================================
	// 5. RESSENYES PÚBLIQUES PER ALS MODALS
	// ==========================================
	private void seedResenyes() {
		upsertResenya("RES-101", "u-cli-joan", "Joan Navarro", "u-pro-fontanals", "Fontanals Lampisteria & Climatització S.L.", "T-101", 5, "L'equip de Fontanals va ser puntualíssim i molt polit. Van desmuntar el termo vell i van deixar el nou funcionant impecable en dues hores.");
		upsertResenya("RES-102", "u-cli-marta", "Marta Albiol", "u-pro-electromartinez", "ElectroMartínez Instal·lacions Elèctriques S.L.", "T-102", 5, "Treball impecable! ElectroMartínez és una gran empresa, ens van explicar tot el quadre i va deixar la cuina perfecta.");
		upsertResenya("RES-103", "u-cli-pau", "Pau Ferrandis", "u-pro-solerpintures", "Soler Pintura & Alta Decoració S.L.", "T-103", 4, "Molt bon acabat i pintura d'alta qualitat. Van protegir el terra a la perfecció i van acabar abans del previst.");
		upsertResenya("RES-104", "u-cli-joan", "Joan Navarro", "u-pro-climabeneyto", "ClimaBeneyto Climatització & Aerotèrmia S.L.", "T-104", 5, "Molt ràpids i eficients. Ara l'equip refreda de meravella i fa molt menys soroll que abans.");
	}

	private void upsertResenya(String id, String clientId, String clientNom, String treballadorId, String treballadorNom, String tascaId, int puntuacio, String descripcio) {
		try {
			Integer exists = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM resenyes WHERE id = ?", Integer.class, id);
			if (exists != null && exists > 0) {
				jdbcTemplate.update(
					"UPDATE resenyes SET client_id = ?, client_nom = ?, treballador_id = ?, treballador_nom = ?, tasca_id = ?, puntuacio = ?, descripcio = ? WHERE id = ?",
					clientId, clientNom, treballadorId, treballadorNom, tascaId, puntuacio, descripcio, id
				);
			} else {
				jdbcTemplate.update(
					"INSERT INTO resenyes (id, client_id, client_nom, treballador_id, treballador_nom, tasca_id, puntuacio, descripcio) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
					id, clientId, clientNom, treballadorId, treballadorNom, tascaId, puntuacio, descripcio
				);
			}
		} catch (Exception ignored) {}
	}
}