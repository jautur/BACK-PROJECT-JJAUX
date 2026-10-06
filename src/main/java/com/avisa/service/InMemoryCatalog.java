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
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Component;

@Component
public class InMemoryCatalog {

	private final List<Usuari> users = new CopyOnWriteArrayList<>(List.of(
		new Usuari(UUID.randomUUID().toString(), "Marta Vila", "marta@jjaux.cat", "client"),
		new Usuari(UUID.randomUUID().toString(), "Jordi Romero", "jordi@jjaux.cat", "professional"),
		new Usuari(UUID.randomUUID().toString(), "Clara Sanz", "clara@jjaux.cat", "professional"),
		new Usuari(UUID.randomUUID().toString(), "Admin JJAUX", "admin@jjaux.cat", "admin")
	));

	private final List<Tasca> tasks = new CopyOnWriteArrayList<>(List.of(
		new Tasca("T-101", "Disseny d’avatar i marca personal", "Crear identitat visual per una marca emergent.", "Branding", "Marta Vila", "€350", "Oberta"),
		new Tasca("T-102", "Desenvolupament d’una landing page", "Landing responsive amb formulari de contacte.", "Web", "Luna Studio", "€900", "En curs"),
		new Tasca("T-103", "Edició de vídeo per promoció", "Vídeo curt per xarxes i promoció comercial.", "Multimèdia", "North & Co", "€500", "Finalitzada")
	));

	private final List<Oferta> offers = new CopyOnWriteArrayList<>(List.of(
		new Oferta("O-201", "T-102", "Jordi Romero", "€780", "Inclou maquetació responsive i SEO bàsic.", "Pendent"),
		new Oferta("O-202", "T-101", "Clara Sanz", "€320", "Proposta amb moodboard i identitat visual.", "Acceptada"),
		new Oferta("O-203", "T-103", "Pau Ferrer", "€430", "Versió curta per reels i xarxes socials.", "Rebutjada")
	));

	private final List<Avis> avisos = new CopyOnWriteArrayList<>(List.of(
		new Avis("A-301", "Nova oferta rebuda per la landing page.", LocalDate.now()),
		new Avis("A-302", "La tasca de branding ha estat acceptada.", LocalDate.now())
	));

	public List<Usuari> users() {
		return List.copyOf(users);
	}

	public List<Tasca> tasks() {
		return List.copyOf(tasks);
	}

	public List<Oferta> offers() {
		return List.copyOf(offers);
	}

	public List<Avis> avisos() {
		return List.copyOf(avisos);
	}

	public Optional<Usuari> findUserByEmail(String email) {
		return users.stream()
			.filter(user -> user.email().equalsIgnoreCase(email))
			.findFirst();
	}

	public Usuari addUser(RegisterRequest request) {
		Usuari user = new Usuari(UUID.randomUUID().toString(), request.nom(), request.email(), request.rol());
		users.add(user);
		return user;
	}

	public Tasca addTask(TascaRequest request) {
		Tasca task = new Tasca(nextTaskId(), request.titol(), request.descripcio(), request.categoria(), request.client(), request.budget(), request.estat());
		tasks.add(task);
		return task;
	}

	public Oferta addOffer(OfertaRequest request) {
		Oferta offer = new Oferta(nextOfferId(), request.tascaId(), request.professional(), request.price(), request.note(), request.estat());
		offers.add(offer);
		return offer;
	}

	public DashboardResponse dashboard() {
		int openTasks = (int) tasks.stream().filter(task -> "Oberta".equalsIgnoreCase(task.estat())).count();
		int activeOffers = (int) offers.stream().filter(offer -> "Pendent".equalsIgnoreCase(offer.estat())).count();
		int completedProjects = (int) tasks.stream().filter(task -> "Finalitzada".equalsIgnoreCase(task.estat())).count();
		return new DashboardResponse(openTasks, activeOffers, completedProjects, tasks(), offers());
	}

	private String nextTaskId() {
		return "T-" + String.format("%03d", 100 + tasks.size() + 1);
	}

	private String nextOfferId() {
		return "O-" + String.format("%03d", 200 + offers.size() + 1);
	}
}