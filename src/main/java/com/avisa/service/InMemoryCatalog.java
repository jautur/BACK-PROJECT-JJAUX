package com.avisa.service;

import com.avisa.dto.CrearInteresRequest;
import com.avisa.dto.CrearPressupostRequest;
import com.avisa.dto.DashboardResponse;
import com.avisa.dto.IncidenciaRequest;
import com.avisa.dto.OfertaRequest;
import com.avisa.dto.RegisterRequest;
import com.avisa.dto.TascaRequest;
import com.avisa.model.Avis;
import com.avisa.model.Incidencia;
import com.avisa.model.Interes;
import com.avisa.model.Oferta;
import com.avisa.model.Pressupost;
import com.avisa.model.Publicacio;
import com.avisa.model.Resenya;
import com.avisa.model.Tasca;
import com.avisa.model.Usuari;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Catàleg central i capa d'accés a dades per a l'aplicació JJAUX.
 * Executa operacions de consulta, inserció i actualització contra la base de dades PostgreSQL (jjaux_db).
 */
@Component
public class InMemoryCatalog {

	private final JdbcTemplate jdbcTemplate;

	public InMemoryCatalog(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
		try {
			jdbcTemplate.execute("ALTER TABLE usuaris ADD COLUMN IF NOT EXISTS percentil INT DEFAULT 3 CHECK (percentil >= 0 AND percentil <= 100)");
			jdbcTemplate.execute("UPDATE usuaris SET percentil = 3 WHERE percentil IS NULL");
			jdbcTemplate.execute("ALTER TABLE usuaris ADD COLUMN IF NOT EXISTS verificat BOOLEAN DEFAULT FALSE");
			jdbcTemplate.execute("ALTER TABLE usuaris ADD COLUMN IF NOT EXISTS sollicitud_verificacio BOOLEAN DEFAULT FALSE");
			jdbcTemplate.execute("UPDATE usuaris SET verificat = FALSE WHERE verificat IS NULL");
			jdbcTemplate.execute("UPDATE usuaris SET sollicitud_verificacio = FALSE WHERE sollicitud_verificacio IS NULL");
			jdbcTemplate.execute("ALTER TABLE usuaris ADD COLUMN IF NOT EXISTS foto TEXT");
			jdbcTemplate.execute("ALTER TABLE avisos ADD COLUMN IF NOT EXISTS enllac VARCHAR(255)");
			jdbcTemplate.execute("ALTER TABLE ofertes ADD COLUMN IF NOT EXISTS categoria VARCHAR(100)");
			jdbcTemplate.execute("ALTER TABLE ofertes ADD COLUMN IF NOT EXISTS incidencia_id VARCHAR(100)");
			jdbcTemplate.execute("ALTER TABLE ofertes ALTER COLUMN tasca_id DROP NOT NULL");
			// Assegurar que la taula d'ofertes públiques no continga pressupostos privats d'incidències
			jdbcTemplate.execute("DELETE FROM ofertes WHERE categoria = 'Pressupost' OR (incidencia_id IS NOT NULL AND incidencia_id <> '')");
			jdbcTemplate.execute("ALTER TABLE resenyes ALTER COLUMN descripcio DROP NOT NULL");
			jdbcTemplate.execute("ALTER TABLE tasques ADD COLUMN IF NOT EXISTS data_inici TIMESTAMP DEFAULT CURRENT_TIMESTAMP");
			jdbcTemplate.execute("ALTER TABLE tasques ADD COLUMN IF NOT EXISTS data_finalitzacio TIMESTAMP");
			jdbcTemplate.execute("ALTER TABLE tasques ADD COLUMN IF NOT EXISTS data_valoracio TIMESTAMP");
			jdbcTemplate.execute("ALTER TABLE tasques ADD COLUMN IF NOT EXISTS interes_id VARCHAR(100)");
			jdbcTemplate.execute("ALTER TABLE tasques ADD COLUMN IF NOT EXISTS pressupost_id VARCHAR(100)");
			jdbcTemplate.execute("ALTER TABLE tasques ADD COLUMN IF NOT EXISTS preu_acordat VARCHAR(50)");
			jdbcTemplate.execute("ALTER TABLE tasques ADD COLUMN IF NOT EXISTS publicacio_id VARCHAR(100)");
			jdbcTemplate.execute(
				"CREATE TABLE IF NOT EXISTS publicacions (" +
				"id VARCHAR(100) PRIMARY KEY, " +
				"usuari_id VARCHAR(100), " +
				"usuari_nom VARCHAR(150) NOT NULL, " +
				"tipus VARCHAR(50) NOT NULL, " +
				"titol VARCHAR(255) NOT NULL, " +
				"descripcio TEXT, " +
				"categoria VARCHAR(100), " +
				"preu_o_pressupost VARCHAR(50), " +
				"created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)"
			);
			jdbcTemplate.execute(
				"CREATE TABLE IF NOT EXISTS interessos (" +
				"id VARCHAR(100) PRIMARY KEY, " +
				"publicacio_id VARCHAR(100), " +
				"client_id VARCHAR(100), " +
				"client_nom VARCHAR(150) NOT NULL, " +
				"missatge TEXT, " +
				"tasca_id VARCHAR(100), " +
				"data_creacio TIMESTAMP DEFAULT CURRENT_TIMESTAMP)"
			);
			jdbcTemplate.execute(
				"CREATE TABLE IF NOT EXISTS pressupostos (" +
				"id VARCHAR(100) PRIMARY KEY, " +
				"incidencia_id VARCHAR(100), " +
				"professional_id VARCHAR(100), " +
				"professional_nom VARCHAR(150) NOT NULL, " +
				"preu VARCHAR(50) NOT NULL, " +
				"descripcio TEXT, " +
				"duracio_estimada VARCHAR(100), " +
				"data_creacio TIMESTAMP DEFAULT CURRENT_TIMESTAMP)"
			);
		} catch (Exception ignored) {}
	}

	// ==========================================
	// GESTIÓ D'USUARIS
	// ==========================================

	private Usuari mapUsuari(java.sql.ResultSet rs) throws java.sql.SQLException {
		Integer percentil = 3;
		try {
			Object p = rs.getObject("percentil");
			if (p != null) {
				percentil = rs.getInt("percentil");
			}
		} catch (java.sql.SQLException ignored) {}
		Boolean verificat = false;
		try {
			Object v = rs.getObject("verificat");
			if (v != null) {
				verificat = rs.getBoolean("verificat");
			}
		} catch (java.sql.SQLException ignored) {}
		Boolean sollicitudVerificacio = false;
		try {
			Object s = rs.getObject("sollicitud_verificacio");
			if (s != null) {
				sollicitudVerificacio = rs.getBoolean("sollicitud_verificacio");
			}
		} catch (java.sql.SQLException ignored) {}
		String foto = null;
		try {
			foto = rs.getString("foto");
		} catch (java.sql.SQLException ignored) {}
		return new Usuari(
			rs.getString("id"),
			rs.getString("nom"),
			rs.getString("email"),
			rs.getString("rol"),
			rs.getString("telefon"),
			rs.getString("bio"),
			percentil,
			null,
			verificat,
			sollicitudVerificacio,
			foto
		);
	}

	/**
	 * Recupera tots els usuaris registrats de la base de dades.
	 * @return Llista completa d'usuaris
	 */
	public List<Usuari> users() {
		try {
			return jdbcTemplate.query(
				"SELECT id, nom, email, rol, telefon, bio, COALESCE(percentil, 3) AS percentil, COALESCE(verificat, FALSE) AS verificat, COALESCE(sollicitud_verificacio, FALSE) AS sollicitud_verificacio, foto FROM usuaris ORDER BY created_at",
				(rs, rowNum) -> mapUsuari(rs)
			);
		} catch (Exception e) {
			return List.of();
		}
	}

	/**
	 * Cerca un usuari concret pel seu identificador únic (ID).
	 * @param id Identificador de l'usuari
	 * @return Optional amb l'usuari trobat
	 */
	public Optional<Usuari> findUserById(String id) {
		try {
			List<Usuari> res = jdbcTemplate.query(
				"SELECT id, nom, email, rol, telefon, bio, COALESCE(percentil, 3) AS percentil, COALESCE(verificat, FALSE) AS verificat, COALESCE(sollicitud_verificacio, FALSE) AS sollicitud_verificacio, foto FROM usuaris WHERE id = ? OR LOWER(nom) = LOWER(?) OR LOWER(email) = LOWER(?)",
				(rs, rowNum) -> mapUsuari(rs),
				id, id, id
			);
			return res.stream().findFirst();
		} catch (Exception e) {
			return Optional.empty();
		}
	}

	/**
	 * Cerca un usuari per la seva adreça de correu electrònic.
	 * @param email Correu de l'usuari
	 * @return Optional amb l'usuari si existeix
	 */
	public Optional<Usuari> findUserByEmail(String email) {
		try {
			List<Usuari> res = jdbcTemplate.query(
				"SELECT id, nom, email, rol, telefon, bio, COALESCE(percentil, 3) AS percentil, COALESCE(verificat, FALSE) AS verificat, COALESCE(sollicitud_verificacio, FALSE) AS sollicitud_verificacio, foto FROM usuaris WHERE LOWER(email) = LOWER(?)",
				(rs, rowNum) -> mapUsuari(rs),
				email
			);
			return res.stream().findFirst();
		} catch (Exception e) {
			return Optional.empty();
		}
	}

	/**
	 * Autentica un usuari validant el seu correu i contrasenya a PostgreSQL.
	 * @param email Correu electrònic d'accés
	 * @param password Contrasenya
	 * @return Optional amb l'usuari autenticat
	 */
	public Optional<Usuari> authenticate(String email, String password) {
		try {
			List<Usuari> res = jdbcTemplate.query(
				"SELECT id, nom, email, rol, telefon, bio, COALESCE(percentil, 3) AS percentil, COALESCE(verificat, FALSE) AS verificat, COALESCE(sollicitud_verificacio, FALSE) AS sollicitud_verificacio, foto FROM usuaris WHERE LOWER(email) = LOWER(?) AND (password = ? OR ? IS NULL)",
				(rs, rowNum) -> mapUsuari(rs),
				email, password, password
			);
			return res.stream().findFirst();
		} catch (Exception e) {
			return Optional.empty();
		}
	}

	/**
	 * Actualitza la informació del perfil d'un usuari.
	 * @param id Identificador de l'usuari
	 * @param nou Dades actualitzades
	 * @return L'usuari amb els canvis aplicats
	 */
	public Usuari updateUser(String id, Usuari nou) {
		return updateUser(id, nou, null, null);
	}

