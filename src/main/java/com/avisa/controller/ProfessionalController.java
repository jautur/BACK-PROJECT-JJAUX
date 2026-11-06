package com.avisa.controller;

import com.avisa.model.Usuari;
import com.avisa.service.UsuariService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Directori públic de professionals (/api/professionals): el que pot veure qualsevol visitant.
 * Només professionals i només els camps públics: sense contacte, comissió ni estat intern.
 * La resta de dades d'usuaris són a /api/users, que demana sessió.
 */
@RestController
@RequestMapping("/api/professionals")
@Tag(name = "Directori de professionals", description = "Perfils públics dels professionals")
public class ProfessionalController {

	private final UsuariService usuariService;

	public ProfessionalController(UsuariService usuariService) {
		this.usuariService = usuariService;
	}

	@GetMapping
	@Operation(summary = "Llistar professionals", description = "Retorna el perfil públic de tots els professionals")
	public List<Usuari> list() {
		return usuariService.list().stream()
			.filter(ProfessionalController::esProfessional)
			.map(Usuari::perfilPublic)
			.toList();
	}

	/**
	 * Perfil públic d'un professional pel seu ID o nom. Un correu no obri cap perfil.
	 */
	@GetMapping("/{id}")
	@Operation(summary = "Perfil públic d'un professional", description = "Retorna el perfil públic d'un professional pel seu ID o nom")
	public ResponseEntity<Usuari> getById(@PathVariable String id) {
		if (id.contains("@")) {
			return ResponseEntity.notFound().build();
		}
		return usuariService.findById(id)
			.filter(ProfessionalController::esProfessional)
			.map(Usuari::perfilPublic)
			.map(ResponseEntity::ok)
			.orElse(ResponseEntity.notFound().build());
	}

	private static boolean esProfessional(Usuari usuari) {
		return "PROFESSIONAL".equalsIgnoreCase(usuari.rol());
	}
}
