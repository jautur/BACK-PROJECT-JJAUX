package com.avisa.controller;

import com.avisa.dto.TascaRequest;
import com.avisa.model.Tasca;
import com.avisa.service.InMemoryCatalog;
import java.util.List;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks")
@CrossOrigin(origins = "http://localhost:4200")
public class TascaController {

	private final InMemoryCatalog catalog;

	public TascaController(InMemoryCatalog catalog) {
		this.catalog = catalog;
	}

	@GetMapping
	public List<Tasca> list() {
		return catalog.tasks();
	}

	@PostMapping
	public Tasca create(@RequestBody TascaRequest request) {
		return catalog.addTask(request);
	}

	// ==========================================
	// ACCIONS DEL CLIENT
	// ==========================================

	/**
	 * Obtenir el detall d'una tasca per ID.
	 * Acció: Consultar estat, descripció i ofertes rebudes.
	 */
	@GetMapping("/{id}")
	public Tasca getById(@org.springframework.web.bind.annotation.PathVariable String id) {
		// TODO: Implementar cerca de tasca per id
		return null;
	}

	/**
	 * Llistar les tasques creades per un client concret.
	 * Acció: Panell del client amb les seves pròpies tasques.
	 */
	@GetMapping("/client/{clientId}")
	public List<Tasca> listByClient(@org.springframework.web.bind.annotation.PathVariable String clientId) {
		// TODO: Implementar filtre per client
		return List.of();
	}

	/**
	 * Actualitzar les dades d'una tasca existent (títol, descripció, pressupost...).
	 * Acció: El client edita la tasca mentre estigui 'Oberta'.
	 */
	@org.springframework.web.bind.annotation.PutMapping("/{id}")
	public Tasca update(@org.springframework.web.bind.annotation.PathVariable String id, @RequestBody TascaRequest request) {
		// TODO: Implementar actualització de tasca
		return null;
	}

	/**
	 * Cancel·lar / eliminar una tasca.
	 * Acció: El client cancel·la la demanda si ja no la necessita.
	 */
	@org.springframework.web.bind.annotation.DeleteMapping("/{id}")
	public void delete(@org.springframework.web.bind.annotation.PathVariable String id) {
		// TODO: Implementar eliminació o marcatge com a cancel·lada
	}

	/**
	 * Finalitzar tasca.
	 * Acció: El client confirma que la feina s'ha completat satisfactòriament.
	 */
	@PostMapping("/{id}/finalitzar")
	public Tasca markAsCompleted(@org.springframework.web.bind.annotation.PathVariable String id) {
		// TODO: Canviar estat a 'Finalitzada'
		return null;
	}

	// ==========================================
	// ACCIONS DEL PROFESSIONAL
	// ==========================================

	/**
	 * Llistar tasques disponibles (estat 'Oberta').
	 * Acció: Explorar oportunitats de feina obertes per fer ofertes.
	 */
	@GetMapping("/disponibles")
	public List<Tasca> listAvailable() {
		// TODO: Filtrar per estat 'Oberta'
		return List.of();
	}

	/**
	 * Filtrar tasques per categoria (e.g. 'Web', 'Disseny', 'Multimèdia').
	 */
	@GetMapping("/categoria/{categoria}")
	public List<Tasca> listByCategory(@org.springframework.web.bind.annotation.PathVariable String categoria) {
		// TODO: Filtrar per categoria
		return List.of();
	}

	// ==========================================
	// ACCIONS DE L'ADMINISTRADOR
	// ==========================================

	/**
	 * Modificació d'estat administrativa (supervisió / moderació).
	 */
	@org.springframework.web.bind.annotation.PatchMapping("/{id}/estat")
	public Tasca changeStatusByAdmin(@org.springframework.web.bind.annotation.PathVariable String id, @RequestBody String nouEstat) {
		// TODO: Validar permís admin i canviar estat
		return null;
	}
}
