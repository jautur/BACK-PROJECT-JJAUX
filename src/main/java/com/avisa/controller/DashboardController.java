package com.avisa.controller;

import com.avisa.dto.DashboardResponse;
import com.avisa.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST per al tauler general d'estadístiques (/api/dashboard).
 * Proporciona el resum consolidat del sistema delegant en DashboardService:
 * - Nombre de tasques obertes i acordades.
 * - Nombre d'ofertes actives al mercat.
 * - Nombre de projectes finalitzats.
 * - Llistat complet de tasques, ofertes i incidències per als taulers de l'aplicació.
 */
@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(originPatterns = "*")
@Tag(name = "Dashboard", description = "Mètriques globals i comptadors del sistema")
public class DashboardController {

	private final DashboardService dashboardService;

	public DashboardController(DashboardService dashboardService) {
		this.dashboardService = dashboardService;
	}

	@GetMapping
	@Operation(summary = "Obtenir mètriques del tauler", description = "Retorna estadístiques globals i llistat de tasques i ofertes")
	public DashboardResponse getDashboard() {
		return dashboardService.getDashboard();
	}
}
