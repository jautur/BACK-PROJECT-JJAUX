package com.avisa.imatges;

/** Les imatges que pot tindre un usuari. La columna és fixa: mai ve de la petició. */
public enum TipusImatge {

	FOTO("foto_url"),
	LOGO("logo_url");

	final String columna;

	TipusImatge(String columna) {
		this.columna = columna;
	}
}
