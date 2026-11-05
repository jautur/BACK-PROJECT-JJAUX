package com.avisa.service;

import com.avisa.dto.AfegirBlacklistRequest;
import com.avisa.model.BlacklistTelefon;
import com.avisa.model.Usuari;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Servei de negoci per a la gestió de la llista negra de números de telèfon.
 * Delega al catàleg de dades les operacions de consulta, bloqueig i desbloqueig.
 */
@Service
public class BlacklistService {

	private final InMemoryCatalog catalog;

	public BlacklistService(InMemoryCatalog catalog) {
		this.catalog = catalog;
	}

	/**
	 * Obté tots els números de telèfon registrats a la llista negra.
	 * @return Llista de registres BlacklistTelefon
	 */
	public List<BlacklistTelefon> list() {
		return catalog.blacklistTelefons();
	}

	/**
	 * Afegeix un nou registre de bloqueig a la llista negra.
	 * @param req Dades del telèfon i motiu
	 * @param requesterRol Rol de l'usuari que fa la petició (ha de ser admin)
	 * @param actorId Identificador de l'administrador: el registre guarda el seu nom
	 * @return Registre de BlacklistTelefon creat
	 */
	public BlacklistTelefon add(AfegirBlacklistRequest req, String requesterRol, String actorId) {
		String nom = catalog.findUserById(actorId).map(Usuari::nom).orElse(null);
		return catalog.addToBlacklist(req.telefon(), req.motiu(), requesterRol, nom);
	}

	/**
	 * Elimina un registre de telèfon de la llista negra (desbloqueig).
	 * @param id Identificador o telèfon a desbloquejar
	 * @param requesterRol Rol de l'usuari sol·licitant
	 * @return true si s'ha suprimit correctament
	 */
	public boolean remove(String id, String requesterRol) {
		return catalog.removeFromBlacklist(id, requesterRol);
	}
}

