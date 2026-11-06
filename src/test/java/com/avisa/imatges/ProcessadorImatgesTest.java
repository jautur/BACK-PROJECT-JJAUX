package com.avisa.imatges;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.GpsDirectory;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.zip.CRC32;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ProcessadorImatgesTest {

	private static final long MAX_BYTES = 5L * 1024 * 1024;
	private static final long MAX_PIXELS = 40_000_000L;

	private final ProcessadorImatges processador = new ProcessadorImatges(MAX_BYTES, MAX_PIXELS);

	// ---------- Mides i format d'eixida ----------

	@Test
	@DisplayName("Una foto gran es redueix a 512 i 128 px pel costat llarg, mantenint la proporció")
	void reduixALesDuesMides() throws IOException {
		ImatgeProcessada r = processador.processar(jpeg(meitats(2000, 1000, Color.RED, Color.BLUE)));

		assertThat(r.extensio()).isEqualTo("jpg");
		assertThat(dimensions(r.gran())).containsExactly(512, 256);
		assertThat(dimensions(r.petita())).containsExactly(128, 64);
		assertThat(color(r.gran(), 50, 128)).satisfies(c -> assertProp(c, Color.RED));
		assertThat(color(r.gran(), 460, 128)).satisfies(c -> assertProp(c, Color.BLUE));
	}

	@Test
	@DisplayName("Una imatge xicoteta no s'amplia")
	void noAmplia() throws IOException {
		ImatgeProcessada r = processador.processar(png(meitats(100, 50, Color.RED, Color.BLUE)));

		assertThat(dimensions(r.gran())).containsExactly(100, 50);
		assertThat(dimensions(r.petita())).containsExactly(100, 50);
	}

	@Test
	@DisplayName("Un PNG amb transparència es queda en PNG i conserva l'alfa")
	void pngTransparent() throws IOException {
		BufferedImage img = new BufferedImage(40, 40, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		g.setColor(Color.GREEN);
		g.fillRect(20, 0, 20, 40);
		g.dispose();

		ImatgeProcessada r = processador.processar(png(img));

		assertThat(r.extensio()).isEqualTo("png");
		assertThat(FormatImatge.detectar(r.gran())).contains(FormatImatge.PNG);
		BufferedImage gran = ImageIO.read(new ByteArrayInputStream(r.gran()));
		assertThat(gran.getRGB(5, 20) >>> 24).isZero();
		assertThat(gran.getRGB(35, 20) >>> 24).isEqualTo(0xFF);
	}

	@Test
	@DisplayName("Un PNG amb canal alfa però tot opac passa a JPEG")
	void pngOpacPassaAJpeg() throws IOException {
		BufferedImage img = new BufferedImage(40, 40, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		g.setColor(Color.ORANGE);
		g.fillRect(0, 0, 40, 40);
		g.dispose();

		ImatgeProcessada r = processador.processar(png(img));

		assertThat(r.extensio()).isEqualTo("jpg");
		assertThat(FormatImatge.detectar(r.gran())).contains(FormatImatge.JPEG);
		assertThat(FormatImatge.detectar(r.petita())).contains(FormatImatge.JPEG);
	}

	// ---------- WebP ----------

	@Test
	@DisplayName("Llig WebP amb pèrdua, n'aplica l'orientació EXIF i n'esborra les metadades")
	void webpGirat() throws IOException {
		byte[] original = recurs("girada-6.webp");
		assertThat(ProcessadorImatges.orientacioExif(original)).isEqualTo(6);

		ImatgeProcessada r = processador.processar(original);

		assertThat(r.extensio()).isEqualTo("jpg");
		assertThat(dimensions(r.gran())).containsExactly(20, 40);
		assertProp(color(r.gran(), 10, 8), Color.RED);
		assertProp(color(r.gran(), 10, 32), Color.BLUE);
		assertSenseMetadades(r.gran());
		assertThat(conte(r.gran(), "Artista Secret")).isFalse();
	}

	@Test
	@DisplayName("Llig WebP sense pèrdua amb transparència")
	void webpTransparent() throws IOException {
		ImatgeProcessada r = processador.processar(recurs("transparent.webp"));

		assertThat(r.extensio()).isEqualTo("png");
		BufferedImage gran = ImageIO.read(new ByteArrayInputStream(r.gran()));
		assertThat(gran.getRGB(5, 15) >>> 24).isZero();
		assertThat(gran.getRGB(25, 15) >>> 24).isEqualTo(0xFF);
	}

	// ---------- Orientació i metadades ----------

	@Nested
	@DisplayName("Orientació EXIF")
	class Orientacio {

		@ParameterizedTest(name = "orientació {0}")
		@DisplayName("Cada valor de l'1 al 8 gira o volteja com diu l'estàndard EXIF")
		@CsvSource({
			// orientació, ample, alt, roig x,y, verd x,y  (a l'original, 3×2: roig a (0,0) i verd a (1,0))
			"1, 3, 2, 0, 0, 1, 0",
			"2, 3, 2, 2, 0, 1, 0",
			"3, 3, 2, 2, 1, 1, 1",
			"4, 3, 2, 0, 1, 1, 1",
			"5, 2, 3, 0, 0, 0, 1",
			"6, 2, 3, 1, 0, 1, 1",
			"7, 2, 3, 1, 2, 1, 1",
			"8, 2, 3, 0, 2, 0, 1",
		})
		void totesLesOrientacions(int orientacio, int ample, int alt, int rx, int ry, int vx, int vy) {
			BufferedImage img = new BufferedImage(3, 2, BufferedImage.TYPE_INT_RGB);
			img.setRGB(0, 0, Color.RED.getRGB());
			img.setRGB(1, 0, Color.GREEN.getRGB());

			BufferedImage r = ProcessadorImatges.orientar(img, orientacio);

			assertThat(r.getWidth()).isEqualTo(ample);
			assertThat(r.getHeight()).isEqualTo(alt);
			assertThat(r.getRGB(rx, ry)).isEqualTo(Color.RED.getRGB());
			assertThat(r.getRGB(vx, vy)).isEqualTo(Color.GREEN.getRGB());
		}

		@Test
		@DisplayName("Una foto JPEG feta amb el mòbil girat (orientació 6) es desa dreta")
		void jpegOrientacio6() throws IOException {
			byte[] foto = ambExif(jpeg(meitats(40, 20, Color.RED, Color.BLUE)), 6);
			assertThat(ProcessadorImatges.orientacioExif(foto)).isEqualTo(6);

			ImatgeProcessada r = processador.processar(foto);

			assertThat(dimensions(r.gran())).containsExactly(20, 40);
			assertProp(color(r.gran(), 10, 8), Color.RED);
			assertProp(color(r.gran(), 10, 32), Color.BLUE);
			assertThat(dimensions(r.petita())).containsExactly(20, 40);
		}

		@Test
		@DisplayName("Orientació 8: gir en sentit antihorari")
		void jpegOrientacio8() throws IOException {
			ImatgeProcessada r = processador.processar(ambExif(jpeg(meitats(40, 20, Color.RED, Color.BLUE)), 8));

			assertThat(dimensions(r.gran())).containsExactly(20, 40);
			assertProp(color(r.gran(), 10, 8), Color.BLUE);
			assertProp(color(r.gran(), 10, 32), Color.RED);
		}
	}

	@Test
	@DisplayName("No queda cap metadada de l'original: ni EXIF, ni GPS, ni l'orientació")
	void esborraLesMetadades() throws Exception {
		byte[] foto = ambExif(jpeg(meitats(600, 300, Color.RED, Color.BLUE)), 3);
		Metadata abans = ImageMetadataReader.readMetadata(new ByteArrayInputStream(foto), foto.length);
		assertThat(abans.getFirstDirectoryOfType(GpsDirectory.class)).isNotNull();

		ImatgeProcessada r = processador.processar(foto);

		assertSenseMetadades(r.gran());
		assertSenseMetadades(r.petita());
		assertThat(conte(r.gran(), "Exif")).isFalse();
	}

	// ---------- Rebuigs ----------

	@Test
	@DisplayName("Rebutja el que no és JPEG, PNG ni WebP pel contingut, siga quin siga el nom")
	void rebutjaFormatsNoAdmesos() {
		byte[][] dolents = {
			"<svg xmlns=\"http://www.w3.org/2000/svg\" onload=\"alert(1)\"/>".getBytes(StandardCharsets.UTF_8),
			"<?xml version=\"1.0\"?><svg/>".getBytes(StandardCharsets.UTF_8),
			"GIF89a\u0001\u0000\u0001\u0000".getBytes(StandardCharsets.ISO_8859_1),
			"%PDF-1.7".getBytes(StandardCharsets.US_ASCII),
			"RIFF\u0000\u0000\u0000\u0000WAVEfmt ".getBytes(StandardCharsets.ISO_8859_1),
			{0x00, 0x00, 0x00, 0x18, 'f', 't', 'y', 'p', 'h', 'e', 'i', 'c'},
			{1, 2, 3},
		};
		for (byte[] dolent : dolents) {
			assertThatThrownBy(() -> processador.processar(dolent))
				.isInstanceOf(ImatgeNoValidaException.class)
				.satisfies(e -> assertThat(((ImatgeNoValidaException) e).massaGran()).isFalse());
		}
	}

	@Test
	@DisplayName("Rebutja fitxers buits")
	void rebutjaBuit() {
		assertThatThrownBy(() -> processador.processar(new byte[0])).isInstanceOf(ImatgeNoValidaException.class);
		assertThatThrownBy(() -> processador.processar(null)).isInstanceOf(ImatgeNoValidaException.class);
	}

	@Test
	@DisplayName("Rebutja fitxers amb la capçalera bona però el contingut malmés o truncat")
	void rebutjaMalmesos() throws IOException {
		byte[] bo = jpeg(meitats(200, 100, Color.RED, Color.BLUE));
		byte[] brossa = new byte[200];
		Arrays.fill(brossa, (byte) 0x41);
		brossa[0] = (byte) 0xFF;
		brossa[1] = (byte) 0xD8;
		brossa[2] = (byte) 0xFF;
		byte[] png = png(meitats(200, 100, Color.RED, Color.BLUE));

		for (byte[] dolent : new byte[][] {brossa, Arrays.copyOf(bo, 30), Arrays.copyOf(png, 40)}) {
			assertThatThrownBy(() -> processador.processar(dolent))
				.isInstanceOf(ImatgeNoValidaException.class)
				.satisfies(e -> assertThat(((ImatgeNoValidaException) e).massaGran()).isFalse());
		}
	}

	@Test
	@DisplayName("Rebutja per mida en bytes abans de mirar el contingut")
	void rebutjaPerBytes() throws IOException {
		ProcessadorImatges petit = new ProcessadorImatges(1000, MAX_PIXELS);
		byte[] foto = png(soroll(100, 100));
		assertThat(foto.length).isGreaterThan(1000);

		assertThatThrownBy(() -> petit.processar(foto))
			.isInstanceOf(ImatgeNoValidaException.class)
			.satisfies(e -> assertThat(((ImatgeNoValidaException) e).massaGran()).isTrue());
	}

	@Test
	@DisplayName("Rebutja per píxels segons el límit configurat")
	void rebutjaPerPixels() throws IOException {
		ProcessadorImatges petit = new ProcessadorImatges(MAX_BYTES, 399);
		byte[] foto = png(meitats(20, 20, Color.RED, Color.BLUE));

		assertThatThrownBy(() -> petit.processar(foto))
			.isInstanceOf(ImatgeNoValidaException.class)
			.satisfies(e -> assertThat(((ImatgeNoValidaException) e).massaGran()).isTrue());
		assertThat(new ProcessadorImatges(MAX_BYTES, 400).processar(foto).extensio()).isEqualTo("jpg");
	}

	@Test
	@DisplayName("Una bomba de descompressió (PNG de 50000×50000 en pocs bytes) es rebutja sense descodificar-la")
	void rebutjaBombaDeDescompressio() throws IOException {
		byte[] bomba = pngNomesCapcalera(50_000, 50_000);
		assertThat(bomba.length).isLessThan(100);

		assertThatThrownBy(() -> processador.processar(bomba))
			.isInstanceOf(ImatgeNoValidaException.class)
			.hasMessageContaining("50000×50000")
			.satisfies(e -> assertThat(((ImatgeNoValidaException) e).massaGran()).isTrue());
	}

	@Test
	@DisplayName("No arrenca amb límits no positius")
	void limitsPositius() {
		assertThatThrownBy(() -> new ProcessadorImatges(0, MAX_PIXELS)).isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> new ProcessadorImatges(MAX_BYTES, -1)).isInstanceOf(IllegalStateException.class);
	}

	// ---------- Ajudes ----------

	private static BufferedImage meitats(int w, int h, Color esquerra, Color dreta) {
		BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = img.createGraphics();
		g.setColor(esquerra);
		g.fillRect(0, 0, w / 2, h);
		g.setColor(dreta);
		g.fillRect(w / 2, 0, w - w / 2, h);
		g.dispose();
		return img;
	}

	private static BufferedImage soroll(int w, int h) {
		BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		java.util.Random r = new java.util.Random(42);
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				img.setRGB(x, y, r.nextInt());
			}
		}
		return img;
	}

	private static byte[] jpeg(BufferedImage img) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ImageIO.write(img, "jpeg", out);
		return out.toByteArray();
	}

	private static byte[] png(BufferedImage img) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ImageIO.write(img, "png", out);
		return out.toByteArray();
	}

	private static byte[] recurs(String nom) throws IOException {
		try (InputStream in = ProcessadorImatgesTest.class.getResourceAsStream("/imatges/" + nom)) {
			return in.readAllBytes();
		}
	}

	/** Afig a un JPEG un segment APP1 EXIF amb l'orientació donada i una ubicació GPS, com fa un mòbil. */
	private static byte[] ambExif(byte[] jpeg, int orientacio) throws IOException {
		ByteBuffer tiff = ByteBuffer.allocate(56); // big-endian per defecte
		tiff.put(new byte[] {'M', 'M', 0, 42}).putInt(8);
		// IFD0 a l'offset 8: 2 entrades
		tiff.putShort((short) 2);
		tiff.putShort((short) 0x0112).putShort((short) 3).putInt(1).putShort((short) orientacio).putShort((short) 0);
		tiff.putShort((short) 0x8825).putShort((short) 4).putInt(1).putInt(38); // punter al GPS IFD
		tiff.putInt(0);
		// GPS IFD a l'offset 38: GPSLatitudeRef = "N"
		tiff.putShort((short) 1);
		tiff.putShort((short) 0x0001).putShort((short) 2).putInt(2).put(new byte[] {'N', 0, 0, 0});
		tiff.putInt(0);

		ByteArrayOutputStream app1 = new ByteArrayOutputStream();
		app1.write("Exif\0\0".getBytes(StandardCharsets.US_ASCII));
		app1.write(tiff.array(), 0, tiff.position());

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		DataOutputStream d = new DataOutputStream(out);
		d.write(jpeg, 0, 2); // SOI
		d.writeShort(0xFFE1);
		d.writeShort(app1.size() + 2);
		app1.writeTo(d);
		d.write(jpeg, 2, jpeg.length - 2);
		return out.toByteArray();
	}

	/** PNG vàlid fins a la capçalera IHDR, que diu unes dimensions enormes, i res més. */
	private static byte[] pngNomesCapcalera(int w, int h) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		DataOutputStream d = new DataOutputStream(out);
		d.write(new byte[] {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'});
		ByteBuffer ihdr = ByteBuffer.allocate(17).put("IHDR".getBytes(StandardCharsets.US_ASCII))
			.putInt(w).putInt(h).put((byte) 8).put((byte) 2).put((byte) 0).put((byte) 0).put((byte) 0);
		CRC32 crc = new CRC32();
		crc.update(ihdr.array());
		d.writeInt(13);
		d.write(ihdr.array());
		d.writeInt((int) crc.getValue());
		return out.toByteArray();
	}

	private static int[] dimensions(byte[] img) throws IOException {
		BufferedImage b = ImageIO.read(new ByteArrayInputStream(img));
		return new int[] {b.getWidth(), b.getHeight()};
	}

	private static Color color(byte[] img, int x, int y) throws IOException {
		return new Color(ImageIO.read(new ByteArrayInputStream(img)).getRGB(x, y));
	}

	/** Prop del color esperat: el JPEG perd una mica en cada codificació. */
	private static void assertProp(Color real, Color esperat) {
		int dif = Math.abs(real.getRed() - esperat.getRed()) + Math.abs(real.getGreen() - esperat.getGreen())
			+ Math.abs(real.getBlue() - esperat.getBlue());
		assertThat(dif).as("%s prop de %s", real, esperat).isLessThan(60);
	}

	private static void assertSenseMetadades(byte[] img) {
		try {
			Metadata m = ImageMetadataReader.readMetadata(new ByteArrayInputStream(img), img.length);
			assertThat(m.getFirstDirectoryOfType(ExifIFD0Directory.class)).as("EXIF").isNull();
			assertThat(m.getFirstDirectoryOfType(GpsDirectory.class)).as("GPS").isNull();
			assertThat(ProcessadorImatges.orientacioExif(img)).isEqualTo(1);
		} catch (Exception e) {
			throw new AssertionError(e);
		}
	}

	private static boolean conte(byte[] dades, String text) {
		return new String(dades, StandardCharsets.ISO_8859_1).contains(text);
	}
}
