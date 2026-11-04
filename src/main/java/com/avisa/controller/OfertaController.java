package com.avisa.controller;

import com.avisa.dto.OfertaRequest;
import com.avisa.model.Oferta;
import com.avisa.model.Tasca;
import com.avisa.service.OfertaService;
import java.security.Principal;
import java.util.List;
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
 * Sol·licitar una oferta crea una sol·licitud pendent del professional, mentre l'oferta original
 * roman disponible al mercat perquè altres clients puguin sol·licitar el servei.
 */
@RestController
@RequestMapping("/api/offers")
@CrossOrigin(originPatterns = "*")
public class OfertaController {

	private final OfertaService ofertaService;

	public OfertaController(OfertaService ofertaService) {
		this.ofertaService = ofertaService;
	}

	/**
	 * Llistar totes les ofertes registrades a la plataforma.
	 * Endpoint: GET /api/offers
	 * @return Llista completa d'ofertes
	 */
	@GetMapping
	public List<Oferta> list() {
		return ofertaService.list();
	}

	/**
	 * Crear i enviar una nova oferta econòmica sobre una tasca.
	 * Endpoint: POST /api/offers
	 * Acció realitzada per usuaris amb rol de Professional.
	 * @param request Dades de l'oferta (tascaId, professional, price, note)
	 * @return L'oferta creada
	 */
	@PostMapping
	public Oferta create(@RequestBody OfertaRequest request, Principal principal) {
		return ofertaService.create(principal != null ? request.ambProfessional(principal.getName()) : request);
	}

	/**
	 * Obtenir el detall d'una oferta concreta pel seu ID.
	 * Endpoint: GET /api/offers/{id}
	 * @param id Identificador de l'oferta
	 * @return L'oferta o 404 Not Found si no existeix
	 */
	@GetMapping("/{id}")
	public ResponseEntity<Oferta> getById(@PathVariable String id) {
		return ofertaService.findById(id)
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
		return ofertaService.listByProfessional(professionalNom);
	}

	/**
	 * "M'interessa": crea una sol·licitud que el professional ha d'acceptar. El client és l'usuari del token.
	 * Endpoint: POST /api/offers/{id}/contractar
	 * @param id Identificador de l'oferta
	 * @return La sol·licitud creada
	 */
	@PostMapping("/{id}/contractar")
	public ResponseEntity<Tasca> hireOffer(@PathVariable String id, Principal principal) {
		return ofertaService.contractDirect(id, Sollicitant.id(principal))
			.map(ResponseEntity::ok)
			.orElse(ResponseEntity.notFound().build());
	}

	/**
	 * Retirar una oferta del mercat. Les sol·licituds pendents es rebutgen avisant els clients.
	 * Endpoint: DELETE /api/offers/{id}
	 * @param id Identificador de l'oferta
	 * @return 204 No Content, o 404 si no existeix
	 */
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteOffer(@PathVariable String id, Principal principal) {
		boolean retirada = ofertaService.delete(id, Sollicitant.id(principal));
		return retirada ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
	}
}
