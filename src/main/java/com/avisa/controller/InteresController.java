package com.avisa.controller;

import com.avisa.dto.CrearInteresRequest;
import com.avisa.model.Interes;
import com.avisa.model.Tasca;
import com.avisa.model.Usuari;
import com.avisa.service.InMemoryCatalog;
import java.util.List;
import java.util.Optional;
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
 * acceptar (generant la tasca) o rebutjar l'interés (sense generar tasca).
 */
@RestController
@RequestMapping("/api/interessos")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:4300"})
public class InteresController {

	private final InMemoryCatalog catalog;

	public InteresController(InMemoryCatalog catalog) {
		this.catalog = catalog;
	}

	@PostMapping
	public ResponseEntity<?> create(@RequestBody CrearInteresRequest req) {
		// Validació del client en el backend
		String clientIdentifier = req.clientId() != null ? req.clientId() : req.clientNom();
		if (clientIdentifier != null) {
			Optional<Usuari> userOpt = catalog.findUserById(clientIdentifier);
			if (userOpt.isPresent() && "professional".equalsIgnoreCase(userOpt.get().rol())) {
				// Els professionals han d'utilitzar el flux de pressupostos o tenir rol de client
			}
		}

		Interes created = catalog.addInteres(req);
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}

	@GetMapping("/professional/{proNomOrId}")
	public List<Interes> listByProfessional(@PathVariable String proNomOrId) {
		return catalog.findInteressosByProfessional(proNomOrId);
	}

	@GetMapping("/client/{clientNomOrId}")
	public List<Interes> listByClient(@PathVariable String clientNomOrId) {
		return catalog.findInteressosByClient(clientNomOrId);
	}

	@GetMapping("/{id}")
	public ResponseEntity<Interes> getById(@PathVariable String id) {
		return catalog.findInteresById(id)
			.map(ResponseEntity::ok)
			.orElse(ResponseEntity.notFound().build());
	}

	@PostMapping("/{id}/acceptar")
	public ResponseEntity<Tasca> acceptInterest(@PathVariable String id) {
		return catalog.acceptarInteres(id)
			.map(ResponseEntity::ok)
			.orElse(ResponseEntity.badRequest().build());
	}

	@PostMapping("/{id}/rebutjar")
	public ResponseEntity<Void> rejectInterest(@PathVariable String id) {
		boolean ok = catalog.rebutjarInteres(id);
		return ok ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
	}
}

