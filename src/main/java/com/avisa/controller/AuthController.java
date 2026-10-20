package com.avisa.controller;

import com.avisa.dto.LoginRequest;
import com.avisa.dto.PasswordChangeRequest;
import com.avisa.dto.RegisterRequest;
import com.avisa.model.Usuari;
import com.avisa.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador d'Autenticació i Gestió de Sessions (/api/auth).
 * Centralitza el login, registre, sessió actual i canvi de contrasenya delegant en AuthService.
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(originPatterns = "*")
@Tag(name = "Autenticació", description = "Endpoints d'inici de sessió, registre, obtenció de JWT i canvi de contrasenya")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	/**
	 * Retorna la informació de l'usuari de la sessió actual.
	 * Endpoint: GET /api/auth/me
	 * @return Usuari actualment autenticat
	 */
	@GetMapping("/me")
	@Operation(summary = "Obtenir usuari actual", description = "Retorna l'usuari actiu de la sessió")
	public ResponseEntity<Usuari> me() {
		return authService.getActiveUser()
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
	 * Permet canviar la contrasenya d'un usuari.
	 * Endpoint: POST /api/auth/canviar-contrasenya
	 * @param request Objecte amb userId i la nova contrasenya validada
	 * @return Missatge de confirmació o error si les dades són invàlides
	 */
	@PostMapping("/canviar-contrasenya")
	@Operation(summary = "Canviar contrasenya", description = "Actualitza la contrasenya d'un usuari validant longitud mínima")
	public ResponseEntity<String> changePassword(@Valid @RequestBody PasswordChangeRequest request) {
		if (request.userId() == null || request.userId().isBlank()) {
			return ResponseEntity.badRequest().body("L'identificador d'usuari és obligatori.");
		}
		if (request.newPassword() == null || request.newPassword().trim().length() < 6) {
			return ResponseEntity.badRequest().body("La nova contrasenya ha de tenir com a mínim 6 caràcters.");
		}
		authService.changePassword(request.userId(), request.newPassword().trim());
		return ResponseEntity.ok("Contrasenya actualitzada correctament.");
	}
}
