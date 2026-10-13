package com.avisa.dto;

/**
 * DTO per a la petició de canvi de contrasenya d'un usuari.
 *
 * @param userId Identificador de l'usuari que canvia la contrasenya
 * @param newPassword Nova contrasenya desitjada
 */
public record PasswordChangeRequest(String userId, String newPassword) {
}

