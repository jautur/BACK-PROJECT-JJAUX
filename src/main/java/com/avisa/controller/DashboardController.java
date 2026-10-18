package com.avisa.controller;

import com.avisa.dto.DashboardResponse;
import com.avisa.service.InMemoryCatalog;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST per al tauler general d'estadístiques (/api/dashboard).
 * Proporciona el resum consolidat del sistema:
 * - Nombre de tasques obertes i acordades.
 * - Nombre d'ofertes actives al mercat.
 * - Nombre de projectes finalitzats.
 * - Llistat complet de tasques, ofertes i incidències per als taulers de l'aplicació.
 */
@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "http://localhost:4200")
public class DashboardController {

	private final InMemoryCatalog catalog;

	public DashboardController(InMemoryCatalog catalog) {
		this.catalog = catalog;
	}

	@GetMapping
	public DashboardResponse getDashboard() {
		return catalog.dashboard();
	}
}