package com.avisa.service;

import com.avisa.model.Tasca;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Servei de lògica de negoci per al cicle de vida de tasques privades entre clients i professionals.
 * Controla la creació, seguiment, disponibilitat, finalització i avaluació de treballs acordats.
 */
@Service
public class TascaService {

	private final InMemoryCatalog catalog;

	public TascaService(InMemoryCatalog catalog) {
		this.catalog = catalog;
	}

	/**
	 * Llistar totes les tasques registrades a la plataforma.
	 * @return Llista completa de tasques
	 */
	public List<Tasca> list() {
		return catalog.tasks();
	}

	/**
	 * Troba una tasca pel seu identificador pur.
	 * @param id Identificador de la tasca
	 * @return Tasca si existeix
	 */
	public Optional<Tasca> findById(String id) {
		return catalog.findTaskById(id);
	}

	/**
	 * Obté el detall d'una tasca, o si no existeix, cerca si correspon a una incidència i l'adapta.
	 * @param id Identificador de tasca o incidència
	 * @return Tasca adaptada o Optional.empty()
	 */
	public Optional<Tasca> findTaskOrIncidenciaById(String id) {
		Optional<Tasca> task = catalog.findTaskById(id);
		if (task.isPresent()) {
			return task;
		}
		return catalog.findIncidenciaById(id)
			.map(inc -> new Tasca(
				inc.id(),
				inc.titol(),
				inc.descripcio(),
				inc.categoria(),
				inc.clientId(),
				inc.clientNom(),
				null,
				null,
				null,
				inc.budget(),
				inc.estat(),
				"incidencia",
				null,
				null
			));
	}

	/**
	 * Llistar tasques associades a un client concret.
	 * @param clientNom Nom o identificador del client
	 * @return Llista de tasques d'aquell client
	 */
	public List<Tasca> listByClient(String clientNom) {
		return catalog.findTasksByClient(clientNom);
	}

	/**
	 * Llistar tasques assignades a un professional concret.
	 * @param professionalNom Nom o identificador del professional
	 * @return Llista de tasques assignades
	 */
	public List<Tasca> listByProfessional(String professionalNom) {
		return catalog.findTasksByProfessional(professionalNom);
	}

	/**
	 * Llistar tasques i incidències que estan obertes a rebre ofertes/pressupostos.
	 * @return Llista de tasques i incidències amb estat 'Oberta'
	 */
	public List<Tasca> listAvailable() {
		List<Tasca> result = new ArrayList<>();
		// 1. Incidències obertes creades per clients (demandes pendents de pressupost)
		catalog.incidencies().stream()
			.filter(inc -> inc.estat() == null || "Oberta".equalsIgnoreCase(inc.estat()))
			.map(inc -> new Tasca(
				inc.id(),
				inc.titol(),
				inc.descripcio(),
				inc.categoria(),
				inc.clientId(),
				inc.clientNom(),
				null,
				null,
				null,
				inc.budget(),
				inc.estat() != null ? inc.estat() : "Oberta",
				"incidencia",
				null,
				null
			))
			.forEach(result::add);

		// 2. Tasques obertes de la taula tasques
		catalog.tasks().stream()
			.filter(t -> "Oberta".equalsIgnoreCase(t.estat()))
			.filter(t -> result.stream().noneMatch(existing -> existing.id().equals(t.id())))
			.forEach(result::add);

		return result;
	}

	/**
	 * Filtrar tasques per una categoria específica.
	 * @param categoria Nom de la categoria
	 * @return Llista de tasques filtrades
	 */
	public List<Tasca> listByCategory(String categoria) {
		return catalog.tasks().stream()
			.filter(t -> t.categoria() != null && t.categoria().equalsIgnoreCase(categoria))
			.toList();
	}

	/**
	 * Finalitza una tasca en curs, opcionalment amb la valoració del client.
	 * @param id Identificador de la tasca
	 * @param actorId Usuari que fa l'acció
	 * @param valoracio Puntuació numèrica d'1 a 5
	 * @param comentari Comentari del servei rebut
	 * @return Tasca actualitzada
	 */
	public Optional<Tasca> completeWithReview(String id, String actorId, Integer valoracio, String comentari) {
		return catalog.completeTask(id, actorId, valoracio, comentari);
	}

	/**
	 * Valora una tasca ja finalitzada.
	 * @param id Identificador de la tasca
	 * @param actorId Usuari que fa l'acció
	 * @param valoracio Puntuació numèrica d'1 a 5
	 * @param comentari Comentari del servei rebut
	 * @return Tasca actualitzada
	 */
	public Optional<Tasca> evaluate(String id, String actorId, Integer valoracio, String comentari) {
		return catalog.valorarTasca(id, actorId, valoracio, comentari);
	}

	/**
	 * Elimina una incidència o retira una sol·licitud. Les tasques acordades no s'esborren.
	 * @param id Identificador
	 * @param actorId Usuari que fa l'acció
	 * @return true si s'ha fet l'acció
	 */
	public boolean delete(String id, String actorId) {
		return catalog.deleteTask(id, actorId);
	}
}
