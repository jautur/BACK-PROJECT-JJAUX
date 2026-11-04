package com.avisa.service;

import com.avisa.model.SollicitudCancellacio;
import com.avisa.model.Tasca;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Servei de lògica de negoci per a la sol·licitud i la resolució de cancel·lacions de tasques.
 */
@Service
public class CancellacioService {

	private final InMemoryCatalog catalog;

	public CancellacioService(InMemoryCatalog catalog) {
		this.catalog = catalog;
	}

	public Optional<Tasca> sollicitar(String tascaId, String actorId, String motiu) {
		return catalog.sollicitarCancellacio(tascaId, actorId, motiu);
	}

	public Optional<SollicitudCancellacio> resoldre(String id, String actorId, String resolucio, String responsableId) {
		return catalog.resoldreCancellacio(id, actorId, resolucio, responsableId);
	}

	public List<SollicitudCancellacio> llistar(String actorId, boolean nomesPendents) {
		return catalog.cancellacions(actorId, nomesPendents);
	}

	public List<SollicitudCancellacio> negligenciesDe(String usuariId, String actorId) {
		return catalog.negligenciesDe(usuariId, actorId);
	}
}
