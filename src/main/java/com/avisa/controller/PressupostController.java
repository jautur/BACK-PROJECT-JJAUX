package com.avisa.controller;

import com.avisa.dto.CrearPressupostRequest;
import com.avisa.model.Pressupost;
import com.avisa.model.Tasca;
import com.avisa.service.PressupostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
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
 * Controlador REST per a la gestió de pressupostos sobre incidències (/api/pressupostos).
 * Permet als professionals enviar pressupostos a incidències obertes per clients.
 * Quan el client accepta un pressupost, l'operació és transaccional:
 * es crea la tasca en curs, la incidència queda assignada i la resta de pressupostos es rebutgen.
 */
@RestController
@RequestMapping("/api/pressupostos")
@CrossOrigin(originPatterns = "*")
@Tag(name = "Pressupostos", description = "Enviament i acceptació transaccional de pressupostos")
public class PressupostController {

	private final PressupostService pressupostService;

	public PressupostController(PressupostService pressupostService) {
		this.pressupostService = pressupostService;
	}

	@GetMapping
	@Operation(summary = "Llistar tots els pressupostos", description = "Privilegi d'administrador: permet supervisar i auditar tots els pressupostos de la plataforma")
	public List<Pressupost> listAll(Principal principal) {
		Sollicitant.exigirAdmin(principal);
		return pressupostService.list();
	}

	@PostMapping
	@Operation(summary = "Crear pressupost", description = "Permet a un professional presentar un pressupost per a una incidència oberta")
	public ResponseEntity<?> create(@Valid @RequestBody CrearPressupostRequest cos, Principal principal) {
		CrearPressupostRequest req = cos.ambProfessional(Sollicitant.id(principal));
		if (!pressupostService.isProfessionalEligible(req.professionalId())) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN)
				.body("Només els professionals poden enviar pressupostos a les incidències.");
		}

		Pressupost created = pressupostService.create(req);
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}

	@GetMapping("/incidencia/{incidenciaId}")
	@Operation(summary = "Llistar pressupostos d'una incidència", description = "Privat: el client titular els veu tots, el professional només el seu, i l'administrador tots")
	public List<Pressupost> listByIncidencia(@PathVariable String incidenciaId, Principal principal) {
		return pressupostService.listByIncidenciaSecure(incidenciaId, Sollicitant.id(principal), Sollicitant.rol(principal).orElse(null));
	}

	@GetMapping("/{id}")
	@Operation(summary = "Obtenir pressupost per ID", description = "Privat: només els 2 usuaris implicats o administrador tenen accés")
	public ResponseEntity<Pressupost> getById(@PathVariable String id, Principal principal) {
		String actor = Sollicitant.id(principal);
		Optional<Pressupost> opt = pressupostService.findById(id);
		if (opt.isEmpty()) {
			return ResponseEntity.notFound().build();
		}
		Pressupost p = opt.get();
		if (!pressupostService.isUserInvolved(p, actor, Sollicitant.rol(principal).orElse(null))) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}
		return ResponseEntity.ok(p);
	}

	@GetMapping("/professional/{proIdentifier}")
	@Operation(summary = "Llistar pressupostos enviats pel professional", description = "Retorna exclusivament els pressupostos emesos pel propi professional")
	public List<Pressupost> listByProfessional(@PathVariable String proIdentifier, Principal principal) {
		Sollicitant.exigirUnDelsOAdmin(principal, proIdentifier);
		return pressupostService.listByProfessional(proIdentifier);
	}

	@PostMapping("/{id}/acceptar")
	public ResponseEntity<Tasca> acceptBudget(@PathVariable String id, Principal principal) {
		return pressupostService.accept(id, Sollicitant.id(principal))
			.map(ResponseEntity::ok)
			.orElse(ResponseEntity.notFound().build());
	}

	@PostMapping("/{id}/rebutjar")
	public ResponseEntity<Void> rejectBudget(@PathVariable String id, Principal principal) {
		boolean ok = pressupostService.reject(id, Sollicitant.id(principal));
		return ok ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
	}
}
