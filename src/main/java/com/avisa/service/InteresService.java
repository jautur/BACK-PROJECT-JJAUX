package com.avisa.service;

import com.avisa.dto.CrearInteresRequest;
import com.avisa.model.Interes;
import com.avisa.model.Tasca;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Servei de lògica de negoci per a la gestió d'interessos expressats en ofertes de serveis.
 * Permet formalitzar la demanda d'un client i la seva acceptació o rebuig pel professional.
 */
@Service
public class InteresService {

	private final InMemoryCatalog catalog;

	public InteresService(InMemoryCatalog catalog) {
		this.catalog = catalog;
	}

	/**
	 * Registrar un nou interés expressat per un client sobre una oferta.
	 * @param req Dades de la sol·licitud d'interés
	 * @return L'interés creat
	 */
	public Interes create(CrearInteresRequest req) {
		return catalog.addInteres(req);
	}

	/**
	 * Llistar interessos rebuts per un professional.
	 * @param proNomOrId Nom o ID del professional
	 * @return Llista d'interessos adreçats al professional
	 */
	public List<Interes> listByProfessional(String proNomOrId) {
		return catalog.findInteressosByProfessional(proNomOrId);
	}

	/**
	 * Llistar interessos enviats per un client.
	 * @param clientNomOrId Nom o ID del client
	 * @return Llista d'interessos del client
	 */
	public List<Interes> listByClient(String clientNomOrId) {
		return catalog.findInteressosByClient(clientNomOrId);
	}

	/**
	 * Troba un interés pel seu identificador.
	 * @param id Identificador de l'interés
	 * @return L'interés si existeix
	 */
	public Optional<Interes> findById(String id) {
		return catalog.findInteresById(id);
	}

	/**
	 * Accepta un interés per part del professional i genera automàticament la tasca en curs.
	 * @param interesId Identificador de l'interés
	 * @return La tasca en curs generada
	 */
	public Optional<Tasca> accept(String interesId) {
		return catalog.acceptarInteres(interesId);
	}

	/**
	 * Rebutja un interés presentat sense generar tasca.
	 * @param interesId Identificador de l'interés
	 * @return true si s'ha rebutjat correctament
	 */
	public boolean reject(String interesId) {
		return catalog.rebutjarInteres(interesId);
	}
}

