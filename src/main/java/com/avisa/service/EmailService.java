package com.avisa.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Servei encarregat de l'enviament de correus electrònics mitjançant SMTP (Gmail).
 */
@Service
public class EmailService {

	private static final Logger log = LoggerFactory.getLogger(EmailService.class);

	private final JavaMailSender mailSender;
	private final String fromEmail;
	private final String baseUrl;

	public EmailService(
		JavaMailSender mailSender,
		@Value("${spring.mail.username:info.avisa.service@gmail.com}") String fromEmail,
		@Value("${avisa.app.base-url:http://localhost:4200}") String baseUrl
	) {
		this.mailSender = mailSender;
		this.fromEmail = fromEmail;
		this.baseUrl = baseUrl;
	}

	/**
	 * Envia l'email combinat de benvinguda i verificació del compte a un usuari nou.
	 *
	 * @param nomUsuari Nom de l'usuari
	 * @param destinatari Correu electrònic
	 * @param token Token únic de verificació
	 * @return true si s'ha enviat correctament, false si hi ha hagut algun error
	 */
	public boolean enviarEmailVerificacio(String nomUsuari, String destinatari, String token) {
		String linkVerificacio = baseUrl + "/verificar-compte?token=" + token;
		String subject = "Benvingut/da a AVISA - Verifica el teu compte";

		String textContent = compondreTextPlanVerificacio(nomUsuari, linkVerificacio);
		String htmlContent = compondreHtmlVerificacio(nomUsuari, linkVerificacio);

		return enviarEmail(destinatari, subject, textContent, htmlContent);
	}

	/**
	 * Envia l'email de sol·licitud de restabliment de contrasenya amb el codi temporal i l'enllaç directe.
	 */
	public boolean enviarEmailRestablimentContrasenya(String nomUsuari, String destinatari, String codi) {
		String linkLogin = baseUrl + "/login";
		String subject = "Recuperació de contrasenya - AVISA";

		String textContent = "Hola " + nomUsuari + ",\n\n"
			+ "Hem rebut una sol·licitud per a restablir la contrasenya del teu compte a AVISA.\n\n"
			+ "El teu codi de recuperació és: " + codi + "\n\n"
			+ "Aquest codi és vàlid durant 15 minuts. Pots introduir-lo directament al formulari de recuperació o accedir a:\n"
			+ linkLogin + "\n\n"
			+ "Si no has sol·licitat aquest restabliment, pots ignorar aquest correu amb seguretat. La teva contrasenya actual no ha canviat.\n\n"
			+ "Equip d'AVISA";

		String htmlContent = "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"></head>"
			+ "<body style=\"font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; color: #111827; background-color: #ffffff; padding: 24px; margin: 0; line-height: 1.6;\">"
			+ "  <div style=\"max-width: 540px; margin: 0 auto;\">"
			+ "    <div style=\"margin-bottom: 24px;\">"
			+ "      <img src=\"cid:logoAvisa\" alt=\"AVISA\" width=\"44\" height=\"44\" style=\"display: inline-block; vertical-align: middle; border-radius: 8px; margin-right: 12px;\">"
			+ "      <span style=\"font-size: 20px; font-weight: 800; letter-spacing: -0.02em; color: #111827; vertical-align: middle;\">AVISA</span>"
			+ "    </div>"
			+ "    <p style=\"font-size: 16px; margin: 0 0 16px;\">Hola <strong>" + escaparHtml(nomUsuari) + "</strong>,</p>"
			+ "    <p style=\"font-size: 15px; margin: 0 0 16px; color: #374151;\">"
			+ "      Hem rebut una sol·licitud per restablir la contrasenya del teu compte a AVISA."
			+ "    </p>"
			+ "    <div style=\"margin: 24px 0; background: #f9fafb; border: 1px solid #e5e7eb; border-radius: 8px; padding: 16px; text-align: center;\">"
			+ "      <div style=\"font-size: 13px; color: #6b7280; margin-bottom: 6px; text-transform: uppercase; font-weight: 700; letter-spacing: 0.05em;\">Codi de verificació</div>"
			+ "      <div style=\"font-size: 28px; font-weight: 800; letter-spacing: 0.25em; color: #111827;\">" + codi + "</div>"
			+ "      <div style=\"font-size: 12px; color: #9ca3af; margin-top: 6px;\">Vàlid durant 15 minuts</div>"
			+ "    </div>"
			+ "    <p style=\"font-size: 14px; color: #4b5563; margin: 16px 0;\">"
			+ "      Pots introduir aquest codi a la pantalla de recuperació de la plataforma per definir la teva nova contrasenya:"
			+ "    </p>"
			+ "    <p style=\"margin: 20px 0;\">"
			+ "      <a href=\"" + linkLogin + "\" style=\"background-color: #111827; color: #ffffff; padding: 10px 20px; text-decoration: none; border-radius: 6px; font-size: 14px; font-weight: 600; display: inline-block;\">"
			+ "        Anar a la pantalla d'inici de sessió"
			+ "      </a>"
			+ "    </p>"
			+ "    <p style=\"font-size: 13px; color: #9ca3af; margin: 24px 0 0; border-top: 1px solid #e5e7eb; padding-top: 16px;\">"
			+ "      Si no has demanat restablir la contrasenya, pots ignorar aquest missatge. El teu compte continua protegit."
			+ "    </p>"
			+ "  </div>"
			+ "</body></html>";

		return enviarEmail(destinatari, subject, textContent, htmlContent);
	}

