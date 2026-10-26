package com.avisa.controller;

import com.avisa.dto.AfegirBlacklistRequest;
import com.avisa.model.BlacklistTelefon;
import com.avisa.service.BlacklistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST per a la gestió de la llista negra de números de telèfon (/api/blacklist).
 * Permet consultar la llista, verificar telèfons individuals i gestionar altes i baixes (bloquejos).
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
	public List<BlacklistTelefon> list() {
		return blacklistService.list();
	}

	/**
	 * Comprova si un telèfon en concret es troba a la llista negra.
	 * Endpoint: GET /api/blacklist/check?telefon=600123456
	 * @param telefon Número a consultar
	 * @return Objecte JSON amb { blacklisted: boolean, telefon: string }
	 */
	@GetMapping("/check")
	@Operation(summary = "Comprovar telèfon", description = "Verifica si un número de telèfon està bloquejat")
	public ResponseEntity<Map<String, Object>> check(@RequestParam String telefon) {
		boolean blocked = blacklistService.isBlacklisted(telefon);
		return ResponseEntity.ok(Map.of(
			"telefon", telefon,
			"blacklisted", blocked
		));
	}

	/**
	 * Afegeix un número de telèfon a la llista negra (requereix rol d'administrador).
	 * Endpoint: POST /api/blacklist
	 * @param req Dades del telèfon i motiu
	 * @param requesterRol Rol de qui executa la petició (X-User-Rol)
	 * @param requesterNom Nom de qui executa la petició (X-User-Nom)
	 * @return Registre creat o error 403 / 400
	 */
	@PostMapping
	@Operation(summary = "Bloquejar telèfon", description = "Afegeix un número de telèfon a la llista negra")
	public ResponseEntity<?> add(
		@Valid @RequestBody AfegirBlacklistRequest req,
		@RequestHeader(value = "X-User-Rol", required = false) String requesterRol,
		@RequestHeader(value = "X-User-Nom", required = false) String requesterNom
	) {
		try {
			BlacklistTelefon created = blacklistService.add(req, requesterRol, requesterNom);
			return ResponseEntity.status(HttpStatus.CREATED).body(created);
		} catch (SecurityException se) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", se.getMessage()));
		} catch (IllegalArgumentException iae) {
			return ResponseEntity.badRequest().body(Map.of("message", iae.getMessage()));
		}
	}

	/**
	 * Elimina un telèfon de la llista negra (desbloqueig).
	 * Endpoint: DELETE /api/blacklist/{id}
	 * @param id Identificador del registre o número de telèfon
	 * @param requesterRol Rol de qui executa la petició (X-User-Rol)
	 * @return 204 No Content si s'ha eliminat o 400 Bad Request
	 */
	@DeleteMapping("/{id}")
	@Operation(summary = "Desbloquejar telèfon", description = "Elimina un número de la llista negra")
	public ResponseEntity<?> remove(
		@PathVariable String id,
		@RequestHeader(value = "X-User-Rol", required = false) String requesterRol
	) {
		try {
			boolean removed = blacklistService.remove(id, requesterRol);
			if (removed) {
				return ResponseEntity.noContent().build();
			}
			return ResponseEntity.notFound().build();
		} catch (SecurityException se) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", se.getMessage()));
		}
	}
}