	/**
	 * Actualitza el perfil d'un usuari amb comprovació de permisos per al percentil de comissió.
	 * El percentil (comissió de plataforma, 0-100):
	 * - Està predeterminat a 3.
	 * - L'usuari només pot modificar-lo per a augmentar-lo (o mantenir-lo).
	 * - Només un administrador pot modificar-lo a la baixa.
	 */
	public Usuari updateUser(String id, Usuari nou, String requesterRol, String requesterId) {
		Usuari actual = findUserById(id).orElse(null);
		int percentilFinal = (actual != null && actual.percentil() != null) ? actual.percentil() : 3;

		if (nou.percentil() != null) {
			int proposed = nou.percentil();
			if (proposed < 0 || proposed > 100) {
				throw new IllegalArgumentException("El percentil ha d'estar comprès entre 0 i 100.");
			}

			boolean isAdmin = (requesterRol != null && requesterRol.equalsIgnoreCase("admin"))
				|| (actual != null && "admin".equalsIgnoreCase(actual.rol()));

			if (proposed < percentilFinal) {
				if (!isAdmin) {
					throw new SecurityException("Només un administrador pot reduir el percentil de comissió (comissió actual: " + percentilFinal + "%). Com a usuari només pots augmentar-lo.");
				}
				percentilFinal = proposed;
			} else {
				percentilFinal = proposed;
			}
		}

		String fotoFinal = (nou.foto() != null) ? (nou.foto().trim().isBlank() ? null : nou.foto().trim()) : (actual != null ? actual.foto() : null);

		try {
			jdbcTemplate.update(
				"UPDATE usuaris SET nom = ?, telefon = ?, bio = ?, percentil = ?, foto = ? WHERE id = ?",
				nou.nom(), nou.telefon(), nou.bio(), percentilFinal, fotoFinal, id
			);
			return findUserById(id).orElse(nou);
		} catch (Exception e) {
			return nou;
		}
	}

	/**
	 * Assigna o elimina la fotografia d'un professional.
	 * @param id Identificador de l'usuari
	 * @param foto URL de la fotografia (o buit per esborrar-la)
	 * @return Usuari actualitzat
	 */
	public Usuari assignarFoto(String id, String foto) {
		String clean = (foto != null && !foto.trim().isBlank()) ? foto.trim() : null;
		jdbcTemplate.update("UPDATE usuaris SET foto = ? WHERE id = ?", clean, id);
		return findUserById(id).orElseThrow(() -> new IllegalArgumentException("Usuari no trobat"));
	}

	/**
	 * Actualitza la contrasenya d'un usuari directament a la base de dades.
	 * Permet a l'administrador o al propi usuari assignar una nova contrasenya sense conèixer l'antiga.
	 * @param id Identificador de l'usuari
	 * @param newPassword Nova contrasenya
	 */
	public void changePassword(String id, String newPassword) {
		try {
			jdbcTemplate.update("UPDATE usuaris SET password = ? WHERE id = ?", newPassword, id);
			addAvis(id, "La teva contrasenya d'accés ha estat actualitzada correctament.");
		} catch (Exception ignored) {}
	}

	/**
	 * Permet a un professional sol·licitar la verificació oficial del seu compte.
	 * Requereix email i telèfon obligatoris perquè l'administrador pugui fer la comprovació telefònica.
	 * @param id Identificador de l'usuari
	 * @return Usuari actualitzat amb sollicitud_verificacio = true
	 */
	public Usuari sollicitarVerificacio(String id) {
		Usuari usuari = findUserById(id).orElseThrow(() -> new IllegalArgumentException("Usuari no trobat"));
		if (usuari.email() == null || usuari.email().trim().isBlank()) {
			throw new IllegalArgumentException("Cal disposar d'un correu electrònic vàlid per sol·licitar la verificació.");
		}
		if (usuari.telefon() == null || usuari.telefon().trim().isBlank()) {
			throw new IllegalArgumentException("El telèfon de contacte és obligatori per sol·licitar la verificació (l'administrador et trucarà per validar la teva identitat).");
		}
		jdbcTemplate.update("UPDATE usuaris SET sollicitud_verificacio = TRUE WHERE id = ?", id);
		addAvis("u-admin", "Nova sol·licitud de verificació professional: " + usuari.nom() + " (Tel: " + usuari.telefon() + ", Email: " + usuari.email() + "). Cal realitzar la trucada de comprovació.");
		return findUserById(id).orElse(usuari);
	}

	/**
	 * Permet a l'administrador verificar o desverificar oficialment un professional després de la comprovació telefònica.
	 * @param id Identificador de l'usuari a verificar
	 * @param estat true per aprovar la verificació, false per desverificar/rebutjar
	 * @param requesterRol Rol de l'usuari que fa la petició (ha de ser 'admin')
	 * @return Usuari actualitzat
	 */
	public Usuari verificarProfessional(String id, boolean estat, String requesterRol) {
		boolean isAdmin = (requesterRol != null && requesterRol.equalsIgnoreCase("admin"));
		if (!isAdmin) {
			throw new SecurityException("Només un administrador té permisos per verificar o desverificar professionals.");
		}
		Usuari usuari = findUserById(id).orElseThrow(() -> new IllegalArgumentException("Usuari no trobat"));
		jdbcTemplate.update("UPDATE usuaris SET verificat = ?, sollicitud_verificacio = FALSE WHERE id = ?", estat, id);
		if (estat) {
			addAvis(id, "Enhorabona! L'administrador ha verificat oficialment el teu compte de professional després de la comprovació telefònica.");
		} else {
			addAvis(id, "El teu estat de professional verificat ha estat desactivat o la teva sol·licitud ha estat rebutjada.");
		}
		return findUserById(id).orElse(usuari);
	}

	/**
	 * Elimina un usuari de la plataforma i neteja els seus registres associats.
	 * Protegeix el compte d'administrador principal contra eliminació accidental.
	 * @param id Identificador de l'usuari
	 * @return true si s'ha eliminat correctament, false si està protegit o hi ha hagut error
	 */
	public boolean deleteUser(String id) {
		if ("u-admin".equals(id) || "admin@admin".equalsIgnoreCase(id) || "admin@admin.es".equalsIgnoreCase(id)) {
			return false;
		}
		try {
			jdbcTemplate.update("DELETE FROM avisos WHERE usuari_id = ?", id);
			jdbcTemplate.update("DELETE FROM incidencies WHERE client_id = ?", id);
			int rows = jdbcTemplate.update("DELETE FROM usuaris WHERE id = ? AND id != 'u-admin'", id);
			return rows > 0;
		} catch (Exception ignored) {
			return false;
		}
	}

	/**
	 * Registra un nou usuari a la plataforma i el desa a PostgreSQL.
	 * Desa el telèfon si s'ha especificat (com és el cas dels professionals).
	 * @param request Dades de registre
	 * @return L'usuari creat
	 */
	public Usuari addUser(RegisterRequest request) {
		if (findUserByEmail(request.email()).isPresent()) {
			throw new IllegalArgumentException("El correu electrònic '" + request.email() + "' ja està registrat a la plataforma.");
		}
		String id = "u-" + UUID.randomUUID().toString().substring(0, 8);
		String password = request.password() != null ? request.password() : "123456";
		try {
			jdbcTemplate.update(
				"INSERT INTO usuaris (id, nom, email, password, rol, telefon, percentil) VALUES (?, ?, ?, ?, ?, ?, 3)",
				id, request.nom(), request.email(), password, request.rol(), request.telefon()
			);
		} catch (Exception e) {
			if (e.getMessage() != null && e.getMessage().toLowerCase().contains("unique")) {
				throw new IllegalArgumentException("El correu electrònic '" + request.email() + "' ja està en ús.");
			}
			throw new RuntimeException("Error en desar l'usuari a la base de dades: " + e.getMessage(), e);
		}
		return new Usuari(id, request.nom(), request.email(), request.rol(), request.telefon(), null, 3);
	}

	// ==========================================
	// GESTIÓ D'INCIDÈNCIES (CLIENTS)
	// Com les ofertes per a professionals, les incidències
	// són creades exclusivament per clients (sols tenen client).
	// ==========================================

	private final org.springframework.jdbc.core.RowMapper<Incidencia> incidenciaRowMapper = (rs, rowNum) -> new Incidencia(
		rs.getString("id"),
		rs.getString("client_id"),
		rs.getString("client_nom"),
		rs.getString("titol"),
		rs.getString("descripcio"),
		rs.getString("categoria"),
		rs.getString("budget"),
		rs.getString("estat"),
		rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : LocalDateTime.now()
	);

	/**
	 * Llistar totes les incidències creades per clients a PostgreSQL.
	 * @return Llista completa d'incidències
	 */
	public List<Incidencia> incidencies() {
		try {
			return jdbcTemplate.query(
				"SELECT id, client_id, client_nom, titol, descripcio, categoria, budget, estat, created_at FROM incidencies ORDER BY created_at DESC",
				incidenciaRowMapper
			);
		} catch (Exception e) {
			return List.of();
		}
	}

	/**
	 * Cerca una incidència pel seu identificador únic (ex: 'I-101').
	 * @param id Identificador de la incidència
	 * @return Optional amb la incidència
	 */
	public Optional<Incidencia> findIncidenciaById(String id) {
		try {
			List<Incidencia> res = jdbcTemplate.query(
				"SELECT id, client_id, client_nom, titol, descripcio, categoria, budget, estat, created_at FROM incidencies WHERE id = ?",
				incidenciaRowMapper,
				id
			);
			return res.stream().findFirst();
		} catch (Exception e) {
			return Optional.empty();
		}
	}

	/**
	 * Llistar incidències d'un client concret.
	 * @param clientNom Nom o ID del client
	 * @return Llista d'incidències d'aquell client
	 */
	public List<Incidencia> findIncidenciesByClient(String clientNom) {
		try {
			return jdbcTemplate.query(
				"SELECT id, client_id, client_nom, titol, descripcio, categoria, budget, estat, created_at FROM incidencies WHERE LOWER(client_nom) = LOWER(?) OR client_id = ? ORDER BY created_at DESC",
				incidenciaRowMapper,
				clientNom, clientNom
			);
		} catch (Exception e) {
			return List.of();
		}
	}

	/**
	 * Publica una nova incidència creada per un client a la taula incidencies.
	 * @param request Dades de la incidència
	 * @return La incidència creada
	 */
	public Incidencia addIncidencia(IncidenciaRequest request) {
		String id = "I-" + String.format("%03d", 100 + incidencies().size() + 1);
		String estat = request.estat() != null ? request.estat() : "Oberta";
		String client = request.resolveClient();

		String clientId = request.clientId();
		String clientNom = client;
		if (client != null) {
			for (Usuari u : users()) {
				if (u.nom().equalsIgnoreCase(client) || u.id().equalsIgnoreCase(client)) {
					clientId = u.id();
					clientNom = u.nom();
					break;
				}
			}
		}
		if (clientNom == null || clientNom.isBlank()) {
			clientNom = "Client AVISA";
		}

		try {
			jdbcTemplate.update(
				"INSERT INTO incidencies (id, client_id, client_nom, titol, descripcio, categoria, budget, estat) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
				id, clientId, clientNom, request.titol(), request.descripcio(), request.categoria(), request.budget(), estat
			);
			jdbcTemplate.update(
				"INSERT INTO publicacions (id, usuari_id, usuari_nom, tipus, titol, descripcio, categoria, preu_o_pressupost) VALUES (?, ?, ?, 'INCIDENCIA', ?, ?, ?, ?)",
				id, clientId, clientNom, request.titol(), request.descripcio(), request.categoria(), request.budget()
			);
		} catch (Exception ignored) {}

		return new Incidencia(id, clientId, clientNom, request.titol(), request.descripcio(), request.categoria(), request.budget(), estat, LocalDateTime.now());
	}

