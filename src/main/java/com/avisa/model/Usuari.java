package com.avisa.model;

/**
 * Model de dades de l'Usuari del sistema JJAUX.
 * Representa un perfil d'usuari registrat a la plataforma (Client, Professional o Administrador).
 *
 * @param id Identificador únic de l'usuari
 * @param nom Nom complet o nom d'empresa
 * @param email Adreça de correu electrònic (utilitzada per a login)
 * @param rol Rol assignat dins l'aplicació ('client', 'professional', 'admin')
 * @param telefon Telèfon de contacte opcional del perfil
 * @param bio Descripció o presentació professional/personal
 */
public record Usuari(
    String id,
    String nom,
    String email,
    String rol,
    String telefon,
    String bio
) {
    /**
     * Constructor sobrecarregat per a compatibilitat quan no s'especifiquen telèfon ni bio.
     */
    public Usuari(String id, String nom, String email, String rol) {
        this(id, nom, email, rol, null, null);
    }
}
