package com.avisa.controller;

import com.avisa.model.Usuari;
import com.avisa.service.InMemoryCatalog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
@CrossOrigin(originPatterns = "*")
@Tag(name = "Usuaris", description = "Gestió de comptes, perfils, bio i comissió de percentils")
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
	@Operation(summary = "Llistar usuaris", description = "Retorna tots els comptes registrats a la plataforma")
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
	@Operation(summary = "Obtenir perfil d'usuari", description = "Retorna el perfil complet per identificador")
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
	@Operation(summary = "Llistar per rol", description = "Filtra usuaris segons siguin clients, professionals o administradors")
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
	@Operation(summary = "Actualitzar perfil", description = "Modifica nom, telèfon, bio o percentil validant els permisos de rol")
	public ResponseEntity<?> updateProfile(
		@PathVariable String id,
		@Valid @RequestBody Usuari usuari,
		@org.springframework.web.bind.annotation.RequestHeader(value = "X-User-Rol", required = false) String requesterRol,
		@org.springframework.web.bind.annotation.RequestHeader(value = "X-User-Id", required = false) String requesterId
	) {
		try {
			Usuari updated = catalog.updateUser(id, usuari, requesterRol, requesterId);
			return ResponseEntity.ok(updated);
		} catch (SecurityException se) {
			return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
				.body(java.util.Map.of("message", se.getMessage()));
		} catch (IllegalArgumentException iae) {
			return ResponseEntity.badRequest()
				.body(java.util.Map.of("message", iae.getMessage()));
		}
	}

	/**
	 * Sol·licitar verificació oficial de professional per comprovació telefònica.
	 * Endpoint: POST /api/users/{id}/sollicitar-verificacio
	 */
	@org.springframework.web.bind.annotation.PostMapping("/{id}/sollicitar-verificacio")
	@Operation(summary = "Sol·licitar verificació", description = "El professional sol·licita ser verificat mitjançant comprovació telefònica per part de l'administrador")
	public ResponseEntity<?> sollicitarVerificacio(@PathVariable String id) {
		try {
			Usuari u = catalog.sollicitarVerificacio(id);
			return ResponseEntity.ok(u);
		} catch (IllegalArgumentException iae) {
			return ResponseEntity.badRequest().body(java.util.Map.of("message", iae.getMessage()));
		}
	}

	/**
	 * Verificar o desverificar oficialment un professional (només administrador).
	 * Endpoint: PUT /api/users/{id}/verificar
	 */
	@PutMapping("/{id}/verificar")
	@Operation(summary = "Verificar professional", description = "L'administrador aprova o rebutja la verificació oficial d'un professional després de la trucada")
	public ResponseEntity<?> verificarProfessional(
		@PathVariable String id,
		@org.springframework.web.bind.annotation.RequestParam(defaultValue = "true") boolean estat,
		@org.springframework.web.bind.annotation.RequestHeader(value = "X-User-Rol", required = false) String requesterRol
	) {
		try {
			Usuari u = catalog.verificarProfessional(id, estat, requesterRol);
			return ResponseEntity.ok(u);
		} catch (SecurityException se) {
			return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
				.body(java.util.Map.of("message", se.getMessage()));
		} catch (IllegalArgumentException iae) {
			return ResponseEntity.badRequest().body(java.util.Map.of("message", iae.getMessage()));
		}
	}

	/**
	 * Donar de baixa o eliminar un usuari de la plataforma.
	 * Endpoint: DELETE /api/users/{id}
	 * @param id Identificador de l'usuari
	 * @return 204 No Content si s'elimina correctament, o 400 Bad Request si no es pot eliminar
	 */
	@DeleteMapping("/{id}")
	@Operation(summary = "Eliminar usuari", description = "Elimina el compte d'usuari de la base de dades")
	public ResponseEntity<Void> deleteUser(@PathVariable String id) {
		boolean deleted = catalog.deleteUser(id);
		if (!deleted) {
			return ResponseEntity.badRequest().build();
		}
		return ResponseEntity.noContent().build();
	}
}
