package com.avisa.service;

import com.avisa.dto.DashboardResponse;
import org.springframework.stereotype.Service;

/**
 * Servei de lògica de negoci per al tauler general d'estadístiques i mètriques del sistema.
 */
@Service
public class DashboardService {

	private final InMemoryCatalog catalog;

	public DashboardService(InMemoryCatalog catalog) {
		this.catalog = catalog;
	}

	/**
	 * Obté les mètriques consolidades de la plataforma:
	 * recompte de tasques obertes/acordades, ofertes, incidències i llistats resumits.
	 * @return Dades del tauler d'estadístiques
	 */
	public DashboardResponse getDashboard() {
		return catalog.dashboard();
	}
}

