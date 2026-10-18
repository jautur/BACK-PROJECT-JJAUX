package com.avisa.controller;

import com.avisa.model.Avis;
import com.avisa.service.InMemoryCatalog;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Controlador REST per a la gestió d'avisos i notificacions (/api/avisos).
 * Permet consultar la safata d'avisos globals o personals, canviar l'estat de lectura i suprimir alertes.
 */
@RestController
@RequestMapping("/api/avisos")
@CrossOrigin(originPatterns = "*")
@Tag(name = "Avisos", description = "Safata d'alertes i notificacions personals dels usuaris")
public class AvisController {

	private final InMemoryCatalog catalog;

	public AvisController(InMemoryCatalog catalog) {
		this.catalog = catalog;
	}

	/**
	 * Llistar tots els avisos del sistema (panell d'administració o global).
	 * @return Llista completa d'avisos
	 */
	@GetMapping
	public List<Avis> list() {
		return catalog.avisos();
	}

	// ==========================================
	// ACCIONS D'AVISOS I NOTIFICACIONS
	// ==========================================

	/**
	 * Crear un nou avís o alerta de sistema manualment.
	 * Acció: Emet un avís a un client o professional.
	 * @param avis Dades de l'avís a donar d'alta
	 * @return L'avís creat
	 */
	@PostMapping
	public Avis create(@RequestBody Avis avis) {
		return catalog.addAvis(avis.usuariId(), avis.missatge());
	}

	/**
	 * Llistar els avisos d'un usuari concret.
	 * Acció: Mostrar les notificacions a la safata d'entrada de l'usuari connectat.
	 * @param usuariId Identificador únic de l'usuari
	 * @return Llista de notificacions de l'usuari
	 */
	@GetMapping("/usuari/{usuariId}")
	public List<Avis> listByUsuari(@PathVariable String usuariId) {
		return catalog.findAvisosByUsuari(usuariId);
	}

	/**
	 * Marcar un avís com a llegit.
	 * Acció: Actualitza la columna llegit a true a la base de dades.
	 * @param id Identificador de l'avís
	 * @return Resposta 200 OK
	 */
	@PutMapping("/{id}/llegit")
	public ResponseEntity<Void> markAsRead(@PathVariable String id) {
		catalog.markAvisAsRead(id);
		return ResponseEntity.ok().build();
	}

	/**
	 * Eliminar un avís del sistema.
	 * Acció: Suprimeix el registre de la taula avisos.
	 * @param id Identificador de l'avís
	 * @return Resposta 204 No Content
	 */
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable String id) {
		catalog.deleteAvis(id);
		return ResponseEntity.noContent().build();
	}
}


