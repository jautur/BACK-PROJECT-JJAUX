package com.avisa.controller;

import com.avisa.dto.LoginRequest;
import com.avisa.dto.PasswordChangeRequest;
import com.avisa.dto.RegisterRequest;
import com.avisa.model.Usuari;
import com.avisa.service.InMemoryCatalog;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador d'Autenticació i Gestió de Sessions (/api/auth).
 * Centralitza el login, registre, sessió actual i canvi de contrasenya.
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:4200")
public class AuthController {

	private final InMemoryCatalog catalog;

	public AuthController(InMemoryCatalog catalog) {
		this.catalog = catalog;
	}

	/**
	 * Retorna la informació de l'usuari de la sessió actual.
	 * Endpoint: GET /api/auth/me
	 * @return Usuari actualment autenticat
	 */
	@GetMapping("/me")
	public ResponseEntity<Usuari> me() {
		return catalog.users().stream().findFirst()
			.map(ResponseEntity::ok)
			.orElse(ResponseEntity.notFound().build());
	}

	/**
	 * Inicia la sessió d'un usuari comprovant el seu correu electrònic i contrasenya.
	 * Endpoint: POST /api/auth/login
	 * @param request Credencials d'accés (email i password)
	 * @return Dades de l'usuari autenticat
	 */
	@PostMapping("/login")
	public ResponseEntity<Usuari> login(@RequestBody LoginRequest request) {
		return catalog.authenticate(request.email(), request.password())
			.map(ResponseEntity::ok)
			.orElseGet(() -> ResponseEntity.status(401).build());
	}

	/**
	 * Registra un nou usuari a la plataforma i el desa a la base de dades.
	 * Endpoint: POST /api/auth/register
	 * @param request Dades de registre (nom, email, rol, contrasenya)
	 * @return L'usuari creat
	 */
	@PostMapping("/register")
	public ResponseEntity<Usuari> register(@RequestBody RegisterRequest request) {
		Usuari created = catalog.addUser(request);
		return ResponseEntity.ok(created);
	}

	/**
	 * Tanca la sessió de l'usuari actiu.
	 * Endpoint: POST /api/auth/logout
	 */
	@PostMapping("/logout")
	public ResponseEntity<Void> logout() {
		return ResponseEntity.ok().build();
	}

	/**
	 * Permet a l'usuari canviar la seva contrasenya des del formulari de perfil.
	 * Endpoint: POST /api/auth/canviar-contrasenya
	 * @param request Objecte amb userId i la nova contrasenya
	 */
	@PostMapping("/canviar-contrasenya")
	public ResponseEntity<String> changePassword(@RequestBody PasswordChangeRequest request) {
		catalog.changePassword(request.userId(), request.newPassword());
		return ResponseEntity.ok("Contrasenya actualitzada correctament.");
	}
}
