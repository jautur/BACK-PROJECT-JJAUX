package com.avisa.controller;

import com.avisa.push.ServeiPush;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.security.Principal;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Subscripció a les notificacions push del navegador (/api/push). Tot demana sessió.
 * Cada usuari només gestiona les seues subscripcions.
 */
@RestController
@RequestMapping("/api/push")
public class PushController {

	public record Claus(@NotBlank @Size(max = 200) String p256dh, @NotBlank @Size(max = 100) String auth) {
	}

	public record Subscripcio(@NotBlank @Size(max = 2048) String endpoint, @Valid Claus keys) {
	}

	public record Baixa(@NotBlank @Size(max = 2048) String endpoint) {
	}

	private final ServeiPush serveiPush;

	public PushController(ServeiPush serveiPush) {
		this.serveiPush = serveiPush;
	}

	/** Clau pública VAPID que el navegador necessita per subscriure's. 404 si el push no està configurat. */
	@GetMapping("/clau-publica")
	public ResponseEntity<Map<String, String>> clauPublica() {
		if (!serveiPush.activat()) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(Map.of("clau", serveiPush.clauPublica()));
	}

	@PostMapping("/subscripcions")
	public ResponseEntity<Void> subscriure(@Valid @RequestBody Subscripcio cos, Principal principal) {
		String usuariId = Sollicitant.id(principal);
		if (!serveiPush.activat()) {
			return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
		}
		if (cos.keys() == null || !serveiPush.subscriure(usuariId, cos.endpoint(), cos.keys().p256dh(), cos.keys().auth())) {
			return ResponseEntity.badRequest().build();
		}
		return ResponseEntity.noContent().build();
	}

	@DeleteMapping("/subscripcions")
	public ResponseEntity<Void> donarDeBaixa(@Valid @RequestBody Baixa cos, Principal principal) {
		serveiPush.donarDeBaixa(Sollicitant.id(principal), cos.endpoint());
		return ResponseEntity.noContent().build();
	}
}
