package com.avisa.controller;

import com.avisa.model.Resenya;
import com.avisa.service.InMemoryCatalog;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST per a la gestió de ressenyes i valoracions (/api/resenyes).
 * Permet consultar les opinions sobre professionals i registrar noves valoracions.
 */
@RestController
@RequestMapping("/api/resenyes")
@CrossOrigin(origins = "http://localhost:4200")
public class ResenyaController {

	private final InMemoryCatalog catalog;

	public ResenyaController(InMemoryCatalog catalog) {
		this.catalog = catalog;
	}

	/**
	 * Llistar totes les ressenyes registrades.
	 * Endpoint: GET /api/resenyes
	 * @return Llista de totes les ressenyes
	 */
	@GetMapping
	public List<Resenya> list() {
		return catalog.resenyes();
	}

	/**
	 * Obtenir les ressenyes rebudes per un professional concret.
	 * Endpoint: GET /api/resenyes/treballador/{id}
	 * @param id Identificador o nom del treballador/professional
	 * @return Llista de ressenyes del professional
	 */
	@GetMapping("/treballador/{id}")
	public List<Resenya> getByTreballador(@PathVariable String id) {
		return catalog.findResenyesByTreballador(id);
	}

	/**
	 * Obtenir les ressenyes emeses per un client concret.
	 * Endpoint: GET /api/resenyes/client/{id}
	 * @param id Identificador de l'usuari client
	 * @return Llista de ressenyes emeses pel client
	 */
	@GetMapping("/client/{id}")
	public List<Resenya> getByClient(@PathVariable String id) {
		return catalog.findResenyesByClient(id);
	}

	/**
	 * Registrar una nova valoració o ressenya per a un professional.
	 * Endpoint: POST /api/resenyes
	 * @param resenya Dades de la ressenya
	 * @return Ressenya desada
	 */
	@PostMapping
	public ResponseEntity<Resenya> create(@RequestBody Resenya resenya) {
		if (resenya.clientId() == null || resenya.treballadorId() == null) {
			return ResponseEntity.badRequest().build();
		}
		Resenya created = catalog.addResenya(resenya);
		return ResponseEntity.ok(created);
	}
}

