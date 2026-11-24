package com.avisa.push;

import java.nio.charset.StandardCharsets;
import java.security.Security;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Enviament real amb la llibreria web-push (VAPID + xifrat RFC 8291).
 * Les claus vénen de AVISA_VAPID_PUBLIC i AVISA_VAPID_PRIVATE (vegeu docs/configuracio.md).
 * Si falten, el push queda desactivat: l'aplicació arrenca igual.
 */
@Component
public class EnviadorWebPush implements EnviadorPush {

	private final String clauPublica;
	private final PushService servei;

	public EnviadorWebPush(@Value("${avisa.push.vapid-public:}") String publica,
			@Value("${avisa.push.vapid-private:}") String privada,
			@Value("${avisa.push.subject:mailto:admin@avisa.local}") String subject) throws Exception {
		if (publica == null || publica.isBlank() || privada == null || privada.isBlank()) {
			this.clauPublica = null;
			this.servei = null;
			return;
		}
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
			Security.addProvider(new BouncyCastleProvider());
		}
		this.clauPublica = publica.trim();
		this.servei = new PushService(publica.trim(), privada.trim(), subject);
	}

	@Override
	public boolean activat() {
		return servei != null;
	}

	@Override
	public String clauPublica() {
		return clauPublica;
	}

	@Override
	public int enviar(SubscripcioPush s, String contingut) throws Exception {
		Notification notificacio = new Notification(s.endpoint(), s.p256dh(), s.auth(), contingut.getBytes(StandardCharsets.UTF_8));
		return servei.send(notificacio).getStatusLine().getStatusCode();
	}
}