	/**
	 * Elimina una incidència de la plataforma.
	 * @param id Identificador de la incidència
	 */
	public void deleteIncidencia(String id) {
		try {
			jdbcTemplate.update("DELETE FROM publicacions WHERE id = ?", id);
			jdbcTemplate.update("DELETE FROM pressupostos WHERE incidencia_id = ?", id);
			jdbcTemplate.update("DELETE FROM incidencies WHERE id = ?", id);
		} catch (Exception ignored) {}
	}

	// ==========================================
	// GESTIÓ DE TASQUES
	// ==========================================

	private final org.springframework.jdbc.core.RowMapper<Tasca> tascaRowMapper = (rs, rowNum) -> new Tasca(
		rs.getString("id"),
		rs.getString("titol"),
		rs.getString("descripcio"),
		rs.getString("categoria"),
		rs.getString("client_id"),
		rs.getString("client_nom"),
		rs.getString("professional_id"),
		rs.getString("professional_nom"),
		rs.getString("publicacio_id"),
		rs.getString("interes_id"),
		rs.getString("pressupost_id"),
		rs.getString("preu_acordat") != null ? rs.getString("preu_acordat") : rs.getString("budget"),
		rs.getTimestamp("data_inici") != null ? rs.getTimestamp("data_inici").toLocalDateTime() : (rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : LocalDateTime.now()),
		rs.getTimestamp("data_finalitzacio") != null ? rs.getTimestamp("data_finalitzacio").toLocalDateTime() : ("Finalitzada".equalsIgnoreCase(rs.getString("estat")) ? LocalDateTime.now() : null),
		rs.getObject("valoracio") != null ? rs.getInt("valoracio") : null,
		rs.getString("opinio"),
		rs.getTimestamp("data_valoracio") != null ? rs.getTimestamp("data_valoracio").toLocalDateTime() : null
	);

	/**
	 * Obté totes les tasques registrades a PostgreSQL.
	 * @return Llista de tasques
	 */
	public List<Tasca> tasks() {
		try {
			return jdbcTemplate.query(
				"SELECT id, titol, descripcio, categoria, client_id, client_nom, professional_id, professional_nom, publicacio_id, interes_id, pressupost_id, preu_acordat, budget, estat, data_inici, data_finalitzacio, valoracio, opinio, data_valoracio, created_at FROM tasques ORDER BY created_at DESC",
				tascaRowMapper
			);
		} catch (Exception e) {
			return List.of();
		}
	}

	/**
	 * Cerca una tasca pel seu identificador únic (ex: 'T-101').
	 * @param id Identificador de la tasca
	 * @return Optional amb la tasca
	 */
	public Optional<Tasca> findTaskById(String id) {
		try {
			List<Tasca> res = jdbcTemplate.query(
				"SELECT id, titol, descripcio, categoria, client_id, client_nom, professional_id, professional_nom, publicacio_id, interes_id, pressupost_id, preu_acordat, budget, estat, data_inici, data_finalitzacio, valoracio, opinio, data_valoracio, created_at FROM tasques WHERE id = ?",
				tascaRowMapper,
				id
			);
			return res.stream().findFirst();
		} catch (Exception e) {
			return Optional.empty();
		}
	}

	/**
	 * Llista les tasques publicades per un client concret.
	 * @param clientNom Nom o ID del client
	 * @return Llista de tasques d'aquell client
	 */
	public List<Tasca> findTasksByClient(String clientNom) {
		try {
			return jdbcTemplate.query(
				"SELECT id, titol, descripcio, categoria, client_id, client_nom, professional_id, professional_nom, publicacio_id, interes_id, pressupost_id, preu_acordat, budget, estat, data_inici, data_finalitzacio, valoracio, opinio, data_valoracio, created_at FROM tasques WHERE LOWER(client_nom) = LOWER(?) OR client_id = ? ORDER BY created_at DESC",
				tascaRowMapper,
				clientNom, clientNom
			);
		} catch (Exception e) {
			return List.of();
		}
	}

	/**
	 * Llista les tasques on el professional ha estat assignat (ja convergeixen client i professional).
	 * @param professionalNom Nom o ID del professional
	 * @return Llista de tasques assignades a aquell professional
	 */
	public List<Tasca> findTasksByProfessional(String professionalNom) {
		try {
			return jdbcTemplate.query(
				"SELECT id, titol, descripcio, categoria, client_id, client_nom, professional_id, professional_nom, publicacio_id, interes_id, pressupost_id, preu_acordat, budget, estat, data_inici, data_finalitzacio, valoracio, opinio, data_valoracio, created_at FROM tasques WHERE LOWER(professional_nom) = LOWER(?) OR professional_id = ? ORDER BY created_at DESC",
				tascaRowMapper,
				professionalNom, professionalNom
			);
		} catch (Exception e) {
			return List.of();
		}
	}

	/**
	 * Afegeix una nova petició o incidència creada per un client a la base de dades.
	 * @param request Dades de la petició a publicar
	 * @return La tasca/incidència creada
	 */
	public Tasca addTask(TascaRequest request) {
		String id = "T-" + String.format("%03d", 100 + tasks().size() + 1);
		String estat = request.estat() != null ? request.estat() : "Oberta";
		String client = request.resolveClient();
		String professional = request.resolveProfessional();
		String tipus = (request.tipus() != null && !request.tipus().isBlank())
			? request.tipus()
			: (professional != null && !professional.isBlank() ? "tasca" : "incidencia");

		String clientId = request.clientId();
		String clientNom = client;
		if (client != null) {
			for (Usuari u : users()) {
				if (u.nom().equalsIgnoreCase(client) || u.id().equalsIgnoreCase(client)) {
					clientId = u.id();
					clientNom = u.nom();
					break;
				}
			}
		}

		String professionalId = request.professionalId();
		String professionalNom = professional;
		if (professional != null) {
			for (Usuari u : users()) {
				if (u.nom().equalsIgnoreCase(professional) || u.id().equalsIgnoreCase(professional)) {
					professionalId = u.id();
					professionalNom = u.nom();
					break;
				}
			}
		}

		try {
			jdbcTemplate.update(
				"INSERT INTO tasques (id, titol, descripcio, categoria, client_id, client_nom, professional_id, professional_nom, origen_id, budget, estat, tipus, valoracio, opinio) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
				id, request.titol(), request.descripcio(), request.categoria(), clientId, clientNom, professionalId, professionalNom, request.origenId(), request.budget(), estat, tipus, request.valoracio(), request.opinio()
			);
			if (professional == null || professional.isBlank() || "incidencia".equalsIgnoreCase(tipus)) {
				jdbcTemplate.update(
					"INSERT INTO incidencies (id, client_id, client_nom, titol, descripcio, categoria, budget, estat) VALUES (?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT (id) DO NOTHING",
					id, clientId, clientNom, request.titol(), request.descripcio(), request.categoria(), request.budget(), estat
				);
			}
		} catch (Exception ignored) {}
		return new Tasca(id, request.titol(), request.descripcio(), request.categoria(), clientId, clientNom, professionalId, professionalNom, request.origenId(), request.budget(), estat, tipus, request.valoracio(), request.opinio());
	}

	/**
	 * Finalitza una tasca canviant el seu estat a 'Finalitzada' i enviant avisos pertinents.
	 * @param id Identificador de la tasca
	 * @return Optional amb la tasca actualitzada
	 */
	public Optional<Tasca> completeTask(String id) {
		return completeTask(id, null, null);
	}

	/**
	 * Finalitza una tasca canviant el seu estat a 'Finalitzada', enregistrant directament la valoració i opinió del client.
	 * @param id Identificador de la tasca
	 * @param valoracio Puntuació del servei (1 a 5 estrelles)
	 * @param opinio Opinió o comentari del client
	 * @return Optional amb la tasca actualitzada
	 */
	public Optional<Tasca> completeTask(String id, Integer valoracio, String opinio) {
		try {
			if (valoracio != null) {
				jdbcTemplate.update(
					"UPDATE tasques SET estat = 'Finalitzada', valoracio = ?, opinio = ? WHERE id = ?",
					valoracio, opinio, id
				);
			} else {
				jdbcTemplate.update("UPDATE tasques SET estat = 'Finalitzada' WHERE id = ?", id);
			}

			Optional<Tasca> tascaOpt = findTaskById(id);
			tascaOpt.ifPresent(task -> {
				// Notifica al client
				if (task.clientId() != null) {
					addAvis(task.clientId(), "Has finalitzat amb èxit la tasca '" + task.titol() + "'.");
				} else if (task.clientNom() != null) {
					users().stream()
						.filter(u -> u.nom().equalsIgnoreCase(task.clientNom()))
						.findFirst()
						.ifPresent(clientUser -> addAvis(clientUser.id(), "Has finalitzat amb èxit la tasca '" + task.titol() + "'."));
				}

				// Notifica al professional
				String proId = task.professionalId();
				if (proId == null && task.professionalNom() != null) {
					proId = users().stream()
						.filter(u -> u.nom().equalsIgnoreCase(task.professionalNom()))
						.findFirst()
						.map(Usuari::id)
						.orElse(null);
				}
				if (proId != null) {
					if (valoracio != null) {
						String opDesc = (opinio != null && !opinio.isBlank()) ? " amb l'opinió: \"" + opinio + "\"" : "";
						addAvis(proId, "La tasca '" + task.titol() + "' ha estat finalitzada per " + (task.clientNom() != null ? task.clientNom() : "el client") + " i has rebut una valoració de " + valoracio + "/5 estrelles" + opDesc + "!");
					} else {
						addAvis(proId, "La tasca '" + task.titol() + "' ha estat marcada com a finalitzada pel client!");
					}
				}
			});
			return tascaOpt;
		} catch (Exception e) {
			return Optional.empty();
		}
	}

	/**
	 * Elimina una tasca de la base de dades.
	 * @param id Identificador de la tasca
	 */
	public void deleteTask(String id) {
		try {
			jdbcTemplate.update("DELETE FROM tasques WHERE id = ?", id);
		} catch (Exception ignored) {}
	}

	// ==========================================
	// GESTIÓ D'OFERTES I PRESSUPOSTOS
	// ==========================================

	/**
	 * Obté totes les ofertes econòmiques registrades.
	 * @return Llista completa d'ofertes
	 */
	public List<Oferta> offers() {
		try {
			return jdbcTemplate.query(
				"SELECT o.id, COALESCE(o.tasca_id, o.incidencia_id) AS tasca_id, o.incidencia_id, o.professional, COALESCE(o.categoria, t.categoria, i.categoria, '') AS categoria, o.price, o.note, o.estat FROM ofertes o LEFT JOIN tasques t ON t.id = o.tasca_id LEFT JOIN incidencies i ON i.id = COALESCE(o.incidencia_id, o.tasca_id) WHERE (o.categoria IS NULL OR o.categoria <> 'Pressupost') AND (o.incidencia_id IS NULL OR o.incidencia_id = '') ORDER BY o.created_at DESC",
				(rs, rowNum) -> new Oferta(
					rs.getString("id"),
					rs.getString("tasca_id"),
					rs.getString("incidencia_id"),
					rs.getString("professional"),
					rs.getString("categoria"),
					rs.getString("price"),
					rs.getString("note"),
					rs.getString("estat")
				)
			);
		} catch (Exception e) {
			return List.of();
		}
	}

