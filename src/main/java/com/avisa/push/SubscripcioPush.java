package com.avisa.push;

/** Una subscripció Web Push d'un navegador: on enviar-li i amb quines claus xifrar. */
public record SubscripcioPush(String id, String usuariId, String endpoint, String p256dh, String auth) {
}
