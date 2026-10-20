package com.avisa.service;

import com.avisa.dto.TascaRequest;
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
	 * Crear i donar d'alta una nova tasca.
	 * @param request Dades de la tasca
	 * @return La tasca creada
	 */
	public Tasca create(TascaRequest request) {
		return catalog.addTask(request);
	}

	/**
	 * Finalitza una tasca sense avaluació inicial.
	 * @param id Identificador de la tasca
	 * @return Tasca actualitzada a estat completat
	 */
	public Optional<Tasca> complete(String id) {
		return catalog.completeTask(id);
	}

	/**
	 * Finalitza una tasca registrant la valoració i opinió del client.
	 * @param id Identificador de la tasca
	 * @param valoracio Puntuació numèrica d'1 a 5
	 * @param opinio Comentari o ressenya del servei rebut
	 * @return Tasca actualitzada
	 */
	public Optional<Tasca> completeWithReview(String id, Integer valoracio, String opinio) {
		return catalog.completeTask(id, valoracio, opinio);
	}

	/**
	 * Elimina una tasca del sistema.
	 * @param id Identificador de la tasca
	 */
	public void delete(String id) {
		catalog.deleteTask(id);
	}
}