	/**
	 * Envia l'email de notificació i confirmació de contrasenya canviada amb èxit.
	 */
	public boolean enviarEmailConfirmacioCanviContrasenya(String nomUsuari, String destinatari) {
		String linkLogin = baseUrl + "/login";
		String subject = "La teva contrasenya d'AVISA ha estat modificada";

		String textContent = "Hola " + nomUsuari + ",\n\n"
			+ "T'informem que la contrasenya del teu compte a AVISA ha estat actualitzada correctament.\n\n"
			+ "Si has estat tu qui ha realitzat aquest canvi, no cal que facis res més.\n\n"
			+ "Si NO has estat tu, si us plau contacta immediatament amb el suport d'AVISA a info.avisa.service@gmail.com per bloquejar i protegir el teu compte.\n\n"
			+ "Equip d'AVISA";

		String htmlContent = "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"></head>"
			+ "<body style=\"font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; color: #111827; background-color: #ffffff; padding: 24px; margin: 0; line-height: 1.6;\">"
			+ "  <div style=\"max-width: 540px; margin: 0 auto;\">"
			+ "    <div style=\"margin-bottom: 24px;\">"
			+ "      <img src=\"cid:logoAvisa\" alt=\"AVISA\" width=\"44\" height=\"44\" style=\"display: inline-block; vertical-align: middle; border-radius: 8px; margin-right: 12px;\">"
			+ "      <span style=\"font-size: 20px; font-weight: 800; letter-spacing: -0.02em; color: #111827; vertical-align: middle;\">AVISA</span>"
			+ "    </div>"
			+ "    <p style=\"font-size: 16px; margin: 0 0 16px;\">Hola <strong>" + escaparHtml(nomUsuari) + "</strong>,</p>"
			+ "    <p style=\"font-size: 15px; margin: 0 0 16px; color: #374151;\">"
			+ "      La contrasenya associada al teu compte a <strong>AVISA</strong> s'ha actualitzat correctament."
			+ "    </p>"
			+ "    <div style=\"margin: 20px 0; background: #ecfdf5; border: 1px solid #a7f3d0; border-radius: 8px; padding: 14px; color: #065f46; font-size: 14px;\">"
			+ "      ✔ El canvi de contrasenya s'ha completat amb èxit."
			+ "    </div>"
			+ "    <p style=\"font-size: 14px; color: #4b5563; margin: 16px 0;\">"
			+ "      Si has estat tu, pots iniciar sessió habitualment amb les teves noves credencials:"
			+ "    </p>"
			+ "    <p style=\"margin: 20px 0;\">"
			+ "      <a href=\"" + linkLogin + "\" style=\"background-color: #111827; color: #ffffff; padding: 10px 20px; text-decoration: none; border-radius: 6px; font-size: 14px; font-weight: 600; display: inline-block;\">"
			+ "        Iniciar sessió a AVISA"
			+ "      </a>"
			+ "    </p>"
			+ "    <p style=\"font-size: 13px; color: #b91c1c; margin: 24px 0 0; border-top: 1px solid #fee2e2; padding-top: 16px;\">"
			+ "      <strong>Atenció:</strong> Si no has realitzat aquesta acció tu personalment, posa't en contacte immediatament amb el suport a info.avisa.service@gmail.com."
			+ "    </p>"
			+ "  </div>"
			+ "</body></html>";

		return enviarEmail(destinatari, subject, textContent, htmlContent);
	}

