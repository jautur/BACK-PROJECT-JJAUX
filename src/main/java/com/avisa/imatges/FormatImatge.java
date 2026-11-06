package com.avisa.imatges;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;

/**
 * Formats d'entrada acceptats, reconeguts pels primers bytes del fitxer (mai pel nom ni pel Content-Type,
 * que els tria qui puja). Qualsevol altre (SVG, GIF, HEIC, PDF...) es rebutja.
 */
enum FormatImatge {

	JPEG("jpeg"),
	PNG("png"),
	WEBP("webp");

	private static final byte[] PNG_SIGNATURA = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};

	/** Nom del format per a ImageIO.getImageReadersByFormatName. */
	final String nomImageIO;

	FormatImatge(String nomImageIO) {
		this.nomImageIO = nomImageIO;
	}

	static Optional<FormatImatge> detectar(byte[] d) {
		if (d.length >= 3 && (d[0] & 0xFF) == 0xFF && (d[1] & 0xFF) == 0xD8 && (d[2] & 0xFF) == 0xFF) {
			return Optional.of(JPEG);
		}
		if (d.length >= 8 && Arrays.equals(d, 0, 8, PNG_SIGNATURA, 0, 8)) {
			return Optional.of(PNG);
		}
		if (d.length >= 12 && ascii(d, 0, 4).equals("RIFF") && ascii(d, 8, 12).equals("WEBP")) {
			return Optional.of(WEBP);
		}
		return Optional.empty();
	}

	private static String ascii(byte[] d, int des, int fins) {
		return new String(d, des, fins - des, StandardCharsets.US_ASCII);
	}
}
