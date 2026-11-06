package com.avisa.imatges;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EmmagatzematgeDiscTest {

	@TempDir
	Path carpeta;

	@Test
	@DisplayName("Desa, llig i esborra pel nom")
	void cicleComplet() {
		EmmagatzematgeDisc disc = new EmmagatzematgeDisc(carpeta.toString());
		String nom = UUID.randomUUID() + ".jpg";

		disc.desar(nom, new byte[] {1, 2, 3});
		assertThat(disc.llegir(nom)).contains(new byte[] {1, 2, 3});

		disc.esborrar(nom);
		assertThat(disc.llegir(nom)).isEmpty();
		disc.esborrar(nom);
	}

	@Test
	@DisplayName("Rebutja noms que no són els generats pel backend: camins, extensions, SVG")
	void rebutjaNomsEstranys() throws Exception {
		Files.writeString(carpeta.resolveSibling("secret.txt"), "no");
		EmmagatzematgeDisc disc = new EmmagatzematgeDisc(carpeta.toString());

		for (String dolent : new String[] {"../secret.txt", "..\\secret.txt", "foto.jpg", UUID.randomUUID() + ".svg",
			UUID.randomUUID() + ".jpg/../../x", "/etc/passwd", ""}) {
			assertThat(disc.llegir(dolent)).isEmpty();
			assertThatThrownBy(() -> disc.desar(dolent, new byte[] {1})).isInstanceOf(IllegalArgumentException.class);
			assertThatThrownBy(() -> disc.esborrar(dolent)).isInstanceOf(IllegalArgumentException.class);
		}
		assertThat(EmmagatzematgeImatges.nomValid(UUID.randomUUID() + "-petita.png")).isTrue();
	}
}
