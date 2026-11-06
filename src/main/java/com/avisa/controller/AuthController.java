package com.avisa.controller;

import com.avisa.dto.DemanaRecuperacioRequest;
import com.avisa.dto.LoginRequest;
import com.avisa.dto.PasswordChangeRequest;
import com.avisa.dto.RecuperacioResponse;
import com.avisa.dto.RegisterRequest;
import com.avisa.dto.RestablirContrasenyaRequest;
import com.avisa.model.Usuari;
import com.avisa.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador d'Autenticació i Gestió de Sessions (/api/auth).
 * Centralitza el login, registre, sessió actual i canvi de contrasenya delegant en AuthService.
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticació", description = "Endpoints d'inici de sessió, registre, obtenció de JWT i canvi de contrasenya")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	/**
	 * Retorna la informació de l'usuari del token.
	 * Endpoint: GET /api/auth/me
	 * @return Usuari actualment autenticat
	 */
	@GetMapping("/me")
	@Operation(summary = "Obtenir usuari actual", description = "Retorna l'usuari del token de la sessió")
	public ResponseEntity<Usuari> me(Principal principal) {
		return authService.usuariActual(Sollicitant.id(principal))
			.map(ResponseEntity::ok)
			.orElse(ResponseEntity.notFound().build());
	}

	/**
	 * Inicia la sessió d'un usuari comprovant el seu correu electrònic i contrasenya.
	 * Genera i adjunta el token JWT d'accés.
	 * Endpoint: POST /api/auth/login
	 * @param request Credencials d'accés validades (email i password)
	 * @return Dades de l'usuari autenticat amb el token JWT
	 */
	@PostMapping("/login")
	@Operation(summary = "Iniciar sessió", description = "Autentica les credencials i retorna l'usuari amb el token JWT Bearer")
	public ResponseEntity<Usuari> login(@Valid @RequestBody LoginRequest request) {
		return authService.login(request)
			.map(ResponseEntity::ok)
			.orElseGet(() -> ResponseEntity.status(401).build());
	}

	/**
	 * Registra un nou usuari a la plataforma i el desa a la base de dades.
	 * Genera automàticament el seu token JWT.
	 * Endpoint: POST /api/auth/register
	 * @param request Dades de registre validades (nom, email, rol, contrasenya)
	 * @return L'usuari creat amb el token JWT
	 */
	@PostMapping("/register")
	@Operation(summary = "Registrar nou compte", description = "Crea un nou compte d'usuari i retorna el token JWT")
	public ResponseEntity<Usuari> register(@Valid @RequestBody RegisterRequest request) {
		Usuari created = authService.register(request);
		return ResponseEntity.ok(created);
	}

	/**
	 * Tanca la sessió de l'usuari actiu.
	 * Endpoint: POST /api/auth/logout
	 */
	@PostMapping("/logout")
	@Operation(summary = "Tancar sessió", description = "Tanca la sessió de l'usuari actiu")
	public ResponseEntity<Void> logout() {
		return ResponseEntity.ok().build();
	}

	/**
	 * Permet canviar la contrasenya pròpia o, a un administrador, la de qualsevol usuari.
	 * Endpoint: POST /api/auth/canviar-contrasenya
	 * @param request Objecte amb userId i la nova contrasenya validada
	 * @return Missatge de confirmació o error si les dades són invàlides
	 */
	@PostMapping("/canviar-contrasenya")
	@Operation(summary = "Canviar contrasenya", description = "Actualitza la contrasenya pròpia, o la de qualsevol usuari si qui ho demana és administrador")
	public ResponseEntity<String> changePassword(@Valid @RequestBody PasswordChangeRequest request, Principal principal) {
		Sollicitant.exigirUnDelsOAdmin(principal, request.userId());
		if (request.newPassword().trim().length() < 6) {
			return ResponseEntity.badRequest().body("La nova contrasenya ha de tenir com a mínim 6 caràcters.");
		}
		authService.changePassword(request.userId(), request.newPassword().trim());
		return ResponseEntity.ok("Contrasenya actualitzada correctament.");
	}

	/**
	 * Sol·licita la recuperació de contrasenya generant un codi temporal de 6 dígits (OTP).
	 * Endpoint: POST /api/auth/demanar-recuperacio
	 * @param request Dades amb el correu o nom d'usuari
	 * @return Resposta amb l'estat i el missatge
	 */
	@PostMapping("/demanar-recuperacio")
	@Operation(summary = "Demanar recuperació de contrasenya", description = "Genera un codi de recuperació de 6 dígits vàlid durant 15 minuts")
	public ResponseEntity<RecuperacioResponse> demanarRecuperacio(@Valid @RequestBody DemanaRecuperacioRequest request) {
		RecuperacioResponse resp = authService.sollicitarRecuperacio(request);
		return ResponseEntity.ok(resp);
	}

	/**
	 * Restableix la contrasenya validant el codi temporal d'un sol ús.
	 * Endpoint: POST /api/auth/restablir-contrasenya
	 * @param request Dades amb el correu/usuari, codi i nova contrasenya
	 * @return Resposta amb l'estat de l'actualització
	 */
	@PostMapping("/restablir-contrasenya")
	@Operation(summary = "Restablir contrasenya amb codi", description = "Valida el codi temporal i assigna la nova contrasenya")
	public ResponseEntity<RecuperacioResponse> restablirContrasenya(@Valid @RequestBody RestablirContrasenyaRequest request) {
		RecuperacioResponse resp = authService.restablirContrasenya(request);
		if (!resp.success()) {
			return ResponseEntity.badRequest().body(resp);
		}
		return ResponseEntity.ok(resp);
	}

	/**
	 * Verifica el compte d'un usuari mitjançant el token rebut per correu electrònic.
	 * Endpoint: GET /api/auth/verificar-email?token=...
	 */
	@GetMapping("/verificar-email")
	@Operation(summary = "Verificar compte per correu electrònic", description = "Valida el token de verificació enviat per email i activa el compte")
	public ResponseEntity<java.util.Map<String, Object>> verificarEmail(@RequestParam("token") String token) {
		return authService.verificarEmail(token)
			.map(u -> ResponseEntity.ok(java.util.Map.<String, Object>of(
				"success", true,
				"message", "El teu compte ha estat verificat correctament.",
				"user", u
			)))
			.orElseGet(() -> ResponseEntity.badRequest().body(java.util.Map.<String, Object>of(
				"success", false,
				"message", "El token de verificació no és vàlid o ha caducat."
			)));
	}

	/**
	 * Reenvia el correu de verificació d'email a l'adreça indicada si el compte encara no està verificat.
	 * Endpoint: POST /api/auth/reenviar-verificacio
	 */
	@PostMapping("/reenviar-verificacio")
	@Operation(summary = "Reenviar correu de verificació", description = "Genera un nou token i torna a enviar l'email de verificació")
	public ResponseEntity<java.util.Map<String, Object>> reenviarVerificacio(@RequestBody java.util.Map<String, String> body) {
		String email = body.get("email");
		boolean enviat = authService.reenviarEmailVerificacio(email);
		if (enviat) {
			return ResponseEntity.ok(java.util.Map.of(
				"success", true,
				"message", "S'ha enviat un nou correu de verificació a la teva adreça."
			));
		}
		return ResponseEntity.badRequest().body(java.util.Map.of(
			"success", false,
			"message", "No s'ha pogut reenviar el correu. Comprova que l'adreça sigui correcta i que el compte no estigui ja verificat."
		));
	}
}
