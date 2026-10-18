package com.avisa.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO de petició de registre per a l'aplicació AVISA (format JavaBean / POJO).
 * Permet rebre o serialitzar les dades del formulari de registre,
 * incloent les dues caselles de verificació obligatòries amb @AssertTrue:
 * - acceptedTerms: Acceptació dels termes i condicions d'ús d'AVISA.
 * - acceptedPrivacy: Acceptació de la política de privacitat i tractament de dades.
 */
@Schema(description = "DTO de registre d'usuari a la plataforma AVISA amb validació de termes")
public class RegisterRequestDTO {

    @Schema(description = "Nom complet de l'usuari o raó social", example = "Maria Garcia")
    @NotBlank(message = "El nom és obligatori")
    private String nom;

    @Schema(description = "Correu electrònic principal", example = "maria@example.com")
    @NotBlank(message = "El correu és obligatori")
    @Email(message = "El format del correu electrònic no és vàlid")
    private String email;

    @Schema(description = "Contrasenya d'accés (mínim 6 caràcters)", example = "123456")
    @NotBlank(message = "La contrasenya és obligatòria")
    @Size(min = 6, message = "La contrasenya ha de tenir almenys 6 caràcters")
    private String password;

    @Schema(description = "Rol a la plataforma ('client' o 'professional')", example = "client")
    @NotBlank(message = "El rol és obligatori")
    private String rol;

    @Schema(description = "Telèfon de contacte", example = "600123456")
    private String telefon;

    @Schema(description = "Acceptació dels termes i condicions d'ús d'AVISA", example = "true")
    @AssertTrue(message = "Cal acceptar els termes i condicions")
    private boolean acceptedTerms;

    @Schema(description = "Acceptació de la política de privacitat", example = "true")
    @AssertTrue(message = "Cal acceptar la política de privacitat")
    private boolean acceptedPrivacy;

    public RegisterRequestDTO() {}

    public RegisterRequestDTO(String nom, String email, String password, String rol, String telefon, boolean acceptedTerms, boolean acceptedPrivacy) {
        this.nom = nom;
        this.email = email;
        this.password = password;
        this.rol = rol;
        this.telefon = telefon;
        this.acceptedTerms = acceptedTerms;
        this.acceptedPrivacy = acceptedPrivacy;
    }

    /**
     * Converteix este DTO al record RegisterRequest per a interoperabilitat.
     */
    public RegisterRequest toRecord() {
        return new RegisterRequest(nom, email, password, rol, telefon, acceptedTerms, acceptedPrivacy);
    }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public String getTelefon() { return telefon; }
    public void setTelefon(String telefon) { this.telefon = telefon; }

    public boolean isAcceptedTerms() { return acceptedTerms; }
    public void setAcceptedTerms(boolean acceptedTerms) { this.acceptedTerms = acceptedTerms; }

    public boolean isAcceptedPrivacy() { return acceptedPrivacy; }
    public void setAcceptedPrivacy(boolean acceptedPrivacy) { this.acceptedPrivacy = acceptedPrivacy; }
}
