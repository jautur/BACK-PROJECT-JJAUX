package com.avisa.service;

import com.avisa.model.Usuari;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Servei de lògica de negoci per a la gestió d'usuaris, perfils i verificacions professionals.
 */
@Service
public class UsuariService {

	private final InMemoryCatalog catalog;

	public UsuariService(InMemoryCatalog catalog) {
		this.catalog = catalog;
	}

	/**
	 * Llistar tots els usuaris registrats a la plataforma.
	 * @return Llista completa d'usuaris
	 */
	public List<Usuari> list() {
		return catalog.users();
	}

	/**
	 * Troba un usuari pel seu identificador únic.
	 * @param id Identificador de l'usuari
	 * @return Usuari si existeix
	 */
	public Optional<Usuari> findById(String id) {
		return catalog.findUserById(id);
	}

	/**
	 * Troba un usuari pel seu correu electrònic.
	 * @param email Correu de l'usuari
	 * @return Usuari si existeix
	 */
	public Optional<Usuari> findByEmail(String email) {
		return catalog.findUserByEmail(email);
	}

	/**
	 * Actualitza el perfil d'un usuari amb control d'autorització segons el rol del sol·licitant.
	 * @param id Identificador de l'usuari a modificar
	 * @param nou Noves dades de l'usuari
	 * @param requesterRol Rol de qui sol·licita l'actualització
	 * @param requesterId ID de qui sol·licita l'actualització
	 * @return Usuari actualitzat
	 */
	public Usuari update(String id, Usuari nou, String requesterRol, String requesterId) {
		return catalog.updateUser(id, nou, requesterRol, requesterId);
	}

	/**
	 * Registra una sol·licitud de verificació oficial per a un professional.
	 * @param id Identificador del professional
	 * @return Usuari amb l'estat de sol·licitud actualitzat
	 */
	public Usuari sollicitarVerificacio(String id) {
		return catalog.sollicitarVerificacio(id);
	}

	/**
	 * Aprova o denega la verificació oficial d'un professional (acció restringida a administradors).
	 * @param id Identificador del professional
	 * @param estat Nou estat de verificació (true/false)
	 * @param requesterRol Rol de qui efectua l'acció
	 * @return Usuari actualitzat
	 */
	public Usuari verificarProfessional(String id, boolean estat, String requesterRol) {
		return catalog.verificarProfessional(id, estat, requesterRol);
	}

	/**
	 * Assigna o elimina la fotografia de fons d'un professional.
	 * @param id Identificador de l'usuari
	 * @param foto URL de la imatge de fons
	 * @return Usuari actualitzat
	 */
	public Usuari assignarFoto(String id, String foto) {
		return catalog.assignarFoto(id, foto);
	}

	/**
	 * Elimina un compte d'usuari de la plataforma.
	 * @param id Identificador de l'usuari
	 * @return true si s'ha suprimit correctament
	 */
	public boolean delete(String id) {
		return catalog.deleteUser(id);
	}
}

