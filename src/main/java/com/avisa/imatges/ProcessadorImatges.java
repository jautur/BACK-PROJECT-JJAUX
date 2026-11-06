package com.avisa.imatges;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Iterator;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Converteix una imatge pujada (JPEG, PNG o WebP) en les dues mides que serveix el backend.
 *
 * <ol>
 *   <li>Rebutja el fitxer si passa de {@code avisa.imatges.max-bytes}.</li>
 *   <li>Reconeix el format pels primers bytes, no pel nom ni pel Content-Type.</li>
 *   <li>Llig només la capçalera i rebutja si l'amplària per l'altura passa de {@code avisa.imatges.max-pixels},
 *       abans de descodificar res (bombes de descompressió).</li>
 *   <li>Descodifica, redueix a 512 i 128 px pel costat llarg (mai amplia) i aplica l'orientació EXIF.</li>
 *   <li>Codifica de nou des dels píxels: JPEG, o PNG si hi ha transparència. Del fitxer original no es copia
 *       cap metadada (EXIF, GPS, XMP, perfil ICC, comentaris); els colors es converteixen a sRGB.</li>
 * </ol>
 */
@Component
public class ProcessadorImatges {

	public static final int MIDA_GRAN = 512;
	public static final int MIDA_PETITA = 128;
	private static final float QUALITAT_JPEG = 0.85f;

	private final long maxBytes;
	private final long maxPixels;

	public ProcessadorImatges(@Value("${avisa.imatges.max-bytes}") long maxBytes,
			@Value("${avisa.imatges.max-pixels}") long maxPixels) {
		if (maxBytes <= 0 || maxPixels <= 0) {
			throw new IllegalStateException("avisa.imatges.max-bytes i avisa.imatges.max-pixels han de ser positius");
		}
		this.maxBytes = maxBytes;
		this.maxPixels = maxPixels;
		// Dins del jar de Spring Boot, ImageIO no sempre troba sol els lectors de TwelveMonkeys (WebP, JPEG CMYK)
		ImageIO.scanForPlugins();
		ImageIO.setUseCache(false);
	}

	public ImatgeProcessada processar(byte[] dades) {
		if (dades == null || dades.length == 0) {
			throw ImatgeNoValidaException.format("La imatge és buida.");
		}
		if (dades.length > maxBytes) {
			throw ImatgeNoValidaException.massaGran("La imatge passa de " + maxBytes / 1024 + " KB.");
		}
		FormatImatge format = FormatImatge.detectar(dades)
			.orElseThrow(() -> ImatgeNoValidaException.format("Només s'accepten imatges JPEG, PNG o WebP."));

		BufferedImage original = descodificar(dades, format);
		int orientacio = orientacioExif(dades);
		boolean potTindreAlfa = original.getColorModel().hasAlpha();

		BufferedImage gran = orientar(reduir(original, MIDA_GRAN, potTindreAlfa), orientacio);
		boolean transparent = potTindreAlfa && teTransparencia(gran);
		BufferedImage petita = reduir(gran, MIDA_PETITA, potTindreAlfa); // gran ja està dreta

		if (transparent) {
			return new ImatgeProcessada("png", codificarPng(gran), codificarPng(petita));
		}
		return new ImatgeProcessada("jpg", codificarJpeg(gran), codificarJpeg(petita));
	}

	private BufferedImage descodificar(byte[] dades, FormatImatge format) {
		Iterator<ImageReader> lectors = ImageIO.getImageReadersByFormatName(format.nomImageIO);
		if (!lectors.hasNext()) {
			throw new IllegalStateException("No hi ha cap lector d'ImageIO per a " + format);
		}
		ImageReader lector = lectors.next();
		try (ImageInputStream entrada = ImageIO.createImageInputStream(new ByteArrayInputStream(dades))) {
			lector.setInput(entrada, true, true);
			int amplaria = lector.getWidth(0);
			int altura = lector.getHeight(0);
			if (amplaria <= 0 || altura <= 0) {
				throw ImatgeNoValidaException.format("La imatge no té dimensions vàlides.");
			}
			if ((long) amplaria * altura > maxPixels) {
				throw ImatgeNoValidaException.massaGran("La imatge fa " + amplaria + "×" + altura
					+ " píxels; el màxim és " + maxPixels + " píxels en total.");
			}
			// Una imatge molt gran es llig ja delmada (sense passar de 2× la mida final), per gastar menys memòria
			ImageReadParam parametres = lector.getDefaultReadParam();
			int delmat = Math.max(1, Math.max(amplaria, altura) / (2 * MIDA_GRAN));
			if (delmat > 1) {
				parametres.setSourceSubsampling(delmat, delmat, 0, 0);
			}
			BufferedImage imatge = lector.read(0, parametres);
			if (imatge == null) {
				throw ImatgeNoValidaException.format("No s'ha pogut llegir la imatge.");
			}
			return imatge;
		} catch (ImatgeNoValidaException e) {
			throw e;
		} catch (IOException | RuntimeException e) {
			// Fitxer truncat o manipulat: els descodificadors poden llançar quasi qualsevol cosa
			throw ImatgeNoValidaException.format("La imatge està malmesa o no es pot llegir.");
		} finally {
			lector.dispose();
		}
	}

