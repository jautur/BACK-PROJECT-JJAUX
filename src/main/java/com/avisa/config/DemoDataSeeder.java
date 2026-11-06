package com.avisa.config;

import java.util.Locale;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Crea les empreses i els clients de demostració, amb la contrasenya de proves 123456.
 * Per això només s'activa en desenvolupament i a petició: perfils dev i demo alhora (SPRING_PROFILES_ACTIVE=dev,demo).
 * El perfil demo tot sol compta com a producció i no crea res.
 * Mai sobreescriu: si l'usuari ja existeix, no el toca.
 */
@Component
@Profile("dev & demo")
@Order(2)
public class DemoDataSeeder implements ApplicationRunner {

	private final JdbcTemplate jdbcTemplate;
	private final PasswordEncoder passwordEncoder;

	public DemoDataSeeder(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder) {
		this.jdbcTemplate = jdbcTemplate;
		this.passwordEncoder = passwordEncoder;
	}

	/** Només es calcula el hash si l'usuari no existeix: BCrypt és lent a propòsit. */
	private boolean existeix(String email) {
		Integer total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM usuaris WHERE LOWER(email) = LOWER(?)", Integer.class, email);
		return total != null && total > 0;
	}

	@Override
	public void run(ApplicationArguments args) {
		// Empreses professionals homologades amb distintiu oficial verificat
		crearSiNoExisteix(
			"Fontanals Lampisteria & Climatització S.L.",
			"contacte@fontanals.es",
			"123456",
			"professional",
			"963112233",
			"Empresa líder en lampisteria urgent 24h, reparació de canonades, descalcificadors, termos elèctrics i calderes a València i àrea metropolitana. Més de 15 anys de garantia i resposta immediata.",
			5,
			true,
			false
		);

		crearSiNoExisteix(
			"ElectroMartínez Instal·lacions Elèctriques S.L.",
			"info@electromartinez.es",
			"123456",
			"professional",
			"963223344",
			"Enginyeria i muntatges elèctrics autoritzats. Quadres industrials, domòtica domèstica, panells solars fotovoltaics, butlletins oficials i punts de recàrrega de vehicles elèctrics.",
			4,
			true,
			false
		);

		crearSiNoExisteix(
			"Soler Pintura & Alta Decoració S.L.",
			"info@solerpintura.es",
			"123456",
			"professional",
			"963334455",
			"Empresa familiar amb 25 anys d'experiència en alta decoració, pintura plàstica ecològica, allisats de parets, microciment continu i impermeabilització de façanes i terrasses.",
			4,
			true,
			false
		);

		crearSiNoExisteix(
			"ClimaBeneyto Climatització & Aerotèrmia S.L.",
			"serveis@climabeneyto.es",
			"123456",
			"professional",
			"963556677",
			"Servei tècnic oficial multimarca. Instal·lació i manteniment d'aire condicionat, bombes de calor aerotèrmiques d'alta eficiència A+++, terra radiant i sistemes de ventilació mecànica.",
			5,
			true,
			false
		);

		// Empresa professional amb sol·licitud de verificació pendent de trucada
		crearSiNoExisteix(
			"Fusteria Artesana Ribes & Fills S.L.",
			"tallers@fusteriaribes.es",
			"123456",
			"professional",
			"963667788",
			"Fusteria a mida i ebenisteria arquitectònica. Fabricació pròpia de cuines de disseny, portes acústiques, vestidors integrats i restauració de fustes nobles per a vivendes i negocis.",
			3,
			false,
			true
		);

		crearSiNoExisteix(
			"Llevant Reformes & Obres Integrals S.L.",
			"projectes@llevantreformes.es",
			"123456",
			"professional",
			"963778899",
			"Empresa constructora homologada per a reformes integrals de vivendes i locals comercials. Projectes clau en mà amb direcció facultativa, paletes, enrajolats i coordinació de gremis.",
			4,
			true,
			false
		);

		crearSiNoExisteix(
			"Serralleria Seguretat València 24H S.L.",
			"urgencies@serralleriavalencia.com",
			"123456",
			"professional",
			"963778899",
			"Obertura de portes sense danys en menys de 25 minuts. Instal·lació de bombins antibumping de màxima seguretat, escuts cuirassats i panys invisibles.",
			5,
			true,
			false
		);

		crearSiNoExisteix(
			"Cristalleria & Finestres Alumini Alvent S.L.",
			"info@alventcristalls.com",
			"123456",
			"professional",
			"963889900",
			"Fabricació pròpia de finestres de PVC i alumini amb RPT. Doble acristallament Climalit, mampares de bany de vidre temperat i tancaments.",
			4,
			true,
			false
		);

		crearSiNoExisteix(
			"EcoJardí Paisatgisme & Manteniment S.L.",
			"projectes@ecojardi.es",
			"123456",
			"professional",
			"963990011",
			"Projectes de xerojardineria mediterrània sostenible, gespa artificial d'alta gamma, reg intel·ligent i poda en alçada.",
			4,
			true,
			false
		);

		crearSiNoExisteix(
			"Neteges Industrials & Desinfeccions Marítima S.L.",
			"serveis@netegesmaritima.com",
			"123456",
			"professional",
			"963001122",
			"Empresa homologada per a neteges finals d'obra, higienització de conductes, neteja de comunitats i poliment de terres.",
			5,
			true,
			false
		);

		// 10 Clients particulars de la comunitat
		crearSiNoExisteix("Joan Albors i Vidal", "joan.albors@gmail.com", "123456", "client", "611223344", "Client particular resident a Russafa (València).", 3, false, false);
		crearSiNoExisteix("Marta Navarro Beltran", "marta.navarro@gmail.com", "123456", "client", "622334455", "Clienta particular resident a Benimaclet.", 3, false, false);
		crearSiNoExisteix("Pau Climent Soler", "pau.climent@outlook.com", "123456", "client", "633445566", "Client particular resident a Torrent (Vedat).", 3, false, false);
		crearSiNoExisteix("Laura Giménez Ortiz", "laura.gimenez@yahoo.es", "123456", "client", "644556677", "Clienta particular resident al Carme (València).", 3, false, false);
		crearSiNoExisteix("Andreu Soriano Puig", "andreu.soriano@gmail.com", "123456", "client", "655667788", "Client particular resident a Gandia.", 3, false, false);
		crearSiNoExisteix("Sílvia Fuster Montaner", "silvia.fuster@gmail.com", "123456", "client", "666778899", "Clienta particular resident a Alzira.", 3, false, false);
		crearSiNoExisteix("Marc Vila Segarra", "marc.vila@hotmail.com", "123456", "client", "677889900", "Client particular resident a Castelló de la Plana.", 3, false, false);
		crearSiNoExisteix("Carla Revert Llopis", "carla.revert@gmail.com", "123456", "client", "688990011", "Clienta particular resident a Alacant.", 3, false, false);
		crearSiNoExisteix("Vicent Barberà Camps", "voro.barbera@gmail.com", "123456", "client", "699001122", "Client particular resident a Paterna.", 3, false, false);
		crearSiNoExisteix("Elena Domènech Sancho", "elena.domenech@gmail.com", "123456", "client", "610112233", "Clienta particular resident a Sagunt.", 3, false, false);
	}

	private void crearSiNoExisteix(String nom, String email, String password, String rol, String telefon, String bio, int percentil, boolean verificat, boolean sollicitudVerificacio) {
		if (existeix(email)) {
			return;
		}
		jdbcTemplate.update(
			"INSERT INTO usuaris (nom, email, password, rol, telefon, bio, percentil, verificat, sollicitud_verificacio) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT DO NOTHING",
			nom, email, passwordEncoder.encode(password), rol.toUpperCase(Locale.ROOT), telefon, bio, percentil, verificat, sollicitudVerificacio
		);
	}
}
