package com.avisa.controller;

import com.avisa.dto.IncidenciaRequest;
import com.avisa.model.Incidencia;
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
 * Controlador REST per a la gestió d'incidències (/api/incidencies).
 * Les incidències són creades exclusivament per clients i representen les seves demandes de serveis.
 */
@RestController
@RequestMapping("/api/incidencies")
@CrossOrigin(origins = "http://localhost:4200")
public class IncidenciaController {

	private final InMemoryCatalog catalog;

	public IncidenciaController(InMemoryCatalog catalog) {
		this.catalog = catalog;
	}

	/**
	 * Llistar totes les incidències registrades a la taula incidencies.
	 * Endpoint: GET /api/incidencies
	 * @return Llista completa d'incidències
	 */
	@GetMapping
	public List<Incidencia> list() {
		return catalog.incidencies();
	}

	/**
	 * Obtenir el detall d'una incidència concreta pel seu ID.
	 * Endpoint: GET /api/incidencies/{id}
	 * @param id Identificador de la incidència (ex: 'I-101')
	 * @return La incidència o 404 Not Found
	 */
	@GetMapping("/{id}")
	public ResponseEntity<Incidencia> getById(@PathVariable String id) {
		return catalog.findIncidenciaById(id)
			.map(ResponseEntity::ok)
			.orElse(ResponseEntity.notFound().build());
	}

	/**
	 * Llistar les incidències creades per un client concret.
	 * Endpoint: GET /api/incidencies/client/{clientNom}
	 * @param clientNom Nom o ID de l'usuari client
	 * @return Llista d'incidències d'aquell client
	 */
	@GetMapping("/client/{clientNom}")
	public List<Incidencia> listByClient(@PathVariable String clientNom) {
		return catalog.findIncidenciesByClient(clientNom);
	}

	/**
	 * Publicar una nova incidència per part d'un client.
	 * Endpoint: POST /api/incidencies
	 * @param request Dades de la incidència (títol, descripció, categoria, client, pressupost)
	 * @return La incidència creada
	 */
	@PostMapping
	public Incidencia create(@RequestBody IncidenciaRequest request) {
		return catalog.addIncidencia(request);
	}

	/**
	 * Eliminar una incidència de la plataforma.
	 * Endpoint: DELETE /api/incidencies/{id}
	 * @param id Identificador de la incidència
	 * @return 204 No Content
	 */
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable String id) {
		catalog.deleteIncidencia(id);
		return ResponseEntity.noContent().build();
	}
}