	/** Valor EXIF Orientation (1-8); 1 si no n'hi ha o no es pot llegir. */
	static int orientacioExif(byte[] dades) {
		try {
			Metadata metadades = ImageMetadataReader.readMetadata(new ByteArrayInputStream(dades), dades.length);
			for (ExifIFD0Directory dir : metadades.getDirectoriesOfType(ExifIFD0Directory.class)) {
				Integer valor = dir.getInteger(ExifIFD0Directory.TAG_ORIENTATION);
				if (valor != null && valor >= 1 && valor <= 8) {
					return valor;
				}
			}
		} catch (Exception e) {
			// Metadades absents o malmeses: es mostra tal com està
		}
		return 1;
	}

	/** Redueix perquè el costat llarg no passe de {@code mida}; mai amplia. Sempre torna una imatge sRGB nova. */
	private static BufferedImage reduir(BufferedImage font, int mida, boolean alfa) {
		int tipus = alfa ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
		double escala = Math.min(1.0, (double) mida / Math.max(font.getWidth(), font.getHeight()));
		int amplariaFinal = Math.max(1, (int) Math.round(font.getWidth() * escala));
		int alturaFinal = Math.max(1, (int) Math.round(font.getHeight() * escala));

		// Reduir de colp a una mida molt més xicoteta fa aliàsing: es va dividint per la meitat fins prop del final
		BufferedImage actual = font;
		while (actual.getWidth() / 2 >= amplariaFinal && actual.getHeight() / 2 >= alturaFinal) {
			actual = dibuixar(actual, actual.getWidth() / 2, actual.getHeight() / 2, tipus, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		}
		return dibuixar(actual, amplariaFinal, alturaFinal, tipus, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
	}

	private static BufferedImage dibuixar(BufferedImage font, int amplaria, int altura, int tipus, Object interpolacio) {
		BufferedImage desti = new BufferedImage(amplaria, altura, tipus);
		Graphics2D g = desti.createGraphics();
		try {
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, interpolacio);
			g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
			g.drawImage(font, 0, 0, amplaria, altura, null);
		} finally {
			g.dispose();
		}
		return desti;
	}

	/**
	 * Gira o volteja segons l'EXIF Orientation, perquè la imatge es veja dreta sense metadades.
	 * La caixa de reducció és quadrada, així que fer-ho després de reduir dona les mateixes dimensions.
	 */
	static BufferedImage orientar(BufferedImage img, int orientacio) {
		if (orientacio <= 1 || orientacio > 8) {
			return img;
		}
		int w = img.getWidth();
		int h = img.getHeight();
		boolean intercanvia = orientacio >= 5;
		BufferedImage desti = new BufferedImage(intercanvia ? h : w, intercanvia ? w : h, img.getType());
		for (int y = 0; y < desti.getHeight(); y++) {
			for (int x = 0; x < desti.getWidth(); x++) {
				int rgb = switch (orientacio) {
					case 2 -> img.getRGB(w - 1 - x, y);         // volteig horitzontal
					case 3 -> img.getRGB(w - 1 - x, h - 1 - y); // 180°
					case 4 -> img.getRGB(x, h - 1 - y);         // volteig vertical
					case 5 -> img.getRGB(y, x);                 // transposada
					case 6 -> img.getRGB(y, h - 1 - x);         // 90° en sentit horari
					case 7 -> img.getRGB(w - 1 - y, h - 1 - x); // transversa
					default -> img.getRGB(w - 1 - y, x);        // 8: 90° en sentit antihorari
				};
				desti.setRGB(x, y, rgb);
			}
		}
		return desti;
	}

	private static boolean teTransparencia(BufferedImage img) {
		for (int y = 0; y < img.getHeight(); y++) {
			for (int x = 0; x < img.getWidth(); x++) {
				if ((img.getRGB(x, y) >>> 24) != 0xFF) {
					return true;
				}
			}
		}
		return false;
	}

	private static byte[] codificarJpeg(BufferedImage img) {
		BufferedImage rgb = img.getType() == BufferedImage.TYPE_INT_RGB ? img
			: dibuixar(img, img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		ImageWriter escriptor = ImageIO.getImageWritersByFormatName("jpeg").next();
		ByteArrayOutputStream eixida = new ByteArrayOutputStream();
		try (ImageOutputStream flux = ImageIO.createImageOutputStream(eixida)) {
			escriptor.setOutput(flux);
			ImageWriteParam parametres = escriptor.getDefaultWriteParam();
			parametres.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
			parametres.setCompressionQuality(QUALITAT_JPEG);
			// Sense metadades d'entrada: només la capçalera JFIF mínima que posa el codificador
			escriptor.write(null, new IIOImage(rgb, null, null), parametres);
		} catch (IOException e) {
			throw new UncheckedIOException("No s'ha pogut codificar la imatge en JPEG", e);
		} finally {
			escriptor.dispose();
		}
		return eixida.toByteArray();
	}

	private static byte[] codificarPng(BufferedImage img) {
		ByteArrayOutputStream eixida = new ByteArrayOutputStream();
		try {
			if (!ImageIO.write(img, "png", eixida)) {
				throw new IllegalStateException("No hi ha cap escriptor PNG");
			}
		} catch (IOException e) {
			throw new UncheckedIOException("No s'ha pogut codificar la imatge en PNG", e);
		}
		return eixida.toByteArray();
	}
}
