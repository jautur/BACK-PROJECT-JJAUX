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
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Catàleg central i capa d'accés a dades per a l'aplicació JJAUX.
 * Executa operacions de consulta, inserció i actualització contra la base de dades PostgreSQL (jjaux_db).
 */
@Component
public class InMemoryCatalog {

	private static final String TELEFON_BLOQUEJAT = "Aquest número de telèfon no es pot utilitzar a la plataforma.";

	private final JdbcTemplate jdbcTemplate;

	public InMemoryCatalog(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
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
		String fotoPerfil = null;
		try {
			fotoPerfil = rs.getString("foto_perfil");
		} catch (java.sql.SQLException ignored) {}
		String logo = null;
		try {
			logo = rs.getString("logo");
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
			foto,
			fotoPerfil,
			logo
		);
	}

	/**
	 * Recupera tots els usuaris registrats de la base de dades.
	 * @return Llista completa d'usuaris
	 */
	public List<Usuari> users() {
		try {
			return jdbcTemplate.query(
				"SELECT id::text AS id, nom, email, LOWER(rol) AS rol, telefon, bio, COALESCE(percentil, 3) AS percentil, COALESCE(verificat, FALSE) AS verificat, COALESCE(sollicitud_verificacio, FALSE) AS sollicitud_verificacio, foto, foto_perfil, logo FROM usuaris ORDER BY created_at",
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
				"SELECT id::text AS id, nom, email, LOWER(rol) AS rol, telefon, bio, COALESCE(percentil, 3) AS percentil, COALESCE(verificat, FALSE) AS verificat, COALESCE(sollicitud_verificacio, FALSE) AS sollicitud_verificacio, foto, foto_perfil, logo FROM usuaris WHERE id::text = ? OR LOWER(nom) = LOWER(?) OR LOWER(email) = LOWER(?)",
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
				"SELECT id::text AS id, nom, email, LOWER(rol) AS rol, telefon, bio, COALESCE(percentil, 3) AS percentil, COALESCE(verificat, FALSE) AS verificat, COALESCE(sollicitud_verificacio, FALSE) AS sollicitud_verificacio, foto, foto_perfil, logo FROM usuaris WHERE LOWER(email) = LOWER(?)",
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
				"SELECT id::text AS id, nom, email, LOWER(rol) AS rol, telefon, bio, COALESCE(percentil, 3) AS percentil, COALESCE(verificat, FALSE) AS verificat, COALESCE(sollicitud_verificacio, FALSE) AS sollicitud_verificacio, foto, foto_perfil, logo FROM usuaris WHERE LOWER(email) = LOWER(?) AND (password = ? OR ? IS NULL)",
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
		Usuari actual = findUserById(id).orElseThrow(() -> new IllegalArgumentException("Usuari no trobat"));
		if (nou.nom() == null || nou.nom().isBlank()) {
			throw new IllegalArgumentException("El nom és obligatori.");
		}
		if (nou.telefon() != null
			&& !normalizePhone(nou.telefon()).equals(normalizePhone(actual.telefon()))
			&& isPhoneBlacklisted(nou.telefon())) {
			throw new IllegalArgumentException(TELEFON_BLOQUEJAT);
		}
		int percentilFinal = actual.percentil() != null ? actual.percentil() : 3;

		if (nou.percentil() != null) {
			int proposed = nou.percentil();
			if (proposed < 0 || proposed > 100) {
				throw new IllegalArgumentException("El percentil ha d'estar comprès entre 0 i 100.");
			}

			boolean isAdmin = (requesterRol != null && requesterRol.equalsIgnoreCase("admin"))
				|| "admin".equalsIgnoreCase(actual.rol());

			if (proposed < percentilFinal) {
				if (!isAdmin) {
					throw new SecurityException("Només un administrador pot reduir el percentil de comissió (comissió actual: " + percentilFinal + "%). Com a usuari només pots augmentar-lo.");
				}
				percentilFinal = proposed;
			} else {
				percentilFinal = proposed;
			}
		}

		String fotoFinal = (nou.foto() != null) ? (nou.foto().trim().isBlank() ? null : nou.foto().trim()) : actual.foto();

		jdbcTemplate.update(
			"UPDATE usuaris SET nom = ?, telefon = ?, bio = ?, percentil = ?, foto = ? WHERE id = ?::uuid",
			nou.nom(), nou.telefon(), nou.bio(), percentilFinal, fotoFinal, actual.id()
		);
		return findUserById(actual.id()).orElse(nou);
	}

	/**
	 * Assigna o elimina la fotografia d'un professional.
	 * @param id Identificador de l'usuari
	 * @param foto URL de la fotografia (o buit per esborrar-la)
	 * @return Usuari actualitzat
	 */
	public Usuari assignarFoto(String id, String foto) {
		String clean = (foto != null && !foto.trim().isBlank()) ? foto.trim() : null;
		jdbcTemplate.update("UPDATE usuaris SET foto = ? WHERE id::text = ?", clean, id);
		return findUserById(id).orElseThrow(() -> new IllegalArgumentException("Usuari no trobat"));
	}

	/**
	 * Actualitza la contrasenya d'un usuari directament a la base de dades.
	 * Permet a l'administrador o al propi usuari assignar una nova contrasenya sense conèixer l'antiga.
	 * @param id Identificador de l'usuari
	 * @param newPassword Nova contrasenya
	 */
	public void changePassword(String id, String newPassword) {
		int rows = jdbcTemplate.update("UPDATE usuaris SET password = ? WHERE id::text = ?", newPassword, id);
		if (rows == 0) {
			throw new IllegalArgumentException("Usuari no trobat");
		}
		addAvis(id, "La teva contrasenya d'accés ha estat actualitzada correctament.");
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
		jdbcTemplate.update("UPDATE usuaris SET sollicitud_verificacio = TRUE WHERE id::text = ?", usuari.id());
		notificarAdmins("Nova sol·licitud de verificació professional: " + usuari.nom() + " (Tel: " + usuari.telefon() + ", Email: " + usuari.email() + "). Cal realitzar la trucada de comprovació.");
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
		jdbcTemplate.update("UPDATE usuaris SET verificat = ?, sollicitud_verificacio = FALSE WHERE id::text = ?", estat, usuari.id());
		if (estat) {
			addAvis(id, "Enhorabona! L'administrador ha verificat oficialment el teu compte de professional després de la comprovació telefònica.");
		} else {
			addAvis(id, "El teu estat de professional verificat ha estat desactivat o la teva sol·licitud ha estat rebutjada.");
		}
		return findUserById(id).orElse(usuari);
	}

	/**
	 * Elimina un usuari de la plataforma. Els avisos s'esborren en cascada.
	 * Els administradors estan protegits pel seu rol, i els usuaris amb historial no es poden esborrar.
	 * @param id Identificador de l'usuari
	 * @return true si s'ha eliminat, false si no existeix, és administrador o té historial
	 */
	public boolean deleteUser(String id) {
		Optional<Usuari> usuari = findUserById(id);
		if (usuari.isEmpty() || "admin".equalsIgnoreCase(usuari.get().rol())) {
			return false;
		}
		try {
			return jdbcTemplate.update("DELETE FROM usuaris WHERE id = ?::uuid", usuari.get().id()) > 0;
		} catch (DataIntegrityViolationException e) {
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
		if (isPhoneBlacklisted(request.telefon())) {
			throw new IllegalArgumentException(TELEFON_BLOQUEJAT);
		}
		String id = UUID.randomUUID().toString();
		String rol = request.rol().toLowerCase(Locale.ROOT);
		try {
			jdbcTemplate.update(
				"INSERT INTO usuaris (id, nom, email, password, rol, telefon) VALUES (?::uuid, ?, ?, ?, ?, ?)",
				id, request.nom(), request.email(), request.password(), rol.toUpperCase(Locale.ROOT), request.telefon()
			);
		} catch (DuplicateKeyException e) {
			throw new IllegalArgumentException("El correu electrònic '" + request.email() + "' ja està en ús.");
		}
		return new Usuari(id, request.nom(), request.email(), rol, request.telefon(), null, 3);
	}

	// ==========================================
	// PUBLICACIONS: INCIDÈNCIES I OFERTES EN UNA SOLA TAULA
	// /api/incidencies, /api/offers i /api/publicacions són vistes de la taula publicacions.
	// ==========================================

	private static final String SELECT_PUBLICACIO =
		"SELECT p.id::text AS id, p.autor_id::text AS autor_id, u.nom AS autor_nom, p.tipus, p.titol, p.descripcio, p.categoria, p.preu, p.estat, p.created_at "
		+ "FROM publicacions p JOIN usuaris u ON u.id = p.autor_id ";

	private static final Map<String, String> ETIQUETES_PUBLICACIO = Map.of(
		"OBERTA", "Oberta",
		"ASSIGNADA", "Assignada",
		"TANCADA", "Tancada",
		"ACTIVA", "Pendent",
		"RETIRADA", "Retirada"
	);

	private final org.springframework.jdbc.core.RowMapper<Publicacio> publicacioRowMapper = (rs, rowNum) -> new Publicacio(
		rs.getString("id"),
		rs.getString("autor_id"),
		rs.getString("autor_nom"),
		rs.getString("tipus"),
		rs.getString("titol"),
		rs.getString("descripcio"),
		rs.getString("categoria"),
		rs.getString("preu"),
		rs.getTimestamp("created_at").toLocalDateTime(),
		ETIQUETES_PUBLICACIO.get(rs.getString("estat"))
	);

	private final org.springframework.jdbc.core.RowMapper<Incidencia> incidenciaRowMapper = (rs, rowNum) -> new Incidencia(
		rs.getString("id"),
		rs.getString("autor_id"),
		rs.getString("autor_nom"),
		rs.getString("titol"),
		rs.getString("descripcio"),
		rs.getString("categoria"),
		rs.getString("preu"),
		ETIQUETES_PUBLICACIO.get(rs.getString("estat")),
		rs.getTimestamp("created_at").toLocalDateTime()
	);

	private final org.springframework.jdbc.core.RowMapper<Oferta> ofertaRowMapper = (rs, rowNum) -> new Oferta(
		rs.getString("id"),
		null,
		null,
		rs.getString("autor_nom"),
		rs.getString("categoria"),
		rs.getString("preu"),
		rs.getString("descripcio"),
		ETIQUETES_PUBLICACIO.get(rs.getString("estat"))
	);

	/** Un identificador que no és un UUID no correspon a cap fila. */
	private static Optional<UUID> uuid(String id) {
		if (id == null) {
			return Optional.empty();
		}
		try {
			return Optional.of(UUID.fromString(id.trim()));
		} catch (IllegalArgumentException e) {
			return Optional.empty();
		}
	}

	private static boolean esAdmin(Usuari usuari) {
		return "admin".equalsIgnoreCase(usuari.rol());
	}

	private Usuari usuariActor(String actorId) {
		return findUserById(actorId).orElseThrow(() -> new SecurityException("Usuari no reconegut."));
	}

	private Usuari autorDe(String identificador, String tipus) {
		Usuari autor = findUserById(identificador)
			.orElseThrow(() -> new IllegalArgumentException("L'autor de la publicació no existeix."));
		if ("OFERTA".equals(tipus) && "client".equalsIgnoreCase(autor.rol())) {
			throw new SecurityException("Només els professionals poden publicar ofertes de serveis.");
		}
		if ("INCIDENCIA".equals(tipus) && "professional".equalsIgnoreCase(autor.rol())) {
			throw new SecurityException("Només els clients poden publicar incidències.");
		}
		return autor;
	}

	private void exigirAutorOAdmin(String autorId, String actorId) {
		Usuari actor = usuariActor(actorId);
		if (!actor.id().equals(autorId) && !esAdmin(actor)) {
			throw new SecurityException("Només l'autor o un administrador poden fer aquesta acció.");
		}
	}

	private String inserirPublicacio(String tipus, Usuari autor, String titol, String descripcio, String categoria, String preu, String estat) {
		if (titol == null || titol.isBlank()) {
			throw new IllegalArgumentException("El títol és obligatori.");
		}
		String categoriaFinal = (categoria == null || categoria.isBlank()) ? "Altres Serveis" : categoria;
		return jdbcTemplate.queryForObject(
			"INSERT INTO publicacions (tipus, autor_id, titol, descripcio, categoria, preu, estat) VALUES (?, ?::uuid, ?, ?, ?, ?, ?) RETURNING id::text",
			String.class,
			tipus, autor.id(), titol, descripcio, categoriaFinal, preu, estat
		);
	}

	private <T> Optional<T> publicacioPerId(String id, String tipus, org.springframework.jdbc.core.RowMapper<T> mapper) {
		Optional<UUID> uuid = uuid(id);
		if (uuid.isEmpty()) {
			return Optional.empty();
		}
		return jdbcTemplate.query(SELECT_PUBLICACIO + "WHERE p.id = ? AND p.tipus = ?", mapper, uuid.get(), tipus).stream().findFirst();
	}

	/**
	 * Llistar totes les incidències creades per clients.
	 * @return Llista completa d'incidències
	 */
	public List<Incidencia> incidencies() {
		return jdbcTemplate.query(SELECT_PUBLICACIO + "WHERE p.tipus = 'INCIDENCIA' ORDER BY p.created_at DESC", incidenciaRowMapper);
	}

	/**
	 * Cerca una incidència pel seu identificador.
	 * @param id Identificador de la incidència
	 * @return Optional amb la incidència
	 */
	public Optional<Incidencia> findIncidenciaById(String id) {
		return publicacioPerId(id, "INCIDENCIA", incidenciaRowMapper);
	}

	/**
	 * Llistar incidències d'un client concret.
	 * @param clientNom Nom, correu o ID del client
	 * @return Llista d'incidències d'aquell client
	 */
	public List<Incidencia> findIncidenciesByClient(String clientNom) {
		return jdbcTemplate.query(
			SELECT_PUBLICACIO + "WHERE p.tipus = 'INCIDENCIA' AND (p.autor_id::text = ? OR LOWER(u.nom) = LOWER(?) OR LOWER(u.email) = LOWER(?)) ORDER BY p.created_at DESC",
			incidenciaRowMapper,
			clientNom, clientNom, clientNom
		);
	}

	/**
	 * Publica una nova incidència oberta.
	 * @param request Dades de la incidència
	 * @return La incidència desada
	 */
	public Incidencia addIncidencia(IncidenciaRequest request) {
		Usuari client = autorDe(request.resolveClient(), "INCIDENCIA");
		String id = inserirPublicacio("INCIDENCIA", client, request.titol(), request.descripcio(), request.categoria(), request.budget(), "OBERTA");
		return findIncidenciaById(id).orElseThrow();
	}

	/**
	 * Elimina una incidència sense pressupostos ni tasques.
	 * Si ja en té, cal conservar-la perquè forma part de l'historial.
	 * @param id Identificador de la incidència
	 * @param actorId Usuari que fa l'acció
	 * @return true si s'ha eliminat, false si no existeix
	 */
	@Transactional
	public boolean deleteIncidencia(String id, String actorId) {
		Optional<Incidencia> incidencia = findIncidenciaById(id);
		if (incidencia.isEmpty()) {
			return false;
		}
		exigirAutorOAdmin(incidencia.get().clientId(), actorId);
		UUID uuid = UUID.fromString(incidencia.get().id());
		Integer referencies = jdbcTemplate.queryForObject(
			"SELECT (SELECT COUNT(*) FROM pressupostos WHERE publicacio_id = ?) + (SELECT COUNT(*) FROM tasques WHERE publicacio_id = ?)",
			Integer.class, uuid, uuid
		);
		if (referencies != null && referencies > 0) {
			throw new IllegalArgumentException("No es pot eliminar una incidència amb pressupostos o tasques.");
		}
		return jdbcTemplate.update("DELETE FROM publicacions WHERE id = ?", uuid) > 0;
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
	// GESTIÓ D'OFERTES
	// ==========================================

	/**
	 * Obté les ofertes actives del mercat.
	 * @return Llista d'ofertes no retirades
	 */
	public List<Oferta> offers() {
		return jdbcTemplate.query(SELECT_PUBLICACIO + "WHERE p.tipus = 'OFERTA' AND p.estat = 'ACTIVA' ORDER BY p.created_at DESC", ofertaRowMapper);
	}

	/**
	 * Cerca una oferta pel seu ID únic, encara que estiga retirada.
	 * @param id Identificador de l'oferta
	 * @return Optional amb l'oferta
	 */
	public Optional<Oferta> findOfferById(String id) {
		return publicacioPerId(id, "OFERTA", ofertaRowMapper);
	}

	/**
	 * Les ofertes ja no pengen d'una tasca o incidència: les propostes sobre incidències són pressupostos.
	 * @param tascaId Identificador de la tasca o incidència
	 * @return Llista buida; es manté mentre el front consumisca aquesta ruta
	 */
	public List<Oferta> findOffersByTasca(String tascaId) {
		return List.of();
	}

	/**
	 * Publica una oferta de servei. Si la petició s'adreça a una incidència, és un pressupost.
	 * @param request Dades de l'oferta
	 * @return L'oferta desada
	 */
	public Oferta addOffer(OfertaRequest request) {
		Optional<Incidencia> incidencia = findIncidenciaById(request.resolveTargetId());
		if (incidencia.isPresent()) {
			Pressupost p = addPressupost(new CrearPressupostRequest(
				incidencia.get().id(), null, request.professional(), request.price(), request.note(), null));
			return new Oferta(p.id(), null, incidencia.get().id(), p.professionalNom(), incidencia.get().categoria(), p.preu(), p.descripcio(), "Pendent");
		}
		Usuari professional = autorDe(request.professional(), "OFERTA");
		String categoria = (request.categoria() == null || request.categoria().isBlank()) ? "Altres Serveis" : request.categoria();
		String titol = (request.note() != null && !request.note().isBlank())
			? (request.note().length() > 255 ? request.note().substring(0, 252) + "..." : request.note())
			: "Servei de " + categoria;
		String id = inserirPublicacio("OFERTA", professional, titol, request.note(), categoria, request.price(), "ACTIVA");
		return findOfferById(id).orElseThrow();
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
	 * Retira una oferta del mercat sense esborrar-la: les tasques ja acordades continuen,
	 * i les sol·licituds pendents es rebutgen avisant els seus clients.
	 * @param id Identificador de l'oferta
	 * @param actorId Usuari que fa l'acció
	 * @return true si l'oferta existeix i ha quedat retirada
	 */
	@Transactional
	public boolean deleteOffer(String id, String actorId) {
		Optional<Publicacio> oferta = publicacioPerId(id, "OFERTA", publicacioRowMapper);
		if (oferta.isEmpty()) {
			return false;
		}
		exigirAutorOAdmin(oferta.get().usuariId(), actorId);
		UUID uuid = UUID.fromString(oferta.get().id());
		jdbcTemplate.queryForObject("SELECT estat FROM publicacions WHERE id = ? FOR UPDATE", String.class, uuid);
		jdbcTemplate.update("UPDATE publicacions SET estat = 'RETIRADA' WHERE id = ?", uuid);
		List<String> clients = jdbcTemplate.queryForList(
			"UPDATE tasques SET estat = 'REBUTJADA' WHERE publicacio_id = ? AND estat = 'SOLLICITADA' RETURNING client_id::text",
			String.class, uuid
		);
		clients.forEach(clientId -> addAvis(clientId,
			"L'oferta '" + oferta.get().titol() + "' s'ha retirat i la teva sol·licitud ha quedat rebutjada.", "/tasques-ofertes"));
		return true;
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
				"SELECT id::text AS id, destinatari_id::text AS usuari_id, missatge, llegit, created_at::date AS data, enllac FROM avisos ORDER BY created_at DESC",
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
				"SELECT id::text AS id, destinatari_id::text AS usuari_id, missatge, llegit, created_at::date AS data, enllac FROM avisos WHERE destinatari_id::text = ? OR destinatari_id IN (SELECT id FROM usuaris WHERE LOWER(nom) = LOWER(?)) ORDER BY created_at DESC",
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
		Usuari destinatari = findUserById(usuariId)
			.orElseThrow(() -> new IllegalArgumentException("Destinatari de l'avís no trobat"));
		String id = UUID.randomUUID().toString();
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

		jdbcTemplate.update(
			"INSERT INTO avisos (id, destinatari_id, missatge, enllac) VALUES (?::uuid, ?::uuid, ?, ?)",
			id, destinatari.id(), missatge, targetEnllac
		);
		return new Avis(id, destinatari.id(), missatge, false, data, targetEnllac);
	}

	/**
	 * Envia un avís del sistema a tots els administradors.
	 * @param missatge Missatge de l'avís
	 */
	public void notificarAdmins(String missatge) {
		jdbcTemplate.queryForList("SELECT id::text FROM usuaris WHERE rol = 'ADMIN'", String.class)
			.forEach(adminId -> addAvis(adminId, missatge));
	}

	/**
	 * Marca un avís com a llegit.
	 * @param id Identificador de l'avís
	 */
	public void markAvisAsRead(String id) {
		try {
			jdbcTemplate.update("UPDATE avisos SET llegit = true WHERE id::text = ?", id);
		} catch (Exception ignored) {}
	}

	/**
	 * Elimina un avís de la base de dades.
	 * @param id Identificador de l'avís
	 */
	public void deleteAvis(String id) {
		try {
			jdbcTemplate.update("DELETE FROM avisos WHERE id::text = ?", id);
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

	public List<Publicacio> publicacions() {
		return jdbcTemplate.query(SELECT_PUBLICACIO + "ORDER BY p.created_at DESC", publicacioRowMapper);
	}

	public List<Publicacio> publicacionsByTipus(String tipus) {
		return jdbcTemplate.query(SELECT_PUBLICACIO + "WHERE p.tipus = UPPER(?) ORDER BY p.created_at DESC", publicacioRowMapper, tipus);
	}

	public Optional<Publicacio> findPublicacioById(String id) {
		Optional<UUID> uuid = uuid(id);
		if (uuid.isEmpty()) {
			return Optional.empty();
		}
		return jdbcTemplate.query(SELECT_PUBLICACIO + "WHERE p.id = ?", publicacioRowMapper, uuid.get()).stream().findFirst();
	}

	public Publicacio addPublicacio(Publicacio pub) {
		String tipus = pub.tipus() != null ? pub.tipus().trim().toUpperCase(Locale.ROOT) : "INCIDENCIA";
		if (!"OFERTA".equals(tipus) && !"INCIDENCIA".equals(tipus)) {
			throw new IllegalArgumentException("El tipus de publicació ha de ser OFERTA o INCIDENCIA.");
		}
		String identificador = pub.usuariId() != null ? pub.usuariId() : pub.usuariNom();
		Usuari autor = autorDe(identificador, tipus);
		String estat = "OFERTA".equals(tipus) ? "ACTIVA" : "OBERTA";
		String id = inserirPublicacio(tipus, autor, pub.titol(), pub.descripcio(), pub.categoria(), pub.preuOPressupost(), estat);
		return findPublicacioById(id).orElseThrow();
	}

	/**
	 * Una oferta es retira; una incidència només s'elimina si no té historial.
	 * @return true si la publicació existia
	 */
	@Transactional
	public boolean deletePublicacio(String id, String actorId) {
		return findPublicacioById(id)
			.map(pub -> "OFERTA".equals(pub.tipus()) ? deleteOffer(id, actorId) : deleteIncidencia(id, actorId))
			.orElse(false);
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
				"SELECT id::text AS id, telefon, motiu, bloquejat_per, created_at FROM blacklist_telefons ORDER BY created_at DESC",
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

		String id = UUID.randomUUID().toString();
		String admin = (requesterNom != null && !requesterNom.isBlank()) ? requesterNom : "admin";
		String motiuFinal = (motiu != null && !motiu.trim().isBlank()) ? motiu.trim() : "Bloquejat per administració";

		jdbcTemplate.update(
			"INSERT INTO blacklist_telefons (id, telefon, motiu, bloquejat_per) VALUES (?::uuid, ?, ?, ?)",
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
		int affected = jdbcTemplate.update("DELETE FROM blacklist_telefons WHERE id::text = ? OR telefon = ?", id, id);
		return affected > 0;
	}
}