	/**
	 * Cerca una oferta pel seu ID únic.
	 * @param id Identificador de l'oferta
	 * @return Optional amb l'oferta
	 */
	public Optional<Oferta> findOfferById(String id) {
		try {
			List<Oferta> res = jdbcTemplate.query(
				"SELECT o.id, COALESCE(o.tasca_id, o.incidencia_id) AS tasca_id, o.incidencia_id, o.professional, COALESCE(o.categoria, t.categoria, i.categoria, '') AS categoria, o.price, o.note, o.estat FROM ofertes o LEFT JOIN tasques t ON t.id = o.tasca_id LEFT JOIN incidencies i ON i.id = COALESCE(o.incidencia_id, o.tasca_id) WHERE o.id = ? AND (o.categoria IS NULL OR o.categoria <> 'Pressupost') AND (o.incidencia_id IS NULL OR o.incidencia_id = '')",
				(rs, rowNum) -> new Oferta(
					rs.getString("id"),
					rs.getString("tasca_id"),
					rs.getString("incidencia_id"),
					rs.getString("professional"),
					rs.getString("categoria"),
					rs.getString("price"),
					rs.getString("note"),
					rs.getString("estat")
				),
				id
			);
			return res.stream().findFirst();
		} catch (Exception e) {
			return Optional.empty();
		}
	}

	/**
	 * Llista totes les ofertes presentades per a una tasca o incidència específica.
	 * @param tascaId Identificador de la tasca o incidència
	 * @return Llista d'ofertes rebudes
	 */
	public List<Oferta> findOffersByTasca(String tascaId) {
		try {
			return jdbcTemplate.query(
				"SELECT o.id, o.tasca_id, o.incidencia_id, o.professional, COALESCE(o.categoria, t.categoria, '') AS categoria, o.price, o.note, o.estat " +
				"FROM ofertes o LEFT JOIN tasques t ON t.id = o.tasca_id " +
				"WHERE (o.tasca_id = ? OR o.id = ?) AND (o.categoria IS NULL OR o.categoria <> 'Pressupost') AND (o.incidencia_id IS NULL OR o.incidencia_id = '') " +
				"ORDER BY o.created_at DESC",
				(rs, rowNum) -> new Oferta(
					rs.getString("id"),
					rs.getString("tasca_id"),
					rs.getString("incidencia_id"),
					rs.getString("professional"),
					rs.getString("categoria"),
					rs.getString("price"),
					rs.getString("note"),
					rs.getString("estat")
				),
				tascaId, tascaId
			);
		} catch (Exception e) {
			return List.of();
		}
	}

	/**
	 * Guarda una nova oferta presentada per un professional i emet un avís al client si escau.
	 * @param request Dades de l'oferta
	 * @return L'oferta creada
	 */
	public Oferta addOffer(OfertaRequest request) {
		String id = "O-" + String.format("%03d", 200 + offers().size() + 1);
		String estat = request.estat() != null ? request.estat() : "Pendent";
		String targetId = request.resolveTargetId();
		String tascaId = targetId;
		String incidenciaId = request.incidenciaId();
		String categoria = request.categoria();

		// Si la petició s'adreça a una incidència, NO és una oferta de servei sinó un pressupost
		boolean isIncidencia = (incidenciaId != null && !incidenciaId.isBlank())
			|| (targetId != null && (targetId.startsWith("I-") || findIncidenciaById(targetId).isPresent()));

		if (isIncidencia) {
			String incId = (incidenciaId != null && !incidenciaId.isBlank()) ? incidenciaId : targetId;
			CrearPressupostRequest cpReq = new CrearPressupostRequest(
				incId,
				null,
				request.professional(),
				request.price(),
				request.note(),
				null
			);
			Pressupost p = addPressupost(cpReq);
			return new Oferta(p.id(), null, incId, p.professionalNom(), categoria != null ? categoria : "Altres Serveis", p.preu(), p.descripcio(), "Pendent");
		}

		// Si és una oferta de servei lliure (no vinculada a una incidència prèvia)
		if (targetId == null || targetId.isBlank() || "standalone".equalsIgnoreCase(targetId)) {
			tascaId = null;
			incidenciaId = null;
			if (categoria == null || categoria.isBlank()) {
				categoria = "Altres Serveis";
			}
		} else {
			if (categoria == null || categoria.isBlank()) {
				categoria = findTaskById(targetId).map(Tasca::categoria).orElse("Altres Serveis");
			}
		}

		if (categoria == null || categoria.isBlank()) {
			categoria = "Altres Serveis";
		}

		try {
			jdbcTemplate.update(
				"INSERT INTO ofertes (id, tasca_id, incidencia_id, professional, price, note, categoria, estat) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
				id, tascaId, incidenciaId, request.professional(), request.price(), request.note(), categoria, estat
			);

			String proId = null;
			for (Usuari u : users()) {
				if (u.nom().equalsIgnoreCase(request.professional()) || u.id().equalsIgnoreCase(request.professional())) {
					proId = u.id();
					break;
				}
			}

			if (incidenciaId == null) {
				String titol = (request.note() != null && !request.note().isBlank()) ? request.note() : ("Servei de " + categoria);
				jdbcTemplate.update(
					"INSERT INTO publicacions (id, usuari_id, usuari_nom, tipus, titol, descripcio, categoria, preu_o_pressupost) VALUES (?, ?, ?, 'OFERTA', ?, ?, ?, ?)",
					id, proId, request.professional(), titol, request.note(), categoria, request.price()
				);
			} else {
				jdbcTemplate.update(
					"INSERT INTO pressupostos (id, incidencia_id, professional_id, professional_nom, preu, descripcio) VALUES (?, ?, ?, ?, ?, ?)",
					id, incidenciaId, proId, request.professional(), request.price(), request.note()
				);
			}
			// Notifica al client propietari si és una incidència d'un particular
			if (incidenciaId != null) {
				findIncidenciaById(incidenciaId).ifPresent(inc -> {
					if (inc.clientNom() != null && !"AVISA".equalsIgnoreCase(inc.clientNom())) {
						users().stream()
							.filter(u -> u.nom().equalsIgnoreCase(inc.clientNom()) || u.id().equalsIgnoreCase(inc.clientNom()))
							.findFirst()
							.ifPresent(clientUser -> addAvis(clientUser.id(), "Nova oferta rebuda de " + request.professional() + " per a la teva incidència '" + inc.titol() + "' (" + request.price() + ")."));
					}
				});
			} else if (tascaId != null) {
				findTaskById(tascaId).ifPresent(task -> {
					if (task.client() != null && !"AVISA".equalsIgnoreCase(task.client())) {
						users().stream()
							.filter(u -> u.nom().equalsIgnoreCase(task.client()) || u.id().equalsIgnoreCase(task.client()))
							.findFirst()
							.ifPresent(clientUser -> addAvis(clientUser.id(), "Nova oferta rebuda de " + request.professional() + " per a '" + task.titol() + "' (" + request.price() + ")."));
					}
				});
			}
		} catch (Exception ignored) {}
		return new Oferta(id, tascaId, incidenciaId, request.professional(), categoria, request.price(), request.note(), estat);
	}

	/**
	 * Accepta una oferta:
	 * - Marca l'oferta seleccionada com 'Acceptada'
	 * - Rebutja automàticament les altres ofertes d'aquella mateixa incidència
	 * - Converteix la incidència en TASCA (ara ja convergeixen client i professional obligatoris!)
	 * - Enregistra professional_id, origen_id (offerId o incidenciaId) i passa l'estat de la tasca a 'En curs'
	 * - Envia una notificació d'enhorabona al professional
	 * @param offerId Identificador de l'oferta a acceptar
	 * @return Optional amb l'oferta acceptada
	 */
	public Optional<Oferta> acceptOffer(String offerId) {
		try {
			Optional<Oferta> opt = findOfferById(offerId);
			if (opt.isPresent()) {
				Oferta of = opt.get();
				String targetId = of.tascaId() != null ? of.tascaId() : of.incidenciaId();

				String proId = null;
				for (Usuari u : users()) {
					if (u.nom().equalsIgnoreCase(of.professional()) || u.id().equalsIgnoreCase(of.professional())) {
						proId = u.id();
						break;
					}
				}

				// Comprovem si el destí prové de la taula incidencies
				Optional<Incidencia> incOpt = findIncidenciaById(targetId);
				if (incOpt.isPresent()) {
					Incidencia inc = incOpt.get();
					jdbcTemplate.update("UPDATE incidencies SET estat = 'Assignada' WHERE id = ?", targetId);

					// Creem la nova TASCA on convergeixen client i professional obligatoris!
					String newTaskId = "T-" + String.format("%03d", 100 + tasks().size() + 1);
					jdbcTemplate.update(
						"INSERT INTO tasques (id, titol, descripcio, categoria, client_id, client_nom, professional_id, professional_nom, origen_id, budget, estat, tipus) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'En curs', 'tasca')",
						newTaskId, inc.titol(), inc.descripcio(), inc.categoria(), inc.clientId(), inc.clientNom(), proId, of.professional(), inc.id(), of.price()
					);

					// Eliminar la incidència i el pressupost acceptat (passa a ser tasca i s'elimina!)
					jdbcTemplate.update("DELETE FROM publicacions WHERE id = ?", targetId);
					jdbcTemplate.update("DELETE FROM incidencies WHERE id = ?", targetId);
					jdbcTemplate.update("DELETE FROM ofertes WHERE id = ? OR tasca_id = ? OR incidencia_id = ?", offerId, targetId, targetId);
					jdbcTemplate.update("DELETE FROM pressupostos WHERE id = ? OR incidencia_id = ?", offerId, targetId);

					final String finalProId = proId;
					if (finalProId != null) {
						addAvis(finalProId, "Enhorabona! La teva oferta per a '" + inc.titol() + "' ha estat ACCEPTADA per " + inc.clientNom() + "! S'ha generat la TASCA en curs.");
					}
					return Optional.of(of);
				}

				// Si ja era una tasca existent:
				Optional<Tasca> taskOpt = findTaskById(targetId);
				if (taskOpt.isPresent()) {
					Tasca t = taskOpt.get();
					if (t.client() != null && !"AVISA".equalsIgnoreCase(t.client())) {
						jdbcTemplate.update(
							"UPDATE tasques SET estat = 'En curs', tipus = 'tasca', professional_id = ?, professional_nom = ?, origen_id = ?, budget = ? WHERE id = ?",
							proId, of.professional(), offerId, of.price(), targetId
						);
					} else {
						String newTaskId = "T-" + String.format("%03d", 100 + tasks().size() + 1);
						jdbcTemplate.update(
							"INSERT INTO tasques (id, titol, descripcio, categoria, client_id, client_nom, professional_id, professional_nom, origen_id, budget, estat, tipus) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'En curs', 'tasca')",
							newTaskId, t.titol(), t.descripcio(), t.categoria(), t.clientId(), t.clientNom(), proId, of.professional(), offerId, of.price()
						);
					}

					// Eliminar el pressupost/oferta acceptat i la resta per a aquesta tasca
					jdbcTemplate.update("DELETE FROM ofertes WHERE id = ? OR tasca_id = ?", offerId, targetId);
					jdbcTemplate.update("DELETE FROM pressupostos WHERE id = ? OR incidencia_id = ?", offerId, targetId);

					findTaskById(targetId).ifPresent(task -> {
						users().stream()
							.filter(u -> u.nom().equalsIgnoreCase(of.professional()) || u.id().equalsIgnoreCase(of.professional()))
							.findFirst()
							.ifPresent(proUser -> addAvis(proUser.id(), "Enhorabona! La teva oferta per a '" + task.titol() + "' ha estat ACCEPTADA per " + task.client() + "! S'ha generat la TASCA en curs."));
					});
				}

				return Optional.of(of);
			}
			return Optional.empty();
		} catch (Exception e) {
			return Optional.empty();
		}
	}

