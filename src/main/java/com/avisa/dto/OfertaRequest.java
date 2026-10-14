package com.avisa.dto;

public record OfertaRequest(String tascaId, String professional, String categoria, String price, String note, String estat) {
	public OfertaRequest(String tascaId, String professional, String price, String note, String estat) {
		this(tascaId, professional, null, price, note, estat);
	}
}

