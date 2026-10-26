package com.avisa.controller;

import com.avisa.model.Resenya;
import com.avisa.service.ResenyaService;
import io.swagger.v3.oas.annotations.tags.Tag;
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
 * Permet consultar les opinions sobre professionals i registrar noves valoracions delegant en ResenyaService.
 */
@RestController
@RequestMapping("/api/resenyes")
@CrossOrigin(originPatterns = "*")
@Tag(name = "Ressenyes", description = "Opinions i puntuacions de servei a professionals")
public class ResenyaController {

	private final ResenyaService resenyaService;

	public ResenyaController(ResenyaService resenyaService) {
		this.resenyaService = resenyaService;
	}

	/**
	 * Llistar totes les ressenyes registrades.
	 * Endpoint: GET /api/resenyes
	 * @return Llista de totes les ressenyes
	 */
	@GetMapping
	public List<Resenya> list() {
		return resenyaService.list();
	}

	/**
	 * Obtenir les ressenyes rebudes per un professional concret.
	 * Endpoint: GET /api/resenyes/treballador/{id}
	 * @param id Identificador o nom del treballador/professional
	 * @return Llista de ressenyes del professional
	 */
	@GetMapping("/treballador/{id}")
	public List<Resenya> getByTreballador(@PathVariable String id) {
		return resenyaService.listByTreballador(id);
	}

	/**
	 * Obtenir les ressenyes emeses per un client concret.
	 * Endpoint: GET /api/resenyes/client/{id}
	 * @param id Identificador de l'usuari client
	 * @return Llista de ressenyes emeses pel client
	 */
	@GetMapping("/client/{id}")
	public List<Resenya> getByClient(@PathVariable String id) {
		return resenyaService.listByClient(id);
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
		Resenya created = resenyaService.create(resenya);
		return ResponseEntity.ok(created);
	}
}
