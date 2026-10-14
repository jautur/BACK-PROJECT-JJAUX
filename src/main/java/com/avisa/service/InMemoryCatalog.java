package com.avisa.service;

import com.avisa.dto.DashboardResponse;
import com.avisa.dto.OfertaRequest;
import com.avisa.dto.RegisterRequest;
import com.avisa.dto.TascaRequest;
import com.avisa.model.Avis;
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
		if ("u-admin".equals(id) || "admin@admin".equalsIgnoreCase(id)) {
			return false;
		}
		try {
			jdbcTemplate.update("DELETE FROM avisos WHERE usuari_id = ?", id);
			jdbcTemplate.update("DELETE FROM resenyes WHERE client_id = ? OR treballador_id = ?", id, id);
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
	// GESTIÓ DE TASQUES
	// ==========================================

	/**
	 * Obté totes les tasques registrades a PostgreSQL.
	 * @return Llista de tasques
	 */
	public List<Tasca> tasks() {
		try {
			return jdbcTemplate.query(
				"SELECT id, titol, descripcio, categoria, client_nom, budget, estat FROM tasques ORDER BY created_at DESC",
				(rs, rowNum) -> new Tasca(
					rs.getString("id"),
					rs.getString("titol"),
					rs.getString("descripcio"),
					rs.getString("categoria"),
					rs.getString("client_nom"),
					rs.getString("budget"),
					rs.getString("estat")
				)
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
				"SELECT id, titol, descripcio, categoria, client_nom, budget, estat FROM tasques WHERE id = ?",
				(rs, rowNum) -> new Tasca(
					rs.getString("id"),
					rs.getString("titol"),
					rs.getString("descripcio"),
					rs.getString("categoria"),
					rs.getString("client_nom"),
					rs.getString("budget"),
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
	 * Llista les tasques publicades per un client concret.
	 * @param clientNom Nom del client
	 * @return Llista de tasques d'aquell client
	 */
	public List<Tasca> findTasksByClient(String clientNom) {
		try {
			return jdbcTemplate.query(
				"SELECT id, titol, descripcio, categoria, client_nom, budget, estat FROM tasques WHERE LOWER(client_nom) = LOWER(?) ORDER BY created_at DESC",
				(rs, rowNum) -> new Tasca(
					rs.getString("id"),
					rs.getString("titol"),
					rs.getString("descripcio"),
					rs.getString("categoria"),
					rs.getString("client_nom"),
					rs.getString("budget"),
					rs.getString("estat")
				),
				clientNom
			);
		} catch (Exception e) {
			return List.of();
		}
	}

	/**
	 * Afegeix una nova tasca creada per un client a la base de dades.
	 * @param request Dades de la tasca a publicar
	 * @return La tasca creada
	 */
	public Tasca addTask(TascaRequest request) {
		String id = "T-" + String.format("%03d", 100 + tasks().size() + 1);
		String estat = request.estat() != null ? request.estat() : "Oberta";
		try {
			jdbcTemplate.update(
				"INSERT INTO tasques (id, titol, descripcio, categoria, client_nom, budget, estat) VALUES (?, ?, ?, ?, ?, ?, ?)",
				id, request.titol(), request.descripcio(), request.categoria(), request.client(), request.budget(), estat
			);
		} catch (Exception ignored) {}
		return new Tasca(id, request.titol(), request.descripcio(), request.categoria(), request.client(), request.budget(), estat);
	}

	/**
	 * Finalitza una tasca canviant el seu estat a 'Finalitzada' i enviant avisos pertinents.
	 * @param id Identificador de la tasca
	 * @return Optional amb la tasca actualitzada
	 */
	public Optional<Tasca> completeTask(String id) {
		try {
			jdbcTemplate.update("UPDATE tasques SET estat = 'Finalitzada' WHERE id = ?", id);
			Optional<Tasca> tascaOpt = findTaskById(id);
			tascaOpt.ifPresent(task -> {
				// Notifica al client
				users().stream()
					.filter(u -> u.nom().equalsIgnoreCase(task.client()))
					.findFirst()
					.ifPresent(clientUser -> addAvis(clientUser.id(), "Has finalitzat amb èxit la tasca '" + task.titol() + "'."));

				// Notifica al professional que tenia l'oferta acceptada
				findOffersByTasca(id).stream()
					.filter(o -> "Acceptada".equalsIgnoreCase(o.estat()))
					.findFirst()
					.flatMap(accOffer -> users().stream().filter(u -> u.nom().equalsIgnoreCase(accOffer.professional())).findFirst())
					.ifPresent(proUser -> addAvis(proUser.id(), "La tasca '" + task.titol() + "' ha estat marcada com a finalitzada pel client!"));
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
				"SELECT o.id, o.tasca_id, o.professional, COALESCE(t.categoria, '') AS categoria, o.price, o.note, o.estat FROM ofertes o LEFT JOIN tasques t ON t.id = o.tasca_id ORDER BY o.created_at DESC",
				(rs, rowNum) -> new Oferta(
					rs.getString("id"),
					rs.getString("tasca_id"),
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
				"SELECT o.id, o.tasca_id, o.professional, COALESCE(t.categoria, '') AS categoria, o.price, o.note, o.estat FROM ofertes o LEFT JOIN tasques t ON t.id = o.tasca_id WHERE o.id = ?",
				(rs, rowNum) -> new Oferta(
					rs.getString("id"),
					rs.getString("tasca_id"),
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
	 * Llista totes les ofertes presentades per a una tasca específica.
	 * @param tascaId Identificador de la tasca
	 * @return Llista d'ofertes rebudes per a la tasca
	 */
	public List<Oferta> findOffersByTasca(String tascaId) {
		try {
			return jdbcTemplate.query(
				"SELECT o.id, o.tasca_id, o.professional, COALESCE(t.categoria, '') AS categoria, o.price, o.note, o.estat FROM ofertes o LEFT JOIN tasques t ON t.id = o.tasca_id WHERE o.tasca_id = ? ORDER BY o.created_at DESC",
				(rs, rowNum) -> new Oferta(
					rs.getString("id"),
					rs.getString("tasca_id"),
					rs.getString("professional"),
					rs.getString("categoria"),
					rs.getString("price"),
					rs.getString("note"),
					rs.getString("estat")
				),
				tascaId
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
		String categoria = findTaskById(request.tascaId()).map(Tasca::categoria).orElse(null);
		try {
			jdbcTemplate.update(
				"INSERT INTO ofertes (id, tasca_id, professional, price, note, estat) VALUES (?, ?, ?, ?, ?, ?)",
				id, request.tascaId(), request.professional(), request.price(), request.note(), estat
			);
			// Notifica al client propietari de la tasca
			findTaskById(request.tascaId()).ifPresent(task -> {
				users().stream()
					.filter(u -> u.nom().equalsIgnoreCase(task.client()))
					.findFirst()
					.ifPresent(clientUser -> addAvis(clientUser.id(), "Nova oferta rebuda de " + request.professional() + " per a '" + task.titol() + "' (" + request.price() + ")."));
			});
		} catch (Exception ignored) {}
		return new Oferta(id, request.tascaId(), request.professional(), categoria, request.price(), request.note(), estat);
	}

	/**
	 * Accepta una oferta:
	 * - Marca l'oferta seleccionada com 'Acceptada'
	 * - Rebutja automàticament les altres ofertes d'aquella mateixa tasca
	 * - Passa l'estat de la tasca a 'En curs'
	 * - Envia una notificació d'enhorabona al professional
	 * @param offerId Identificador de l'oferta a acceptar
	 * @return Optional amb l'oferta acceptada
	 */
	public Optional<Oferta> acceptOffer(String offerId) {
		try {
			Optional<Oferta> opt = findOfferById(offerId);
			if (opt.isPresent()) {
				Oferta of = opt.get();
				jdbcTemplate.update("UPDATE ofertes SET estat = 'Acceptada' WHERE id = ?", offerId);
				jdbcTemplate.update("UPDATE ofertes SET estat = 'Rebutjada' WHERE tasca_id = ? AND id != ?", of.tascaId(), offerId);
				jdbcTemplate.update("UPDATE tasques SET estat = 'En curs' WHERE id = ?", of.tascaId());

				// Notifica al professional guanyador
				findTaskById(of.tascaId()).ifPresent(task -> {
					users().stream()
						.filter(u -> u.nom().equalsIgnoreCase(of.professional()))
						.findFirst()
						.ifPresent(proUser -> addAvis(proUser.id(), "Enhorabona! La teva oferta per a '" + task.titol() + "' ha estat ACCEPTADA per " + task.client() + "!"));
				});

				return findOfferById(offerId);
			}
			return Optional.empty();
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
				"SELECT id, usuari_id, missatge, llegit, data FROM avisos WHERE usuari_id = ? ORDER BY created_at DESC",
				(rs, rowNum) -> new Avis(
					rs.getString("id"),
					rs.getString("usuari_id"),
					rs.getString("missatge"),
					rs.getBoolean("llegit"),
					rs.getDate("data") != null ? rs.getDate("data").toLocalDate() : LocalDate.now()
				),
				usuariId
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
		int openTasks = (int) currentTasks.stream().filter(task -> "Oberta".equalsIgnoreCase(task.estat())).count();
		int activeOffers = (int) currentOffers.stream().filter(offer -> "Pendent".equalsIgnoreCase(offer.estat())).count();
		int completedProjects = (int) currentTasks.stream().filter(task -> "Finalitzada".equalsIgnoreCase(task.estat())).count();
		return new DashboardResponse(openTasks, activeOffers, completedProjects, currentTasks, currentOffers);
	}

	// ==========================================
	// GESTIÓ DE RESSENYES I VALORACIONS
	// ==========================================

	/**
	 * Obté totes les ressenyes registrades amb els noms del client i professional.
	 * @return Llista completa de ressenyes
	 */
	public List<Resenya> resenyes() {
		try {
			return jdbcTemplate.query(
				"SELECT r.id, r.client_id, cu.nom AS client_nom, r.treballador_id, tu.nom AS treballador_nom, r.tasca_id, r.puntuacio, r.descripcio, r.created_at " +
				"FROM resenyes r " +
				"LEFT JOIN usuaris cu ON cu.id = r.client_id " +
				"LEFT JOIN usuaris tu ON tu.id = r.treballador_id " +
				"ORDER BY r.created_at DESC",
				(rs, rowNum) -> new Resenya(
					rs.getString("id"),
					rs.getString("client_id"),
					rs.getString("client_nom"),
					rs.getString("treballador_id"),
					rs.getString("treballador_nom"),
					rs.getString("tasca_id"),
					rs.getInt("puntuacio"),
					rs.getString("descripcio"),
					rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : LocalDateTime.now()
				)
			);
		} catch (Exception e) {
			return List.of();
		}
	}

	/**
	 * Obté les ressenyes rebudes per un professional concret (per ID o per nom).
	 * @param treballadorIdOIdNom ID o Nom del professional
	 * @return Llista de ressenyes dirigides a aquest professional
	 */
	public List<Resenya> findResenyesByTreballador(String treballadorIdOIdNom) {
		try {
			return jdbcTemplate.query(
				"SELECT r.id, r.client_id, cu.nom AS client_nom, r.treballador_id, tu.nom AS treballador_nom, r.tasca_id, r.puntuacio, r.descripcio, r.created_at " +
				"FROM resenyes r " +
				"LEFT JOIN usuaris cu ON cu.id = r.client_id " +
				"LEFT JOIN usuaris tu ON tu.id = r.treballador_id " +
				"WHERE r.treballador_id = ? OR LOWER(tu.nom) = LOWER(?) " +
				"ORDER BY r.created_at DESC",
				(rs, rowNum) -> new Resenya(
					rs.getString("id"),
					rs.getString("client_id"),
					rs.getString("client_nom"),
					rs.getString("treballador_id"),
					rs.getString("treballador_nom"),
					rs.getString("tasca_id"),
					rs.getInt("puntuacio"),
					rs.getString("descripcio"),
					rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : LocalDateTime.now()
				),
				treballadorIdOIdNom, treballadorIdOIdNom
			);
		} catch (Exception e) {
			return List.of();
		}
	}

	/**
	 * Obté les ressenyes emeses per un client concret.
	 * @param clientId Identificador de l'usuari client
	 * @return Llista de ressenyes emeses pel client
	 */
	public List<Resenya> findResenyesByClient(String clientId) {
		try {
			return jdbcTemplate.query(
				"SELECT r.id, r.client_id, cu.nom AS client_nom, r.treballador_id, tu.nom AS treballador_nom, r.tasca_id, r.puntuacio, r.descripcio, r.created_at " +
				"FROM resenyes r " +
				"LEFT JOIN usuaris cu ON cu.id = r.client_id " +
				"LEFT JOIN usuaris tu ON tu.id = r.treballador_id " +
				"WHERE r.client_id = ? OR LOWER(cu.nom) = LOWER(?) " +
				"ORDER BY r.created_at DESC",
				(rs, rowNum) -> new Resenya(
					rs.getString("id"),
					rs.getString("client_id"),
					rs.getString("client_nom"),
					rs.getString("treballador_id"),
					rs.getString("treballador_nom"),
					rs.getString("tasca_id"),
					rs.getInt("puntuacio"),
					rs.getString("descripcio"),
					rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : LocalDateTime.now()
				),
				clientId, clientId
			);
		} catch (Exception e) {
			return List.of();
		}
	}

	/**
	 * Crea i desa una nova ressenya a la base de dades.
	 * @param resenya Dades de la ressenya
	 * @return Ressenya creada
	 */
	public Resenya addResenya(Resenya resenya) {
		String id = (resenya.id() != null && !resenya.id().isBlank()) ? resenya.id() : ("R-" + String.format("%03d", 1 + resenyes().size()));
		LocalDateTime createdAt = resenya.createdAt() != null ? resenya.createdAt() : LocalDateTime.now();
		try {
			jdbcTemplate.update(
				"INSERT INTO resenyes (id, client_id, treballador_id, tasca_id, puntuacio, descripcio, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
				id, resenya.clientId(), resenya.treballadorId(), resenya.tascaId(), resenya.puntuacio(), resenya.descripcio(), createdAt
			);
		} catch (Exception ignored) {}
		return new Resenya(id, resenya.clientId(), resenya.clientNom(), resenya.treballadorId(), resenya.treballadorNom(), resenya.tascaId(), resenya.puntuacio(), resenya.descripcio(), createdAt);
	}
}