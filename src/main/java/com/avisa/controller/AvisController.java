package com.avisa.controller;

import com.avisa.model.Avis;
import com.avisa.service.AvisService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.Principal;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST per a la gestió d'avisos i notificacions (/api/avisos).
 * Permet consultar la safata d'avisos globals o personals, canviar l'estat de lectura i suprimir alertes.
 */
@RestController
@RequestMapping("/api/avisos")
@Tag(name = "Avisos", description = "Safata d'alertes i notificacions personals dels usuaris")
public class AvisController {

	private final AvisService avisService;

	public AvisController(AvisService avisService) {
		this.avisService = avisService;
	}

	/**
	 * Llistar tots els avisos del sistema (només administrador).
	 * @return Llista completa d'avisos
	 */
	@GetMapping
	public List<Avis> list(Principal principal) {
		Sollicitant.exigirAdmin(principal);
		return avisService.list();
	}

	// ==========================================
	// ACCIONS D'AVISOS I NOTIFICACIONS
	// ==========================================

	/**
	 * Crear un nou avís manualment (només administrador).
	 * Acció: Emet un avís a un client o professional.
	 * @param avis Dades de l'avís a donar d'alta
	 * @return L'avís creat
	 */
	@PostMapping
	public Avis create(@RequestBody Avis avis, Principal principal) {
		Sollicitant.exigirAdmin(principal);
		if (avis.enllac() != null && !avis.enllac().isBlank()) {
			return avisService.create(avis.destinatariId(), avis.missatge(), avis.enllac());
		}
		return avisService.create(avis.destinatariId(), avis.missatge());
	}

	/**
	 * Llistar els avisos d'un usuari: el mateix usuari o un administrador.
	 * Acció: Mostrar les notificacions a la safata d'entrada de l'usuari connectat.
	 * @param usuariId Identificador únic de l'usuari
	 * @return Llista de notificacions de l'usuari
	 */
	@GetMapping("/usuari/{usuariId}")
	public List<Avis> listByUsuari(@PathVariable String usuariId, Principal principal) {
		Sollicitant.exigirUnDelsOAdmin(principal, usuariId);
		return avisService.listByUsuari(usuariId);
	}

	/**
	 * Marcar un avís com a llegit: el seu destinatari o un administrador.
	 * Acció: Actualitza la columna llegit a true a la base de dades.
	 * @param id Identificador de l'avís
	 * @return Resposta 200 OK
	 */
	@PutMapping("/{id}/llegit")
	public ResponseEntity<Void> markAsRead(@PathVariable String id, Principal principal) {
		exigirDestinatariOAdmin(id, principal);
		avisService.markAsRead(id);
		return ResponseEntity.ok().build();
	}

	/**
	 * Eliminar un avís: el seu destinatari o un administrador.
	 * Acció: Suprimeix el registre de la taula avisos.
	 * @param id Identificador de l'avís
	 * @return Resposta 204 No Content
	 */
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable String id, Principal principal) {
		exigirDestinatariOAdmin(id, principal);
		avisService.delete(id);
		return ResponseEntity.noContent().build();
	}

	private void exigirDestinatariOAdmin(String avisId, Principal principal) {
		Sollicitant.id(principal);
		avisService.destinatari(avisId).ifPresent(destinatari -> Sollicitant.exigirUnDelsOAdmin(principal, destinatari));
	}
}