	private boolean enviarEmail(String to, String subject, String textBody, String htmlBody) {
		try {
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

			helper.setFrom(fromEmail, "AVISA");
			helper.setTo(to);
			helper.setSubject(subject);
			helper.setText(textBody, htmlBody);

			// Adjuntem el logotip com a recurs inline (cid:logoAvisa)
			try {
				ClassPathResource logoRes = new ClassPathResource("static/images/logo.png");
				if (logoRes.exists()) {
					helper.addInline("logoAvisa", logoRes, "image/png");
				}
			} catch (Exception e) {
				log.warn("No s'ha pogut adjuntar la imatge del logotip al correu: {}", e.getMessage());
			}

			mailSender.send(message);
			log.info("Email [{}] enviat satisfactòriament a {}", subject, to);
			return true;
		} catch (Exception e) {
			log.error("Error enviant email [{}] a {}: {}", subject, to, e.getMessage(), e);
			return false;
		}
	}

	private String compondreTextPlanVerificacio(String nom, String enllac) {
		return "Hola " + nom + ",\n\n"
			+ "Gràcies per registrar-te a AVISA! Estem encantats de donar-te la benvinguda a la nostra comunitat.\n\n"
			+ "Com a recordatori, pots accedir a la plataforma amb el teu correu i trobar totes les ofertes i tasques a https://avisa.app/tasques-ofertes\n\n"
			+ "Per verificar el teu compte i activar totes les seves funcionalitats, fes clic a l'enllaç següent:\n"
			+ enllac + "\n\n"
			+ "Aquest enllaç caduca en 24 hores.\n\n"
			+ "Si no has creat cap compte a AVISA, pots ignorar aquest missatge tranquil·lament.\n\n"
			+ "Equip d'AVISA\n"
			+ "https://avisa.app";
	}

	/**
	 * Disseny net, pla i sobri combinant benvinguda, recordatori d'accés i verificació.
	 */
	private String compondreHtmlVerificacio(String nom, String enllac) {
		return "<!DOCTYPE html>"
			+ "<html>"
			+ "<head><meta charset=\"UTF-8\"></head>"
			+ "<body style=\"font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; color: #111827; background-color: #ffffff; padding: 24px; margin: 0; line-height: 1.6;\">"
			+ "  <div style=\"max-width: 540px; margin: 0 auto;\">"
			+ "    <div style=\"margin-bottom: 24px;\">"
			+ "      <img src=\"cid:logoAvisa\" alt=\"AVISA\" width=\"44\" height=\"44\" style=\"display: inline-block; vertical-align: middle; border-radius: 8px; margin-right: 12px;\">"
			+ "      <span style=\"font-size: 20px; font-weight: 800; letter-spacing: -0.02em; color: #111827; vertical-align: middle;\">AVISA</span>"
			+ "    </div>"
			+ "    <p style=\"font-size: 16px; margin: 0 0 16px;\">Hola <strong>" + escaparHtml(nom) + "</strong>,</p>"
			+ "    <p style=\"font-size: 15px; margin: 0 0 16px; color: #374151;\">"
			+ "      Et donem la benvinguda a <strong>AVISA</strong>. Des del teu compte podràs gestionar encàrrecs, publicar tasques o trobar professionals qualificats."
			+ "    </p>"
			+ "    <p style=\"font-size: 15px; margin: 0 0 16px; color: #374151;\">"
			+ "      Per activar el teu compte i verificar la teva adreça de correu electrònic, fes clic a l'enllaç següent:"
			+ "    </p>"
			+ "    <p style=\"margin: 24px 0;\">"
			+ "      <a href=\"" + enllac + "\" style=\"background-color: #111827; color: #ffffff; padding: 10px 20px; text-decoration: none; border-radius: 6px; font-size: 14px; font-weight: 600; display: inline-block;\">"
			+ "        Verificar el meu compte"
			+ "      </a>"
			+ "    </p>"
			+ "    <p style=\"font-size: 13px; color: #6b7280; margin: 24px 0 8px;\">"
			+ "      O copia i enganxa aquest enllaç al teu navegador:<br>"
			+ "      <a href=\"" + enllac + "\" style=\"color: #2563eb; text-decoration: underline; word-break: break-all;\">" + enllac + "</a>"
			+ "    </p>"
			+ "    <p style=\"font-size: 13px; color: #9ca3af; margin: 24px 0 0; border-top: 1px solid #e5e7eb; padding-top: 16px;\">"
			+ "      Aquest enllaç caduca en 24 hores. Si no has sol·licitat aquest registre, pots ignorar aquest correu tranquil·lament."
			+ "    </p>"
			+ "  </div>"
			+ "</body>"
			+ "</html>";
	}

	private String escaparHtml(String text) {
		if (text == null) return "";
		return text.replace("&", "&amp;")
			.replace("<", "&lt;")
			.replace(">", "&gt;")
			.replace("\"", "&quot;")
			.replace("'", "&#39;");
	}
}