	/**
	 * Converteix una oferta d'un professional en una TASCA directa per a un client ('OFERTA to TAREA').
	 * Es crea una nova TASCA 'En curs' on convergeixen client i professional obligatoris.
	 * IMPORTANT: Les ofertes no desapareixen al crear-se una tasca, ja que més d'un client es pot
	 * interessar pel mateix servei del professional. L'oferta original roman activa i disponible.
	 *
	 * @param offerId Identificador de l'oferta
	 * @param clientNom Nom de l'usuari client que contracta
	 * @return Optional amb la Tasca creada
	 */
	public Optional<Tasca> convertOfferToTask(String offerId, String clientNom) {
		try {
			Optional<Oferta> opt = findOfferById(offerId);
			String proNom = null;
			String categoria = "Altres Serveis";
			String titol = "Servei contractat";
			String descripcio = "Servei contractat a través de l'oferta directa.";
			String preu = "A convenir";

			if (opt.isPresent()) {
				Oferta of = opt.get();
				proNom = of.professional();
				if (of.categoria() != null && !of.categoria().isBlank()) categoria = of.categoria();
				if (of.price() != null && !of.price().isBlank()) preu = of.price();
				if (of.note() != null && !of.note().isBlank()) {
					titol = (of.note().length() > 60) ? of.note().substring(0, 57) + "..." : of.note();
					descripcio = of.note();
				} else {
					titol = "Servei de " + categoria + " (" + proNom + ")";
				}
			} else {
				Optional<Publicacio> pubOpt = findPublicacioById(offerId);
				if (pubOpt.isPresent()) {
					Publicacio pub = pubOpt.get();
					proNom = pub.usuariNom();
					if (pub.categoria() != null && !pub.categoria().isBlank()) categoria = pub.categoria();
					if (pub.preuOPressupost() != null && !pub.preuOPressupost().isBlank()) preu = pub.preuOPressupost();
					if (pub.titol() != null && !pub.titol().isBlank()) titol = pub.titol();
					if (pub.descripcio() != null && !pub.descripcio().isBlank()) descripcio = pub.descripcio();
				} else {
					return Optional.empty();
				}
			}

			// Creació d'una nova TASCA directa en curs on convergeixen client i professional!
			String newTaskId = "T-" + String.format("%03d", 100 + tasks().size() + 1);

			String clientId = null;
			String resolvedClientNom = clientNom;
			for (Usuari u : users()) {
				if (u.nom().equalsIgnoreCase(clientNom) || u.id().equalsIgnoreCase(clientNom)) {
					clientId = u.id();
					resolvedClientNom = u.nom();
					break;
				}
			}

			String proId = null;
			for (Usuari u : users()) {
				if (u.nom().equalsIgnoreCase(proNom) || u.id().equalsIgnoreCase(proNom)) {
					proId = u.id();
					break;
				}
			}

			jdbcTemplate.update(
				"INSERT INTO tasques (id, titol, descripcio, categoria, client_id, client_nom, professional_id, professional_nom, origen_id, budget, estat, tipus, data_inici, preu_acordat, publicacio_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'En curs', 'tasca', CURRENT_TIMESTAMP, ?, ?)",
				newTaskId, titol, descripcio, categoria, clientId, resolvedClientNom, proId, proNom, offerId, preu, preu, offerId
			);

			// L'oferta original roman persistent a publicacions/ofertes per a altres clients!

			// Avisos en temps real
			final String finalTitol = titol;
			final String finalClientNom = resolvedClientNom;
			final String finalProNom = proNom;
			final String finalPreu = preu;
			if (proId != null) {
				addAvis(proId, "El client " + finalClientNom + " ha contractat la teva oferta per a '" + finalTitol + "' (" + finalPreu + "). S'ha generat una nova TASCA en curs!");
			} else {
				users().stream()
					.filter(u -> u.nom().equalsIgnoreCase(finalProNom))
					.findFirst()
					.ifPresent(proUser -> addAvis(proUser.id(), "El client " + finalClientNom + " ha contractat la teva oferta per a '" + finalTitol + "' (" + finalPreu + "). S'ha generat una nova TASCA en curs!"));
			}

			if (clientId != null) {
				addAvis(clientId, "Has contractat amb èxit el servei de " + finalProNom + " (" + finalPreu + "). La tasca '" + finalTitol + "' ja està en curs!");
			} else {
				users().stream()
					.filter(u -> u.nom().equalsIgnoreCase(clientNom))
					.findFirst()
					.ifPresent(clientUser -> addAvis(clientUser.id(), "Has contractat amb èxit el servei de " + finalProNom + " (" + finalPreu + "). La tasca '" + finalTitol + "' ja està en curs!"));
			}

			return findTaskById(newTaskId);
		} catch (Exception e) {
			return Optional.empty();
		}
	}

