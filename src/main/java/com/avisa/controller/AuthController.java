package com.avisa.controller;

import com.avisa.dto.LoginRequest;
import com.avisa.dto.RegisterRequest;
import com.avisa.model.Usuari;
import com.avisa.service.InMemoryCatalog;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:4200")
public class AuthController {

	private final InMemoryCatalog catalog;

	public AuthController(InMemoryCatalog catalog) {
		this.catalog = catalog;
	}

	@GetMapping("/me")
	public Usuari me() {
		return catalog.users().getFirst();
	}

	@PostMapping("/login")
	public Usuari login(@RequestBody LoginRequest request) {
		return catalog.findUserByEmail(request.email()).orElseGet(() -> catalog.users().getFirst());
	}

	@PostMapping("/register")
	public Usuari register(@RequestBody RegisterRequest request) {
		return catalog.addUser(request);
	}

	// ==========================================
	// ACCIONS D'AUTENTICACIÓ I SESSIÓ
	// ==========================================

	/**
	 * Tancar la sessió de l'usuari.
	 * Acció: Invalidar el token o la sessió activa al backend.
	 */
	@PostMapping("/logout")
	public void logout() {
		// TODO: Invalidar sessió o token de l'usuari actual
	}

	/**
	 * Canviar la contrasenya de l'usuari autenticat.
	 */
	@PostMapping("/canviar-contrasenya")
	public void changePassword(@RequestBody String novaContrasenya) {
		// TODO: Validar contrasenya actual i actualitzar-la de forma segura (hash bcrypt)
	}
}

