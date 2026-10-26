package com.avisa.service;

import com.avisa.dto.IncidenciaRequest;
import com.avisa.model.Incidencia;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Servei de lògica de negoci per a la gestió d'incidències creades exclusivament per clients.
 * Representa les demandes públiques de serveis que rebran pressupostos de professionals.
 */
@Service
public class IncidenciaService {

	private final InMemoryCatalog catalog;

	public IncidenciaService(InMemoryCatalog catalog) {
		this.catalog = catalog;
	}

	/**
	 * Llistar totes les incidències registrades.
	 * @return Llista completa d'incidències
	 */
	public List<Incidencia> list() {
		return catalog.incidencies();
	}

	/**
	 * Troba una incidència pel seu identificador.
	 * @param id Identificador de la incidència
	 * @return La incidència si existeix
	 */
	public Optional<Incidencia> findById(String id) {
		return catalog.findIncidenciaById(id);
	}

	/**
	 * Llistar les incidències creades per un client concret.
	 * @param clientNom Nom o identificador del client
	 * @return Llista d'incidències del client
	 */
	public List<Incidencia> listByClient(String clientNom) {
		return catalog.findIncidenciesByClient(clientNom);
	}

	/**
	 * Publicar una nova incidència o demanda de servei.
	 * @param request Dades de la incidència
	 * @return La incidència creada
	 */
	public Incidencia create(IncidenciaRequest request) {
		return catalog.addIncidencia(request);
	}

	/**
	 * Elimina una incidència de la plataforma.
	 * @param id Identificador de la incidència
	 */
	public void delete(String id) {
		catalog.deleteIncidencia(id);
	}
}

