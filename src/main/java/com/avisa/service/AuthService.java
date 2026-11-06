package com.avisa.service;

import com.avisa.config.JwtService;
import com.avisa.dto.DemanaRecuperacioRequest;
import com.avisa.dto.LoginRequest;
import com.avisa.dto.RecuperacioResponse;
import com.avisa.dto.RegisterRequest;
import com.avisa.dto.RestablirContrasenyaRequest;
import com.avisa.model.Usuari;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Service;

/**
 * Servei de lògica de negoci per a l'autenticació i gestió de sessions d'usuaris.
 * Coordina la verificació de credencials, registre de nous comptes, emissió de tokens JWT
 * i el restabliment de contrasenya amb codis temporals d'un sol ús.
 *
 * Recuperació de contrasenya:
 * - La resposta mai porta el codi ni el correu, i diu el mateix existisca o no el compte.
 * - El codi només s'escriu al log, i només amb el perfil dev (no hi ha enviament de correu).
 * - Caduca als 15 minuts i queda invalidat després de 5 intents fallits.
 * - Es guarda per usuari: un cop usat s'esborra, s'haja identificat l'usuari per correu, nom o ID.
 */
@Service
public class AuthService {

	private static final Logger log = LoggerFactory.getLogger(AuthService.class);

	static final Duration VALIDESA_CODI = Duration.ofMinutes(15);
	static final int INTENTS_MAXIMS = 5;

	static final String MISSATGE_SOL_LICITUD = "Si el compte existeix, s'ha generat un codi de recuperació vàlid durant 15 minuts.";
	static final String MISSATGE_CODI_NO_VALID = "El codi no és correcte o ja no és vàlid. Sol·licita'n un de nou.";

	private final InMemoryCatalog catalog;
	private final JwtService jwtService;
	private final Environment environment;
	private final Supplier<String> generadorDeCodis;
	private final Clock rellotge;
	private final LimitIntentsLogin limitLogin;
	private final EmailService emailService;

	/** Codis pendents, per identificador d'usuari. */
	private final Map<String, CodiRecuperacio> codis = new ConcurrentHashMap<>();

	private static final class CodiRecuperacio {
		private final String codi;
		private final Instant caducitat;
		private int intentsFallits;

		CodiRecuperacio(String codi, Instant caducitat) {
			this.codi = codi;
			this.caducitat = caducitat;
		}
	}

	@Autowired
	public AuthService(InMemoryCatalog catalog, JwtService jwtService, Environment environment, LimitIntentsLogin limitLogin, EmailService emailService) {
		this(catalog, jwtService, environment, limitLogin, emailService, AuthService::codiAleatori, Clock.systemUTC());
	}

	/** Per als tests: permet fixar el codi i el rellotge. */
	AuthService(InMemoryCatalog catalog, JwtService jwtService, Environment environment, LimitIntentsLogin limitLogin, EmailService emailService, Supplier<String> generadorDeCodis, Clock rellotge) {
		this.catalog = catalog;
		this.jwtService = jwtService;
		this.environment = environment;
		this.generadorDeCodis = generadorDeCodis;
		this.rellotge = rellotge;
		this.limitLogin = limitLogin;
		this.emailService = emailService;
	}

	private static final SecureRandom ALEATORI = new SecureRandom();

	private static String codiAleatori() {
		return String.format("%06d", ALEATORI.nextInt(1_000_000));
	}

	/**
	 * Obté l'usuari de la sessió.
	 * @param id Identificador que porta el token
	 * @return Usuari autenticat si encara existeix
	 */
	public Optional<Usuari> usuariActual(String id) {
		return catalog.findUserById(id);
	}

	/**
	 * Autentica les credencials d'un usuari i genera el token JWT Bearer associat.
	 * @param request Credencials d'accés (email i contrasenya)
	 * @return Usuari amb el token generat si les credencials són vàlides, o Optional.empty()
	 * @throws MassaIntentsException si el compte té massa intents fallits recents, encara que ara la contrasenya siga bona
	 */
	public Optional<Usuari> login(LoginRequest request) {
		long minuts = limitLogin.minutsBloquejat(request.email());
		if (minuts > 0) {
			throw new MassaIntentsException(minuts);
		}
		Optional<Usuari> usuari = catalog.authenticate(request.email(), request.password());
		if (usuari.isEmpty()) {
			limitLogin.fallada(request.email());
			return Optional.empty();
		}
		limitLogin.exit(request.email());
		return usuari.map(u -> u.withToken(jwtService.generateToken(u.id(), u.email(), u.rol())));
	}

