package com.avisa.service;

import com.avisa.model.Publicacio;
import com.avisa.model.Tasca;
import com.avisa.model.Usuari;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Servei de lògica de negoci per a la gestió del mercat unificat de publicacions (ofertes i incidències).
 */
@Service
public class PublicacioService {

	private final InMemoryCatalog catalog;

	public PublicacioService(InMemoryCatalog catalog) {
		this.catalog = catalog;
	}

	/**
	 * Llistar totes les publicacions del mercat.
	 * @return Llista completa de publicacions
	 */
	public List<Publicacio> list() {
		return catalog.publicacions();
	}

	/**
	 * Llistar publicacions filtrades per tipus ('OFERTA' o 'INCIDENCIA').
	 * @param tipus Tipus de publicació
	 * @return Llista de publicacions del tipus especificat
	 */
	public List<Publicacio> listByTipus(String tipus) {
		return catalog.publicacionsByTipus(tipus);
	}

	/**
	 * Troba una publicació pel seu identificador.
	 * @param id Identificador de la publicació
	 * @return La publicació si existeix
	 */
	public Optional<Publicacio> findById(String id) {
		return catalog.findPublicacioById(id);
	}

	/**
	 * Resol i cerca un usuari per ID o nom per a la validació de publicacions.
	 * @param identifier Identificador o nom d'usuari
	 * @return Usuari si existeix
	 */
	public Optional<Usuari> findUser(String identifier) {
		return catalog.findUserById(identifier);
	}

	/**
	 * Crear i publicar un nou anunci al mercat.
	 * @param pub Dades de la publicació
	 * @return La publicació creada
	 */
	public Publicacio create(Publicacio pub) {
		return catalog.addPublicacio(pub);
	}

	/**
	 * Contractació directa d'una oferta des del mercat de publicacions.
	 * @param id Identificador de la publicació/oferta
	 * @param clientNom Nom o identificador del client
	 * @return La tasca creada
	 */
	public Optional<Tasca> contractDirect(String id, String clientNom) {
		return catalog.convertOfferToTask(id, clientNom);
	}

	/**
	 * Elimina una publicació de la plataforma.
	 * @param id Identificador de la publicació
	 */
	public void delete(String id) {
		catalog.deletePublicacio(id);
	}
}

