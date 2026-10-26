package com.avisa.service;

import com.avisa.dto.CrearPressupostRequest;
import com.avisa.model.Pressupost;
import com.avisa.model.Tasca;
import com.avisa.model.Usuari;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Servei de lògica de negoci per a la gestió de pressupostos sobre incidències.
 * Regula la presentació de propostes econòmiques per professionals i la seva acceptació transaccional.
 */
@Service
public class PressupostService {

	private final InMemoryCatalog catalog;

	public PressupostService(InMemoryCatalog catalog) {
		this.catalog = catalog;
	}

	/**
	 * Llistar tots els pressupostos registrats a la plataforma (panell d'administració).
	 */
	public List<Pressupost> list() {
		return catalog.pressupostos();
	}

	/**
	 * Valida si l'usuari té permís per emetre un pressupost (només professionals o usuaris no-client).
	 * @param proIdentifier Identificador o nom del professional
	 * @return true si l'emissor és vàlid
	 */
	public boolean isProfessionalEligible(String proIdentifier) {
		if (proIdentifier == null) {
			return true;
		}
		Optional<Usuari> userOpt = catalog.findUserById(proIdentifier);
		return userOpt.isEmpty() || !"client".equalsIgnoreCase(userOpt.get().rol());
	}

	/**
	 * Crear i donar d'alta un nou pressupost sobre una incidència.
	 * @param req Dades del pressupost
	 * @return El pressupost creat
	 */
	public Pressupost create(CrearPressupostRequest req) {
		return catalog.addPressupost(req);
	}

	/**
	 * Llistar els pressupostos presentats per a una incidència concreta (sense filtre de privadesa).
	 * @param incidenciaId Identificador de la incidència
	 * @return Llista de pressupostos rebuts
	 */
	public List<Pressupost> listByIncidencia(String incidenciaId) {
		return catalog.findPressupostosByIncidencia(incidenciaId);
	}

	/**
	 * Llistar els pressupostos presentats per a una incidència aplicant PRIVADESA ESTRICTA:
	 * Només els 2 usuaris implicats (client que publica o professional que pressuposta) o admin poden veure'ls.
	 */
	public List<Pressupost> listByIncidenciaSecure(String incidenciaId, String requesterId, String requesterNom, String requesterRol) {
		return catalog.findPressupostosByIncidenciaSecure(incidenciaId, requesterId, requesterNom, requesterRol);
	}

	/**
	 * Llistar els pressupostos presentats per un professional en concret.
	 */
	public List<Pressupost> listByProfessional(String proIdentifier) {
		return catalog.findPressupostosByProfessional(proIdentifier);
	}

	/**
	 * Comprova si un usuari és un dels 2 implicats en el pressupost.
	 */
	public boolean isUserInvolved(Pressupost p, String requesterId, String requesterNom, String requesterRol) {
		return catalog.isUserInvolvedInPressupost(p, requesterId, requesterNom, requesterRol);
	}

	/**
	 * Troba un pressupost pel seu identificador.
	 * @param id Identificador del pressupost
	 * @return El pressupost si existeix
	 */
	public Optional<Pressupost> findById(String id) {
		return catalog.findPressupostById(id);
	}

	/**
	 * Acceptació transaccional d'un pressupost per part del client:
	 * Converteix la incidència i el pressupost en una tasca en curs acordada.
	 * @param pressupostId Identificador del pressupost acceptat
	 * @return La tasca en curs generada
	 */
	public Optional<Tasca> accept(String pressupostId) {
		return catalog.acceptarPressupost(pressupostId);
	}

	/**
	 * Rebutja un pressupost presentat.
	 * @param pressupostId Identificador del pressupost
	 * @return true si s'ha rebutjat correctament
	 */
	public boolean reject(String pressupostId) {
		return catalog.rebutjarPressupost(pressupostId);
	}
}

