package com.avisa.controller;

import com.avisa.dto.OfertaRequest;
import com.avisa.model.Oferta;
import com.avisa.service.InMemoryCatalog;
import java.util.List;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/offers")
@CrossOrigin(origins = "http://localhost:4200")
public class OfertaController {

	private final InMemoryCatalog catalog;

	public OfertaController(InMemoryCatalog catalog) {
		this.catalog = catalog;
	}

	@GetMapping
	public List<Oferta> list() {
		return catalog.offers();
	}

	@PostMapping
	public Oferta create(@RequestBody OfertaRequest request) {
		return catalog.addOffer(request);
	}

	// ==========================================
	// ACCIONS DEL CLIENT
	// ==========================================

	/**
	 * Llistar totes les ofertes rebudes per a una tasca concreta.
	 * Acció: El client compara els pressupostos dels professionals.
	 */
	@GetMapping("/tasca/{tascaId}")
	public List<Oferta> listByTasca(@org.springframework.web.bind.annotation.PathVariable String tascaId) {
		// TODO: Retornar ofertes que coincideixin amb tascaId
		return List.of();
	}

	/**
	 * Acceptar una oferta.
	 * Acció: El client accepta la proposta econòmica (canvi d'estat a 'Acceptada').
	 */
	@PostMapping("/{id}/acceptar")
	public Oferta acceptOffer(@org.springframework.web.bind.annotation.PathVariable String id) {
		// TODO: Marcar oferta com 'Acceptada' i passar la tasca a 'En curs'
		return null;
	}

	/**
	 * Rebutjar una oferta.
	 * Acció: El client descarta una proposta (canvi d'estat a 'Rebutjada').
	 */
	@PostMapping("/{id}/rebutjar")
	public Oferta rejectOffer(@org.springframework.web.bind.annotation.PathVariable String id) {
		// TODO: Marcar oferta com 'Rebutjada'
		return null;
	}

	// ==========================================
	// ACCIONS DEL PROFESSIONAL
	// ==========================================

	/**
	 * Obtenir detall d'una oferta concreta per ID.
	 */
	@GetMapping("/{id}")
	public Oferta getById(@org.springframework.web.bind.annotation.PathVariable String id) {
		// TODO: Buscar oferta per id
		return null;
	}

	/**
	 * Llistar ofertes enviades per un professional concret.
	 * Acció: Panell del professional per fer el seguiment de les seves candidatures.
	 */
	@GetMapping("/professional/{professionalNom}")
	public List<Oferta> listByProfessional(@org.springframework.web.bind.annotation.PathVariable String professionalNom) {
		// TODO: Filtrar ofertes pel nom o identificador del professional
		return List.of();
	}

	/**
	 * Actualitzar una oferta existent (preu, nota explicativa).
	 * Acció: El professional retoca la seva proposta si encara està pendent.
	 */
	@org.springframework.web.bind.annotation.PutMapping("/{id}")
	public Oferta updateOffer(@org.springframework.web.bind.annotation.PathVariable String id, @RequestBody OfertaRequest request) {
		// TODO: Actualitzar oferta si estat == 'Pendent'
		return null;
	}

	/**
	 * Retirar / cancel·lar una oferta.
	 * Acció: El professional es retira d'una tasca.
	 */
	@org.springframework.web.bind.annotation.DeleteMapping("/{id}")
	public void deleteOffer(@org.springframework.web.bind.annotation.PathVariable String id) {
		// TODO: Eliminar l'oferta
	}

	// ==========================================
	// ACCIONS DE L'ADMINISTRADOR
	// ==========================================

	/**
	 * Moderació d'ofertes inapropiades.
	 */
	@org.springframework.web.bind.annotation.DeleteMapping("/{id}/moderar")
	public void moderateOffer(@org.springframework.web.bind.annotation.PathVariable String id) {
		// TODO: Validar permís admin i eliminar oferta
	}
}
