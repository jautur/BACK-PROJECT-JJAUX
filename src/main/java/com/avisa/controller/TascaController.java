package com.avisa.controller;

import com.avisa.dto.FinalitzarTascaRequest;
import com.avisa.model.Tasca;
import com.avisa.service.TascaService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.Principal;
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
 * Gestiona el cicle de vida dels projectes en curs i conclosos delegant en TascaService:
 * 1. Les tasques acordades són privades entre el client propietari, el professional assignat i l'administrador.
 * 2. El client, el professional assignat o un administrador poden donar la tasca per finalitzada.
 * 3. Només el client pot valorar-la, també si l'ha finalitzada una altra persona.
 */
@RestController
@RequestMapping("/api/tasks")
@CrossOrigin(originPatterns = "*")
@Tag(name = "Tasques", description = "Cicle de vida de tasques privades, seguiment, finalització i avaluacions")
public class TascaController {

	private final TascaService tascaService;

	public TascaController(TascaService tascaService) {
		this.tascaService = tascaService;
	}

	/**
	 * Llistar totes les tasques de la plataforma.
	 * Endpoint: GET /api/tasks
	 * @return Llista de tasques
	 */
	@GetMapping
	public List<Tasca> list() {
		return tascaService.list();
	}

	/**
	 * Obtenir el detall d'una tasca o d'una incidència pel seu ID.
	 * Una incidència és pública al mercat; una tasca només la veuen les seues dues parts i un administrador.
	 * Endpoint: GET /api/tasks/{id}
	 * @param id Identificador de la tasca o incidència
	 * @return La tasca o 404 Not Found si no existeix
	 */
	@GetMapping("/{id}")
	public ResponseEntity<Tasca> getById(@PathVariable String id, Principal principal) {
		return tascaService.findTaskOrIncidenciaById(id)
			.map(tasca -> {
				if (!"incidencia".equals(tasca.tipus())) {
					Sollicitant.exigirUnDelsOAdmin(principal, tasca.clientId(), tasca.professionalId());
				}
				return ResponseEntity.ok(tasca);
			})
			.orElse(ResponseEntity.notFound().build());
	}

	/**
	 * Llistar les tasques d'un client: el mateix client o un administrador.
	 * Endpoint: GET /api/tasks/client/{clientId}
	 * @param clientId Identificador de l'usuari client
	 * @return Llista de tasques d'aquell client
	 */
	@GetMapping("/client/{clientId}")
	public List<Tasca> listByClient(@PathVariable String clientId, Principal principal) {
		Sollicitant.exigirUnDelsOAdmin(principal, clientId);
		return tascaService.listByClient(clientId);
	}

	/**
	 * Llistar les tasques assignades a un professional: el mateix professional o un administrador.
	 * Endpoint: GET /api/tasks/professional/{professionalId}
	 * @param professionalId Identificador del professional
	 * @return Llista de tasques on el professional ha estat assignat
	 */
	@GetMapping("/professional/{professionalId}")
	public List<Tasca> listByProfessional(@PathVariable String professionalId, Principal principal) {
		Sollicitant.exigirUnDelsOAdmin(principal, professionalId);
		return tascaService.listByProfessional(professionalId);
	}

	/**
	 * Finalitzar una tasca.
	 * Endpoint: POST /api/tasks/{id}/finalitzar
	 * Acció: El client, el professional o un administrador donen la tasca per acabada. Només el client pot afegir-hi valoració.
	 * @param id Identificador de la tasca
	 * @param body Dades opcionals de valoració i opinió del client
	 * @return La tasca actualitzada
	 */
	@PostMapping("/{id}/finalitzar")
	public ResponseEntity<Tasca> markAsCompleted(
		@PathVariable String id,
		@RequestBody(required = false) FinalitzarTascaRequest body,
		Principal principal
	) {
		Integer valoracio = body != null ? body.valoracio() : null;
		String comentari = body != null ? body.comentari() : null;
		return tascaService.completeWithReview(id, Sollicitant.id(principal), valoracio, comentari)
			.map(ResponseEntity::ok)
			.orElse(ResponseEntity.notFound().build());
	}

	/**
	 * Avaluar una tasca per part del client (només un cop finalitzada i sense valoració prèvia).
	 * Endpoint: POST /api/tasks/{id}/avaluar
	 * @param id Identificador de la tasca
	 * @param body Dades de valoració (1-5) i opinió del client
	 * @return La tasca actualitzada
	 */
	@PostMapping("/{id}/avaluar")
	public ResponseEntity<Tasca> evaluateTask(
		@PathVariable String id,
		@RequestBody FinalitzarTascaRequest body,
		Principal principal
	) {
		return tascaService.evaluate(id, Sollicitant.id(principal), body.valoracio(), body.comentari())
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
		return tascaService.listAvailable();
	}

	/**
	 * Filtrar tasques per una categoria específica (només administrador: són tasques de tothom).
	 * Endpoint: GET /api/tasks/categoria/{categoria}
	 * @param categoria Nom de la categoria
	 * @return Llista de tasques d'aquella categoria
	 */
	@GetMapping("/categoria/{categoria}")
	public List<Tasca> listByCategory(@PathVariable String categoria, Principal principal) {
		Sollicitant.exigirAdmin(principal);
		return tascaService.listByCategory(categoria);
	}

	/**
	 * Eliminar una incidència o retirar una sol·licitud. Les tasques acordades no s'esborren.
	 * Endpoint: DELETE /api/tasks/{id}
	 * @param id Identificador de la tasca
	 * @return 204 No Content, o 404 si no existeix
	 */
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable String id, Principal principal) {
		return tascaService.delete(id, Sollicitant.id(principal)) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
	}
}
