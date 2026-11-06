package com.avisa.imatges;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Imatges en una carpeta del disc local (per defecte ~/.avisa/imatges).
 * És l'emmagatzematge per defecte; GCS s'activa amb avisa.imatges.emmagatzematge=gcs.
 */
@Component
@ConditionalOnProperty(name = "avisa.imatges.emmagatzematge", havingValue = "disc", matchIfMissing = true)
public class EmmagatzematgeDisc implements EmmagatzematgeImatges {

	private final Path carpeta;

	public EmmagatzematgeDisc(@Value("${avisa.imatges.directori}") String directori) {
		this.carpeta = Path.of(directori).toAbsolutePath().normalize();
		try {
			Files.createDirectories(carpeta);
		} catch (IOException e) {
			throw new IllegalStateException("No es pot crear la carpeta d'imatges " + carpeta, e);
		}
	}

	private Path cami(String nom) {
		Path p = carpeta.resolve(EmmagatzematgeImatges.exigirNomValid(nom)).normalize();
		if (!p.getParent().equals(carpeta)) {
			throw new IllegalArgumentException("Nom d'imatge no vàlid");
		}
		return p;
	}

	@Override
	public void desar(String nom, byte[] contingut) {
		Path desti = cami(nom);
		try {
			Path temporal = Files.createTempFile(carpeta, ".pujada-", ".tmp");
			Files.write(temporal, contingut);
			Files.move(temporal, desti, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (IOException e) {
			throw new UncheckedIOException("No s'ha pogut desar la imatge", e);
		}
	}

	@Override
	public Optional<byte[]> llegir(String nom) {
		if (!EmmagatzematgeImatges.nomValid(nom)) {
			return Optional.empty();
		}
		Path p = cami(nom);
		try {
			return Files.isRegularFile(p) ? Optional.of(Files.readAllBytes(p)) : Optional.empty();
		} catch (IOException e) {
			throw new UncheckedIOException("No s'ha pogut llegir la imatge", e);
		}
	}

	@Override
	public void esborrar(String nom) {
		try {
			Files.deleteIfExists(cami(nom));
		} catch (IOException e) {
			throw new UncheckedIOException("No s'ha pogut esborrar la imatge", e);
		}
	}
}
