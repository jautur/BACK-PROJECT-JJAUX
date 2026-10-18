package com.avisa.controller;

import com.avisa.dto.CrearPressupostRequest;
import com.avisa.model.Pressupost;
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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Controlador REST per a la gestió de pressupostos sobre incidències (/api/pressupostos).
 * Permet als professionals enviar pressupostos a incidències obertes per clients.
 * Quan el client accepta un pressupost, l'operació és transaccional:
 * es crea la tasca en curs i s'elimina la incidència de les publicacions.
 */
@RestController
@RequestMapping("/api/pressupostos")
@CrossOrigin(originPatterns = "*")
@Tag(name = "Pressupostos", description = "Enviament i acceptació transaccional de pressupostos")
public class PressupostController {

	private final InMemoryCatalog catalog;

	public PressupostController(InMemoryCatalog catalog) {
		this.catalog = catalog;
	}

	@PostMapping
	@Operation(summary = "Crear pressupost", description = "Permet a un professional presentar un pressupost per a una incidència oberta")
	public ResponseEntity<?> create(@Valid @RequestBody CrearPressupostRequest req) {
		// Validació del professional en el backend
		String proIdentifier = req.professionalId() != null ? req.professionalId() : req.professionalNom();
		if (proIdentifier != null) {
			Optional<Usuari> userOpt = catalog.findUserById(proIdentifier);
			if (userOpt.isPresent() && "client".equalsIgnoreCase(userOpt.get().rol())) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN)
					.body("Només els professionals poden enviar pressupostos a les incidències.");
			}
		}

		Pressupost created = catalog.addPressupost(req);
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}

	@GetMapping("/incidencia/{incidenciaId}")
	public List<Pressupost> listByIncidencia(@PathVariable String incidenciaId) {
		return catalog.findPressupostosByIncidencia(incidenciaId);
	}

	@GetMapping("/{id}")
	public ResponseEntity<Pressupost> getById(@PathVariable String id) {
		return catalog.findPressupostById(id)
			.map(ResponseEntity::ok)
			.orElse(ResponseEntity.notFound().build());
	}

	@PostMapping("/{id}/acceptar")
	public ResponseEntity<Tasca> acceptBudget(@PathVariable String id) {
		return catalog.acceptarPressupost(id)
			.map(ResponseEntity::ok)
			.orElse(ResponseEntity.badRequest().build());
	}

	@PostMapping("/{id}/rebutjar")
	public ResponseEntity<Void> rejectBudget(@PathVariable String id) {
		boolean ok = catalog.rebutjarPressupost(id);
		return ok ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
	}
}

