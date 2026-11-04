package com.avisa.controller;

import com.avisa.model.Interes;
import com.avisa.model.Tasca;
import com.avisa.service.InteresService;
import java.security.Principal;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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

	@GetMapping("/professional/{proNomOrId}")
	public List<Interes> listByProfessional(@PathVariable String proNomOrId) {
		return interesService.listByProfessional(proNomOrId);
	}

	@GetMapping("/client/{clientNomOrId}")
	public List<Interes> listByClient(@PathVariable String clientNomOrId) {
		return interesService.listByClient(clientNomOrId);
	}

	@PostMapping("/{id}/acceptar")
	public ResponseEntity<Tasca> acceptInterest(@PathVariable String id, Principal principal) {
		return interesService.accept(id, Sollicitant.id(principal))
			.map(ResponseEntity::ok)
			.orElse(ResponseEntity.notFound().build());
	}

	@PostMapping("/{id}/rebutjar")
	public ResponseEntity<Void> rejectInterest(@PathVariable String id, Principal principal) {
		boolean ok = interesService.reject(id, Sollicitant.id(principal));
		return ok ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
	}

	@PostMapping("/{id}/retirar")
	public ResponseEntity<Void> withdrawInterest(@PathVariable String id, Principal principal) {
		boolean ok = interesService.withdraw(id, Sollicitant.id(principal));
		return ok ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
	}
}
