package com.avisa.controller;

import com.avisa.model.Publicacio;
import com.avisa.model.Usuari;
import com.avisa.service.InMemoryCatalog;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST per a la gestió de publicacions (/api/publicacions).
 * Una publicació pot ser de dos tipus:
 * - 'OFERTA': Servei ofertat per un professional. Roman actiu i pot generar múltiples tasques per a diferents clients.
 * - 'INCIDENCIA': Demanda d'un client per resoldre una necessitat. Rebrà pressupostos i s'elimina un cop contractada.
 */
@RestController
@RequestMapping("/api/publicacions")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:4300"})
@Tag(name = "Publicacions", description = "Gestió del mercat d'ofertes de professionals i incidències de clients")
public class PublicacioController {

	private final InMemoryCatalog catalog;

	public PublicacioController(InMemoryCatalog catalog) {
		this.catalog = catalog;
	}

	@GetMapping
	@Operation(summary = "Llistar publicacions", description = "Retorna totes les publicacions o filtrades per tipus ('OFERTA' o 'INCIDENCIA')")
	public List<Publicacio> list(@RequestParam(required = false) String tipus) {
		if (tipus != null && !tipus.isBlank()) {
			return catalog.publicacionsByTipus(tipus);
		}
		return catalog.publicacions();
	}

	@GetMapping("/{id}")
	@Operation(summary = "Obtenir publicació per ID", description = "Retorna els detalls d'una oferta o incidència")
	public ResponseEntity<Publicacio> getById(@PathVariable String id) {
		return catalog.findPublicacioById(id)
			.map(ResponseEntity::ok)
			.orElse(ResponseEntity.notFound().build());
	}

	@PostMapping
	public ResponseEntity<?> create(@RequestBody Publicacio req) {
		String tipus = req.tipus() != null ? req.tipus().trim().toUpperCase() : "INCIDENCIA";
		String usuariNom = req.usuariNom();
		String usuariId = req.usuariId();

		// Resolució d'usuari i validació de rols en el backend
		Optional<Usuari> userOpt = Optional.empty();
		if (usuariId != null) {
			userOpt = catalog.findUserById(usuariId);
		}
		if (userOpt.isEmpty() && usuariNom != null) {
			userOpt = catalog.findUserById(usuariNom);
		}

		if (userOpt.isPresent()) {
			Usuari u = userOpt.get();
			usuariId = u.id();
			usuariNom = u.nom();

			// Validació de rol per a crear OFERTA: només professionals o administradors
			if ("OFERTA".equalsIgnoreCase(tipus) && "client".equalsIgnoreCase(u.rol())) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN)
					.body("Només els professionals poden publicar ofertes de serveis.");
			}
		}

		String prefix = "OFERTA".equalsIgnoreCase(tipus) ? "O-" : "I-";
		String id = req.id();
		if (id == null || id.isBlank()) {
			id = prefix + UUID.randomUUID().toString().substring(0, 8);
		}

		Publicacio pub = new Publicacio(
			id,
			usuariId,
			usuariNom != null ? usuariNom : "Usuari AVISA",
			tipus,
			req.titol(),
			req.descripcio(),
			req.categoria() != null ? req.categoria() : "Altres Serveis",
			req.preuOPressupost(),
			LocalDateTime.now()
		);

		Publicacio created = catalog.addPublicacio(pub);
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable String id) {
		catalog.deletePublicacio(id);
		return ResponseEntity.noContent().build();
	}

	/**
	 * Contractar directament una oferta d'un professional ('OFERTA to TAREA').
	 * L'oferta roman persistent a publicacions i es crea directament la tasca en curs.
	 * Endpoint: POST /api/publicacions/{id}/contractar
	 */
	@PostMapping("/{id}/contractar")
	public ResponseEntity<com.avisa.model.Tasca> hireOffer(
		@PathVariable String id,
		@RequestBody(required = false) java.util.Map<String, String> body
	) {
		String clientNom = (body != null && body.get("clientNom") != null && !body.get("clientNom").isBlank())
			? body.get("clientNom")
			: "Client AVISA";
		return catalog.convertOfferToTask(id, clientNom)
			.map(ResponseEntity::ok)
			.orElse(ResponseEntity.notFound().build());
	}
}

