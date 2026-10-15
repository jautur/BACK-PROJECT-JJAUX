package com.avisa.controller;

import com.avisa.dto.TascaRequest;
import com.avisa.model.Tasca;
import com.avisa.service.InMemoryCatalog;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST per a la gestió de tasques (/api/tasks).
 * Gestiona el cicle de vida dels projectes en curs i conclosos:
 * 1. Les tasques acordades són estrictament privades entre el client propietari, el professional assignat i l'administrador.
 * 2. Tant el client com el professional assignat tenen la capacitat de donar la tasca per finalitzada.
 * 3. En finalitzar, el client pot registrar o actualitzar la valoració i opinió del servei rebut.
 */
@RestController
@RequestMapping("/api/tasks")
@CrossOrigin(origins = "http://localhost:4200")
public class TascaController {

	private final InMemoryCatalog catalog;

	public TascaController(InMemoryCatalog catalog) {
		this.catalog = catalog;
	}

	/**
	 * Llistar totes les tasques de la plataforma.
	 * Endpoint: GET /api/tasks
	 * @return Llista de tasques
	 */
	@GetMapping
	public List<Tasca> list() {
		return catalog.tasks();
	}

	/**
	 * Crear i publicar una nova tasca.
	 * Endpoint: POST /api/tasks
	 * Acció realitzada per usuaris amb rol de Client.
	 * @param request Dades de la tasca (títol, descripció, categoria, client, pressupost)
	 * @return La tasca creada
	 */
	@PostMapping
	public Tasca create(@RequestBody TascaRequest request) {
		return catalog.addTask(request);
	}

	/**
	 * Obtenir el detall d'una tasca concreta pel seu ID.
	 * Endpoint: GET /api/tasks/{id}
	 * @param id Identificador de la tasca (ex: 'T-101')
	 * @return La tasca o 404 Not Found si no existeix
	 */
	@GetMapping("/{id}")
	public ResponseEntity<Tasca> getById(@PathVariable String id) {
		java.util.Optional<Tasca> task = catalog.findTaskById(id);
		if (task.isPresent()) {
			return ResponseEntity.ok(task.get());
		}
		return catalog.findIncidenciaById(id)
			.map(inc -> new Tasca(
				inc.id(),
				inc.titol(),
				inc.descripcio(),
				inc.categoria(),
				inc.clientId(),
				inc.clientNom(),
				null,
				null,
				null,
				inc.budget(),
				inc.estat(),
				"incidencia",
				null,
				null
			))
			.map(ResponseEntity::ok)
			.orElse(ResponseEntity.notFound().build());
	}

	/**
	 * Llistar les tasques creades per un client concret pel seu nom.
	 * Endpoint: GET /api/tasks/client/{clientNom}
	 * @param clientNom Nom de l'usuari client
	 * @return Llista de tasques publicades per aquell client
	 */
	@GetMapping("/client/{clientNom}")
	public List<Tasca> listByClient(@PathVariable String clientNom) {
		return catalog.findTasksByClient(clientNom);
	}

	/**
	 * Llistar les tasques assignades a un professional concret (on ja convergeixen client i professional).
	 * Endpoint: GET /api/tasks/professional/{professionalNom}
	 * @param professionalNom Nom del professional
	 * @return Llista de tasques on el professional ha estat assignat
	 */
	@GetMapping("/professional/{professionalNom}")
	public List<Tasca> listByProfessional(@PathVariable String professionalNom) {
		return catalog.findTasksByProfessional(professionalNom);
	}

	/**
	 * Finalitzar una tasca.
	 * Endpoint: POST /api/tasks/{id}/finalitzar
	 * Acció: El client confirma la conclusió del projecte (passa l'estat a 'Finalitzada') i enregistra la valoració i opinió.
	 * @param id Identificador de la tasca
	 * @param body Dades opcionals de valoració i opinió del client
	 * @return La tasca actualitzada
	 */
	@PostMapping("/{id}/finalitzar")
	public ResponseEntity<Tasca> markAsCompleted(
		@PathVariable String id,
		@RequestBody(required = false) com.avisa.dto.FinalitzarTascaRequest body
	) {
		Integer valoracio = body != null ? body.valoracio() : null;
		String opinio = body != null ? body.opinio() : null;
		return catalog.completeTask(id, valoracio, opinio)
			.map(ResponseEntity::ok)
			.orElse(ResponseEntity.notFound().build());
	}

	/**
	 * Avaluar una tasca per part del client (encara que ja estiga finalitzada).
	 * Endpoint: POST /api/tasks/{id}/avaluar
	 * @param id Identificador de la tasca
	 * @param body Dades de valoració (1-5) i opinió del client
	 * @return La tasca actualitzada
	 */
	@PostMapping("/{id}/avaluar")
	public ResponseEntity<Tasca> evaluateTask(
		@PathVariable String id,
		@RequestBody com.avisa.dto.FinalitzarTascaRequest body
	) {
		Integer valoracio = body != null ? body.valoracio() : null;
		String opinio = body != null ? body.opinio() : null;
		return catalog.completeTask(id, valoracio, opinio)
			.map(ResponseEntity::ok)
			.orElse(ResponseEntity.notFound().build());
	}

	/**
	 * Llistar tasques i incidències que estan obertes a rebre ofertes/pressupostos.
	 * Endpoint: GET /api/tasks/disponibles
	 * Acció: Permet als professionals trobar oportunitats actives per presentar pressupost.
	 * @return Llista de tasques i incidències amb estat 'Oberta'
	 */
	@GetMapping("/disponibles")
	public List<Tasca> listAvailable() {
		List<Tasca> result = new java.util.ArrayList<>();
		// 1. Incidències obertes creades per clients (demandes pendents de pressupost)
		catalog.incidencies().stream()
			.filter(inc -> inc.estat() == null || "Oberta".equalsIgnoreCase(inc.estat()))
			.map(inc -> new Tasca(
				inc.id(),
				inc.titol(),
				inc.descripcio(),
				inc.categoria(),
				inc.clientId(),
				inc.clientNom(),
				null,
				null,
				null,
				inc.budget(),
				inc.estat() != null ? inc.estat() : "Oberta",
				"incidencia",
				null,
				null
			))
			.forEach(result::add);

		// 2. Tasques obertes de la taula tasques
		catalog.tasks().stream()
			.filter(t -> "Oberta".equalsIgnoreCase(t.estat()))
			.filter(t -> result.stream().noneMatch(existing -> existing.id().equals(t.id())))
			.forEach(result::add);

		return result;
	}

	/**
	 * Filtrar tasques per una categoria específica (ex: 'Web', 'Branding', 'Multimèdia').
	 * Endpoint: GET /api/tasks/categoria/{categoria}
	 * @param categoria Nom de la categoria
	 * @return Llista de tasques d'aquella categoria
	 */
	@GetMapping("/categoria/{categoria}")
	public List<Tasca> listByCategory(@PathVariable String categoria) {
		return catalog.tasks().stream()
			.filter(t -> t.categoria() != null && t.categoria().equalsIgnoreCase(categoria))
			.toList();
	}

	/**
	 * Eliminar una tasca de la plataforma.
	 * Endpoint: DELETE /api/tasks/{id}
	 * @param id Identificador de la tasca
	 * @return 204 No Content
	 */
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable String id) {
		catalog.deleteTask(id);
		return ResponseEntity.noContent().build();
	}
}
