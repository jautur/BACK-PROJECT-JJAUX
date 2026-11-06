package com.avisa.imatges;

import jakarta.servlet.MultipartConfigElement;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Límit del multipart lligat a avisa.imatges.max-bytes: el servidor talla la pujada (413) sense llegir-la sencera.
 * Per defecte Spring Boot només admet 1 MB, i hauria rebutjat fotos que el processador sí que accepta.
 */
@Configuration
public class ImatgesConfig {

	/** Marge per a les capçaleres i el límit de cada part del multipart. */
	private static final long MARGE_PETICIO = 64 * 1024;

	@Bean
	MultipartConfigElement multipartConfigElement(@Value("${avisa.imatges.max-bytes}") long maxBytes) {
		int llindarMemoria = (int) Math.min(maxBytes, 1024 * 1024);
		return new MultipartConfigElement("", maxBytes, maxBytes + MARGE_PETICIO, llindarMemoria);
	}
}
