package com.avisa.controller;

import com.avisa.dto.AfegirBlacklistRequest;
import com.avisa.model.BlacklistTelefon;
import com.avisa.service.BlacklistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
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
 * Controlador REST per a la gestió de la llista negra de números de telèfon (/api/blacklist).
 * Totes les operacions són d'administrador, segons el rol del token JWT.
 */
@RestController
@RequestMapping("/api/blacklist")
@CrossOrigin(originPatterns = "*")
@Tag(name = "Blacklist de Telèfons", description = "Control i moderació de números de telèfon bloquejats a la plataforma")
public class BlacklistController {

	private final BlacklistService blacklistService;

	public BlacklistController(BlacklistService blacklistService) {
		this.blacklistService = blacklistService;
	}

	/**
	 * Llista tots els números de telèfon inclosos a la llista negra.
	 * Endpoint: GET /api/blacklist
	 * @return Llista completa de telèfons bloquejats
	 */
	@GetMapping
	@Operation(summary = "Llistar telèfons bloquejats", description = "Retorna tots els registres de telèfons a la llista negra")
	public List<BlacklistTelefon> list(Principal principal) {
		Sollicitant.exigirAdmin(principal);
		return blacklistService.list();
	}

	/**
	 * Afegeix un número de telèfon a la llista negra.
	 * Endpoint: POST /api/blacklist
	 * @param req Dades del telèfon i motiu
	 * @return Registre creat o error 403 / 400
	 */
	@PostMapping
	@Operation(summary = "Bloquejar telèfon", description = "Afegeix un número de telèfon a la llista negra")
	public ResponseEntity<?> add(@Valid @RequestBody AfegirBlacklistRequest req, Principal principal) {
		Sollicitant.exigirAdmin(principal);
		try {
			BlacklistTelefon created = blacklistService.add(req, Sollicitant.rol(principal).orElse(null), Sollicitant.id(principal));
			return ResponseEntity.status(HttpStatus.CREATED).body(created);
		} catch (IllegalArgumentException iae) {
			return ResponseEntity.badRequest().body(Map.of("message", iae.getMessage()));
		}
	}

	/**
	 * Elimina un telèfon de la llista negra (desbloqueig).
	 * Endpoint: DELETE /api/blacklist/{id}
	 * @param id Identificador del registre o número de telèfon
	 * @return 204 No Content si s'ha eliminat o 404 si no existeix
	 */
	@DeleteMapping("/{id}")
	@Operation(summary = "Desbloquejar telèfon", description = "Elimina un número de la llista negra")
	public ResponseEntity<Void> remove(@PathVariable String id, Principal principal) {
		Sollicitant.exigirAdmin(principal);
		boolean removed = blacklistService.remove(id, Sollicitant.rol(principal).orElse(null));
		return removed ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
	}
}
