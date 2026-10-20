package com.avisa.controller;

import com.avisa.dto.CrearInteresRequest;
import com.avisa.model.Interes;
import com.avisa.model.Tasca;
import com.avisa.service.InteresService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST per a la gestió d'interessos en ofertes (/api/interessos).
 * Permet als clients mostrar interés en els serveis dels professionals, i als professionals
 * acceptar (generant la tasca) o rebutjar l'interés (sense generar tasca) delegant en InteresService.
 */
@RestController
@RequestMapping("/api/interessos")
@CrossOrigin(originPatterns = "*")
public class InteresController {

	private final InteresService interesService;

	public InteresController(InteresService interesService) {
		this.interesService = interesService;
	}

	@PostMapping
	public ResponseEntity<?> create(@RequestBody CrearInteresRequest req) {
		Interes created = interesService.create(req);
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}

	@GetMapping("/professional/{proNomOrId}")
	public List<Interes> listByProfessional(@PathVariable String proNomOrId) {
		return interesService.listByProfessional(proNomOrId);
	}

	@GetMapping("/client/{clientNomOrId}")
	public List<Interes> listByClient(@PathVariable String clientNomOrId) {
		return interesService.listByClient(clientNomOrId);
	}

	@GetMapping("/{id}")
	public ResponseEntity<Interes> getById(@PathVariable String id) {
		return interesService.findById(id)
			.map(ResponseEntity::ok)
			.orElse(ResponseEntity.notFound().build());
	}

	@PostMapping("/{id}/acceptar")
	public ResponseEntity<Tasca> acceptInterest(@PathVariable String id) {
		return interesService.accept(id)
			.map(ResponseEntity::ok)
			.orElse(ResponseEntity.badRequest().build());
	}

	@PostMapping("/{id}/rebutjar")
	public ResponseEntity<Void> rejectInterest(@PathVariable String id) {
		boolean ok = interesService.reject(id);
		return ok ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
	}
}
