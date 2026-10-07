package com.avisa.controller;

import com.avisa.model.Usuari;
import com.avisa.service.InMemoryCatalog;
import java.util.List;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:4200")
public class UsuariController {

	private final InMemoryCatalog catalog;

	public UsuariController(InMemoryCatalog catalog) {
		this.catalog = catalog;
	}

	@GetMapping
	public List<Usuari> list() {
		return catalog.users();
	}

	// ==========================================
	// ACCIONS DE GESTIÓ D'USUARIS I PERFILS
	// ==========================================

	/**
	 * Obtenir el perfil d'un usuari per ID.
	 * Acció: Veure el perfil públic d'un professional o client.
	 */
	@GetMapping("/{id}")
	public Usuari getById(@org.springframework.web.bind.annotation.PathVariable String id) {
		// TODO: Buscar usuari per identificador
		return null;
	}

	/**
	 * Filtrar usuaris per rol ('client', 'professional', 'admin').
	 * Acció: Llistar el directori de professionals o usuaris actius.
	 */
	@GetMapping("/rol/{rol}")
	public List<Usuari> listByRol(@org.springframework.web.bind.annotation.PathVariable String rol) {
		// TODO: Filtrar usuaris pel rol indicat
		return List.of();
	}

	/**
	 * Actualitzar perfil d'usuari (nom, dades personals/professionals).
	 * Acció: L'usuari edita les dades del seu compte.
	 */
	@org.springframework.web.bind.annotation.PutMapping("/{id}")
	public Usuari updateProfile(@org.springframework.web.bind.annotation.PathVariable String id, @org.springframework.web.bind.annotation.RequestBody Usuari usuari) {
		// TODO: Actualitzar dades de l'usuari
		return null;
	}

	/**
	 * Donar de baixa o desactivar un usuari.
	 * Acció: Gestió d'administració o sol·licitud de baixa de compte.
	 */
	@org.springframework.web.bind.annotation.DeleteMapping("/{id}")
	public void deleteUser(@org.springframework.web.bind.annotation.PathVariable String id) {
		// TODO: Donar de baixa l'usuari
	}
}

