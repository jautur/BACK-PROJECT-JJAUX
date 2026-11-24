package com.avisa.controller;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.LongSupplier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

/**
 * Paginació opcional dels llistats: sense {@code size} es retorna la llista completa, com sempre,
 * perquè els clients que no la demanen no es trenquen. Amb {@code size} (1..{@value #MIDA_MAXIMA})
 * i {@code page} (des de 0) es retorna només aquella pàgina, i el total va a la capçalera {@value #CAPCALERA_TOTAL}.
 */
final class Paginacio {

	static final int MIDA_MAXIMA = 100;
	static final String CAPCALERA_TOTAL = "X-Total-Count";

	private Paginacio() {
	}

	/** Troba la pàgina d'una llista que ja és en memòria (límit i desplaçament). */
	static <T> List<T> tallar(List<T> llista, int limit, int offset) {
		if (offset >= llista.size()) {
			return List.of();
		}
		return llista.subList(offset, (int) Math.min((long) offset + limit, llista.size()));
	}

	/**
	 * @param page pàgina demanada (null = 0)
	 * @param size mida de pàgina (null = sense paginar)
	 * @param completa llista sencera, si no es pagina
	 * @param pagina pàgina concreta a partir de (límit, desplaçament)
	 * @param total nombre total d'elements
	 */
	static <T> ResponseEntity<List<T>> resposta(Integer page, Integer size, java.util.function.Supplier<List<T>> completa,
			BiFunction<Integer, Integer, List<T>> pagina, LongSupplier total) {
		if (size == null) {
			if (page != null) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'page' només es pot usar amb 'size'.");
			}
			return ResponseEntity.ok(completa.get());
		}
		int pagTriada = page == null ? 0 : page;
		if (size < 1 || size > MIDA_MAXIMA || pagTriada < 0) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"'size' ha d'estar entre 1 i " + MIDA_MAXIMA + " i 'page' no pot ser negativa.");
		}
		List<T> elements = pagina.apply(size, Math.multiplyExact(pagTriada, size));
		return ResponseEntity.ok().header(CAPCALERA_TOTAL, String.valueOf(total.getAsLong())).body(elements);
	}
}
