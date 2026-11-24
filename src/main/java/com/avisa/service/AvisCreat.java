package com.avisa.service;

/** Es publica quan es crea un avís per a un usuari: qui vulga reaccionar-hi (p. ex. el push) l'escolta. */
public record AvisCreat(String usuariId, String missatge, String enllac) {
}
