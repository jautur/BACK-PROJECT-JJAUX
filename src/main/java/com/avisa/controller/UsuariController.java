package com.avisa.controller;

import com.avisa.model.Usuari;
import com.avisa.service.UsuariService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
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
 * Qui fa la petició i el seu rol surten sempre del token JWT.
 * El telèfon i el correu només els veuen el mateix usuari, un administrador
 * i l'altra part d'una tasca acceptada.
 */
@RestController
@RequestMapping("/api/users")
@CrossOrigin(originPatterns = "*")
@Tag(name = "Usuaris", description = "Gestió de comptes, perfils, bio i comissió de percentils")
public class UsuariController {

	private final UsuariService usuariService;

	public UsuariController(UsuariService usuariService) {
		this.usuariService = usuariService;
	}

	/**
	 * Llistar tots els usuaris registrats (només administrador).
	 * El directori públic de professionals és a /api/professionals.
	 * Endpoint: GET /api/users
	 * @return Llista completa d'usuaris
	 */
	@GetMapping
	@Operation(summary = "Llistar usuaris", description = "Retorna tots els comptes registrats; només administrador")
	public List<Usuari> list(Principal principal) {
		Sollicitant.exigirAdmin(principal);
		return usuariService.list();
	}

	/**
	 * Obtenir un perfil pel seu ID o nom (amb sessió).
	 * Endpoint: GET /api/users/{id}
	 * Un correu només obri el perfil a un administrador: si no, serviria per a saber qui està registrat.
	 * @param id Identificador o nom de l'usuari
	 * @return L'usuari amb els camps que pot veure qui pregunta, o 404 si no existeix
	 */
	@GetMapping("/{id}")
	@Operation(summary = "Obtenir perfil d'usuari", description = "Retorna el perfil amb els camps que pot veure qui pregunta")
	public ResponseEntity<Usuari> getById(@PathVariable String id, Principal principal) {
		Sollicitant.id(principal);
		if (id.contains("@") && !Sollicitant.esAdmin(principal)) {
			return ResponseEntity.notFound().build();
		}
		return usuariService.findById(id)
			.map(u -> vistaDelPerfil(u, principal))
			.map(ResponseEntity::ok)
			.orElse(ResponseEntity.notFound().build());
	}

	/**
	 * Filtrar usuaris pel seu rol (només administrador).
	 * Endpoint: GET /api/users/rol/{rol}
	 * @param rol Rol a filtrar
	 * @return Llista d'usuaris que coincideixen amb el rol
	 */
	@GetMapping("/rol/{rol}")
	@Operation(summary = "Llistar per rol", description = "Filtra usuaris segons siguin clients, professionals o administradors")
	public List<Usuari> listByRol(@PathVariable String rol, Principal principal) {
		Sollicitant.exigirAdmin(principal);
		return usuariService.list().stream()
			.filter(u -> u.rol().equalsIgnoreCase(rol))
			.toList();
	}

	/**
	 * Actualitzar la informació del perfil d'un usuari (el mateix usuari o un administrador).
	 * Endpoint: PUT /api/users/{id}
	 * @param id Identificador de l'usuari
	 * @param usuari Dades modificades
	 * @return Usuari actualitzat
	 */
	@PutMapping("/{id}")
	@Operation(summary = "Actualitzar perfil", description = "Modifica nom, telèfon, bio o percentil; només el mateix usuari o un administrador")
	public ResponseEntity<?> updateProfile(
		@PathVariable String id,
		@Valid @RequestBody Usuari usuari,
		Principal principal
	) {
		try {
			Sollicitant.exigirUnDelsOAdmin(principal, id);
			Usuari updated = usuariService.update(id, usuari, Sollicitant.rol(principal).orElse(null), Sollicitant.id(principal));
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
	public ResponseEntity<?> sollicitarVerificacio(@PathVariable String id, Principal principal) {
		Sollicitant.exigirUnDelsOAdmin(principal, id);
		try {
			Usuari u = usuariService.sollicitarVerificacio(id);
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
		Principal principal
	) {
		Sollicitant.exigirAdmin(principal);
		try {
			Usuari u = usuariService.verificarProfessional(id, estat, Sollicitant.rol(principal).orElse(null));
			return ResponseEntity.ok(u);
		} catch (IllegalArgumentException iae) {
			return ResponseEntity.badRequest().body(java.util.Map.of("message", iae.getMessage()));
		}
	}

	/**
	 * Assignar fotografia de fons a un professional (només administrador).
	 * Endpoint: PUT /api/users/{id}/foto
	 */
	@PutMapping("/{id}/foto")
	@Operation(summary = "Assignar fotografia al professional", description = "L'administrador assigna una imatge de fons per a la targeta del professional recomanat")
	public ResponseEntity<?> assignarFoto(
		@PathVariable String id,
		@RequestBody java.util.Map<String, String> body,
		Principal principal
	) {
		Sollicitant.exigirAdmin(principal);
		String foto = body != null ? body.get("foto") : null;
		try {
			Usuari u = usuariService.assignarFoto(id, foto);
			return ResponseEntity.ok(u);
		} catch (IllegalArgumentException iae) {
			return ResponseEntity.badRequest().body(java.util.Map.of("message", iae.getMessage()));
		}
	}

	/**
	 * Donar de baixa un usuari de la plataforma (només administrador).
	 * Endpoint: DELETE /api/users/{id}
	 * @param id Identificador de l'usuari
	 * @return 204 No Content si s'elimina correctament, o 400 Bad Request si no es pot eliminar
	 */
	@DeleteMapping("/{id}")
	@Operation(summary = "Eliminar usuari", description = "Elimina el compte d'usuari de la base de dades; només un administrador")
	public ResponseEntity<Void> deleteUser(@PathVariable String id, Principal principal) {
		Sollicitant.exigirAdmin(principal);
		boolean deleted = usuariService.delete(id);
		if (!deleted) {
			return ResponseEntity.badRequest().build();
		}
		return ResponseEntity.noContent().build();
	}

	private Usuari vistaDelPerfil(Usuari usuari, Principal principal) {
		if (veuTotesLesDades(usuari, principal)) {
			return usuari;
		}
		if (principal != null && usuariService.comparteixenTascaAcceptada(principal.getName(), usuari.id())) {
			return usuari.perfilPublicAmbContacte();
		}
		return usuari.perfilPublic();
	}

	private boolean veuTotesLesDades(Usuari usuari, Principal principal) {
		return Sollicitant.esAdmin(principal)
			|| (principal != null && principal.getName().equalsIgnoreCase(usuari.id()));
	}
}
