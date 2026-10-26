package com.avisa.service;

import com.avisa.model.Avis;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Servei de lògica de negoci per a la gestió d'avisos, alertes i notificacions personals d'usuaris.
 */
@Service
public class AvisService {

	private final InMemoryCatalog catalog;

	public AvisService(InMemoryCatalog catalog) {
		this.catalog = catalog;
	}

	/**
	 * Llistar tots els avisos del sistema (panell d'administració o auditoria).
	 * @return Llista completa d'avisos
	 */
	public List<Avis> list() {
		return catalog.avisos();
	}

	/**
	 * Llistar els avisos adreçats a un usuari concret pel seu ID o nom.
	 * @param usuariId Identificador o nom d'usuari
	 * @return Llista d'avisos personals
	 */
	public List<Avis> listByUsuari(String usuariId) {
		return catalog.findAvisosByUsuari(usuariId);
	}

	/**
	 * Crear i emetre un nou avís a un usuari.
	 * @param usuariId Destinatari de l'avís
	 * @param missatge Text de la notificació
	 * @return L'avís creat
	 */
	public Avis create(String usuariId, String missatge) {
		return catalog.addAvis(usuariId, missatge);
	}

	/**
	 * Crear i emetre un nou avís amb enllaç de navegació directe.
	 * @param usuariId Destinatari de l'avís
	 * @param missatge Text de la notificació
	 * @param enllac Ruta interna o URL per navegar en fer clic
	 * @return L'avís creat
	 */
	public Avis create(String usuariId, String missatge, String enllac) {
		return catalog.addAvis(usuariId, missatge, enllac);
	}

	/**
	 * Marca un avís com a llegit.
	 * @param id Identificador de l'avís
	 */
	public void markAsRead(String id) {
		catalog.markAvisAsRead(id);
	}

	/**
	 * Elimina un avís de la safata de notificacions.
	 * @param id Identificador de l'avís
	 */
	public void delete(String id) {
		catalog.deleteAvis(id);
	}
}

