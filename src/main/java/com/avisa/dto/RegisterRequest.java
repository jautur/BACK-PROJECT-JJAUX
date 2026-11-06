package com.avisa.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO per a la petició de registre d'un nou compte d'usuari a JJAUX / AVISA.
 * Els clients necessiten correu i contrasenya per publicar tasques.
 * Els professionals requereixen a més un número de telèfon per resoldre peticions i presentar ofertes.
 * Requereix obligatòriament l'acceptació dels termes i condicions d'ús i de la política de privacitat (@AssertTrue).
 */
@Schema(description = "Petició de registre d'usuari (client o professional)")
public record RegisterRequest(
    @Schema(description = "Nom complet de l'usuari o raó social", example = "Maria Garcia")
    @NotBlank(message = "El nom és obligatori")
    @Size(max = 150, message = "El nom pot tenir com a màxim 150 caràcters")
    @Pattern(regexp = "^[^<>]*$", message = "No pot contenir els caràcters < ni >")
    String nom,

    @Schema(description = "Correu electrònic principal", example = "maria@example.com")
    @NotBlank(message = "El correu és obligatori")
    @Email(message = "El format del correu electrònic no és vàlid")
    @Size(max = 150, message = "El correu pot tenir com a màxim 150 caràcters")
    String email,

    @Schema(description = "Contrasenya d'accés (mínim 6 caràcters)", example = "123456")
    @NotBlank(message = "La contrasenya és obligatòria")
    @Size(min = 6, max = 72, message = "La contrasenya ha de tenir entre 6 i 72 caràcters")
    String password,

    @Schema(description = "Rol a la plataforma ('CLIENT' o 'PROFESSIONAL')", example = "CLIENT")
    @NotBlank(message = "El rol és obligatori")
    @Pattern(regexp = "(?i)client|professional", message = "El rol ha de ser client o professional")
    String rol,

    @Schema(description = "Telèfon de contacte", example = "600123456")
    @Pattern(regexp = "^$|^[0-9+()\\s.-]{6,20}$", message = "El telèfon només pot tenir xifres, espais, +, -, . i parèntesis (entre 6 i 20)")
    String telefon,

    @Schema(description = "Acceptació dels termes i condicions d'ús d'AVISA", example = "true")
    @AssertTrue(message = "Cal acceptar els termes i condicions")
    boolean acceptedTerms,

    @Schema(description = "Acceptació de la política de privacitat", example = "true")
    @AssertTrue(message = "Cal acceptar la política de privacitat")
    boolean acceptedPrivacy
) {
    public RegisterRequest(String nom, String email, String password, String rol) {
        this(nom, email, password, rol, null, true, true);
    }

    public RegisterRequest(String nom, String email, String password, String rol, String telefon) {
        this(nom, email, password, rol, telefon, true, true);
    }

    /** Un professional ha de donar un telèfon: l'administrador el truca per verificar-lo. */
    @jakarta.validation.constraints.AssertTrue(message = "El telèfon és obligatori per als professionals")
    @io.swagger.v3.oas.annotations.media.Schema(hidden = true)
    public boolean isTelefonSiEsProfessional() {
        return !"professional".equalsIgnoreCase(rol) || (telefon != null && !telefon.isBlank());
    }
}
