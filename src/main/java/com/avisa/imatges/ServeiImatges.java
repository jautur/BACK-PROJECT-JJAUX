package com.avisa.imatges;

import java.io.UncheckedIOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Foto de perfil i logotip dels usuaris: processa, desa, apunta a la base i esborra els fitxers que ja no es fan servir.
 *
 * Cada imatge són dos fitxers amb el mateix UUID: {@code <uuid>.jpg} (512 px) i {@code <uuid>-petita.jpg} (128 px),
 * o {@code .png} si té transparència. La base només guarda la URL de la gran; la petita es deriva amb {@link #urlPetita}.
 */
@Service
public class ServeiImatges {

	public static final String PREFIX_URL = "/api/imatges/";

	private static final Logger log = LoggerFactory.getLogger(ServeiImatges.class);

	/** URLs de la imatge acabada de pujar. */
	public record ImatgePujada(String url, String urlPetita) {
	}

	private final ProcessadorImatges processador;
	private final EmmagatzematgeImatges magatzem;
	private final JdbcTemplate jdbc;
	private final TransactionTemplate transaccio;

	public ServeiImatges(ProcessadorImatges processador, EmmagatzematgeImatges magatzem, JdbcTemplate jdbc,
			PlatformTransactionManager transaccions) {
		this.processador = processador;
		this.magatzem = magatzem;
		this.jdbc = jdbc;
		this.transaccio = new TransactionTemplate(transaccions);
	}

	/**
	 * Processa la imatge i la posa com a foto o logo de l'usuari; esborra la que tenia abans.
	 *
	 * @return les URLs noves, o buit si l'usuari no existeix
	 * @throws ImatgeNoValidaException si el fitxer no és una imatge acceptada o és massa gran
	 */
	public Optional<ImatgePujada> pujar(UUID usuari, TipusImatge tipus, byte[] dades) {
		ImatgeProcessada imatge = processador.processar(dades);
		String base = UUID.randomUUID().toString();
		String url = PREFIX_URL + base + "." + imatge.extensio();
		// Primer la petita: si existeix la gran, sempre existeix la petita
		magatzem.desar(nom(urlPetita(url)), imatge.petita());
		magatzem.desar(nom(url), imatge.gran());

		List<String> anterior;
		try {
			anterior = substituir(usuari, tipus, url);
		} catch (RuntimeException e) {
			esborrarFitxers(url);
			throw e;
		}
		if (anterior.isEmpty()) {
			esborrarFitxers(url);
			return Optional.empty();
		}
		esborrarFitxers(anterior.get(0));
		return Optional.of(new ImatgePujada(url, urlPetita(url)));
	}

	/**
	 * Lleva la foto o el logo de l'usuari i n'esborra els fitxers.
	 *
	 * @return fals si l'usuari no existeix
	 */
	public boolean esborrar(UUID usuari, TipusImatge tipus) {
		List<String> anterior = substituir(usuari, tipus, null);
		if (anterior.isEmpty()) {
			return false;
		}
		esborrarFitxers(anterior.get(0));
		return true;
	}

	/** Fitxer d'una imatge pel seu nom ({@code <uuid>.jpg}, {@code <uuid>-petita.png}...). Buit si el nom no és vàlid. */
	public Optional<byte[]> llegir(String nom) {
		return magatzem.llegir(nom);
	}

	/**
	 * Esborra els dos fitxers d'una URL de la base (foto_url o logo_url). No fa res amb null ni amb URLs alienes.
	 * Un error de disc només es registra: la base ja no hi apunta i no ha de fer fallar la petició.
	 */
	public void esborrarFitxers(String url) {
		if (url == null || !url.startsWith(PREFIX_URL) || !EmmagatzematgeImatges.nomValid(nom(url))) {
			return;
		}
		for (String nom : new String[] {nom(url), nom(urlPetita(url))}) {
			try {
				magatzem.esborrar(nom);
			} catch (UncheckedIOException | IllegalArgumentException e) {
				log.warn("No s'ha pogut esborrar la imatge {}", nom, e);
			}
		}
	}

	/** {@code /api/imatges/<uuid>.jpg} → {@code /api/imatges/<uuid>-petita.jpg}. */
	public static String urlPetita(String url) {
		if (url == null || url.contains("-petita.")) {
			return url;
		}
		return url.replaceFirst("\\.(jpg|png)$", "-petita.$1");
	}

	/**
	 * Canvia la columna dins d'una transacció, amb la fila bloquejada: dues pujades alhora no poden deixar
	 * cap fitxer orfe ni esborrar el que acaba de posar l'altra.
	 *
	 * @return buit si l'usuari no existeix; si no, una llista amb la URL anterior (que pot ser null)
	 */
	private List<String> substituir(UUID usuari, TipusImatge tipus, String url) {
		return transaccio.execute(estat -> {
			List<String> anterior = jdbc.query("SELECT " + tipus.columna + " FROM usuaris WHERE id = CAST(? AS UUID) FOR UPDATE",
				(rs, fila) -> rs.getString(1), usuari.toString());
			if (!anterior.isEmpty()) {
				jdbc.update("UPDATE usuaris SET " + tipus.columna + " = ? WHERE id = CAST(? AS UUID)", url, usuari.toString());
			}
			return anterior;
		});
	}

	private static String nom(String url) {
		return url.substring(PREFIX_URL.length());
	}
}
