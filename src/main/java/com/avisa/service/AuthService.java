package com.avisa.service;

import com.avisa.config.JwtService;
import com.avisa.dto.DemanaRecuperacioRequest;
import com.avisa.dto.LoginRequest;
import com.avisa.dto.RecuperacioResponse;
import com.avisa.dto.RegisterRequest;
import com.avisa.dto.RestablirContrasenyaRequest;
import com.avisa.model.Usuari;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

/**
 * Servei de lògica de negoci per a l'autenticació i gestió de sessions d'usuaris.
 * Coordina la verificació de credencials, registre de nous comptes, emissió de tokens JWT
 * i flux complet de recuperació de contrasenya mitjançant codis temporals d'un sol ús (OTP).
 */
@Service
public class AuthService {

	private final InMemoryCatalog catalog;
	private final JwtService jwtService;

	/**
	 * Magatzem en memòria per als codis temporals de recuperació de contrasenya.
	 * Clau: correu o identificador en minúscules -> Dades de recuperació.
	 */
	private final Map<String, RecoveryEntry> recoveryCodes = new ConcurrentHashMap<>();

	public record RecoveryEntry(
		String userId,
		String email,
		String code,
		Instant expiry
	) {}

	public AuthService(InMemoryCatalog catalog, JwtService jwtService) {
		this.catalog = catalog;
		this.jwtService = jwtService;
	}

	/**
	 * Obté l'usuari actiu de la sessió.
	 * @return Usuari actualment autenticat si existeix
	 */
	public Optional<Usuari> getActiveUser() {
		return catalog.users().stream().findFirst();
	}

	/**
	 * Autentica les credencials d'un usuari i genera el token JWT Bearer associat.
	 * @param request Credencials d'accés (email i contrasenya)
	 * @return Usuari amb el token generat si les credencials són vàlides, o Optional.empty()
	 */
	public Optional<Usuari> login(LoginRequest request) {
		return catalog.authenticate(request.email(), request.password())
			.map(u -> {
				String token = jwtService.generateToken(u.id(), u.email(), u.rol());
				return u.withToken(token);
			});
	}

	/**
	 * Registra un nou compte d'usuari a la plataforma i genera el token JWT inicial.
	 * @param request Dades de registre de l'usuari
	 * @return L'usuari donat d'alta amb el seu token JWT
	 */
	public Usuari register(RegisterRequest request) {
		Usuari created = catalog.addUser(request);
		String token = jwtService.generateToken(created.id(), created.email(), created.rol());
		return created.withToken(token);
	}

	/**
	 * Canvia la contrasenya d'un usuari identificat.
	 * @param userId Identificador de l'usuari
	 * @param newPassword Nova contrasenya en text pla (serà xifrada pel catàleg)
	 */
	public void changePassword(String userId, String newPassword) {
		catalog.changePassword(userId, newPassword);
	}

	/**
	 * Sol·licita un codi temporal de recuperació de contrasenya (OTP de 6 dígits)
	 * per a l'usuari indicat pel seu email o nom d'usuari.
	 * En producció s'enviaria per correu SMTP; en desenvolupament es mostra a la consola
	 * i es retorna al camp codiDev per facilitar les proves.
	 * @param req Dades amb el correu o identificador d'usuari
	 * @return Resposta amb l'estat i el missatge informatiu
	 */
	public RecuperacioResponse sollicitarRecuperacio(DemanaRecuperacioRequest req) {
		String query = req.email() != null ? req.email().trim() : "";
		if (query.isBlank()) {
			return new RecuperacioResponse(false, "Cal indicar un correu o nom d'usuari vàlid.", query, null);
		}

		Optional<Usuari> userOpt = catalog.findUserById(query);
		if (userOpt.isEmpty()) {
			userOpt = catalog.findUserByEmail(query);
		}

		// Per bones pràctiques de seguretat (OWASP), no revelem si l'usuari existeix públicament
		if (userOpt.isEmpty()) {
			return new RecuperacioResponse(true, "Si el compte existeix, s'ha enviat el codi de restabliment al correu associat.", query, null);
		}

		Usuari user = userOpt.get();
		String code = String.format("%06d", new SecureRandom().nextInt(1_000_000));
		Instant expiry = Instant.now().plus(Duration.ofMinutes(15));

		RecoveryEntry entry = new RecoveryEntry(user.id(), user.email(), code, expiry);
		recoveryCodes.put(user.id().toLowerCase(), entry);
		if (user.email() != null) {
			recoveryCodes.put(user.email().toLowerCase(), entry);
		}
		if (user.nom() != null) {
			recoveryCodes.put(user.nom().toLowerCase(), entry);
		}

		System.out.println("\n============================================================");
		System.out.println("🔐 [RECUPERACIÓ DE CONTRASENYA - AVISA]");
		System.out.println("👤 Usuari: " + user.nom() + " (" + user.email() + ")");
		System.out.println("🔑 CODI TEMPORAL (OTP): " + code);
		System.out.println("⏳ Vàlid durant: 15 minuts");
		System.out.println("============================================================\n");

		return new RecuperacioResponse(
			true,
			"S'ha generat el codi de recuperació de 6 dígits amb èxit (vàlid durant 15 minuts).",
			user.email() != null ? user.email() : user.id(),
			code
		);
	}

	/**
	 * Valida el codi temporal d'un sol ús i aplica la nova contrasenya triada per l'usuari.
	 * @param req Dades de la petició amb correu/usuari, codi i nova contrasenya
	 * @return Resposta amb l'estat de l'operació
	 */
	public RecuperacioResponse restablirContrasenya(RestablirContrasenyaRequest req) {
		String identifier = req.email() != null ? req.email().trim().toLowerCase() : "";
		String codi = req.codi() != null ? req.codi().trim() : "";
		String nova = req.novaContrasenya() != null ? req.novaContrasenya().trim() : "";

		if (identifier.isBlank() || codi.isBlank()) {
			return new RecuperacioResponse(false, "El correu/usuari i el codi de 6 dígits són obligatoris.", identifier, null);
		}
		if (nova.length() < 6) {
			return new RecuperacioResponse(false, "La nova contrasenya ha de tenir com a mínim 6 caràcters.", identifier, null);
		}

		RecoveryEntry entry = recoveryCodes.get(identifier);
		if (entry == null) {
			Optional<Usuari> u = catalog.findUserById(identifier);
			if (u.isPresent()) {
				entry = recoveryCodes.get(u.get().id().toLowerCase());
			}
		}

		if (entry == null || !entry.code().equals(codi)) {
			return new RecuperacioResponse(false, "El codi de verificació és incorrecte o no s'ha trobat.", identifier, null);
		}

		if (Instant.now().isAfter(entry.expiry())) {
			recoveryCodes.remove(identifier);
			return new RecuperacioResponse(false, "El codi de verificació ha caducat (temps límit 15 minuts). Sol·licita'n un de nou.", identifier, null);
		}

		// Actualitzem la contrasenya a la base de dades PostgreSQL
		catalog.changePassword(entry.userId(), nova);

		// Eliminem el codi d'un sol ús
		recoveryCodes.remove(identifier);
		if (entry.email() != null) {
			recoveryCodes.remove(entry.email().toLowerCase());
		}
		recoveryCodes.remove(entry.userId().toLowerCase());

		System.out.println("✅ [RECUPERACIÓ-AVISA] Contrasenya canviada amb èxit per a l'usuari ID: " + entry.userId());

		return new RecuperacioResponse(true, "Contrasenya actualitzada correctament! Ja pots iniciar sessió amb la teva nova contrasenya.", identifier, null);
	}
}