	/**
	 * Registra un nou compte d'usuari a la plataforma, genera el token de verificació d'email
	 * i envia el correu combinat de benvinguda i verificació.
	 * @param request Dades de registre de l'usuari
	 * @return L'usuari donat d'alta amb el seu token JWT
	 */
	public Usuari register(RegisterRequest request) {
		Usuari created = catalog.addUser(request);
		
		// Generar token segur de verificació (UUID v4) amb caducitat de 24 hores
		String emailToken = UUID.randomUUID().toString();
		Instant caducitat = Instant.now(rellotge).plus(Duration.ofHours(24));
		catalog.guardarTokenVerificacioEmail(created.id(), emailToken, caducitat);

		// Disparar l'enviament de correu
		try {
			emailService.enviarEmailVerificacio(created.nom(), created.email(), emailToken);
		} catch (Exception e) {
			log.error("No s'ha pogut enviar el correu de verificació inicial a {}: {}", created.email(), e.getMessage());
		}

		String token = jwtService.generateToken(created.id(), created.email(), created.rol());
		return created.withToken(token);
	}

	/**
	 * Verifica el compte d'un usuari utilitzant el token de verificació rebut per correu.
	 * @param token Token únic rebut a l'enllaç de l'email
	 * @return Usuari verificat si el token és vàlid i no ha caducat, o Optional.empty()
	 */
	public Optional<Usuari> verificarEmail(String token) {
		if (token == null || token.isBlank()) {
			return Optional.empty();
		}
		Optional<Usuari> usuariOpt = catalog.trobarUsuariPerTokenVerificacio(token, Instant.now(rellotge));
		if (usuariOpt.isEmpty()) {
			return Optional.empty();
		}
		Usuari usuari = usuariOpt.get();
		catalog.marcarEmailComVerificat(usuari.id());
		return Optional.of(usuari.withEmailVerificat(true));
	}

	/**
	 * Reenvia l'email de verificació a un usuari registrat que encara no ha activat el seu compte.
	 * @param email Correu de l'usuari
	 * @return true si s'ha pogut processar i enviar, false si l'usuari no existeix o ja està verificat
	 */
	public boolean reenviarEmailVerificacio(String email) {
		if (email == null || email.isBlank()) {
			return false;
		}
		Optional<Usuari> usuariOpt = catalog.findUserByEmail(email.trim());
		if (usuariOpt.isEmpty()) {
			return false;
		}
		Usuari usuari = usuariOpt.get();
		if (Boolean.TRUE.equals(usuari.emailVerificat())) {
			return false;
		}
		String emailToken = UUID.randomUUID().toString();
		Instant caducitat = Instant.now(rellotge).plus(Duration.ofHours(24));
		catalog.guardarTokenVerificacioEmail(usuari.id(), emailToken, caducitat);

		return emailService.enviarEmailVerificacio(usuari.nom(), usuari.email(), emailToken);
	}

	/**
	 * Canvia la contrasenya d'un usuari identificat.
	 * @param userId Identificador de l'usuari
	 * @param newPassword Nova contrasenya en text pla (el catàleg en guarda el hash)
	 */
	public void changePassword(String userId, String newPassword) {
		catalog.changePassword(userId, newPassword);
		catalog.findUserById(userId).ifPresent(u -> {
			try {
				emailService.enviarEmailConfirmacioCanviContrasenya(u.nom(), u.email());
			} catch (Exception e) {
				log.warn("No s'ha pogut enviar el correu de confirmació de canvi de contrasenya a {}: {}", u.email(), e.getMessage());
			}
		});
	}

	/** L'usuari que correspon a un correu, un nom o un ID. */
	private Optional<Usuari> usuariPer(String identificador) {
		if (identificador == null || identificador.isBlank()) {
			return Optional.empty();
		}
		String id = identificador.trim();
		Optional<Usuari> usuari = catalog.findUserById(id);
		return usuari.isPresent() ? usuari : catalog.findUserByEmail(id);
	}

