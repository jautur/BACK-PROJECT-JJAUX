package com.avisa.controller;

import com.avisa.dto.OfertaRequest;
import com.avisa.model.Oferta;
import com.avisa.model.Tasca;
import com.avisa.service.InMemoryCatalog;
import java.util.List;
import java.util.Map;
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
 * Controlador REST per a la gestió d'ofertes i pressupostos (/api/offers).
 * Permet enviar ofertes per a tasques, consultar les ofertes d'una tasca, i acceptar-les o rebutjar-les.
 */
@RestController
@RequestMapping("/api/offers")
@CrossOrigin(origins = "http://localhost:4200")
public class OfertaController {

	private final InMemoryCatalog catalog;

	public OfertaController(InMemoryCatalog catalog) {
		this.catalog = catalog;
	}

	/**
	 * Llistar totes les ofertes registrades a la plataforma.
	 * Endpoint: GET /api/offers
	 * @return Llista completa d'ofertes
	 */
	@GetMapping
	public List<Oferta> list() {
		return catalog.offers();
	}

	/**
	 * Crear i enviar una nova oferta econòmica sobre una tasca.
	 * Endpoint: POST /api/offers
	 * Acció realitzada per usuaris amb rol de Professional.
	 * @param request Dades de l'oferta (tascaId, professional, price, note)
	 * @return L'oferta creada
	 */
	@PostMapping
	public Oferta create(@RequestBody OfertaRequest request) {
		return catalog.addOffer(request);
	}

	/**
	 * Llistar totes les ofertes rebudes per a una tasca concreta.
	 * Endpoint: GET /api/offers/tasca/{tascaId}
	 * Acció: El client consulta i compara les ofertes abans d'acceptar-ne una.
	 * @param tascaId Identificador de la tasca
	 * @return Llista d'ofertes associades
	 */
	@GetMapping("/tasca/{tascaId}")
	public List<Oferta> listByTasca(@PathVariable String tascaId) {
		return catalog.findOffersByTasca(tascaId);
	}

	/**
	 * Obtenir el detall d'una oferta concreta pel seu ID.
	 * Endpoint: GET /api/offers/{id}
	 * @param id Identificador de l'oferta
	 * @return L'oferta o 404 Not Found si no existeix
	 */
	@GetMapping("/{id}")
	public ResponseEntity<Oferta> getById(@PathVariable String id) {
		return catalog.findOfferById(id)
			.map(ResponseEntity::ok)
			.orElse(ResponseEntity.notFound().build());
	}

	/**
	 * Llistar les ofertes presentades per un professional concret.
	 * Endpoint: GET /api/offers/professional/{professionalNom}
	 * Acció: Panell del professional per al seguiment de candidatures.
	 * @param professionalNom Nom del professional
	 * @return Llista d'ofertes enviades per aquell professional
	 */
	@GetMapping("/professional/{professionalNom}")
	public List<Oferta> listByProfessional(@PathVariable String professionalNom) {
		return catalog.offers().stream()
			.filter(o -> o.professional() != null && o.professional().equalsIgnoreCase(professionalNom))
			.toList();
	}

	/**
	 * Acceptar una oferta.
	 * Endpoint: POST /api/offers/{id}/acceptar
	 * Acció: El client accepta la proposta. L'oferta passa a 'Acceptada',
	 * les altres ofertes de la tasca es rebutgen automàticament i la tasca passa a 'En curs'.
	 * @param id Identificador de l'oferta acceptada
	 * @return L'oferta actualitzada
	 */
	@PostMapping("/{id}/acceptar")
	public ResponseEntity<Oferta> acceptOffer(@PathVariable String id) {
		return catalog.acceptOffer(id)
			.map(ResponseEntity::ok)
			.orElse(ResponseEntity.notFound().build());
	}

	/**
	 * Contractar directament una oferta d'un professional i convertir-la en Tasca ('OFERTA to TAREA').
	 * Endpoint: POST /api/offers/{id}/contractar
	 * @param id Identificador de l'oferta
	 * @param body Cos opcional amb { "clientNom": "Nom del client" }
	 * @return La Tasca creada o actualitzada en estat 'En curs'
	 */
	@PostMapping("/{id}/contractar")
	public ResponseEntity<Tasca> hireOffer(@PathVariable String id, @RequestBody(required = false) Map<String, String> body) {
		String clientNom = (body != null && body.get("clientNom") != null && !body.get("clientNom").isBlank())
			? body.get("clientNom")
			: "Client AVISA";
		return catalog.convertOfferToTask(id, clientNom)
			.map(ResponseEntity::ok)
			.orElse(ResponseEntity.notFound().build());
	}

	/**
	 * Rebutjar una oferta.
	 * Endpoint: POST /api/offers/{id}/rebutjar
	 * Acció: El client descarta la proposta (passa l'estat a 'Rebutjada').
	 * @param id Identificador de l'oferta rebutjada
	 * @return L'oferta actualitzada
	 */
	@PostMapping("/{id}/rebutjar")
	public ResponseEntity<Oferta> rejectOffer(@PathVariable String id) {
		return catalog.rejectOffer(id)
			.map(ResponseEntity::ok)
			.orElse(ResponseEntity.notFound().build());
	}

	/**
	 * Eliminar o moderar una oferta.
	 * Endpoint: DELETE /api/offers/{id}
	 * @param id Identificador de l'oferta
	 * @return 204 No Content
	 */
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteOffer(@PathVariable String id) {
		return ResponseEntity.noContent().build();
	}
}
