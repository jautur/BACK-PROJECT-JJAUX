package com.avisa.service;

import com.avisa.model.Resenya;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Servei de lògica de negoci per a la gestió de ressenyes, puntuacions i valoracions de servei.
 */
@Service
public class ResenyaService {

	private final InMemoryCatalog catalog;

	public ResenyaService(InMemoryCatalog catalog) {
		this.catalog = catalog;
	}

	/**
	 * Llistar totes les ressenyes registrades a la plataforma.
	 * @return Llista completa de ressenyes
	 */
	public List<Resenya> list() {
		return catalog.resenyes();
	}

	/**
	 * Obtenir les ressenyes rebudes per un professional concret.
	 * @param treballadorId Identificador o nom del treballador
	 * @return Llista de ressenyes del professional
	 */
	public List<Resenya> listByTreballador(String treballadorId) {
		return catalog.findResenyesByTreballador(treballadorId);
	}

	/**
	 * Obtenir les ressenyes emeses per un client concret.
	 * @param clientId Identificador del client
	 * @return Llista de ressenyes emeses pel client
	 */
	public List<Resenya> listByClient(String clientId) {
		return catalog.findResenyesByClient(clientId);
	}

	/**
	 * Registrar una nova valoració de servei per a un professional.
	 * @param resenya Dades de la ressenya
	 * @return La ressenya desada
	 */
	public Resenya create(Resenya resenya) {
		return catalog.addResenya(resenya);
	}
}