	/**
	 * Sol·licita un codi temporal de recuperació (6 dígits) per a l'usuari indicat pel seu correu, nom o ID.
	 * La resposta és la mateixa existisca o no el compte.
	 * @param req Dades amb el correu o identificador d'usuari
	 * @return Resposta amb l'estat i el missatge informatiu
	 */
	public RecuperacioResponse sollicitarRecuperacio(DemanaRecuperacioRequest req) {
		String identificador = req.email() != null ? req.email().trim() : "";
		if (identificador.isBlank()) {
			return new RecuperacioResponse(false, "Cal indicar un correu o nom d'usuari vàlid.");
		}
		usuariPer(identificador).ifPresent(usuari -> {
			String codi = generadorDeCodis.get();
			codis.put(usuari.id(), new CodiRecuperacio(codi, rellotge.instant().plus(VALIDESA_CODI)));
			// Enviament de correu de restabliment de contrasenya
			try {
				emailService.enviarEmailRestablimentContrasenya(usuari.nom(), usuari.email(), codi);
			} catch (Exception e) {
				log.error("No s'ha pogut enviar el correu de recuperació a {}: {}", usuari.email(), e.getMessage());
			}
			if (environment.acceptsProfiles(Profiles.of("dev"))) {
				log.info("[dev] Codi de recuperació de l'usuari {}: {} (vàlid {} minuts)", usuari.id(), codi, VALIDESA_CODI.toMinutes());
			}
		});
		return new RecuperacioResponse(true, MISSATGE_SOL_LICITUD);
	}

	/**
	 * Valida el codi temporal d'un sol ús i aplica la nova contrasenya.
	 * Tots els errors del codi donen el mateix missatge, existisca o no el compte.
	 * @param req Dades de la petició amb correu/usuari, codi i nova contrasenya
	 * @return Resposta amb l'estat de l'operació
	 */
	public RecuperacioResponse restablirContrasenya(RestablirContrasenyaRequest req) {
		String codiRebut = req.codi() != null ? req.codi().trim() : "";
		String nova = req.novaContrasenya() != null ? req.novaContrasenya().trim() : "";

		if (req.email() == null || req.email().isBlank() || codiRebut.isBlank()) {
			return new RecuperacioResponse(false, "El correu/usuari i el codi de 6 dígits són obligatoris.");
		}
		if (nova.length() < 6) {
			return new RecuperacioResponse(false, "La nova contrasenya ha de tenir com a mínim 6 caràcters.");
		}

		Optional<Usuari> usuari = usuariPer(req.email());
		if (usuari.isEmpty()) {
			return new RecuperacioResponse(false, MISSATGE_CODI_NO_VALID);
		}
		String usuariId = usuari.get().id();

		boolean valid;
		synchronized (codis) {
			CodiRecuperacio pendent = codis.get(usuariId);
			if (pendent == null) {
				return new RecuperacioResponse(false, MISSATGE_CODI_NO_VALID);
			}
			if (rellotge.instant().isAfter(pendent.caducitat)) {
				codis.remove(usuariId);
				return new RecuperacioResponse(false, MISSATGE_CODI_NO_VALID);
			}
			valid = pendent.codi.equals(codiRebut);
			if (valid) {
				// Un sol ús: s'esborra abans de canviar la contrasenya
				codis.remove(usuariId);
			} else if (++pendent.intentsFallits >= INTENTS_MAXIMS) {
				codis.remove(usuariId);
			}
		}
		if (!valid) {
			return new RecuperacioResponse(false, MISSATGE_CODI_NO_VALID);
		}

		catalog.changePassword(usuariId, nova);
		usuari.ifPresent(u -> {
			try {
				emailService.enviarEmailConfirmacioCanviContrasenya(u.nom(), u.email());
			} catch (Exception e) {
				log.warn("No s'ha pogut enviar el correu de confirmació de canvi de contrasenya a {}: {}", u.email(), e.getMessage());
			}
		});
		return new RecuperacioResponse(true, "Contrasenya actualitzada correctament! Ja pots iniciar sessió amb la teva nova contrasenya.");
	}
}
