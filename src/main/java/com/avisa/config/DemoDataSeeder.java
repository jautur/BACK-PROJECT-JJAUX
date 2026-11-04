package com.avisa.config;

import java.util.Locale;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Crea les empreses i els clients de demostració, només en el perfil demo.
 * Mai sobreescriu: si l'usuari ja existeix, no el toca.
 */
@Component
@Profile("demo")
@Order(2)
public class DemoDataSeeder implements ApplicationRunner {

	private final JdbcTemplate jdbcTemplate;

	public DemoDataSeeder(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public void run(ApplicationArguments args) {
		// Empreses professionals homologades amb distintiu oficial verificat, foto de perfil (isotip) i logo (fons targeta)
		crearSiNoExisteix(
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

		crearSiNoExisteix(
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

		crearSiNoExisteix(
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

		crearSiNoExisteix(
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
		crearSiNoExisteix(
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

		crearSiNoExisteix(
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

		crearSiNoExisteix(
			"Serralleria Seguretat València 24H S.L.",
			"urgencies@serralleriavalencia.com",
			"123456",
			"professional",
			"963778899",
			"Obertura de portes sense danys en menys de 25 minuts. Instal·lació de bombins antibumping de màxima seguretat, escuts cuirassats i panys invisibles.",
			5,
			true,
			false,
			"https://images.unsplash.com/photo-1558002038-1055907df827?w=800&auto=format&fit=crop&q=80",
			"https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=300&auto=format&fit=crop&q=80",
			"https://images.unsplash.com/photo-1558002038-1055907df827?w=800&auto=format&fit=crop&q=80"
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
			false,
			"https://images.unsplash.com/photo-1513694203232-719a280e022f?w=800&auto=format&fit=crop&q=80",
			"https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=300&auto=format&fit=crop&q=80",
			"https://images.unsplash.com/photo-1513694203232-719a280e022f?w=800&auto=format&fit=crop&q=80"
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
			false,
			"https://images.unsplash.com/photo-1558904541-efa8c4a08931?w=800&auto=format&fit=crop&q=80",
			"https://images.unsplash.com/photo-1517841905240-472988babdf9?w=300&auto=format&fit=crop&q=80",
			"https://images.unsplash.com/photo-1558904541-efa8c4a08931?w=800&auto=format&fit=crop&q=80"
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
			false,
			"https://images.unsplash.com/photo-1581578731548-c64695cc6952?w=800&auto=format&fit=crop&q=80",
			"https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?w=300&auto=format&fit=crop&q=80",
			"https://images.unsplash.com/photo-1581578731548-c64695cc6952?w=800&auto=format&fit=crop&q=80"
		);

		// 10 Clients particulars de la comunitat
		crearSiNoExisteix("Joan Albors i Vidal", "joan.albors@gmail.com", "123456", "client", "611223344", "Client particular resident a Russafa (València).", 3, false, false, "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=300&auto=format&fit=crop&q=80");
		crearSiNoExisteix("Marta Navarro Beltran", "marta.navarro@gmail.com", "123456", "client", "622334455", "Clienta particular resident a Benimaclet.", 3, false, false, "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=300&auto=format&fit=crop&q=80");
		crearSiNoExisteix("Pau Climent Soler", "pau.climent@outlook.com", "123456", "client", "633445566", "Client particular resident a Torrent (Vedat).", 3, false, false, "https://images.unsplash.com/photo-1570295999919-56ceb5ecca61?w=300&auto=format&fit=crop&q=80");
		crearSiNoExisteix("Laura Giménez Ortiz", "laura.gimenez@yahoo.es", "123456", "client", "644556677", "Clienta particular resident al Carme (València).", 3, false, false, "https://images.unsplash.com/photo-1580489944761-15a19d654956?w=300&auto=format&fit=crop&q=80");
		crearSiNoExisteix("Andreu Soriano Puig", "andreu.soriano@gmail.com", "123456", "client", "655667788", "Client particular resident a Gandia.", 3, false, false, "https://images.unsplash.com/photo-1527980965255-d3b416303d12?w=300&auto=format&fit=crop&q=80");
		crearSiNoExisteix("Sílvia Fuster Montaner", "silvia.fuster@gmail.com", "123456", "client", "666778899", "Clienta particular resident a Alzira.", 3, false, false, "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=300&auto=format&fit=crop&q=80");
		crearSiNoExisteix("Marc Vila Segarra", "marc.vila@hotmail.com", "123456", "client", "677889900", "Client particular resident a Castelló de la Plana.", 3, false, false, "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=300&auto=format&fit=crop&q=80");
		crearSiNoExisteix("Carla Revert Llopis", "carla.revert@gmail.com", "123456", "client", "688990011", "Clienta particular resident a Alacant.", 3, false, false, "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=300&auto=format&fit=crop&q=80");
		crearSiNoExisteix("Vicent Barberà Camps", "voro.barbera@gmail.com", "123456", "client", "699001122", "Client particular resident a Paterna.", 3, false, false, "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=300&auto=format&fit=crop&q=80");
		crearSiNoExisteix("Elena Domènech Sancho", "elena.domenech@gmail.com", "123456", "client", "610112233", "Clienta particular resident a Sagunt.", 3, false, false, "https://images.unsplash.com/photo-1548142813-c348350df52b?w=300&auto=format&fit=crop&q=80");
	}

	private void crearSiNoExisteix(String nom, String email, String password, String rol, String telefon, String bio, int percentil, boolean verificat, boolean sollicitudVerificacio) {
		crearSiNoExisteix(nom, email, password, rol, telefon, bio, percentil, verificat, sollicitudVerificacio, null, null, null);
	}

	private void crearSiNoExisteix(String nom, String email, String password, String rol, String telefon, String bio, int percentil, boolean verificat, boolean sollicitudVerificacio, String foto) {
		crearSiNoExisteix(nom, email, password, rol, telefon, bio, percentil, verificat, sollicitudVerificacio, foto, null, foto);
	}

	private void crearSiNoExisteix(String nom, String email, String password, String rol, String telefon, String bio, int percentil, boolean verificat, boolean sollicitudVerificacio, String foto, String fotoPerfil, String logo) {
		jdbcTemplate.update(
			"INSERT INTO usuaris (nom, email, password, rol, telefon, bio, percentil, verificat, sollicitud_verificacio, foto, foto_perfil, logo) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT DO NOTHING",
			nom, email, password, rol.toUpperCase(Locale.ROOT), telefon, bio, percentil, verificat, sollicitudVerificacio, foto, fotoPerfil, logo
		);
	}
}
