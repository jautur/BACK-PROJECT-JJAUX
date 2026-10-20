package com.avisa.service;

import com.avisa.config.JwtService;
import com.avisa.dto.LoginRequest;
import com.avisa.dto.RegisterRequest;
import com.avisa.model.Usuari;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Servei de lògica de negoci per a l'autenticació i gestió de sessions d'usuaris.
 * Coordina la verificació de credencials, registre de nous comptes i emissió de tokens JWT.
 */
@Service
public class AuthService {

	private final InMemoryCatalog catalog;
	private final JwtService jwtService;

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
}

