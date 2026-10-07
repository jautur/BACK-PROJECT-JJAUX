package com.avisa.controller;

import com.avisa.model.Avis;
import com.avisa.service.InMemoryCatalog;
import java.util.List;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/avisos")
@CrossOrigin(origins = "http://localhost:4200")
public class AvísController {

	private final InMemoryCatalog catalog;

	public AvísController(InMemoryCatalog catalog) {
		this.catalog = catalog;
	}

	@GetMapping
	public List<Avis> list() {
		return catalog.avisos();
	}

	// ==========================================
	// ACCIONS D'AVISOS I NOTIFICACIONS
	// ==========================================

	/**
	 * Crear un nou avís o alerta de sistema.
	 * Acció: Emet un avís a un client o professional (per ex. quan rep una oferta o s'accepta una tasca).
	 */
	@org.springframework.web.bind.annotation.PostMapping
	public Avis create(@org.springframework.web.bind.annotation.RequestBody Avis avis) {
		// TODO: Crear i afegir l'avís al catàleg/repositori
		return null;
	}

	/**
	 * Llistar els avisos d'un usuari concret.
	 * Acció: Mostrar les notificacions a la safata d'entrada de l'usuari.
	 */
	@GetMapping("/usuari/{usuariId}")
	public List<Avis> listByUsuari(@org.springframework.web.bind.annotation.PathVariable String usuariId) {
		// TODO: Filtrar avisos per identificador d'usuari
		return List.of();
	}

	/**
	 * Marcar un avís com a llegit.
	 */
	@org.springframework.web.bind.annotation.PutMapping("/{id}/llegit")
	public void markAsRead(@org.springframework.web.bind.annotation.PathVariable String id) {
		// TODO: Actualitzar l'estat de l'avís a llegit
	}

	/**
	 * Eliminar un avís.
	 */
	@org.springframework.web.bind.annotation.DeleteMapping("/{id}")
	public void delete(@org.springframework.web.bind.annotation.PathVariable String id) {
		// TODO: Eliminar l'avís
	}
}

