package com.avisa.service;

import com.avisa.dto.DashboardResponse;
import com.avisa.model.Tasca;
import java.util.List;
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
	 * Obté les mètriques consolidades de la plataforma i el mercat.
	 * Les tasques són privades: cadascú rep les seues, i només un administrador les rep totes.
	 * @param actorId Usuari que pregunta, o null si no hi ha sessió
	 * @param admin Si qui pregunta és administrador
	 * @return Dades del tauler d'estadístiques
	 */
	public DashboardResponse getDashboard(String actorId, boolean admin) {
		DashboardResponse tauler = catalog.dashboard();
		if (admin) {
			return tauler;
		}
		List<Tasca> propies = actorId == null ? List.of() : tauler.tasks().stream()
			.filter(t -> actorId.equalsIgnoreCase(t.clientId()) || actorId.equalsIgnoreCase(t.professionalId()))
			.toList();
		return new DashboardResponse(tauler.openTasks(), tauler.activeOffers(), tauler.completedProjects(), propies, tauler.offers(), tauler.incidencies());
	}
}

