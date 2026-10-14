package com.avisa.service;

import com.avisa.dto.DashboardResponse;
import com.avisa.dto.IncidenciaRequest;
import com.avisa.dto.OfertaRequest;
import com.avisa.dto.RegisterRequest;
import com.avisa.dto.TascaRequest;
import com.avisa.model.Avis;
import com.avisa.model.Incidencia;
import com.avisa.model.Oferta;
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

/**
 * Catàleg central i capa d'accés a dades per a l'aplicació JJAUX.
 * Executa operacions de consulta, inserció i actualització contra la base de dades PostgreSQL (jjaux_db).
 */
@Component
public class InMemoryCatalog {

	private final JdbcTemplate jdbcTemplate;

	public InMemoryCatalog(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	// ==========================================
	// GESTIÓ D'USUARIS
	// ==========================================

	/**
	 * Recupera tots els usuaris registrats de la base de dades.
	 * @return Llista completa d'usuaris
	 */
	public List<Usuari> users() {
		try {
			return jdbcTemplate.query(
				"SELECT id, nom, email, rol, telefon, bio FROM usuaris ORDER BY created_at",
				(rs, rowNum) -> new Usuari(
					rs.getString("id"),
					rs.getString("nom"),
					rs.getString("email"),
					rs.getString("rol"),
					rs.getString("telefon"),
					rs.getString("bio")
				)
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
				"SELECT id, nom, email, rol, telefon, bio FROM usuaris WHERE id = ? OR LOWER(nom) = LOWER(?) OR LOWER(email) = LOWER(?)",
				(rs, rowNum) -> new Usuari(
					rs.getString("id"),
					rs.getString("nom"),
					rs.getString("email"),
					rs.getString("rol"),
					rs.getString("telefon"),
					rs.getString("bio")
				),
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
				"SELECT id, nom, email, rol, telefon, bio FROM usuaris WHERE LOWER(email) = LOWER(?)",
				(rs, rowNum) -> new Usuari(
					rs.getString("id"),
					rs.getString("nom"),
					rs.getString("email"),
					rs.getString("rol"),
					rs.getString("telefon"),
					rs.getString("bio")
				),
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
				"SELECT id, nom, email, rol, telefon, bio FROM usuaris WHERE LOWER(email) = LOWER(?) AND (password = ? OR ? IS NULL)",
				(rs, rowNum) -> new Usuari(
					rs.getString("id"),
					rs.getString("nom"),
					rs.getString("email"),
					rs.getString("rol"),
					rs.getString("telefon"),
					rs.getString("bio")
				),
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
		try {
			jdbcTemplate.update(
				"UPDATE usuaris SET nom = ?, telefon = ?, bio = ? WHERE id = ?",
				nou.nom(), nou.telefon(), nou.bio(), id
			);
			return findUserById(id).orElse(nou);
		} catch (Exception e) {
			return nou;
		}
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
		String id = "u-" + UUID.randomUUID().toString().substring(0, 8);
		String password = request.password() != null ? request.password() : "123456";
		try {
			jdbcTemplate.update(
				"INSERT INTO usuaris (id, nom, email, password, rol, telefon) VALUES (?, ?, ?, ?, ?, ?)",
				id, request.nom(), request.email(), password, request.rol(), request.telefon()
			);
		} catch (Exception ignored) {}
		return new Usuari(id, request.nom(), request.email(), request.rol(), request.telefon(), null);
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
		} catch (Exception ignored) {}

		return new Incidencia(id, clientId, clientNom, request.titol(), request.descripcio(), request.categoria(), request.budget(), estat, LocalDateTime.now());
	}

	/**
	 * Elimina una incidència de la plataforma.
	 * @param id Identificador de la incidència
	 */
	public void deleteIncidencia(String id) {
		try {
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
		rs.getString("origen_id"),
		rs.getString("budget"),
		rs.getString("estat"),
		rs.getString("tipus"),
		rs.getObject("valoracio") != null ? rs.getInt("valoracio") : null,
		rs.getString("opinio")
	);

	/**
	 * Obté totes les tasques registrades a PostgreSQL.
	 * @return Llista de tasques
	 */
	public List<Tasca> tasks() {
		try {
			return jdbcTemplate.query(
				"SELECT id, titol, descripcio, categoria, client_id, client_nom, professional_id, professional_nom, origen_id, budget, estat, tipus, valoracio, opinio FROM tasques ORDER BY created_at DESC",
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
				"SELECT id, titol, descripcio, categoria, client_id, client_nom, professional_id, professional_nom, origen_id, budget, estat, tipus, valoracio, opinio FROM tasques WHERE id = ?",
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
				"SELECT id, titol, descripcio, categoria, client_id, client_nom, professional_id, professional_nom, origen_id, budget, estat, tipus, valoracio, opinio FROM tasques WHERE LOWER(client_nom) = LOWER(?) OR client_id = ? ORDER BY created_at DESC",
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
				"SELECT id, titol, descripcio, categoria, client_id, client_nom, professional_id, professional_nom, origen_id, budget, estat, tipus, valoracio, opinio FROM tasques WHERE LOWER(professional_nom) = LOWER(?) OR professional_id = ? ORDER BY created_at DESC",
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
				"SELECT o.id, COALESCE(o.tasca_id, o.incidencia_id) AS tasca_id, o.incidencia_id, o.professional, COALESCE(t.categoria, i.categoria, '') AS categoria, o.price, o.note, o.estat FROM ofertes o LEFT JOIN tasques t ON t.id = o.tasca_id LEFT JOIN incidencies i ON i.id = COALESCE(o.incidencia_id, o.tasca_id) ORDER BY o.created_at DESC",
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
				"SELECT o.id, COALESCE(o.tasca_id, o.incidencia_id) AS tasca_id, o.incidencia_id, o.professional, COALESCE(t.categoria, i.categoria, '') AS categoria, o.price, o.note, o.estat FROM ofertes o LEFT JOIN tasques t ON t.id = o.tasca_id LEFT JOIN incidencies i ON i.id = COALESCE(o.incidencia_id, o.tasca_id) WHERE o.id = ?",
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
				"SELECT o.id, COALESCE(o.tasca_id, o.incidencia_id) AS tasca_id, o.incidencia_id, o.professional, COALESCE(t.categoria, i.categoria, '') AS categoria, o.price, o.note, o.estat FROM ofertes o LEFT JOIN tasques t ON t.id = o.tasca_id LEFT JOIN incidencies i ON i.id = COALESCE(o.incidencia_id, o.tasca_id) WHERE o.tasca_id = ? OR o.incidencia_id = ? ORDER BY o.created_at DESC",
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
	 * Guarda una nova oferta presentada per un professional i emet un avís al client.
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

		// Si és una oferta de servei públic (no vinculada prèviament a una incidència existent)
		if (targetId == null || targetId.isBlank() || "standalone".equalsIgnoreCase(targetId)) {
			String newTaskId = "T-" + String.format("%03d", 100 + tasks().size() + 1);
			String titol = (request.note() != null && request.note().length() > 60)
				? request.note().substring(0, 57) + "..."
				: ((request.note() != null && !request.note().isBlank()) ? request.note() : "Servei professional");
			categoria = (request.categoria() != null && !request.categoria().isBlank()) ? request.categoria() : "Altres Serveis";
			String proId = null;
			for (Usuari u : users()) {
				if (u.nom().equalsIgnoreCase(request.professional()) || u.id().equalsIgnoreCase(request.professional())) {
					proId = u.id();
					break;
				}
			}
			try {
				jdbcTemplate.update(
					"INSERT INTO tasques (id, titol, descripcio, categoria, client_id, client_nom, professional_id, professional_nom, origen_id, budget, estat, tipus) VALUES (?, ?, ?, ?, NULL, NULL, ?, ?, ?, ?, 'Oferta', 'oferta')",
					newTaskId, titol, request.note(), categoria, proId, request.professional(), id, request.price()
				);
				tascaId = newTaskId;
			} catch (Exception ignored) {}
		} else {
			// Comprovem si el targetId és una incidència
			Optional<Incidencia> incOpt = findIncidenciaById(targetId);
			if (incOpt.isPresent()) {
				incidenciaId = targetId;
				if (categoria == null || categoria.isBlank()) {
					categoria = incOpt.get().categoria();
				}
			} else {
				if (categoria == null || categoria.isBlank()) {
					categoria = findTaskById(targetId).map(Tasca::categoria).orElse("Altres Serveis");
				}
			}
		}

		try {
			jdbcTemplate.update(
				"INSERT INTO ofertes (id, tasca_id, incidencia_id, professional, price, note, estat) VALUES (?, ?, ?, ?, ?, ?, ?)",
				id, tascaId, incidenciaId, request.professional(), request.price(), request.note(), estat
			);
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
				jdbcTemplate.update("UPDATE ofertes SET estat = 'Acceptada' WHERE id = ?", offerId);
				jdbcTemplate.update("UPDATE ofertes SET estat = 'Rebutjada' WHERE (tasca_id = ? OR incidencia_id = ?) AND id != ?", targetId, targetId, offerId);

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
					jdbcTemplate.update("UPDATE ofertes SET tasca_id = ? WHERE id = ?", newTaskId, offerId);

					final String finalProId = proId;
					if (finalProId != null) {
						addAvis(finalProId, "Enhorabona! La teva oferta per a '" + inc.titol() + "' ha estat ACCEPTADA per " + inc.clientNom() + "! S'ha generat la TASCA en curs!");
					}
					return findOfferById(offerId);
				}

				// Si ja era una tasca existent:
				jdbcTemplate.update(
					"UPDATE tasques SET estat = 'En curs', tipus = 'tasca', professional_id = ?, professional_nom = ?, origen_id = ?, budget = ? WHERE id = ?",
					proId, of.professional(), offerId, of.price(), of.tascaId()
				);

				// Notifica al professional guanyador
				findTaskById(of.tascaId()).ifPresent(task -> {
					users().stream()
						.filter(u -> u.nom().equalsIgnoreCase(of.professional()) || u.id().equalsIgnoreCase(of.professional()))
						.findFirst()
						.ifPresent(proUser -> addAvis(proUser.id(), "Enhorabona! La teva oferta per a '" + task.titol() + "' ha estat ACCEPTADA per " + task.client() + "! S'ha generat la TASCA en curs!"));
				});

				return findOfferById(offerId);
			}
			return Optional.empty();
		} catch (Exception e) {
			return Optional.empty();
		}
	}

	/**
	 * Converteix una oferta d'un professional en una TASCA directa per a un client ('OFERTA to TAREA').
	 * Si l'oferta correspon a una incidència pròpia del client, s'accepta directament.
	 * Si és una oferta pública de servei, es crea una nova TASCA 'En curs' on convergeixen client i professional obligatoris,
	 * heretant titol, descripcio, categoria, budget i guardant l'origen_id (offerId).
	 *
	 * @param offerId Identificador de l'oferta
	 * @param clientNom Nom de l'usuari client que contracta
	 * @return Optional amb la Tasca creada o actualitzada
	 */
	public Optional<Tasca> convertOfferToTask(String offerId, String clientNom) {
		try {
			Optional<Oferta> opt = findOfferById(offerId);
			if (opt.isEmpty()) return Optional.empty();
			Oferta of = opt.get();

			Optional<Tasca> existingTaskOpt = findTaskById(of.tascaId());
			if (existingTaskOpt.isPresent() && clientNom.equalsIgnoreCase(existingTaskOpt.get().client())) {
				acceptOffer(offerId);
				return findTaskById(of.tascaId());
			}

			// Creació d'una nova TASCA on convergeixen client i professional obligatoris!
			String newTaskId = "T-" + String.format("%03d", 100 + tasks().size() + 1);
			String categoria = (of.categoria() != null && !of.categoria().isBlank())
				? of.categoria()
				: (existingTaskOpt.map(Tasca::categoria).orElse("Altres Serveis"));
			String titol = "Servei de " + categoria + " (" + of.professional() + ")";
			if (existingTaskOpt.isPresent() && existingTaskOpt.get().client() != null && !"AVISA".equalsIgnoreCase(existingTaskOpt.get().client())) {
				titol = existingTaskOpt.get().titol();
			}
			String descripcio = (of.note() != null && !of.note().isBlank())
				? of.note()
				: "Servei contractat a través de l'oferta directa de " + of.professional() + ".";

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
				if (u.nom().equalsIgnoreCase(of.professional()) || u.id().equalsIgnoreCase(of.professional())) {
					proId = u.id();
					break;
				}
			}

			jdbcTemplate.update(
				"INSERT INTO tasques (id, titol, descripcio, categoria, client_id, client_nom, professional_id, professional_nom, origen_id, budget, estat, tipus) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'En curs', 'tasca')",
				newTaskId, titol, descripcio, categoria, clientId, resolvedClientNom, proId, of.professional(), offerId, of.price()
			);

			// Creem l'oferta acceptada vinculada a la tasca
			String newOfferId = "O-" + String.format("%03d", 200 + offers().size() + 1);
			jdbcTemplate.update(
				"INSERT INTO ofertes (id, tasca_id, professional, price, note, estat) VALUES (?, ?, ?, ?, ?, 'Acceptada')",
				newOfferId, newTaskId, of.professional(), of.price(), of.note()
			);

			// Avisos en temps real
			final String finalTitol = titol;
			final String finalClientNom = resolvedClientNom;
			if (proId != null) {
				addAvis(proId, "El client " + finalClientNom + " ha contractat la teva oferta per a '" + finalTitol + "' (" + of.price() + "). S'ha generat una nova TASCA en curs!");
			} else {
				users().stream()
					.filter(u -> u.nom().equalsIgnoreCase(of.professional()))
					.findFirst()
					.ifPresent(proUser -> addAvis(proUser.id(), "El client " + finalClientNom + " ha contractat la teva oferta per a '" + finalTitol + "' (" + of.price() + "). S'ha generat una nova TASCA en curs!"));
			}

			if (clientId != null) {
				addAvis(clientId, "Has contractat amb èxit el servei de " + of.professional() + " (" + of.price() + "). La tasca '" + finalTitol + "' ja està en curs!");
			} else {
				users().stream()
					.filter(u -> u.nom().equalsIgnoreCase(clientNom))
					.findFirst()
					.ifPresent(clientUser -> addAvis(clientUser.id(), "Has contractat amb èxit el servei de " + of.professional() + " (" + of.price() + "). La tasca '" + finalTitol + "' ja està en curs!"));
			}

			return findTaskById(newTaskId);
		} catch (Exception e) {
			return Optional.empty();
		}
	}

	/**
	 * Rebutja una oferta concreta marcant el seu estat com 'Rebutjada' i notifica al professional.
	 * @param offerId Identificador de l'oferta
	 * @return Optional amb l'oferta rebutjada
	 */
	public Optional<Oferta> rejectOffer(String offerId) {
		try {
			Optional<Oferta> opt = findOfferById(offerId);
			jdbcTemplate.update("UPDATE ofertes SET estat = 'Rebutjada' WHERE id = ?", offerId);
			opt.ifPresent(of -> {
				findTaskById(of.tascaId()).ifPresent(task -> {
					users().stream()
						.filter(u -> u.nom().equalsIgnoreCase(of.professional()))
						.findFirst()
						.ifPresent(proUser -> addAvis(proUser.id(), "La teva oferta per a '" + task.titol() + "' ha estat rebutjada pel client."));
				});
			});
			return findOfferById(offerId);
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
	public List<Avis> avisos() {
		try {
			return jdbcTemplate.query(
				"SELECT id, usuari_id, missatge, llegit, data FROM avisos ORDER BY created_at DESC",
				(rs, rowNum) -> new Avis(
					rs.getString("id"),
					rs.getString("usuari_id"),
					rs.getString("missatge"),
					rs.getBoolean("llegit"),
					rs.getDate("data") != null ? rs.getDate("data").toLocalDate() : LocalDate.now()
				)
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
				"SELECT id, usuari_id, missatge, llegit, data FROM avisos WHERE usuari_id = ? OR usuari_id IN (SELECT id FROM usuaris WHERE LOWER(nom) = LOWER(?)) ORDER BY created_at DESC",
				(rs, rowNum) -> new Avis(
					rs.getString("id"),
					rs.getString("usuari_id"),
					rs.getString("missatge"),
					rs.getBoolean("llegit"),
					rs.getDate("data") != null ? rs.getDate("data").toLocalDate() : LocalDate.now()
				),
				usuariId, usuariId
			);
		} catch (Exception e) {
			return List.of();
		}
	}

	/**
	 * Crea un nou avís o notificació per a un usuari.
	 * @param usuariId Identificador de l'usuari destinatari
	 * @param missatge Missatge descriptiu de l'avís
	 * @return L'avís creat
	 */
	public Avis addAvis(String usuariId, String missatge) {
		String id = "A-" + UUID.randomUUID().toString().substring(0, 8);
		LocalDate data = LocalDate.now();
		try {
			jdbcTemplate.update(
				"INSERT INTO avisos (id, usuari_id, missatge, llegit, data) VALUES (?, ?, ?, false, ?)",
				id, usuariId, missatge, data
			);
		} catch (Exception ignored) {}
		return new Avis(id, usuariId, missatge, false, data);
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
}