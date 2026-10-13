package com.avisa.service;

import com.avisa.dto.DashboardResponse;
import com.avisa.dto.OfertaRequest;
import com.avisa.dto.RegisterRequest;
import com.avisa.dto.TascaRequest;
import com.avisa.model.Avis;
import com.avisa.model.Oferta;
import com.avisa.model.Tasca;
import com.avisa.model.Usuari;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class InMemoryCatalog {

	private final JdbcTemplate jdbcTemplate;

	public InMemoryCatalog(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public List<Usuari> users() {
		try {
			return jdbcTemplate.query(
				"SELECT id, nom, email, rol FROM usuaris ORDER BY created_at",
				(rs, rowNum) -> new Usuari(
					rs.getString("id"),
					rs.getString("nom"),
					rs.getString("email"),
					rs.getString("rol")
				)
			);
		} catch (Exception e) {
			return List.of();
		}
	}

	public List<Tasca> tasks() {
		try {
			return jdbcTemplate.query(
				"SELECT id, titol, descripcio, categoria, client_nom, budget, estat FROM tasques ORDER BY created_at",
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

	public List<Oferta> offers() {
		try {
			return jdbcTemplate.query(
				"SELECT id, tasca_id, professional, price, note, estat FROM ofertes ORDER BY created_at",
				(rs, rowNum) -> new Oferta(
					rs.getString("id"),
					rs.getString("tasca_id"),
					rs.getString("professional"),
					rs.getString("price"),
					rs.getString("note"),
					rs.getString("estat")
				)
			);
		} catch (Exception e) {
			return List.of();
		}
	}

	public List<Avis> avisos() {
		try {
			return jdbcTemplate.query(
				"SELECT id, missatge, data FROM avisos ORDER BY created_at",
				(rs, rowNum) -> new Avis(
					rs.getString("id"),
					rs.getString("missatge"),
					rs.getDate("data") != null ? rs.getDate("data").toLocalDate() : LocalDate.now()
				)
			);
		} catch (Exception e) {
			return List.of();
		}
	}

	public Optional<Usuari> findUserByEmail(String email) {
		try {
			List<Usuari> res = jdbcTemplate.query(
				"SELECT id, nom, email, rol FROM usuaris WHERE LOWER(email) = LOWER(?)",
				(rs, rowNum) -> new Usuari(
					rs.getString("id"),
					rs.getString("nom"),
					rs.getString("email"),
					rs.getString("rol")
				),
				email
			);
			return res.stream().findFirst();
		} catch (Exception e) {
			return Optional.empty();
		}
	}

	public Usuari addUser(RegisterRequest request) {
		String id = "u-" + UUID.randomUUID().toString().substring(0, 8);
		String password = request.password() != null ? request.password() : "123456";
		try {
			jdbcTemplate.update(
				"INSERT INTO usuaris (id, nom, email, password, rol) VALUES (?, ?, ?, ?, ?)",
				id, request.nom(), request.email(), password, request.rol()
			);
		} catch (Exception ignored) {}
		return new Usuari(id, request.nom(), request.email(), request.rol());
	}

	public Tasca addTask(TascaRequest request) {
		String id = "T-" + String.format("%03d", 100 + tasks().size() + 1);
		try {
			jdbcTemplate.update(
				"INSERT INTO tasques (id, titol, descripcio, categoria, client_nom, budget, estat) VALUES (?, ?, ?, ?, ?, ?, ?)",
				id, request.titol(), request.descripcio(), request.categoria(), request.client(), request.budget(), request.estat()
			);
		} catch (Exception ignored) {}
		return new Tasca(id, request.titol(), request.descripcio(), request.categoria(), request.client(), request.budget(), request.estat());
	}

	public Oferta addOffer(OfertaRequest request) {
		String id = "O-" + String.format("%03d", 200 + offers().size() + 1);
		try {
			jdbcTemplate.update(
				"INSERT INTO ofertes (id, tasca_id, professional, price, note, estat) VALUES (?, ?, ?, ?, ?, ?)",
				id, request.tascaId(), request.professional(), request.price(), request.note(), request.estat()
			);
		} catch (Exception ignored) {}
		return new Oferta(id, request.tascaId(), request.professional(), request.price(), request.note(), request.estat());
	}

	public DashboardResponse dashboard() {
		List<Tasca> currentTasks = tasks();
		List<Oferta> currentOffers = offers();
		int openTasks = (int) currentTasks.stream().filter(task -> "Oberta".equalsIgnoreCase(task.estat())).count();
		int activeOffers = (int) currentOffers.stream().filter(offer -> "Pendent".equalsIgnoreCase(offer.estat())).count();
		int completedProjects = (int) currentTasks.stream().filter(task -> "Finalitzada".equalsIgnoreCase(task.estat())).count();
		return new DashboardResponse(openTasks, activeOffers, completedProjects, currentTasks, currentOffers);
	}
}