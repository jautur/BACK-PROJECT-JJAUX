package com.avisa.imatges;

import java.util.Optional;

/**
 * On es guarden els fitxers d'imatge ja processats. Els noms els genera el backend
 * (UUID + sufix + extensió) i mai venen de l'usuari.
 */
public interface EmmagatzematgeImatges {

	/** Desa (o sobreescriu) el fitxer amb aquest nom. */
	void desar(String nom, byte[] contingut);

	/** Llig el fitxer, si existeix. */
	Optional<byte[]> llegir(String nom);

	/** Esborra el fitxer; no fa res si no existeix. */
	void esborrar(String nom);

	/** Noms vàlids: el que genera NomsImatge. Qualsevol altre (camins, ../, etc.) es rebutja. */
	static boolean nomValid(String nom) {
		return nom != null && nom.matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}(-petita)?\\.(jpg|png)$");
	}

	static String exigirNomValid(String nom) {
		if (!nomValid(nom)) {
			throw new IllegalArgumentException("Nom d'imatge no vàlid");
		}
		return nom;
	}
}
