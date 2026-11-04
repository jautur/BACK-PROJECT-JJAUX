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
import com.avisa.model.SollicitudCancellacio;
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
				"SELECT id::text AS id, nom, email, rol, telefon, bio, COALESCE(percentil, 3) AS percentil, COALESCE(verificat, FALSE) AS verificat, COALESCE(sollicitud_verificacio, FALSE) AS sollicitud_verificacio, foto, foto_perfil, logo FROM usuaris ORDER BY created_at",
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
				"SELECT id::text AS id, nom, email, rol, telefon, bio, COALESCE(percentil, 3) AS percentil, COALESCE(verificat, FALSE) AS verificat, COALESCE(sollicitud_verificacio, FALSE) AS sollicitud_verificacio, foto, foto_perfil, logo FROM usuaris WHERE id::text = ? OR LOWER(nom) = LOWER(?) OR LOWER(email) = LOWER(?)",
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
				"SELECT id::text AS id, nom, email, rol, telefon, bio, COALESCE(percentil, 3) AS percentil, COALESCE(verificat, FALSE) AS verificat, COALESCE(sollicitud_verificacio, FALSE) AS sollicitud_verificacio, foto, foto_perfil, logo FROM usuaris WHERE LOWER(email) = LOWER(?)",
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
				"SELECT id::text AS id, nom, email, rol, telefon, bio, COALESCE(percentil, 3) AS percentil, COALESCE(verificat, FALSE) AS verificat, COALESCE(sollicitud_verificacio, FALSE) AS sollicitud_verificacio, foto, foto_perfil, logo FROM usuaris WHERE LOWER(email) = LOWER(?) AND (password = ? OR ? IS NULL)",
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
		String rol = request.rol().toUpperCase(Locale.ROOT);
		try {
			jdbcTemplate.update(
				"INSERT INTO usuaris (id, nom, email, password, rol, telefon) VALUES (?::uuid, ?, ?, ?, ?, ?)",
				id, request.nom(), request.email(), request.password(), rol, request.telefon()
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

	/**
	 * El client (o un administrador) tanca una incidència oberta sense triar cap professional.
	 * Els pressupostos pendents es rebutgen avisant els professionals, i no tornen a pendent mai.
	 * @param id Identificador de la incidència
	 * @param actorId Usuari que fa l'acció
	 * @return Optional amb la incidència tancada, buit si no existeix
	 */
	@Transactional
	public Optional<Incidencia> tancarIncidencia(String id, String actorId) {
		Optional<Incidencia> trobada = findIncidenciaById(id);
		if (trobada.isEmpty()) {
			return Optional.empty();
		}
		Incidencia incidencia = trobada.get();
		exigirAutorOAdmin(incidencia.clientId(), actorId);
		UUID uuid = UUID.fromString(incidencia.id());
		int files = jdbcTemplate.update("UPDATE publicacions SET estat = 'TANCADA' WHERE id = ? AND estat = 'OBERTA'", uuid);
		if (files == 0) {
			throw new IllegalStateException("Només es pot tancar una incidència oberta.");
		}
		jdbcTemplate.queryForList(
			"UPDATE pressupostos SET estat = 'REBUTJAT' WHERE publicacio_id = ? AND estat = 'PENDENT' RETURNING professional_id::text",
			String.class, uuid
		).forEach(professionalId -> addAvis(professionalId,
			"El client ha tancat la incidència '" + incidencia.titol() + "' i el teu pressupost ha quedat rebutjat.", "/tasques-ofertes"));
		return findIncidenciaById(id);
	}

	// ==========================================
	// GESTIÓ DE TASQUES
	// ==========================================

	private static final String SELECT_TASCA =
		"SELECT t.id::text AS id, p.titol, p.descripcio, p.categoria, t.client_id::text AS client_id, c.nom AS client_nom, "
		+ "t.professional_id::text AS professional_id, pr.nom AS professional_nom, t.publicacio_id::text AS publicacio_id, "
		+ "t.pressupost_id::text AS pressupost_id, t.preu_acordat, t.estat, COALESCE(t.data_inici, t.created_at) AS data_inici, "
		+ "t.data_finalitzacio, t.valoracio, t.comentari, t.data_valoracio, t.created_at "
		+ "FROM tasques t JOIN publicacions p ON p.id = t.publicacio_id "
		+ "JOIN usuaris c ON c.id = t.client_id JOIN usuaris pr ON pr.id = t.professional_id ";

	/** Una sol·licitud no acceptada encara no és treball acordat: es consulta a /api/interessos. */
	private static final String TASCA_ACORDADA = "t.estat NOT IN ('SOLLICITADA', 'REBUTJADA', 'RETIRADA') ";

	private static final Map<String, String> ETIQUETES_TASCA = Map.of(
		"SOLLICITADA", "Sol·licitada",
		"REBUTJADA", "Rebutjada",
		"RETIRADA", "Retirada",
		"EN_CURS", "En curs",
		"EN_REVISIO", "En revisió",
		"FINALITZADA", "Finalitzada",
		"CANCELLADA", "Cancel·lada"
	);

	private static LocalDateTime dataOpcional(java.sql.ResultSet rs, String columna) throws java.sql.SQLException {
		java.sql.Timestamp data = rs.getTimestamp(columna);
		return data != null ? data.toLocalDateTime() : null;
	}

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
		null,
		rs.getString("pressupost_id"),
		rs.getString("preu_acordat"),
		dataOpcional(rs, "data_inici"),
		dataOpcional(rs, "data_finalitzacio"),
		rs.getObject("valoracio") != null ? rs.getInt("valoracio") : null,
		rs.getString("comentari"),
		dataOpcional(rs, "data_valoracio"),
		ETIQUETES_TASCA.get(rs.getString("estat"))
	);

	/**
	 * Obté tot el treball acordat, en curs o acabat.
	 * @return Llista de tasques
	 */
	public List<Tasca> tasks() {
		return jdbcTemplate.query(SELECT_TASCA + "WHERE " + TASCA_ACORDADA + "ORDER BY t.created_at DESC", tascaRowMapper);
	}

	/**
	 * Cerca una tasca pel seu identificador, siga quin siga el seu estat.
	 * @param id Identificador de la tasca
	 * @return Optional amb la tasca
	 */
	public Optional<Tasca> findTaskById(String id) {
		Optional<UUID> uuid = uuid(id);
		if (uuid.isEmpty()) {
			return Optional.empty();
		}
		return jdbcTemplate.query(SELECT_TASCA + "WHERE t.id = ?", tascaRowMapper, uuid.get()).stream().findFirst();
	}

	/**
	 * Llista el treball acordat d'un client concret.
	 * @param clientNom Nom, correu o ID del client
	 * @return Llista de tasques d'aquell client
	 */
	public List<Tasca> findTasksByClient(String clientNom) {
		return jdbcTemplate.query(
			SELECT_TASCA + "WHERE " + TASCA_ACORDADA + "AND (t.client_id::text = ? OR LOWER(c.nom) = LOWER(?) OR LOWER(c.email) = LOWER(?)) ORDER BY t.created_at DESC",
			tascaRowMapper,
			clientNom, clientNom, clientNom
		);
	}

	/**
	 * Llista el treball acordat d'un professional concret.
	 * @param professionalNom Nom, correu o ID del professional
	 * @return Llista de tasques assignades a aquell professional
	 */
	public List<Tasca> findTasksByProfessional(String professionalNom) {
		return jdbcTemplate.query(
			SELECT_TASCA + "WHERE " + TASCA_ACORDADA + "AND (t.professional_id::text = ? OR LOWER(pr.nom) = LOWER(?) OR LOWER(pr.email) = LOWER(?)) ORDER BY t.created_at DESC",
			tascaRowMapper,
			professionalNom, professionalNom, professionalNom
		);
	}

	private Tasca tascaExistent(String id) {
		return findTaskById(id).orElseThrow(() -> new IllegalArgumentException("La tasca no existeix."));
	}

	/**
	 * Les tasques naixen d'un pressupost o d'una sol·licitud. Aquesta ruta, sense professional,
	 * publica una incidència; amb professional, sol·licita l'oferta indicada a origenId.
	 * @param request Dades de la petició
	 * @return La incidència adaptada o la sol·licitud desada
	 */
	@Transactional
	public Tasca addTask(TascaRequest request) {
		String professional = request.resolveProfessional();
		if (professional == null || professional.isBlank()) {
			Incidencia inc = addIncidencia(new IncidenciaRequest(null, null, request.resolveClient(), request.titol(), request.descripcio(), request.categoria(), request.budget(), null));
			return new Tasca(inc.id(), inc.titol(), inc.descripcio(), inc.categoria(), inc.clientId(), inc.clientNom(), null, null, null, inc.budget(), inc.estat(), "incidencia", null, null);
		}
		return convertOfferToTask(request.origenId(), request.resolveClient())
			.orElseThrow(() -> new IllegalArgumentException("Una tasca només pot nàixer d'un pressupost o d'una oferta existent."));
	}

	/**
	 * Finalitza una tasca en curs. Ho pot fer el client, el professional o un administrador, sense confirmació
	 * de l'altra part. Si ve d'una incidència, la tanca. Només el client hi pot afegir la valoració.
	 * @param id Identificador de la tasca
	 * @param actorId Usuari que fa l'acció
	 * @param valoracio Puntuació opcional (1 a 5 estrelles)
	 * @param comentari Comentari opcional del client
	 * @return Optional amb la tasca actualitzada, buit si no existeix
	 */
	@Transactional
	public Optional<Tasca> completeTask(String id, String actorId, Integer valoracio, String comentari) {
		Optional<Tasca> trobada = findTaskById(id);
		if (trobada.isEmpty()) {
			return Optional.empty();
		}
		Tasca tasca = trobada.get();
		Usuari actor = usuariActor(actorId);
		boolean esClient = actor.id().equals(tasca.clientId());
		if (!esClient && !actor.id().equals(tasca.professionalId()) && !esAdmin(actor)) {
			throw new SecurityException("Només el client, el professional o un administrador poden finalitzar la tasca.");
		}
		if (valoracio != null) {
			validarValoracio(valoracio);
			if (!esClient) {
				throw new SecurityException("Només el client pot valorar la tasca.");
			}
		}
		UUID uuid = UUID.fromString(tasca.id());
		int files = jdbcTemplate.update("UPDATE tasques SET estat = 'FINALITZADA', data_finalitzacio = CURRENT_TIMESTAMP WHERE id = ? AND estat = 'EN_CURS'", uuid);
		if (files == 0) {
			throw new IllegalStateException("Només es pot finalitzar una tasca en curs.");
		}
		jdbcTemplate.update("UPDATE publicacions SET estat = 'TANCADA' WHERE id = ?::uuid AND tipus = 'INCIDENCIA'", tasca.publicacioId());
		if (valoracio != null) {
			guardarValoracio(uuid, valoracio, comentari);
		}
		String missatge = "La tasca '" + tasca.titol() + "' ha estat finalitzada.";
		if (!actor.id().equals(tasca.clientId())) {
			addAvis(tasca.clientId(), missatge);
		}
		if (!actor.id().equals(tasca.professionalId())) {
			addAvis(tasca.professionalId(), valoracio != null ? textValoracio(tasca, valoracio, comentari) : missatge);
		}
		return findTaskById(id);
	}

	/**
	 * Valora una tasca finalitzada. Només el client pot fer-ho, encara que l'haja finalitzat una altra persona.
	 * @param id Identificador de la tasca
	 * @param actorId Usuari que fa l'acció
	 * @param valoracio Puntuació (1 a 5 estrelles)
	 * @param comentari Comentari opcional
	 * @return Optional amb la tasca valorada, buit si no existeix
	 */
	@Transactional
	public Optional<Tasca> valorarTasca(String id, String actorId, Integer valoracio, String comentari) {
		Optional<Tasca> trobada = findTaskById(id);
		if (trobada.isEmpty()) {
			return Optional.empty();
		}
		Tasca tasca = trobada.get();
		if (!usuariActor(actorId).id().equals(tasca.clientId())) {
			throw new SecurityException("Només el client pot valorar la tasca.");
		}
		validarValoracio(valoracio);
		if (!guardarValoracio(UUID.fromString(tasca.id()), valoracio, comentari)) {
			throw new IllegalStateException("Només es pot valorar una vegada una tasca finalitzada.");
		}
		addAvis(tasca.professionalId(), textValoracio(tasca, valoracio, comentari));
		return findTaskById(id);
	}

	private static void validarValoracio(Integer valoracio) {
		if (valoracio == null || valoracio < 1 || valoracio > 5) {
			throw new IllegalArgumentException("La valoració ha de ser entre 1 i 5 estrelles.");
		}
	}

	private boolean guardarValoracio(UUID tasca, int valoracio, String comentari) {
		return jdbcTemplate.update(
			"UPDATE tasques SET valoracio = ?, comentari = ?, data_valoracio = CURRENT_TIMESTAMP WHERE id = ? AND estat = 'FINALITZADA' AND valoracio IS NULL",
			valoracio, comentari, tasca
		) > 0;
	}

	private static String textValoracio(Tasca tasca, int valoracio, String comentari) {
		String text = (comentari != null && !comentari.isBlank()) ? ": \"" + comentari + "\"" : "";
		return tasca.clientNom() + " t'ha puntuat amb " + valoracio + " estrelles per a la tasca '" + tasca.titol() + "'" + text;
	}

	/**
	 * Les tasques són historial i no s'esborren. Es mantenen dos casos que el front envia a aquesta ruta:
	 * l'identificador d'una incidència, i la retirada d'una sol·licitud pròpia.
	 * @param id Identificador de la tasca o incidència
	 * @param actorId Usuari que fa l'acció
	 * @return true si s'ha fet l'acció, false si no existeix
	 */
	@Transactional
	public boolean deleteTask(String id, String actorId) {
		if (findIncidenciaById(id).isPresent()) {
			return deleteIncidencia(id, actorId);
		}
		Optional<Tasca> tasca = findTaskById(id);
		if (tasca.isEmpty()) {
			return false;
		}
		if ("Sol·licitada".equals(tasca.get().estat())) {
			return retirarInteres(id, actorId);
		}
		throw new IllegalArgumentException("Una tasca acordada no es pot esborrar: forma part de l'historial.");
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
	@Transactional
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
	 * Ruta antiga d'acceptar del front: si l'identificador és un pressupost, l'accepta;
	 * si és una oferta, la sol·licita.
	 * @param id Identificador del pressupost o de l'oferta
	 * @param actorId Usuari que fa l'acció
	 * @return Optional amb l'oferta, buit si no existeix
	 */
	@Transactional
	public Optional<Oferta> acceptOffer(String id, String actorId) {
		Optional<Pressupost> pressupost = findPressupostById(id);
		if (pressupost.isPresent()) {
			Pressupost p = pressupost.get();
			return acceptarPressupost(id, actorId).map(tasca ->
				new Oferta(p.id(), tasca.id(), p.incidenciaId(), p.professionalNom(), tasca.categoria(), p.preu(), p.descripcio(), "Acceptada"));
		}
		Optional<Oferta> oferta = findOfferById(id);
		if (oferta.isPresent()) {
			convertOfferToTask(id, actorId);
		}
		return oferta;
	}

	/**
	 * "M'interessa": crea una sol·licitud pendent que el professional ha d'acceptar.
	 * L'oferta continua activa perquè més clients la puguen sol·licitar.
	 * @param offerId Identificador de l'oferta
	 * @param clientIdentificador Identificador del client que sol·licita
	 * @return Optional amb la sol·licitud creada, buit si l'oferta no existeix
	 */
	@Transactional
	public Optional<Tasca> convertOfferToTask(String offerId, String clientIdentificador) {
		Optional<Publicacio> trobada = publicacioPerId(offerId, "OFERTA", publicacioRowMapper);
		if (trobada.isEmpty()) {
			return Optional.empty();
		}
		Publicacio oferta = trobada.get();
		Usuari client = findUserById(clientIdentificador).orElseThrow(() -> new SecurityException("Usuari no reconegut."));
		if (!"client".equalsIgnoreCase(client.rol())) {
			throw new SecurityException("Només els clients poden sol·licitar una oferta.");
		}
		UUID uuid = UUID.fromString(oferta.id());
		String estat = jdbcTemplate.queryForObject("SELECT estat FROM publicacions WHERE id = ? FOR SHARE", String.class, uuid);
		if (!"ACTIVA".equals(estat)) {
			throw new IllegalStateException("Aquesta oferta ja no està disponible.");
		}
		String preu = (oferta.preuOPressupost() != null && !oferta.preuOPressupost().isBlank()) ? oferta.preuOPressupost() : "A convenir";
		String id;
		try {
			id = jdbcTemplate.queryForObject(
				"INSERT INTO tasques (publicacio_id, client_id, professional_id, preu_acordat, estat) VALUES (?, ?::uuid, ?::uuid, ?, 'SOLLICITADA') RETURNING id::text",
				String.class,
				uuid, client.id(), oferta.autorId(), preu
			);
		} catch (DuplicateKeyException e) {
			throw new IllegalStateException("Ja tens una sol·licitud pendent per a aquesta oferta.");
		}
		addAvis(oferta.autorId(), "El client " + client.nom() + " ha mostrat interés en la teva oferta '" + oferta.titol() + "'. Pots acceptar o rebutjar la sol·licitud.");
		return findTaskById(id);
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
		exigirAutorOAdmin(oferta.get().autorId(), actorId);
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
		jdbcTemplate.update("UPDATE avisos SET llegit = true WHERE id::text = ?", id);
	}

	/**
	 * Elimina un avís de la base de dades.
	 * @param id Identificador de l'avís
	 */
	public void deleteAvis(String id) {
		jdbcTemplate.update("DELETE FROM avisos WHERE id::text = ?", id);
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
	// Una ressenya és la valoració que el client deixa a la tasca finalitzada.
	// ==========================================

	private final org.springframework.jdbc.core.RowMapper<Resenya> resenyaRowMapper = (rs, rowNum) -> new Resenya(
		"R-" + rs.getString("id"),
		rs.getString("client_id"),
		rs.getString("client_nom"),
		rs.getString("professional_id"),
		rs.getString("professional_nom"),
		rs.getString("id"),
		rs.getInt("valoracio"),
		rs.getString("comentari"),
		rs.getTimestamp("created_at").toLocalDateTime()
	);

	/**
	 * Obté totes les valoracions enregistrades a la taula tasques.
	 * @return Llista completa de valoracions
	 */
	public List<Resenya> resenyes() {
		return jdbcTemplate.query(SELECT_TASCA + "WHERE t.valoracio IS NOT NULL ORDER BY t.data_valoracio DESC", resenyaRowMapper);
	}

	/**
	 * Obté les valoracions rebudes per un professional.
	 * @param treballadorIdOIdNom ID, nom o correu del professional
	 * @return Llista de valoracions dirigides a aquest professional
	 */
	public List<Resenya> findResenyesByTreballador(String treballadorIdOIdNom) {
		return jdbcTemplate.query(
			SELECT_TASCA + "WHERE t.valoracio IS NOT NULL AND (t.professional_id::text = ? OR LOWER(pr.nom) = LOWER(?) OR LOWER(pr.email) = LOWER(?)) ORDER BY t.data_valoracio DESC",
			resenyaRowMapper,
			treballadorIdOIdNom, treballadorIdOIdNom, treballadorIdOIdNom
		);
	}

	/**
	 * Obté les valoracions emeses per un client.
	 * @param clientId ID, nom o correu del client
	 * @return Llista de valoracions emeses pel client
	 */
	public List<Resenya> findResenyesByClient(String clientId) {
		return jdbcTemplate.query(
			SELECT_TASCA + "WHERE t.valoracio IS NOT NULL AND (t.client_id::text = ? OR LOWER(c.nom) = LOWER(?) OR LOWER(c.email) = LOWER(?)) ORDER BY t.data_valoracio DESC",
			resenyaRowMapper,
			clientId, clientId, clientId
		);
	}

	/**
	 * Desa una valoració sobre la seua tasca, amb les mateixes regles que valorar la tasca directament.
	 * @param resenya Dades de la valoració
	 * @param actorId Usuari que fa l'acció
	 * @return Valoració desada
	 */
	@Transactional
	public Resenya addResenya(Resenya resenya, String actorId) {
		Tasca tasca = valorarTasca(resenya.tascaId(), actorId, resenya.puntuacio(), resenya.descripcio())
			.orElseThrow(() -> new IllegalArgumentException("La tasca no existeix."));
		return new Resenya("R-" + tasca.id(), tasca.clientId(), tasca.clientNom(), tasca.professionalId(), tasca.professionalNom(),
			tasca.id(), tasca.valoracio(), tasca.comentari(), tasca.dataValoracio());
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
		String identificador = pub.autorId() != null ? pub.autorId() : pub.autorNom();
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
	// SOL·LICITUDS D'OFERTES ("M'INTERESSA")
	// /api/interessos és una vista de les tasques nascudes d'una oferta. L'interés i la tasca són la mateixa fila.
	// ==========================================

	private static final String SOLLICITUD_VIGENT = "t.pressupost_id IS NULL AND t.estat NOT IN ('REBUTJADA', 'RETIRADA') ";

	private final org.springframework.jdbc.core.RowMapper<Interes> interesRowMapper = (rs, rowNum) -> new Interes(
		rs.getString("id"),
		rs.getString("publicacio_id"),
		rs.getString("titol"),
		rs.getString("client_id"),
		rs.getString("client_nom"),
		rs.getString("professional_id"),
		rs.getString("professional_nom"),
		null,
		"SOLLICITADA".equals(rs.getString("estat")) ? null : rs.getString("id"),
		rs.getTimestamp("created_at").toLocalDateTime()
	);

	public List<Interes> findInteressosByProfessional(String proNomOrId) {
		return jdbcTemplate.query(
			SELECT_TASCA + "WHERE " + SOLLICITUD_VIGENT + "AND (t.professional_id::text = ? OR LOWER(pr.nom) = LOWER(?) OR LOWER(pr.email) = LOWER(?)) ORDER BY t.created_at DESC",
			interesRowMapper,
			proNomOrId, proNomOrId, proNomOrId
		);
	}

	public List<Interes> findInteressosByClient(String clientNomOrId) {
		return jdbcTemplate.query(
			SELECT_TASCA + "WHERE " + SOLLICITUD_VIGENT + "AND (t.client_id::text = ? OR LOWER(c.nom) = LOWER(?) OR LOWER(c.email) = LOWER(?)) ORDER BY t.created_at DESC",
			interesRowMapper,
			clientNomOrId, clientNomOrId, clientNomOrId
		);
	}

	public Optional<Interes> findInteresById(String id) {
		Optional<UUID> uuid = uuid(id);
		if (uuid.isEmpty()) {
			return Optional.empty();
		}
		return jdbcTemplate.query(SELECT_TASCA + "WHERE t.id = ? AND t.pressupost_id IS NULL", interesRowMapper, uuid.get()).stream().findFirst();
	}

	@Transactional
	public Interes addInteres(CrearInteresRequest req) {
		String client = req.clientId() != null ? req.clientId() : req.clientNom();
		Tasca sollicitud = convertOfferToTask(req.publicacioId(), client)
			.orElseThrow(() -> new IllegalArgumentException("L'oferta no existeix."));
		return findInteresById(sollicitud.id()).orElseThrow();
	}

	/**
	 * Canvia l'estat d'una sol·licitud pendent. La condició SQL evita respondre dues vegades a la mateixa.
	 */
	private void resoldreSollicitud(Tasca tasca, String nouEstat, String dataInici) {
		int files = jdbcTemplate.update(
			"UPDATE tasques SET estat = ?, data_inici = " + dataInici + " WHERE id = ? AND estat = 'SOLLICITADA'",
			nouEstat, UUID.fromString(tasca.id())
		);
		if (files == 0) {
			throw new IllegalStateException("Aquesta sol·licitud ja no està pendent.");
		}
	}

	private void exigirProfessionalDeLaTasca(Tasca tasca, String actorId) {
		if (!usuariActor(actorId).id().equals(tasca.professionalId())) {
			throw new SecurityException("Només el professional de l'oferta pot respondre la sol·licitud.");
		}
	}

	/**
	 * El professional accepta la sol·licitud i comença la tasca.
	 * @return Optional amb la tasca en curs, buit si no existeix
	 */
	@Transactional
	public Optional<Tasca> acceptarInteres(String interesId, String actorId) {
		Optional<Tasca> tasca = findTaskById(interesId);
		if (tasca.isEmpty()) {
			return Optional.empty();
		}
		exigirProfessionalDeLaTasca(tasca.get(), actorId);
		resoldreSollicitud(tasca.get(), "EN_CURS", "CURRENT_TIMESTAMP");
		addAvis(tasca.get().clientId(), "El professional " + tasca.get().professionalNom() + " ha acceptat la teva sol·licitud! S'ha iniciat la tasca en curs '" + tasca.get().titol() + "'.");
		return findTaskById(interesId);
	}

	/**
	 * El professional rebutja la sol·licitud, que queda a l'historial.
	 * @return true si s'ha rebutjat, false si no existeix
	 */
	@Transactional
	public boolean rebutjarInteres(String interesId, String actorId) {
		Optional<Tasca> tasca = findTaskById(interesId);
		if (tasca.isEmpty()) {
			return false;
		}
		exigirProfessionalDeLaTasca(tasca.get(), actorId);
		resoldreSollicitud(tasca.get(), "REBUTJADA", "NULL");
		addAvis(tasca.get().clientId(), "El professional " + tasca.get().professionalNom() + " ha rebutjat la teva sol·licitud per a '" + tasca.get().titol() + "'.");
		return true;
	}

	/**
	 * El client retira la seua sol·licitud sense passar per l'administrador.
	 * @return true si s'ha retirat, false si no existeix
	 */
	@Transactional
	public boolean retirarInteres(String interesId, String actorId) {
		Optional<Tasca> tasca = findTaskById(interesId);
		if (tasca.isEmpty()) {
			return false;
		}
		if (!usuariActor(actorId).id().equals(tasca.get().clientId())) {
			throw new SecurityException("Només el client pot retirar la seua sol·licitud.");
		}
		resoldreSollicitud(tasca.get(), "RETIRADA", "NULL");
		addAvis(tasca.get().professionalId(), "El client " + tasca.get().clientNom() + " ha retirat la sol·licitud per a '" + tasca.get().titol() + "'.");
		return true;
	}

	// ==========================================
	// GESTIÓ DE PRESSUPOSTOS PER A INCIDÈNCIES
	// ==========================================

	private static final String SELECT_PRESSUPOST =
		"SELECT pr.id::text AS id, pr.publicacio_id::text AS incidencia_id, p.titol AS incidencia_titol, pr.professional_id::text AS professional_id, "
		+ "u.nom AS professional_nom, pr.preu, pr.descripcio, pr.duracio_estimada, pr.estat, pr.created_at "
		+ "FROM pressupostos pr JOIN publicacions p ON p.id = pr.publicacio_id JOIN usuaris u ON u.id = pr.professional_id ";

	private static final Map<String, String> ETIQUETES_PRESSUPOST = Map.of(
		"PENDENT", "Pendent",
		"ACCEPTAT", "Acceptat",
		"REBUTJAT", "Rebutjat"
	);

	private final org.springframework.jdbc.core.RowMapper<Pressupost> pressupostRowMapper = (rs, rowNum) -> new Pressupost(
		rs.getString("id"),
		rs.getString("incidencia_id"),
		rs.getString("incidencia_titol"),
		rs.getString("professional_id"),
		rs.getString("professional_nom"),
		rs.getString("preu"),
		rs.getString("descripcio"),
		rs.getString("duracio_estimada"),
		rs.getTimestamp("created_at").toLocalDateTime(),
		ETIQUETES_PRESSUPOST.get(rs.getString("estat"))
	);

	public List<Pressupost> pressupostos() {
		return jdbcTemplate.query(SELECT_PRESSUPOST + "ORDER BY pr.created_at DESC", pressupostRowMapper);
	}

	/**
	 * Els pressupostos vigents d'una incidència. Els rebutjats es consulten per professional.
	 */
	public List<Pressupost> findPressupostosByIncidencia(String incidenciaId) {
		Optional<UUID> uuid = uuid(incidenciaId);
		if (uuid.isEmpty()) {
			return List.of();
		}
		return jdbcTemplate.query(
			SELECT_PRESSUPOST + "WHERE pr.publicacio_id = ? AND pr.estat <> 'REBUTJAT' ORDER BY pr.created_at DESC",
			pressupostRowMapper,
			uuid.get()
		);
	}

	public Optional<Pressupost> findPressupostById(String id) {
		Optional<UUID> uuid = uuid(id);
		if (uuid.isEmpty()) {
			return Optional.empty();
		}
		return jdbcTemplate.query(SELECT_PRESSUPOST + "WHERE pr.id = ?", pressupostRowMapper, uuid.get()).stream().findFirst();
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
	 * Comprova si l'usuari és el client titular d'una incidència.
	 */
	public boolean isClientOwnerOfIncidencia(String incidenciaId, String requesterId, String requesterNom) {
		return findIncidenciaById(incidenciaId)
			.map(inc -> (requesterId != null && requesterId.equalsIgnoreCase(inc.clientId()))
				|| (requesterNom != null && requesterNom.equalsIgnoreCase(inc.clientNom())))
			.orElse(false);
	}

	/**
	 * Llistar tots els pressupostos enviats per un professional, també els rebutjats.
	 */
	public List<Pressupost> findPressupostosByProfessional(String proIdentifier) {
		if (proIdentifier == null || proIdentifier.isBlank()) return List.of();
		return jdbcTemplate.query(
			SELECT_PRESSUPOST + "WHERE pr.professional_id::text = LOWER(?) OR LOWER(u.nom) = LOWER(?) OR LOWER(u.email) = LOWER(?) ORDER BY pr.created_at DESC",
			pressupostRowMapper,
			proIdentifier, proIdentifier, proIdentifier
		);
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

	/**
	 * Bloqueja la incidència mentre dura la transacció.
	 * Diversos professionals poden pressupostar alhora, però cap mentre el client n'accepta un.
	 */
	private Optional<String> estatIncidenciaBloquejada(UUID incidencia, String bloqueig) {
		return jdbcTemplate.queryForList(
			"SELECT estat FROM publicacions WHERE id = ? AND tipus = 'INCIDENCIA' " + bloqueig, String.class, incidencia
		).stream().findFirst();
	}

	/**
	 * Desa el pressupost d'un professional per a una incidència oberta i n'avisa el client.
	 * @param req Dades del pressupost
	 * @return El pressupost desat
	 */
	@Transactional
	public Pressupost addPressupost(CrearPressupostRequest req) {
		String identificador = req.professionalId() != null ? req.professionalId() : req.professionalNom();
		Usuari professional = findUserById(identificador)
			.orElseThrow(() -> new IllegalArgumentException("El professional no existeix."));
		if (!"professional".equalsIgnoreCase(professional.rol())) {
			throw new SecurityException("Només els professionals poden enviar pressupostos a les incidències.");
		}
		Incidencia incidencia = findIncidenciaById(req.incidenciaId())
			.orElseThrow(() -> new IllegalArgumentException("La incidència no existeix."));
		UUID incidenciaId = UUID.fromString(incidencia.id());
		if (!estatIncidenciaBloquejada(incidenciaId, "FOR SHARE").filter("OBERTA"::equals).isPresent()) {
			throw new IllegalStateException("La incidència ja no admet pressupostos.");
		}

		String id;
		try {
			id = jdbcTemplate.queryForObject(
				"INSERT INTO pressupostos (publicacio_id, professional_id, preu, descripcio, duracio_estimada) VALUES (?, ?::uuid, ?, ?, ?) RETURNING id::text",
				String.class,
				incidenciaId, professional.id(), req.preu(), req.descripcio(), req.duracioEstimada()
			);
		} catch (DuplicateKeyException e) {
			throw new IllegalStateException("Ja has enviat un pressupost per a aquesta incidència.");
		}
		addAvis(incidencia.clientId(), "Nova proposta de pressupost de " + professional.nom() + " (" + req.preu() + ") per a la teva incidència '" + incidencia.titol() + "'.");
		return findPressupostById(id).orElseThrow();
	}

	/**
	 * El client accepta un pressupost: la resta de pendents es rebutgen avisant els seus professionals,
	 * la incidència queda assignada i naix la tasca en curs. O tot o res.
	 * @param pressupostId Identificador del pressupost
	 * @param actorId Usuari que fa l'acció, que ha de ser el client de la incidència
	 * @return La tasca en curs, o buit si el pressupost no existeix
	 */
	@Transactional
	public Optional<Tasca> acceptarPressupost(String pressupostId, String actorId) {
		Optional<Pressupost> trobat = findPressupostById(pressupostId);
		if (trobat.isEmpty()) {
			return Optional.empty();
		}
		UUID incidenciaId = UUID.fromString(trobat.get().incidenciaId());
		String estatIncidencia = estatIncidenciaBloquejada(incidenciaId, "FOR UPDATE").orElseThrow();
		Pressupost p = findPressupostById(pressupostId).orElseThrow();
		Incidencia incidencia = findIncidenciaById(p.incidenciaId()).orElseThrow();
		if (!usuariActor(actorId).id().equals(incidencia.clientId())) {
			throw new SecurityException("Només el client de la incidència pot acceptar un pressupost.");
		}
		if (!"OBERTA".equals(estatIncidencia) || !"Pendent".equals(p.estat())) {
			throw new IllegalStateException("Aquest pressupost ja no es pot acceptar.");
		}

		UUID id = UUID.fromString(p.id());
		List<String> rebutjats = jdbcTemplate.queryForList(
			"UPDATE pressupostos SET estat = 'REBUTJAT', rebutjat_automaticament = TRUE WHERE publicacio_id = ? AND estat = 'PENDENT' AND id <> ? RETURNING professional_id::text",
			String.class, incidenciaId, id
		);
		jdbcTemplate.update("UPDATE pressupostos SET estat = 'ACCEPTAT' WHERE id = ?", id);
		jdbcTemplate.update("UPDATE publicacions SET estat = 'ASSIGNADA' WHERE id = ?", incidenciaId);
		String tascaId = jdbcTemplate.queryForObject(
			"INSERT INTO tasques (publicacio_id, client_id, professional_id, pressupost_id, preu_acordat, estat, data_inici) VALUES (?, ?::uuid, ?::uuid, ?, ?, 'EN_CURS', CURRENT_TIMESTAMP) RETURNING id::text",
			String.class,
			incidenciaId, incidencia.clientId(), p.professionalId(), id, p.preu()
		);

		addAvis(p.professionalId(), "Enhorabona! " + incidencia.clientNom() + " ha acceptat el teu pressupost per a '" + incidencia.titol() + "' (" + p.preu() + ")! S'ha generat la tasca en curs.");
		rebutjats.forEach(professionalId -> addAvis(professionalId, "El client ha triat un altre pressupost per a la incidència '" + incidencia.titol() + "'."));
		return findTaskById(tascaId);
	}

	/**
	 * Rebuig manual d'un pressupost pendent. No es recupera encara que es cancel·le la tasca d'un altre.
	 * @param pressupostId Identificador del pressupost
	 * @param actorId Usuari que fa l'acció: el client de la incidència o un administrador
	 * @return true si s'ha rebutjat, false si no existeix
	 */
	@Transactional
	public boolean rebutjarPressupost(String pressupostId, String actorId) {
		Optional<Pressupost> trobat = findPressupostById(pressupostId);
		if (trobat.isEmpty()) {
			return false;
		}
		Pressupost p = trobat.get();
		Incidencia incidencia = findIncidenciaById(p.incidenciaId()).orElseThrow();
		exigirAutorOAdmin(incidencia.clientId(), actorId);
		int files = jdbcTemplate.update("UPDATE pressupostos SET estat = 'REBUTJAT' WHERE id = ? AND estat = 'PENDENT'", UUID.fromString(p.id()));
		if (files == 0) {
			throw new IllegalStateException("Només es pot rebutjar un pressupost pendent.");
		}
		addAvis(p.professionalId(), "El teu pressupost per a la incidència '" + incidencia.titol() + "' ha estat rebutjat.");
		return true;
	}

	// ==========================================
	// CANCEL·LACIÓ DE TASQUES
	// ==========================================

	private static final String SELECT_CANCELLACIO =
		"SELECT s.id::text AS id, s.tasca_id::text AS tasca_id, p.titol AS tasca_titol, s.sollicitant_id::text AS sollicitant_id, so.nom AS sollicitant_nom, s.motiu, s.resolucio, "
		+ "s.responsable_id::text AS responsable_id, re.nom AS responsable_nom, s.admin_id::text AS admin_id, s.data_resolucio, s.created_at "
		+ "FROM sollicituds_cancellacio s JOIN tasques t ON t.id = s.tasca_id JOIN publicacions p ON p.id = t.publicacio_id "
		+ "JOIN usuaris so ON so.id = s.sollicitant_id LEFT JOIN usuaris re ON re.id = s.responsable_id ";

	private final org.springframework.jdbc.core.RowMapper<SollicitudCancellacio> cancellacioRowMapper = (rs, rowNum) -> new SollicitudCancellacio(
		rs.getString("id"),
		rs.getString("tasca_id"),
		rs.getString("tasca_titol"),
		rs.getString("sollicitant_id"),
		rs.getString("sollicitant_nom"),
		rs.getString("motiu"),
		rs.getString("resolucio"),
		rs.getString("responsable_id"),
		rs.getString("responsable_nom"),
		rs.getString("admin_id"),
		dataOpcional(rs, "data_resolucio"),
		rs.getTimestamp("created_at").toLocalDateTime()
	);

	private void exigirAdmin(String actorId) {
		if (!esAdmin(usuariActor(actorId))) {
			throw new SecurityException("Només un administrador pot gestionar les cancel·lacions.");
		}
	}

	/**
	 * El client o el professional sol·liciten cancel·lar una tasca en curs. La tasca queda en revisió,
	 * sense poder-se finalitzar, i s'avisa l'altra part i tots els administradors.
	 * @param tascaId Identificador de la tasca
	 * @param actorId Usuari que fa l'acció
	 * @param motiu Motiu de la sol·licitud
	 * @return Optional amb la tasca en revisió, buit si no existeix
	 */
	@Transactional
	public Optional<Tasca> sollicitarCancellacio(String tascaId, String actorId, String motiu) {
		Optional<Tasca> trobada = findTaskById(tascaId);
		if (trobada.isEmpty()) {
			return Optional.empty();
		}
		Tasca tasca = trobada.get();
		Usuari actor = usuariActor(actorId);
		boolean esClient = actor.id().equals(tasca.clientId());
		if (!esClient && !actor.id().equals(tasca.professionalId())) {
			throw new SecurityException("Només el client o el professional de la tasca poden sol·licitar la cancel·lació.");
		}
		if (motiu == null || motiu.isBlank()) {
			throw new IllegalArgumentException("El motiu de la cancel·lació és obligatori.");
		}
		UUID uuid = UUID.fromString(tasca.id());
		int files = jdbcTemplate.update("UPDATE tasques SET estat = 'EN_REVISIO' WHERE id = ? AND estat = 'EN_CURS'", uuid);
		if (files == 0) {
			throw new IllegalStateException("Només es pot sol·licitar la cancel·lació d'una tasca en curs.");
		}
		jdbcTemplate.update("INSERT INTO sollicituds_cancellacio (tasca_id, sollicitant_id, motiu) VALUES (?, ?::uuid, ?)", uuid, actor.id(), motiu.trim());
		addAvis(esClient ? tasca.professionalId() : tasca.clientId(),
			actor.nom() + " ha sol·licitat cancel·lar la tasca '" + tasca.titol() + "'. Motiu: " + motiu.trim(), "/tasques-ofertes");
		notificarAdmins("Nova sol·licitud de cancel·lació de la tasca '" + tasca.titol() + "' per part de " + actor.nom() + ". Motiu: " + motiu.trim());
		return findTaskById(tascaId);
	}

	/**
	 * L'administrador resol una sol·licitud: sense conseqüències o per negligència cancel·la la tasca;
	 * rebutjada torna la tasca a en curs. Les parts no reben el detall d'una negligència, que només veu l'administrador.
	 * @param id Identificador de la sol·licitud
	 * @param actorId Usuari que fa l'acció, que ha de ser administrador
	 * @param resolucio SENSE_CONSEQUENCIES, NEGLIGENCIA o REBUTJADA
	 * @param responsableId Client o professional de la tasca, només en cas de negligència
	 * @return Optional amb la sol·licitud resolta, buit si no existeix
	 */
	@Transactional
	public Optional<SollicitudCancellacio> resoldreCancellacio(String id, String actorId, String resolucio, String responsableId) {
		exigirAdmin(actorId);
		Optional<UUID> uuid = uuid(id);
		if (uuid.isEmpty()) {
			return Optional.empty();
		}
		List<SollicitudCancellacio> bloquejada = jdbcTemplate.query(
			"SELECT id::text AS id, tasca_id::text AS tasca_id, NULL AS tasca_titol, sollicitant_id::text AS sollicitant_id, NULL AS sollicitant_nom, motiu, resolucio, "
			+ "responsable_id::text AS responsable_id, NULL AS responsable_nom, admin_id::text AS admin_id, data_resolucio, created_at FROM sollicituds_cancellacio WHERE id = ? FOR UPDATE",
			cancellacioRowMapper, uuid.get());
		if (bloquejada.isEmpty()) {
			return Optional.empty();
		}
		if (bloquejada.get(0).resolucio() != null) {
			throw new IllegalStateException("Aquesta sol·licitud ja s'ha resolt.");
		}
		Tasca tasca = tascaExistent(bloquejada.get(0).tascaId());
		String resolucioFinal = resolucio != null ? resolucio.trim().toUpperCase(Locale.ROOT) : "";
		if (!List.of("SENSE_CONSEQUENCIES", "NEGLIGENCIA", "REBUTJADA").contains(resolucioFinal)) {
			throw new IllegalArgumentException("La resolució ha de ser SENSE_CONSEQUENCIES, NEGLIGENCIA o REBUTJADA.");
		}
		boolean negligencia = "NEGLIGENCIA".equals(resolucioFinal);
		if (negligencia && !tasca.clientId().equalsIgnoreCase(responsableId) && !tasca.professionalId().equalsIgnoreCase(responsableId)) {
			throw new IllegalArgumentException("El responsable d'una negligència ha de ser el client o el professional de la tasca.");
		}
		if (!negligencia && responsableId != null && !responsableId.isBlank()) {
			throw new IllegalArgumentException("Només una negligència té responsable.");
		}

		String admin = usuariActor(actorId).id();
		jdbcTemplate.update(
			"UPDATE sollicituds_cancellacio SET resolucio = ?, responsable_id = ?::uuid, admin_id = ?::uuid, data_resolucio = CURRENT_TIMESTAMP WHERE id = ?",
			resolucioFinal, negligencia ? responsableId : null, admin, uuid.get());

		String missatge;
		if ("REBUTJADA".equals(resolucioFinal)) {
			jdbcTemplate.update("UPDATE tasques SET estat = 'EN_CURS' WHERE id = ?::uuid", tasca.id());
			missatge = "L'administrador ha rebutjat la cancel·lació de la tasca '" + tasca.titol() + "', que continua en curs.";
		} else {
			jdbcTemplate.update("UPDATE tasques SET estat = 'CANCELLADA' WHERE id = ?::uuid", tasca.id());
			if (tasca.pressupostId() != null) {
				reobrirIncidencia(tasca);
			}
			missatge = "L'administrador ha cancel·lat la tasca '" + tasca.titol() + "'.";
		}
		addAvis(tasca.clientId(), missatge, "/tasques-ofertes");
		addAvis(tasca.professionalId(), missatge, "/tasques-ofertes");
		return jdbcTemplate.query(SELECT_CANCELLACIO + "WHERE s.id = ?", cancellacioRowMapper, uuid.get()).stream().findFirst();
	}

	/**
	 * La incidència torna a estar oberta: el pressupost de la tasca cancel·lada queda rebutjat i
	 * tornen a pendent els que es van rebutjar automàticament en acceptar-lo, no els rebutjats a mà.
	 */
	private void reobrirIncidencia(Tasca tasca) {
		UUID incidencia = UUID.fromString(tasca.publicacioId());
		jdbcTemplate.update("UPDATE publicacions SET estat = 'OBERTA' WHERE id = ?", incidencia);
		jdbcTemplate.update("UPDATE pressupostos SET estat = 'REBUTJAT' WHERE id = ?::uuid", tasca.pressupostId());
		List<String> professionals = jdbcTemplate.queryForList(
			"UPDATE pressupostos SET estat = 'PENDENT', rebutjat_automaticament = FALSE WHERE publicacio_id = ? AND rebutjat_automaticament RETURNING professional_id::text",
			String.class, incidencia);
		professionals.forEach(professionalId -> addAvis(professionalId,
			"La incidència '" + tasca.titol() + "' torna a estar oberta i el teu pressupost torna a estar pendent.", "/tasques-ofertes"));
	}

	/**
	 * Sol·licituds de cancel·lació, per a l'administrador.
	 * @param actorId Usuari que fa l'acció
	 * @param nomesPendents Si només cal retornar les que falten per resoldre
	 */
	public List<SollicitudCancellacio> cancellacions(String actorId, boolean nomesPendents) {
		exigirAdmin(actorId);
		String filtre = nomesPendents ? "WHERE s.resolucio IS NULL " : "";
		return jdbcTemplate.query(SELECT_CANCELLACIO + filtre + "ORDER BY s.created_at DESC", cancellacioRowMapper);
	}

	/**
	 * Historial de negligències d'un usuari, visible només per a l'administrador.
	 * @param usuariId Identificador de l'usuari
	 * @param actorId Usuari que fa l'acció
	 */
	public List<SollicitudCancellacio> negligenciesDe(String usuariId, String actorId) {
		exigirAdmin(actorId);
		return jdbcTemplate.query(
			SELECT_CANCELLACIO + "WHERE s.resolucio = 'NEGLIGENCIA' AND s.responsable_id::text = LOWER(?) ORDER BY s.data_resolucio DESC",
			cancellacioRowMapper, usuariId);
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

		for (com.avisa.model.BlacklistTelefon bl : blacklistTelefons()) {
			if (bl.telefon() == null) continue;
			String blClean = bl.telefon().trim();
			if (blClean.equalsIgnoreCase(cleanInput)) return true;
			String normalizedBl = normalizePhone(blClean);
			if (!normalizedBl.isBlank() && normalizedBl.equals(normalizedInput)) return true;
		}
		return false;
	}

	/**
	 * Retorna tots els números de telèfon bloquejats a la llista negra.
	 */
	public List<com.avisa.model.BlacklistTelefon> blacklistTelefons() {
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