	/**
	 * Elimina/borra una oferta de la base de dades.
	 * Les ofertes sols s'accepten o es borren.
	 * @param id Identificador de l'oferta
	 * @return true si s'ha eliminat, false en cas contrari
	 */
	public boolean deleteOffer(String id) {
		try {
			jdbcTemplate.update("DELETE FROM publicacions WHERE id = ?", id);
			jdbcTemplate.update("DELETE FROM pressupostos WHERE id = ?", id);
			jdbcTemplate.update("DELETE FROM interessos WHERE publicacio_id = ?", id);
			int rows = jdbcTemplate.update("DELETE FROM ofertes WHERE id = ?", id);
			return rows > 0;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Descarta una oferta esborrant-la de la base de dades.
	 * Les ofertes no es rebutgen, es borren o s'accepten.
	 * @param offerId Identificador de l'oferta
	 * @return Optional amb l'oferta abans de ser eliminada
	 */
	public Optional<Oferta> rejectOffer(String offerId) {
		try {
			Optional<Oferta> opt = findOfferById(offerId);
			deleteOffer(offerId);
			return opt;
		} catch (Exception e) {
			return Optional.empty();
		}
	}

	// ==========================================
	// GESTIÓ D'AVISOS I DASHBOARD
	// ==========================================

	/**
	 * Obté tots els avisos des de PostgreSQL ordenats per data descendent.
	 * @return Llista d'avisos
	 */
	private Avis mapAvis(java.sql.ResultSet rs) throws java.sql.SQLException {
		String enllac = null;
		try {
			enllac = rs.getString("enllac");
		} catch (java.sql.SQLException ignored) {}
		return new Avis(
			rs.getString("id"),
			rs.getString("usuari_id"),
			rs.getString("missatge"),
			rs.getBoolean("llegit"),
			rs.getDate("data") != null ? rs.getDate("data").toLocalDate() : LocalDate.now(),
			enllac
		);
	}

	public List<Avis> avisos() {
		try {
			return jdbcTemplate.query(
				"SELECT id, usuari_id, missatge, llegit, data, enllac FROM avisos ORDER BY created_at DESC",
				(rs, rowNum) -> mapAvis(rs)
			);
		} catch (Exception e) {
			return List.of();
		}
	}

	/**
	 * Obté els avisos dirigits a un usuari concret.
	 * @param usuariId Identificador de l'usuari
	 * @return Llista d'avisos de l'usuari
	 */
	public List<Avis> findAvisosByUsuari(String usuariId) {
		try {
			return jdbcTemplate.query(
				"SELECT id, usuari_id, missatge, llegit, data, enllac FROM avisos WHERE usuari_id = ? OR usuari_id IN (SELECT id FROM usuaris WHERE LOWER(nom) = LOWER(?)) ORDER BY created_at DESC",
				(rs, rowNum) -> mapAvis(rs),
				usuariId, usuariId
			);
		} catch (Exception e) {
			return List.of();
		}
	}

	/**
	 * Crea un nou avís o notificació per a un usuari sense enllaç explícit (auto-detectat).
	 */
	public Avis addAvis(String usuariId, String missatge) {
		return addAvis(usuariId, missatge, null);
	}

	/**
	 * Crea un nou avís o notificació per a un usuari amb enllaç de redirecció.
	 * @param usuariId Identificador de l'usuari destinatari
	 * @param missatge Missatge descriptiu de l'avís
	 * @param enllac Enllaç o ruta cap a la secció corresponent
	 * @return L'avís creat
	 */
	public Avis addAvis(String usuariId, String missatge, String enllac) {
		String id = "A-" + UUID.randomUUID().toString().substring(0, 8);
		LocalDate data = LocalDate.now();
		String targetEnllac = enllac;
		if (targetEnllac == null || targetEnllac.isBlank()) {
			String m = missatge != null ? missatge.toLowerCase() : "";
			if (m.contains("oferta") || m.contains("contractat")) {
				targetEnllac = "/professional/les-meves-ofertes";
			} else if (m.contains("tasca") || m.contains("pressupost") || m.contains("incidència")) {
				targetEnllac = "/tasques-ofertes";
			} else if (m.contains("perfil") || m.contains("contrasenya") || m.contains("valoració") || m.contains("estrelles")) {
				targetEnllac = "/perfil";
			} else {
				targetEnllac = "/tasques-ofertes";
			}
		}

		try {
			jdbcTemplate.update(
				"INSERT INTO avisos (id, usuari_id, missatge, llegit, data, enllac) VALUES (?, ?, ?, false, ?, ?)",
				id, usuariId, missatge, data, targetEnllac
			);
		} catch (Exception ignored) {}
		return new Avis(id, usuariId, missatge, false, data, targetEnllac);
	}

	/**
	 * Marca un avís com a llegit.
	 * @param id Identificador de l'avís
	 */
	public void markAvisAsRead(String id) {
		try {
			jdbcTemplate.update("UPDATE avisos SET llegit = true WHERE id = ?", id);
		} catch (Exception ignored) {}
	}

	/**
	 * Elimina un avís de la base de dades.
	 * @param id Identificador de l'avís
	 */
	public void deleteAvis(String id) {
		try {
			jdbcTemplate.update("DELETE FROM avisos WHERE id = ?", id);
		} catch (Exception ignored) {}
	}

	/**
	 * Genera les mètriques i el resum de dades per al Dashboard principal.
	 * @return Resum de tasques obertes, ofertes actives i projectes finalitzats
	 */
	public DashboardResponse dashboard() {
		List<Tasca> currentTasks = tasks();
		List<Oferta> currentOffers = offers();
		List<Incidencia> currentIncidencies = incidencies();
		int openTasks = (int) currentIncidencies.stream().filter(inc -> "Oberta".equalsIgnoreCase(inc.estat())).count()
			+ (int) currentTasks.stream().filter(task -> "Oberta".equalsIgnoreCase(task.estat())).count();
		int activeOffers = (int) currentOffers.stream().filter(offer -> "Pendent".equalsIgnoreCase(offer.estat())).count();
		int completedProjects = (int) currentTasks.stream().filter(task -> "Finalitzada".equalsIgnoreCase(task.estat())).count();
		return new DashboardResponse(openTasks, activeOffers, completedProjects, currentTasks, currentOffers, currentIncidencies);
	}

	// ==========================================
	// GESTIÓ DE RESSENYES I VALORACIONS
	// Ara s'obtenen directament de la taula TASQUES
	// on el client ha valorat la feina en finalitzar-la!
	// ==========================================

	private final org.springframework.jdbc.core.RowMapper<Resenya> resenyaRowMapper = (rs, rowNum) -> new Resenya(
		"R-" + rs.getString("id"),
		rs.getString("client_id"),
		rs.getString("client_nom"),
		rs.getString("professional_id"),
		rs.getString("professional_nom"),
		rs.getString("id"),
		rs.getInt("valoracio"),
		rs.getString("opinio"),
		rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : LocalDateTime.now()
	);

	/**
	 * Obté totes les valoracions enregistrades a la taula tasques.
	 * @return Llista completa de valoracions
	 */
	public List<Resenya> resenyes() {
		try {
			return jdbcTemplate.query(
				"SELECT id, client_id, client_nom, professional_id, professional_nom, valoracio, opinio, created_at FROM tasques WHERE valoracio IS NOT NULL ORDER BY created_at DESC",
				resenyaRowMapper
			);
		} catch (Exception e) {
			return List.of();
		}
	}

	/**
	 * Obté les ressenyes i valoracions rebudes per un professional concret a partir de les seves tasques completades.
	 * @param treballadorIdOIdNom ID o Nom del professional
	 * @return Llista de valoracions dirigides a aquest professional
	 */
	public List<Resenya> findResenyesByTreballador(String treballadorIdOIdNom) {
		try {
			return jdbcTemplate.query(
				"SELECT id, client_id, client_nom, professional_id, professional_nom, valoracio, opinio, created_at FROM tasques WHERE valoracio IS NOT NULL AND (professional_id = ? OR LOWER(professional_nom) = LOWER(?)) ORDER BY created_at DESC",
				resenyaRowMapper,
				treballadorIdOIdNom, treballadorIdOIdNom
			);
		} catch (Exception e) {
			return List.of();
		}
	}

	/**
	 * Obté les ressenyes emeses per un client concret a partir de les seves tasques completades.
	 * @param clientId Identificador o nom de l'usuari client
	 * @return Llista de valoracions emeses pel client
	 */
	public List<Resenya> findResenyesByClient(String clientId) {
		try {
			return jdbcTemplate.query(
				"SELECT id, client_id, client_nom, professional_id, professional_nom, valoracio, opinio, created_at FROM tasques WHERE valoracio IS NOT NULL AND (client_id = ? OR LOWER(client_nom) = LOWER(?)) ORDER BY created_at DESC",
				resenyaRowMapper,
				clientId, clientId
			);
		} catch (Exception e) {
			return List.of();
		}
	}

	/**
	 * Desa una valoració directament sobre la taula TASQUES i envia avís en temps real al professional.
	 * @param resenya Dades de la valoració
	 * @return Valoració processada
	 */
	public Resenya addResenya(Resenya resenya) {
		String tascaId = resenya.tascaId();
		if (tascaId != null && !tascaId.isBlank()) {
			try {
				jdbcTemplate.update(
					"UPDATE tasques SET valoracio = ?, opinio = ? WHERE id = ?",
					resenya.puntuacio(), resenya.descripcio(), tascaId
				);

				findTaskById(tascaId).ifPresent(task -> {
					String proId = task.professionalId();
					if (proId == null && task.professionalNom() != null) {
						proId = users().stream()
							.filter(u -> u.nom().equalsIgnoreCase(task.professionalNom()))
							.findFirst()
							.map(Usuari::id)
							.orElse(null);
					}
					if (proId != null) {
						String clientNom = task.clientNom() != null ? task.clientNom() : "Un client";
						String desc = resenya.descripcio() != null ? resenya.descripcio() : "";
						addAvis(proId, clientNom + " t'ha puntuat amb " + resenya.puntuacio() + " estrelles per a la tasca '" + task.titol() + "': \"" + desc + "\"");
					}
				});
			} catch (Exception ignored) {}
		}
		return resenya;
	}

	// ==========================================
	// GESTIÓ DE PUBLICACIONS (OFERTES / INCIDÈNCIES)
	// ==========================================

	private final org.springframework.jdbc.core.RowMapper<Publicacio> publicacioRowMapper = (rs, rowNum) -> new Publicacio(
		rs.getString("id"),
		rs.getString("usuari_id"),
		rs.getString("usuari_nom"),
		rs.getString("tipus"),
		rs.getString("titol"),
		rs.getString("descripcio"),
		rs.getString("categoria"),
		rs.getString("preu_o_pressupost"),
		rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : LocalDateTime.now()
	);

	public List<Publicacio> publicacions() {
		try {
			return jdbcTemplate.query(
				"SELECT id, usuari_id, usuari_nom, tipus, titol, descripcio, categoria, preu_o_pressupost, created_at FROM publicacions ORDER BY created_at DESC",
				publicacioRowMapper
			);
		} catch (Exception e) {
			return List.of();
		}
	}

	public List<Publicacio> publicacionsByTipus(String tipus) {
		try {
			return jdbcTemplate.query(
				"SELECT id, usuari_id, usuari_nom, tipus, titol, descripcio, categoria, preu_o_pressupost, created_at FROM publicacions WHERE UPPER(tipus) = UPPER(?) ORDER BY created_at DESC",
				publicacioRowMapper,
				tipus
			);
		} catch (Exception e) {
			return List.of();
		}
	}

	public Optional<Publicacio> findPublicacioById(String id) {
		try {
			List<Publicacio> res = jdbcTemplate.query(
				"SELECT id, usuari_id, usuari_nom, tipus, titol, descripcio, categoria, preu_o_pressupost, created_at FROM publicacions WHERE id = ?",
				publicacioRowMapper,
				id
			);
			return res.stream().findFirst();
		} catch (Exception e) {
			return Optional.empty();
		}
	}

	public Publicacio addPublicacio(Publicacio pub) {
		try {
			jdbcTemplate.update(
				"INSERT INTO publicacions (id, usuari_id, usuari_nom, tipus, titol, descripcio, categoria, preu_o_pressupost) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
				pub.id(), pub.usuariId(), pub.usuariNom(), pub.tipus().toUpperCase(), pub.titol(), pub.descripcio(), pub.categoria(), pub.preuOPressupost()
			);
		} catch (Exception ignored) {}
		return pub;
	}

	public void deletePublicacio(String id) {
		try {
			jdbcTemplate.update("DELETE FROM publicacions WHERE id = ?", id);
		} catch (Exception ignored) {}
	}

	// ==========================================
	// GESTIÓ D'INTERESSOS EN OFERTES
	// ==========================================

	private final org.springframework.jdbc.core.RowMapper<Interes> interesRowMapper = (rs, rowNum) -> new Interes(
		rs.getString("id"),
		rs.getString("publicacio_id"),
		rs.getString("publicacio_titol"),
		rs.getString("client_id"),
		rs.getString("client_nom"),
		rs.getString("professional_id"),
		rs.getString("professional_nom"),
		rs.getString("missatge"),
		rs.getString("tasca_id"),
		rs.getTimestamp("data_creacio") != null ? rs.getTimestamp("data_creacio").toLocalDateTime() : LocalDateTime.now()
	);

	public List<Interes> findInteressosByProfessional(String proNomOrId) {
		try {
			return jdbcTemplate.query(
				"SELECT i.id, i.publicacio_id, COALESCE(p.titol, o.categoria, 'Servei Professional') AS publicacio_titol, i.client_id, i.client_nom, COALESCE(p.usuari_id, u.id) AS professional_id, COALESCE(p.usuari_nom, o.professional) AS professional_nom, i.missatge, i.tasca_id, i.data_creacio " +
				"FROM interessos i " +
				"LEFT JOIN publicacions p ON p.id = i.publicacio_id " +
				"LEFT JOIN ofertes o ON o.id = i.publicacio_id " +
				"LEFT JOIN usuaris u ON LOWER(u.nom) = LOWER(COALESCE(p.usuari_nom, o.professional)) " +
				"WHERE LOWER(COALESCE(p.usuari_nom, o.professional)) = LOWER(?) OR COALESCE(p.usuari_id, u.id) = ? " +
				"ORDER BY i.data_creacio DESC",
				interesRowMapper,
				proNomOrId, proNomOrId
			);
		} catch (Exception e) {
			return List.of();
		}
	}

	public List<Interes> findInteressosByClient(String clientNomOrId) {
		try {
			return jdbcTemplate.query(
				"SELECT i.id, i.publicacio_id, COALESCE(p.titol, o.categoria, 'Servei Professional') AS publicacio_titol, i.client_id, i.client_nom, COALESCE(p.usuari_id, u.id) AS professional_id, COALESCE(p.usuari_nom, o.professional) AS professional_nom, i.missatge, i.tasca_id, i.data_creacio " +
				"FROM interessos i " +
				"LEFT JOIN publicacions p ON p.id = i.publicacio_id " +
				"LEFT JOIN ofertes o ON o.id = i.publicacio_id " +
				"LEFT JOIN usuaris u ON LOWER(u.nom) = LOWER(COALESCE(p.usuari_nom, o.professional)) " +
				"WHERE LOWER(i.client_nom) = LOWER(?) OR i.client_id = ? " +
				"ORDER BY i.data_creacio DESC",
				interesRowMapper,
				clientNomOrId, clientNomOrId
			);
		} catch (Exception e) {
			return List.of();
		}
	}

	public Optional<Interes> findInteresById(String id) {
		try {
			List<Interes> res = jdbcTemplate.query(
				"SELECT i.id, i.publicacio_id, COALESCE(p.titol, o.categoria, 'Servei Professional') AS publicacio_titol, i.client_id, i.client_nom, COALESCE(p.usuari_id, u.id) AS professional_id, COALESCE(p.usuari_nom, o.professional) AS professional_nom, i.missatge, i.tasca_id, i.data_creacio " +
				"FROM interessos i " +
				"LEFT JOIN publicacions p ON p.id = i.publicacio_id " +
				"LEFT JOIN ofertes o ON o.id = i.publicacio_id " +
				"LEFT JOIN usuaris u ON LOWER(u.nom) = LOWER(COALESCE(p.usuari_nom, o.professional)) " +
				"WHERE i.id = ?",
				interesRowMapper,
				id
			);
			return res.stream().findFirst();
		} catch (Exception e) {
			return Optional.empty();
		}
	}

	public Interes addInteres(CrearInteresRequest req) {
		String id = "INT-" + UUID.randomUUID().toString().substring(0, 8);
		String clientId = req.clientId();
		String clientNom = req.clientNom();
		if (clientNom != null) {
			for (Usuari u : users()) {
				if (u.nom().equalsIgnoreCase(clientNom) || u.id().equalsIgnoreCase(clientNom)) {
					clientId = u.id();
					clientNom = u.nom();
					break;
				}
			}
		}

		final String finalClientNom = clientNom;
		try {
			jdbcTemplate.update(
				"INSERT INTO interessos (id, publicacio_id, client_id, client_nom, missatge) VALUES (?, ?, ?, ?, ?)",
				id, req.publicacioId(), clientId, finalClientNom, req.missatge()
			);

			// Avís al professional propietari de l'oferta
			findPublicacioById(req.publicacioId()).ifPresent(pub -> {
				if (pub.usuariId() != null) {
					addAvis(pub.usuariId(), "El client " + finalClientNom + " ha mostrat interés en la teva oferta '" + pub.titol() + "'!");
				} else if (pub.usuariNom() != null) {
					users().stream()
						.filter(u -> u.nom().equalsIgnoreCase(pub.usuariNom()))
						.findFirst()
						.ifPresent(pro -> addAvis(pro.id(), "El client " + finalClientNom + " ha mostrat interés en la teva oferta '" + pub.titol() + "'!"));
				}
			});
		} catch (Exception ignored) {}

		return new Interes(id, req.publicacioId(), clientId, finalClientNom, req.missatge(), null, LocalDateTime.now());
	}

	@Transactional
	public Optional<Tasca> acceptarInteres(String interesId) {
		Optional<Interes> intOpt = findInteresById(interesId);
		if (intOpt.isEmpty()) return Optional.empty();
		Interes interes = intOpt.get();

		// Si ja té una tasca vinculada, no en creem una segona
		if (interes.tascaId() != null && !interes.tascaId().isBlank()) {
			return findTaskById(interes.tascaId());
		}

		Optional<Publicacio> pubOpt = findPublicacioById(interes.publicacioId());
		Optional<Oferta> ofOpt = findOfferById(interes.publicacioId());

		String titol = pubOpt.map(Publicacio::titol).orElse(ofOpt.map(o -> "Servei de " + o.categoria()).orElse("Servei acordat"));
		String desc = pubOpt.map(Publicacio::descripcio).orElse(ofOpt.map(Oferta::note).orElse(interes.missatge()));
		String cat = pubOpt.map(Publicacio::categoria).orElse(ofOpt.map(Oferta::categoria).orElse("Altres Serveis"));
		String preu = pubOpt.map(Publicacio::preuOPressupost).orElse(ofOpt.map(Oferta::price).orElse("Acordat"));
		String proId = pubOpt.map(Publicacio::usuariId).orElse(interes.professionalId());
		String proNom = pubOpt.map(Publicacio::usuariNom).orElse(interes.professionalNom());

		String newTaskId = "T-" + String.format("%03d", 100 + tasks().size() + 1);

		// Crea la TASCA en curs (sense estat redundant a la BD, data_finalitzacio = null)
		jdbcTemplate.update(
			"INSERT INTO tasques (id, titol, descripcio, categoria, client_id, client_nom, professional_id, professional_nom, publicacio_id, interes_id, preu_acordat, data_inici, estat, tipus) " +
			"VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, 'En curs', 'tasca')",
			newTaskId, titol, desc, cat, interes.clientId(), interes.clientNom(), proId, proNom, interes.publicacioId(), interesId, preu
		);

		// Vincula la tasca a l'interès per evitar duplicats
		jdbcTemplate.update("UPDATE interessos SET tasca_id = ? WHERE id = ?", newTaskId, interesId);

		// Notifica al client
		if (interes.clientId() != null) {
			addAvis(interes.clientId(), "El professional " + proNom + " ha acceptat el teu interés! S'ha iniciat la tasca en curs '" + titol + "'.");
		}

		return findTaskById(newTaskId);
	}

	public boolean rebutjarInteres(String interesId) {
		Optional<Interes> intOpt = findInteresById(interesId);
		if (intOpt.isEmpty()) return false;
		Interes interes = intOpt.get();

		if (interes.tascaId() != null && !interes.tascaId().isBlank()) {
			return false;
		}

		int rows = jdbcTemplate.update("DELETE FROM interessos WHERE id = ?", interesId);
		if (rows > 0 && interes.clientId() != null) {
			addAvis(interes.clientId(), "El teu interés en l'oferta ha estat descartat pel professional.");
		}
		return rows > 0;
	}

	// ==========================================
	// GESTIÓ DE PRESSUPOSTOS PER A INCIDÈNCIES
	// ==========================================

	private final org.springframework.jdbc.core.RowMapper<Pressupost> pressupostRowMapper = (rs, rowNum) -> new Pressupost(
		rs.getString("id"),
		rs.getString("incidencia_id"),
		rs.getString("incidencia_titol"),
		rs.getString("professional_id"),
		rs.getString("professional_nom"),
		rs.getString("preu"),
		rs.getString("descripcio"),
		rs.getString("duracio_estimada"),
		rs.getTimestamp("data_creacio") != null ? rs.getTimestamp("data_creacio").toLocalDateTime() : LocalDateTime.now()
	);

	public List<Pressupost> pressupostos() {
		try {
			return jdbcTemplate.query(
				"SELECT pr.id, pr.incidencia_id, COALESCE(p.titol, i.titol, 'Incidència') AS incidencia_titol, pr.professional_id, pr.professional_nom, pr.preu, pr.descripcio, pr.duracio_estimada, pr.data_creacio " +
				"FROM pressupostos pr " +
				"LEFT JOIN publicacions p ON p.id = pr.incidencia_id " +
				"LEFT JOIN incidencies i ON i.id = pr.incidencia_id " +
				"ORDER BY pr.data_creacio DESC",
				pressupostRowMapper
			);
		} catch (Exception e) {
			return List.of();
		}
	}

	public List<Pressupost> findPressupostosByIncidencia(String incidenciaId) {
		try {
			return jdbcTemplate.query(
				"SELECT pr.id, pr.incidencia_id, COALESCE(p.titol, i.titol, 'Incidència') AS incidencia_titol, pr.professional_id, pr.professional_nom, pr.preu, pr.descripcio, pr.duracio_estimada, pr.data_creacio " +
				"FROM pressupostos pr " +
				"LEFT JOIN publicacions p ON p.id = pr.incidencia_id " +
				"LEFT JOIN incidencies i ON i.id = pr.incidencia_id " +
				"WHERE pr.incidencia_id = ? " +
				"ORDER BY pr.data_creacio DESC",
				pressupostRowMapper,
				incidenciaId
			);
		} catch (Exception e) {
			return List.of();
		}
	}

	public Optional<Pressupost> findPressupostById(String id) {
		try {
			List<Pressupost> res = jdbcTemplate.query(
				"SELECT pr.id, pr.incidencia_id, COALESCE(p.titol, i.titol, 'Incidència') AS incidencia_titol, pr.professional_id, pr.professional_nom, pr.preu, pr.descripcio, pr.duracio_estimada, pr.data_creacio " +
				"FROM pressupostos pr " +
				"LEFT JOIN publicacions p ON p.id = pr.incidencia_id " +
				"LEFT JOIN incidencies i ON i.id = pr.incidencia_id " +
				"WHERE pr.id = ?",
				pressupostRowMapper,
				id
			);
			return res.stream().findFirst();
		} catch (Exception e) {
			return Optional.empty();
		}
	}

	/**
	 * Llistar els pressupostos presentats per a una incidència aplicant PRIVADESA ESTRICTA:
	 * Només els 2 usuaris implicats (el client propietari de la incidència i el professional autor del pressupost)
	 * o l'administrador poden visualitzar els pressupostos.
	 * Els professionals només poden veure la seua pròpia oferta, no les dels competidors.
	 */
	public List<Pressupost> findPressupostosByIncidenciaSecure(String incidenciaId, String requesterId, String requesterNom, String requesterRol) {
		List<Pressupost> all = findPressupostosByIncidencia(incidenciaId);
		if (all.isEmpty()) return List.of();

		boolean isAdmin = (requesterRol != null && requesterRol.equalsIgnoreCase("admin"));
		if (isAdmin) return all;

		if ((requesterId == null || requesterId.isBlank()) && (requesterNom == null || requesterNom.isBlank())) {
			return List.of();
		}

		boolean isClientOwner = isClientOwnerOfIncidencia(incidenciaId, requesterId, requesterNom);
		if (isClientOwner) {
			// El client titular veu tots els pressupostos rebuts per al seu anunci
			return all;
		}

		// Si és un professional, només veu el seu propi pressupost
		final String rId = requesterId != null ? requesterId.trim().toLowerCase() : "";
		final String rNom = requesterNom != null ? requesterNom.trim().toLowerCase() : "";

		return all.stream()
			.filter(p -> {
				String pId = p.professionalId() != null ? p.professionalId().trim().toLowerCase() : "";
				String pNom = p.professionalNom() != null ? p.professionalNom().trim().toLowerCase() : "";
				return (!rId.isEmpty() && rId.equals(pId)) || (!rNom.isEmpty() && rNom.equals(pNom));
			})
			.toList();
	}

	/**
	 * Comprova si l'usuari és el client titular d'una incidència o tasca.
	 */
	public boolean isClientOwnerOfIncidencia(String incidenciaId, String requesterId, String requesterNom) {
		Optional<Publicacio> pubOpt = findPublicacioById(incidenciaId);
		if (pubOpt.isPresent()) {
			Publicacio pub = pubOpt.get();
			if ((requesterId != null && requesterId.equalsIgnoreCase(pub.usuariId())) ||
			    (requesterNom != null && requesterNom.equalsIgnoreCase(pub.usuariNom()))) {
				return true;
			}
		}
		Optional<Incidencia> incOpt = findIncidenciaById(incidenciaId);
		if (incOpt.isPresent()) {
			Incidencia inc = incOpt.get();
			if ((requesterId != null && requesterId.equalsIgnoreCase(inc.clientId())) ||
			    (requesterNom != null && requesterNom.equalsIgnoreCase(inc.clientNom()))) {
				return true;
			}
		}
		Optional<Tasca> taskOpt = findTaskById(incidenciaId);
		if (taskOpt.isPresent()) {
			Tasca t = taskOpt.get();
			if ((requesterId != null && requesterId.equalsIgnoreCase(t.clientId())) ||
			    (requesterNom != null && requesterNom.equalsIgnoreCase(t.client()))) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Llistar els pressupostos enviats per un professional en concret.
	 */
	public List<Pressupost> findPressupostosByProfessional(String proIdentifier) {
		if (proIdentifier == null || proIdentifier.isBlank()) return List.of();
		try {
			return jdbcTemplate.query(
				"SELECT pr.id, pr.incidencia_id, COALESCE(p.titol, i.titol, 'Incidència') AS incidencia_titol, pr.professional_id, pr.professional_nom, pr.preu, pr.descripcio, pr.duracio_estimada, pr.data_creacio " +
				"FROM pressupostos pr " +
				"LEFT JOIN publicacions p ON p.id = pr.incidencia_id " +
				"LEFT JOIN incidencies i ON i.id = pr.incidencia_id " +
				"WHERE LOWER(pr.professional_id) = LOWER(?) OR LOWER(pr.professional_nom) = LOWER(?) " +
				"ORDER BY pr.data_creacio DESC",
				pressupostRowMapper,
				proIdentifier, proIdentifier
			);
		} catch (Exception e) {
			return List.of();
		}
	}

	/**
	 * Comprova si un usuari és un dels 2 implicats en aquest pressupost o administrador.
	 */
	public boolean isUserInvolvedInPressupost(Pressupost p, String requesterId, String requesterNom, String requesterRol) {
		if (requesterRol != null && requesterRol.equalsIgnoreCase("admin")) return true;
		if (requesterId == null && requesterNom == null) return false;

		// 1. És el professional emissor?
		if (requesterId != null && requesterId.equalsIgnoreCase(p.professionalId())) return true;
		if (requesterNom != null && requesterNom.equalsIgnoreCase(p.professionalNom())) return true;

		// 2. És el client titular de la incidència?
		return isClientOwnerOfIncidencia(p.incidenciaId(), requesterId, requesterNom);
	}

	public Pressupost addPressupost(CrearPressupostRequest req) {
		String id = "P-" + String.format("%03d", 200 + offers().size() + 1);
		String proId = req.professionalId();
		String proNom = req.professionalNom();
		if (proNom != null) {
			for (Usuari u : users()) {
				if (u.nom().equalsIgnoreCase(proNom) || u.id().equalsIgnoreCase(proNom)) {
					proId = u.id();
					proNom = u.nom();
					break;
				}
			}
		}

		try {
			jdbcTemplate.update(
				"INSERT INTO pressupostos (id, incidencia_id, professional_id, professional_nom, preu, descripcio, duracio_estimada) VALUES (?, ?, ?, ?, ?, ?, ?)",
				id, req.incidenciaId(), proId, proNom, req.preu(), req.descripcio(), req.duracioEstimada()
			);

			final String finalProNom = proNom;
			// Notifica al client titular de la incidència
			findPublicacioById(req.incidenciaId()).ifPresent(inc -> {
				if (inc.usuariId() != null) {
					addAvis(inc.usuariId(), "Nova proposta de pressupost de " + finalProNom + " (" + req.preu() + ") per a la teva incidència '" + inc.titol() + "'.");
				}
			});
			findIncidenciaById(req.incidenciaId()).ifPresent(inc -> {
				if (inc.clientId() != null) {
					addAvis(inc.clientId(), "Nova proposta de pressupost de " + finalProNom + " (" + req.preu() + ") per a la teva incidència '" + inc.titol() + "'.");
				}
			});
		} catch (Exception ignored) {}

		return new Pressupost(id, req.incidenciaId(), null, proId, proNom, req.preu(), req.descripcio(), req.duracioEstimada(), LocalDateTime.now());
	}

	@Transactional
	public Optional<Tasca> acceptarPressupost(String pressupostId) {
		Optional<Pressupost> pOpt = findPressupostById(pressupostId);
		if (pOpt.isEmpty()) return Optional.empty();
		Pressupost p = pOpt.get();

		// 1. Validar que la incidència o tasca existeix
		Optional<Publicacio> pubOpt = findPublicacioById(p.incidenciaId());
		Optional<Incidencia> incOpt = findIncidenciaById(p.incidenciaId());
		Optional<Tasca> taskOpt = findTaskById(p.incidenciaId());
		if (pubOpt.isEmpty() && incOpt.isEmpty() && taskOpt.isEmpty()) {
			return Optional.empty();
		}

		if (taskOpt.isPresent()) {
			Tasca t = taskOpt.get();
			jdbcTemplate.update(
				"UPDATE tasques SET estat = 'En curs', tipus = 'tasca', professional_id = ?, professional_nom = ?, pressupost_id = ?, preu_acordat = ?, budget = ? WHERE id = ?",
				p.professionalId(), p.professionalNom(), pressupostId, p.preu(), p.preu(), t.id()
			);

			// El pressupost acceptat i la resta s'eliminen perquè ja és una tasca en curs
			jdbcTemplate.update("DELETE FROM pressupostos WHERE incidencia_id = ? OR id = ?", p.incidenciaId(), pressupostId);

			if (p.professionalId() != null) {
				addAvis(p.professionalId(), "Enhorabona! " + t.client() + " ha acceptat el teu pressupost per a '" + t.titol() + "' (" + p.preu() + ")! S'ha generat la tasca en curs.");
			}
			return findTaskById(t.id());
		}

		String titol = pubOpt.map(Publicacio::titol).orElse(incOpt.map(Incidencia::titol).orElse("Treball acordat"));
		String desc = pubOpt.map(Publicacio::descripcio).orElse(incOpt.map(Incidencia::descripcio).orElse(p.descripcio()));
		String cat = pubOpt.map(Publicacio::categoria).orElse(incOpt.map(Incidencia::categoria).orElse("Altres Serveis"));
		String clientId = pubOpt.map(Publicacio::usuariId).orElse(incOpt.map(Incidencia::clientId).orElse(null));
		String clientNom = pubOpt.map(Publicacio::usuariNom).orElse(incOpt.map(Incidencia::clientNom).orElse("Client AVISA"));

		String newTaskId = "T-" + String.format("%03d", 100 + tasks().size() + 1);

		// 2. Crear la Tasca
		jdbcTemplate.update(
			"INSERT INTO tasques (id, titol, descripcio, categoria, client_id, client_nom, professional_id, professional_nom, pressupost_id, preu_acordat, data_inici, estat, tipus) " +
			"VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, 'En curs', 'tasca')",
			newTaskId, titol, desc, cat, clientId, clientNom, p.professionalId(), p.professionalNom(), pressupostId, p.preu()
		);

		// 3. Eliminar la incidència de PUBLICACIONS i INCIDENCIES (ja contractada!)
		jdbcTemplate.update("DELETE FROM publicacions WHERE id = ?", p.incidenciaId());
		jdbcTemplate.update("DELETE FROM incidencies WHERE id = ?", p.incidenciaId());

		// 4. Eliminar el pressupost acceptat i la resta de propostes (passa a ser una tasca i s'elimina!)
		jdbcTemplate.update("DELETE FROM pressupostos WHERE incidencia_id = ? OR id = ?", p.incidenciaId(), pressupostId);

		// 5. Notificar al professional seleccionat
		if (p.professionalId() != null) {
			addAvis(p.professionalId(), "Enhorabona! " + clientNom + " ha acceptat el teu pressupost per a '" + titol + "' (" + p.preu() + ")! S'ha generat la tasca en curs.");
		}

		return findTaskById(newTaskId);
	}

	public boolean rebutjarPressupost(String pressupostId) {
		int rows = jdbcTemplate.update("DELETE FROM pressupostos WHERE id = ?", pressupostId);
		return rows > 0;
	}

	// ==========================================
	// GESTIÓ DE LA LLISTA NEGRA DE TELÈFONS
	// ==========================================

	/**
	 * Normalitza un telèfon eliminant espais, guions, parèntesis i prefixos internacionals de l'estat espanyol
	 * per fer comparacions robustes sense falsos positius o evasions de format.
	 */
	public String normalizePhone(String phone) {
		if (phone == null) return "";
		String digits = phone.replaceAll("[^0-9]", "");
		if (digits.startsWith("0034") && digits.length() >= 13) {
			digits = digits.substring(4);
		} else if (digits.startsWith("34") && digits.length() >= 11) {
			digits = digits.substring(2);
		}
		return digits;
	}

	/**
	 * Comprova si un número de telèfon està inclòs a la llista negra de la plataforma.
	 */
	public boolean isPhoneBlacklisted(String telefon) {
		if (telefon == null || telefon.trim().isBlank()) return false;
		String cleanInput = telefon.trim();
		String normalizedInput = normalizePhone(cleanInput);

		try {
			List<com.avisa.model.BlacklistTelefon> list = blacklistTelefons();
			for (com.avisa.model.BlacklistTelefon bl : list) {
				if (bl.telefon() == null) continue;
				String blClean = bl.telefon().trim();
				if (blClean.equalsIgnoreCase(cleanInput)) return true;
				String normalizedBl = normalizePhone(blClean);
				if (!normalizedBl.isBlank() && normalizedBl.equals(normalizedInput)) return true;
			}
		} catch (Exception ignored) {}
		return false;
	}

	/**
	 * Retorna tots els números de telèfon bloquejats a la llista negra.
	 */
	public List<com.avisa.model.BlacklistTelefon> blacklistTelefons() {
		try {
			return jdbcTemplate.query(
				"SELECT id, telefon, motiu, bloquejat_per, created_at FROM blacklist_telefons ORDER BY created_at DESC",
				(rs, rowNum) -> new com.avisa.model.BlacklistTelefon(
					rs.getString("id"),
					rs.getString("telefon"),
					rs.getString("motiu"),
					rs.getString("bloquejat_per"),
					rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : LocalDateTime.now()
				)
			);
		} catch (Exception e) {
			return List.of();
		}
	}

	/**
	 * Afegeix un número de telèfon a la llista negra (privilegi exclusiu d'administrador).
	 */
	public com.avisa.model.BlacklistTelefon addToBlacklist(String telefon, String motiu, String requesterRol, String requesterNom) {
		boolean isAdmin = (requesterRol != null && requesterRol.equalsIgnoreCase("admin"));
		if (!isAdmin) {
			throw new SecurityException("Només un administrador té permisos per afegir números de telèfon a la llista negra.");
		}
		if (telefon == null || telefon.trim().isBlank()) {
			throw new IllegalArgumentException("El número de telèfon és obligatori.");
		}
		String cleanPhone = telefon.trim();
		if (isPhoneBlacklisted(cleanPhone)) {
			throw new IllegalArgumentException("El número de telèfon " + cleanPhone + " ja està registrat a la llista negra.");
		}

		String id = "BL-" + String.format("%03d", blacklistTelefons().size() + 1);
		String admin = (requesterNom != null && !requesterNom.isBlank()) ? requesterNom : "admin";
		String motiuFinal = (motiu != null && !motiu.trim().isBlank()) ? motiu.trim() : "Bloquejat per administració";

		jdbcTemplate.update(
			"INSERT INTO blacklist_telefons (id, telefon, motiu, bloquejat_per) VALUES (?, ?, ?, ?)",
			id, cleanPhone, motiuFinal, admin
		);

		return new com.avisa.model.BlacklistTelefon(id, cleanPhone, motiuFinal, admin, LocalDateTime.now());
	}

	/**
	 * Desbloqueja i elimina un telèfon de la llista negra.
	 */
	public boolean removeFromBlacklist(String id, String requesterRol) {
		boolean isAdmin = (requesterRol != null && requesterRol.equalsIgnoreCase("admin"));
		if (!isAdmin) {
			throw new SecurityException("Només un administrador té permisos per desbloquejar telèfons de la llista negra.");
		}
		int affected = jdbcTemplate.update("DELETE FROM blacklist_telefons WHERE id = ? OR telefon = ?", id, id);
		return affected > 0;
	}
}