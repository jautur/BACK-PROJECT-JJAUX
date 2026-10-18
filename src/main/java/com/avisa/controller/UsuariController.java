package com.avisa.controller;

import com.avisa.model.Usuari;
import com.avisa.service.InMemoryCatalog;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST per a la gestió d'usuaris i perfils (/api/users).
 * Permet consultar el perfil d'un usuari, editar les seves dades i llistar usuaris per rol.
 */
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:4200")
public class UsuariController {

	private final InMemoryCatalog catalog;

	public UsuariController(InMemoryCatalog catalog) {
		this.catalog = catalog;
	}

	/**
	 * Llistar tots els usuaris registrats.
	 * Endpoint: GET /api/users
	 * @return Llista completa d'usuaris
	 */
	@GetMapping
	public List<Usuari> list() {
		return catalog.users();
	}

	/**
	 * Obtenir el perfil detallat d'un usuari pel seu ID.
	 * Endpoint: GET /api/users/{id}
	 * Ús: Carrega les dades a la vista de perfil de l'usuari connectat.
	 * @param id Identificador de l'usuari
	 * @return L'objecte Usuari o 404 Not Found si no existeix
	 */
	@GetMapping("/{id}")
	public ResponseEntity<Usuari> getById(@PathVariable String id) {
		return catalog.findUserById(id)
			.map(ResponseEntity::ok)
			.orElse(ResponseEntity.notFound().build());
	}

	/**
	 * Filtrar usuaris pel seu rol ('client', 'professional', 'admin').
	 * Endpoint: GET /api/users/rol/{rol}
	 * Ús: Llistar el directori públic de professionals o administració d'usuaris.
	 * @param rol Rol a filtrar
	 * @return Llista d'usuaris que coincideixen amb el rol
	 */
	@GetMapping("/rol/{rol}")
	public List<Usuari> listByRol(@PathVariable String rol) {
		return catalog.users().stream()
			.filter(u -> u.rol().equalsIgnoreCase(rol))
			.toList();
	}

	/**
	 * Actualitzar la informació del perfil d'un usuari.
	 * Endpoint: PUT /api/users/{id}
	 * Ús: Modificar nom, telèfon i descripció/bio des de la pantalla de perfil.
	 * @param id Identificador de l'usuari
	 * @param usuari Dades modificades
	 * @return Usuari actualitzat
	 */
	@PutMapping("/{id}")
	public ResponseEntity<Usuari> updateProfile(@PathVariable String id, @RequestBody Usuari usuari) {
		Usuari updated = catalog.updateUser(id, usuari);
		return ResponseEntity.ok(updated);
	}

	/**
	 * Donar de baixa o eliminar un usuari de la plataforma.
	 * Endpoint: DELETE /api/users/{id}
	 * @param id Identificador de l'usuari
	 * @return 204 No Content si s'elimina correctament, o 400 Bad Request si no es pot eliminar
	 */
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteUser(@PathVariable String id) {
		boolean deleted = catalog.deleteUser(id);
		if (!deleted) {
			return ResponseEntity.badRequest().build();
		}
		return ResponseEntity.noContent().build();
	}
}
