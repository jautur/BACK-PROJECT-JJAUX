package com.avisa.controller;

import com.avisa.imatges.ServeiImatges;
import com.avisa.imatges.ServeiImatges.ImatgePujada;
import com.avisa.imatges.TipusImatge;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.Principal;
import java.time.Duration;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * Foto de perfil i logotip.
 *
 * <ul>
 *   <li>Pujar la foto: només el mateix usuari, de qualsevol rol.</li>
 *   <li>Pujar el logotip: només el mateix usuari, i ha de ser professional.</li>
 *   <li>Esborrar-les: el mateix usuari o un administrador (moderació). L'administrador no en pot pujar en nom d'un altre.</li>
 *   <li>Llegir-les ({@code GET /api/imatges/{nom}}): públic, perquè un {@code <img src>} no envia el token.
 *       Els noms són UUID aleatoris i no es poden endevinar ni llistar.</li>
 * </ul>
 *
 * En pujar-ne o esborrar-ne una, els fitxers de l'anterior s'esborren del disc.
 */
@RestController
@Tag(name = "Imatges", description = "Foto de perfil i logotip: pujar, esborrar i servir")
public class ImatgeController {

	private static final Pattern UUID_CANONIC = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

	/** Els noms no canvien mai de contingut: es poden guardar en memòria cau per sempre. */
	private static final CacheControl CACHE = CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable();

	private final ServeiImatges servei;

	public ImatgeController(ServeiImatges servei) {
		this.servei = servei;
	}

	@PostMapping(path = "/api/users/{id}/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(summary = "Pujar la foto de perfil", description = "Camp multipart 'fitxer' (JPEG, PNG o WebP); només el mateix usuari")
	public ImatgePujada pujarFoto(@PathVariable String id, @RequestParam("fitxer") MultipartFile fitxer, Principal principal) {
		exigirPropi(principal, id);
		return pujar(id, TipusImatge.FOTO, fitxer);
	}

	@PostMapping(path = "/api/users/{id}/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(summary = "Pujar el logotip", description = "Camp multipart 'fitxer' (JPEG, PNG o WebP); només el mateix professional")
	public ImatgePujada pujarLogo(@PathVariable String id, @RequestParam("fitxer") MultipartFile fitxer, Principal principal) {
		exigirPropi(principal, id);
		if (!Sollicitant.rol(principal).filter("PROFESSIONAL"::equalsIgnoreCase).isPresent()) {
			throw new SecurityException("Només un professional pot tindre logotip.");
		}
		return pujar(id, TipusImatge.LOGO, fitxer);
	}

	@DeleteMapping("/api/users/{id}/foto")
	@Operation(summary = "Esborrar la foto de perfil", description = "El mateix usuari o un administrador")
	public ResponseEntity<Void> esborrarFoto(@PathVariable String id, Principal principal) {
		return esborrar(id, TipusImatge.FOTO, principal);
	}

	@DeleteMapping("/api/users/{id}/logo")
	@Operation(summary = "Esborrar el logotip", description = "El mateix usuari o un administrador")
	public ResponseEntity<Void> esborrarLogo(@PathVariable String id, Principal principal) {
		return esborrar(id, TipusImatge.LOGO, principal);
	}

	@GetMapping("/api/imatges/{nom}")
	@Operation(summary = "Servir una imatge", description = "Públic. Noms <uuid>.jpg/png (512 px) i <uuid>-petita.jpg/png (128 px)")
	public ResponseEntity<byte[]> servir(@PathVariable String nom) {
		return servei.llegir(nom)
			.map(dades -> ResponseEntity.ok()
				.contentType(nom.endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG)
				.cacheControl(CACHE)
				// Encara que algú aconseguira colar-hi un altre contingut, el navegador no l'executaria
				.header("X-Content-Type-Options", "nosniff")
				.header("Content-Security-Policy", "default-src 'none'; sandbox")
				.header(HttpHeaders.CONTENT_DISPOSITION, "inline")
				.body(dades))
			.orElseGet(() -> ResponseEntity.notFound().build());
	}

	private ImatgePujada pujar(String id, TipusImatge tipus, MultipartFile fitxer) {
		byte[] dades;
		try {
			dades = fitxer.getBytes();
		} catch (IOException e) {
			throw new UncheckedIOException("No s'ha pogut llegir el fitxer pujat", e);
		}
		return servei.pujar(uuid(id), tipus, dades).orElseThrow(UsuariNoTrobat::new);
	}

	private ResponseEntity<Void> esborrar(String id, TipusImatge tipus, Principal principal) {
		Sollicitant.exigirUnDelsOAdmin(principal, id);
		if (!servei.esborrar(uuid(id), tipus)) {
			throw new UsuariNoTrobat();
		}
		return ResponseEntity.noContent().build();
	}

	/** Sense sessió, 401; amb la d'un altre (també d'un administrador), 403. */
	private static void exigirPropi(Principal principal, String id) {
		if (!Sollicitant.id(principal).equalsIgnoreCase(id)) {
			throw new SecurityException("Només pots canviar les teues pròpies imatges.");
		}
	}

	private static UUID uuid(String id) {
		if (id == null || !UUID_CANONIC.matcher(id).matches()) {
			throw new UsuariNoTrobat();
		}
		return UUID.fromString(id);
	}

	private static final class UsuariNoTrobat extends ResponseStatusException {
		UsuariNoTrobat() {
			super(HttpStatus.NOT_FOUND, "Usuari no trobat.");
		}
	}
}
