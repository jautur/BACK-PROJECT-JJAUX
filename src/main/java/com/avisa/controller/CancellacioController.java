package com.avisa.controller;

import com.avisa.dto.ResoldreCancellacioRequest;
import com.avisa.dto.SollicitarCancellacioRequest;
import com.avisa.model.SollicitudCancellacio;
import com.avisa.model.Tasca;
import com.avisa.service.CancellacioService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.Principal;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST per a les cancel·lacions de tasques.
 * El client o el professional la sol·liciten; un administrador la resol.
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Cancel·lacions", description = "Sol·licitud i resolució de cancel·lacions de tasques")
public class CancellacioController {

	private final CancellacioService cancellacioService;

	public CancellacioController(CancellacioService cancellacioService) {
		this.cancellacioService = cancellacioService;
	}

	@PostMapping("/tasks/{id}/cancellar")
	public ResponseEntity<Tasca> sollicitar(@PathVariable String id, @jakarta.validation.Valid @RequestBody SollicitarCancellacioRequest body, Principal principal) {
		return cancellacioService.sollicitar(id, Sollicitant.id(principal), body.motiu())
			.map(ResponseEntity::ok)
			.orElse(ResponseEntity.notFound().build());
	}

	@GetMapping("/cancellacions")
	public List<SollicitudCancellacio> llistar(@RequestParam(defaultValue = "false") boolean pendents, Principal principal) {
		return cancellacioService.llistar(Sollicitant.id(principal), pendents);
	}

	@GetMapping("/cancellacions/negligencies/{usuariId}")
	public List<SollicitudCancellacio> negligencies(@PathVariable String usuariId, Principal principal) {
		return cancellacioService.negligenciesDe(usuariId, Sollicitant.id(principal));
	}

	@PostMapping("/cancellacions/{id}/resoldre")
	public ResponseEntity<SollicitudCancellacio> resoldre(@PathVariable String id, @jakarta.validation.Valid @RequestBody ResoldreCancellacioRequest body, Principal principal) {
		return cancellacioService.resoldre(id, Sollicitant.id(principal), body.resolucio(), body.responsableId())
			.map(ResponseEntity::ok)
			.orElse(ResponseEntity.notFound().build());
	}
}
