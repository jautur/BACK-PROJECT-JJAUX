package com.avisa.controller;

import com.avisa.model.Publicacio;
import com.avisa.model.Tasca;
import com.avisa.service.PublicacioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST per a la gestió de publicacions (/api/publicacions).
 * Una publicació pot ser de dos tipus:
 * - 'OFERTA': Servei ofertat per un professional. Roman actiu i pot generar múltiples tasques per a diferents clients.
 * - 'INCIDENCIA': Demanda d'un client per resoldre una necessitat. Rebrà pressupostos i es conserva com a historial.
 */
@RestController
@RequestMapping("/api/publicacions")
@CrossOrigin(originPatterns = "*")
@Tag(name = "Publicacions", description = "Gestió del mercat d'ofertes de professionals i incidències de clients")
public class PublicacioController {

	private final PublicacioService publicacioService;

	public PublicacioController(PublicacioService publicacioService) {
		this.publicacioService = publicacioService;
	}

	@GetMapping
	@Operation(summary = "Llistar publicacions", description = "Retorna totes les publicacions o filtrades per tipus ('OFERTA' o 'INCIDENCIA')")
	public List<Publicacio> list(@RequestParam(required = false) String tipus) {
		if (tipus != null && !tipus.isBlank()) {
			return publicacioService.listByTipus(tipus);
		}
		return publicacioService.list();
	}

	@GetMapping("/{id}")
	@Operation(summary = "Obtenir publicació per ID", description = "Retorna els detalls d'una oferta o incidència")
	public ResponseEntity<Publicacio> getById(@PathVariable String id) {
		return publicacioService.findById(id)
			.map(ResponseEntity::ok)
			.orElse(ResponseEntity.notFound().build());
	}

	@PostMapping
	public ResponseEntity<Publicacio> create(@RequestBody Publicacio req, Principal principal) {
		Publicacio created = publicacioService.create(principal != null ? req.ambAutor(principal.getName()) : req);
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable String id, Principal principal) {
		boolean existia = publicacioService.delete(id, Sollicitant.id(principal));
		return existia ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
	}

	/**
	 * Contractar directament una oferta d'un professional ('OFERTA to TAREA').
	 * L'oferta roman persistent a publicacions i es crea directament la tasca en curs.
	 * Endpoint: POST /api/publicacions/{id}/contractar
	 */
	@PostMapping("/{id}/contractar")
	public ResponseEntity<Tasca> hireOffer(
		@PathVariable String id,
		@RequestBody(required = false) Map<String, String> body
	) {
		String clientNom = (body != null && body.get("clientNom") != null && !body.get("clientNom").isBlank())
			? body.get("clientNom")
			: "Client AVISA";
		return publicacioService.contractDirect(id, clientNom)
			.map(ResponseEntity::ok)
			.orElse(ResponseEntity.notFound().build());
	}
}
