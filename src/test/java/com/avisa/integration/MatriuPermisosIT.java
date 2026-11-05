package com.avisa.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Matriu de permisos: cada ruta de l'API amb cada tipus d'usuari, sobre la pila real i amb tokens reals.
 *
 * Les dades pertanyen a CLIENT i PROFESSIONAL. ALTRE_CLIENT i ALTRE_PROFESSIONAL tenen el mateix rol
 * però cap relació amb elles: són els que detecten l'accés a dades d'un altre.
 * Les columnes de cada fila són: anònim, client, professional, admin, altre client, altre professional.
 */
class MatriuPermisosIT extends MercatIntegrationTest {

	private enum Actor { ANONIM, CLIENT, PROFESSIONAL, ADMIN, ALTRE_CLIENT, ALTRE_PROFESSIONAL }

	/** Sense sessió. */
	private static final int N = 401;
	/** Amb sessió, sense permís. */
	private static final int F = 403;

	private record Fila(String metode, String ruta, String cos, int... esperat) {
		@Override
		public String toString() {
			return metode + " " + ruta;
		}
	}

	private static Fila fila(String metode, String ruta, int... esperat) {
		return new Fila(metode, ruta, null, esperat);
	}

	private static Fila fila(String metode, String ruta, String cos, int... esperat) {
		return new Fila(metode, ruta, cos, esperat);
	}

	/** Qualsevol, també sense sessió. */
	private static Fila publica(String metode, String ruta, String cos, int estat) {
		return new Fila(metode, ruta, cos, new int[] {estat, estat, estat, estat, estat, estat});
	}

	private static Fila publica(String ruta) {
		return publica("GET", ruta, null, 200);
	}

	private static Fila nomesAdmin(String metode, String ruta, String cos, int estat) {
		return new Fila(metode, ruta, cos, new int[] {N, F, F, estat, F, F});
	}

	private static final List<Fila> FILES = List.of(
		// ---------- Autenticació
		fila("GET", "/api/auth/me", N, 200, 200, 200, 200, 200),
		publica("POST", "/api/auth/login", "{\"email\":\"{correuClient}\",\"password\":\"secreta\"}", 200),
		publica("POST", "/api/auth/register", "{\"nom\":\"Nou\",\"email\":\"nou@test.es\",\"password\":\"123456\",\"rol\":\"client\",\"telefon\":null,\"acceptedTerms\":true,\"acceptedPrivacy\":true}", 200),
		publica("POST", "/api/auth/logout", "{}", 200),
		fila("POST", "/api/auth/canviar-contrasenya", "{\"userId\":\"{client}\",\"newPassword\":\"novaClau1\"}", N, 200, F, 200, F, F),
		publica("POST", "/api/auth/demanar-recuperacio", "{\"email\":\"{correuClient}\"}", 200),
		publica("POST", "/api/auth/restablir-contrasenya", "{\"email\":\"{correuClient}\",\"codi\":\"no-val\",\"novaContrasenya\":\"novaClau1\"}", 400),

		// ---------- Usuaris. Les dues lectures generals són PENDENTS DE DECISIÓ: les usen les pantalles de convidat
		publica("/api/users"),
		publica("/api/users/{client}"),
		nomesAdmin("GET", "/api/users/rol/{rolProfessional}", null, 200),
		fila("PUT", "/api/users/{client}", "{\"nom\":\"Editat\",\"telefon\":\"600000009\",\"bio\":\"b\"}", N, 200, F, 200, F, F),
		fila("POST", "/api/users/{professional}/sollicitar-verificacio", "{}", N, F, 200, 200, F, F),
		nomesAdmin("PUT", "/api/users/{professional}/verificar", "{}", 200),
		nomesAdmin("PUT", "/api/users/{professional}/foto", "{\"foto\":\"https://foto.jpg\"}", 200),
		nomesAdmin("DELETE", "/api/users/{usuariLliure}", null, 204),

		// ---------- Tasques. GET /api/tasks és PENDENT DE DECISIÓ: el perfil públic en calcula les valoracions
		publica("/api/tasks"),
		fila("GET", "/api/tasks/{tasca}", N, 200, 200, 200, F, F),
		fila("GET", "/api/tasks/{sollicitud}", N, 200, 200, 200, F, F),
		fila("GET", "/api/tasks/{incidencia}", N, 200, 200, 200, 200, 200),
		fila("GET", "/api/tasks/client/{client}", N, 200, F, 200, F, F),
		fila("GET", "/api/tasks/professional/{professional}", N, F, 200, 200, F, F),
		fila("POST", "/api/tasks/{tasca}/finalitzar", "{}", N, 200, 200, 200, F, F),
		fila("POST", "/api/tasks/{finalitzada}/avaluar", "{\"valoracio\":4,\"comentari\":\"ok\"}", N, 200, F, F, F, F),
		fila("POST", "/api/tasks/{tasca}/cancellar", "{\"motiu\":\"No puc continuar\"}", N, 200, 200, F, F, F),
		publica("/api/tasks/disponibles"),
		nomesAdmin("GET", "/api/tasks/categoria/{categoria}", null, 200),
		fila("DELETE", "/api/tasks/{incidenciaLliure}", N, 204, F, 204, F, F),
		fila("DELETE", "/api/tasks/{sollicitud}", N, 204, F, F, F, F),
		fila("DELETE", "/api/tasks/{tasca}", N, 400, 400, 400, 400, 400),

		// ---------- Façana d'ofertes
		publica("/api/offers"),
		publica("/api/offers/{oferta}"),
		publica("/api/offers/professional/{professional}"),
		fila("POST", "/api/offers", "{\"categoria\":\"Pintura\",\"price\":\"30€\",\"note\":\"Pinte\"}", N, F, 200, 200, F, 200),
		fila("POST", "/api/offers", "{\"incidenciaId\":\"{incidenciaLliure}\",\"price\":\"30€\",\"note\":\"Pinte\"}", N, F, 200, F, F, 200),
		fila("POST", "/api/offers/{ofertaLliure}/contractar", "{}", N, 200, F, F, 200, F),
		fila("DELETE", "/api/offers/{oferta}", N, F, 204, 204, F, F),

		// ---------- Façana d'incidències
		publica("/api/incidencies"),
		publica("/api/incidencies/{incidencia}"),
		publica("/api/incidencies/client/{client}"),
		fila("POST", "/api/incidencies", "{\"titol\":\"Avaria\"}", N, 200, F, 200, 200, F),
		fila("DELETE", "/api/incidencies/{incidenciaLliure}", N, 204, F, 204, F, F),
		fila("POST", "/api/incidencies/{incidenciaLliure}/tancar", "{}", N, 200, F, 200, F, F),

		// ---------- Façana de sol·licituds
		fila("GET", "/api/interessos/professional/{professional}", N, F, 200, 200, F, F),
		fila("GET", "/api/interessos/client/{client}", N, 200, F, 200, F, F),
		fila("POST", "/api/interessos/{sollicitud}/acceptar", "{}", N, F, 200, F, F, F),
		fila("POST", "/api/interessos/{sollicitud}/rebutjar", "{}", N, F, 204, F, F, F),
		fila("POST", "/api/interessos/{sollicitud}/retirar", "{}", N, 204, F, F, F, F),

		// ---------- Pressupostos
		nomesAdmin("GET", "/api/pressupostos", null, 200),
		fila("POST", "/api/pressupostos", "{\"incidenciaId\":\"{incidenciaLliure}\",\"preu\":\"100€\"}", N, F, 201, F, F, 201),
		fila("GET", "/api/pressupostos/incidencia/{incidencia}", N, 200, 200, 200, 200, 200),
		fila("GET", "/api/pressupostos/{pressupost}", N, 200, 200, 200, F, F),
		fila("GET", "/api/pressupostos/professional/{professional}", N, F, 200, 200, F, F),
		fila("POST", "/api/pressupostos/{pressupost}/acceptar", "{}", N, 200, F, F, F, F),
		fila("POST", "/api/pressupostos/{pressupost}/rebutjar", "{}", N, 204, F, 204, F, F),

		// ---------- Avisos
		nomesAdmin("GET", "/api/avisos", null, 200),
		nomesAdmin("POST", "/api/avisos", "{\"destinatariId\":\"{client}\",\"missatge\":\"Hola\"}", 200),
		fila("GET", "/api/avisos/usuari/{client}", N, 200, F, 200, F, F),
		fila("PUT", "/api/avisos/{avis}/llegit", "{}", N, 200, F, 200, F, F),
		fila("DELETE", "/api/avisos/{avis}", N, 204, F, 204, F, F),

		// ---------- Ressenyes
		publica("/api/resenyes"),
		publica("/api/resenyes/treballador/{professional}"),
		publica("/api/resenyes/client/{client}"),
		fila("POST", "/api/resenyes", "{\"tascaId\":\"{finalitzada}\",\"puntuacio\":4,\"descripcio\":\"ok\"}", N, 200, F, F, F, F),

		// ---------- Llista negra
		nomesAdmin("GET", "/api/blacklist", null, 200),
		nomesAdmin("POST", "/api/blacklist", "{\"telefon\":\"699999999\",\"motiu\":\"Frau\"}", 201),
		nomesAdmin("DELETE", "/api/blacklist/{bloquejat}", null, 204),

		// ---------- Tauler
		publica("/api/dashboard"),

		// ---------- Cancel·lacions
		nomesAdmin("GET", "/api/cancellacions", null, 200),
		nomesAdmin("GET", "/api/cancellacions/negligencies/{client}", null, 200),
		nomesAdmin("POST", "/api/cancellacions/{cancellacio}/resoldre", "{\"resolucio\":\"REBUTJADA\"}", 200)
	);

	static Stream<Arguments> casos() {
		List<Arguments> casos = new ArrayList<>();
		for (Fila fila : FILES) {
			for (Actor actor : Actor.values()) {
				casos.add(Arguments.of(fila, actor, fila.esperat()[actor.ordinal()]));
			}
		}
		return casos.stream();
	}

	@Autowired
	@Qualifier("requestMappingHandlerMapping")
	private RequestMappingHandlerMapping rutes;

	private final Map<String, String> valors = new HashMap<>();
	private final Map<Actor, UUID> actors = new HashMap<>();

	@BeforeEach
	void dades() {
		UUID altreClient = usuari("CLIENT");
		UUID altreProfessional = usuari("PROFESSIONAL");
		jdbc.update("UPDATE usuaris SET telefon = '600000001' WHERE id = ?", professional);
		actors.put(Actor.CLIENT, client);
		actors.put(Actor.PROFESSIONAL, professional);
		actors.put(Actor.ADMIN, admin);
		actors.put(Actor.ALTRE_CLIENT, altreClient);
		actors.put(Actor.ALTRE_PROFESSIONAL, altreProfessional);

		UUID incidencia = publicacio("INCIDENCIA", client, "OBERTA");
		UUID assignada = publicacio("INCIDENCIA", client, "ASSIGNADA");
		UUID oferta = publicacio("OFERTA", professional, "ACTIVA");
		UUID tasca = tasca(assignada, client, professional, "EN_CURS");
		UUID enRevisio = tasca(publicacio("INCIDENCIA", client, "ASSIGNADA"), client, professional, "EN_REVISIO");

		valors.put("client", client.toString());
		valors.put("correuClient", client + "@test.es");
		valors.put("professional", professional.toString());
		valors.put("usuariLliure", usuari("CLIENT").toString());
		valors.put("rolProfessional", "PROFESSIONAL");
		valors.put("categoria", "Pintura");
		valors.put("incidencia", incidencia.toString());
		valors.put("incidenciaLliure", publicacio("INCIDENCIA", client, "OBERTA").toString());
		valors.put("oferta", oferta.toString());
		valors.put("ofertaLliure", publicacio("OFERTA", professional, "ACTIVA").toString());
		valors.put("tasca", tasca.toString());
		valors.put("sollicitud", tasca(oferta, client, professional, "SOLLICITADA").toString());
		valors.put("finalitzada", tasca(publicacio("INCIDENCIA", client, "TANCADA"), client, professional, "FINALITZADA").toString());
		valors.put("pressupost", inserir("INSERT INTO pressupostos (id, publicacio_id, professional_id, preu) VALUES (?, ?, ?, '100€')", incidencia, professional));
		valors.put("avis", inserir("INSERT INTO avisos (id, destinatari_id, missatge) VALUES (?, ?, 'Hola')", client));
		valors.put("bloquejat", inserir("INSERT INTO blacklist_telefons (id, telefon) VALUES (?, '688888888')"));
		valors.put("cancellacio", inserir("INSERT INTO sollicituds_cancellacio (id, tasca_id, sollicitant_id, motiu) VALUES (?, ?, ?, 'Motiu')", enRevisio, client));
	}

	private String inserir(String sql, Object... parametres) {
		UUID id = UUID.randomUUID();
		Object[] tots = new Object[parametres.length + 1];
		tots[0] = id;
		System.arraycopy(parametres, 0, tots, 1, parametres.length);
		jdbc.update(sql, tots);
		return id.toString();
	}

	private String omplir(String plantilla) {
		String resultat = plantilla;
		for (Map.Entry<String, String> valor : valors.entrySet()) {
			resultat = resultat.replace("{" + valor.getKey() + "}", valor.getValue());
		}
		return resultat;
	}

	@ParameterizedTest(name = "{0} com a {1} dona {2}")
	@MethodSource("casos")
	@DisplayName("Cada ruta respon el que toca a cada actor")
	void matriu(Fila fila, Actor actor, int esperat) throws Exception {
		MockHttpServletRequestBuilder peticio = request(HttpMethod.valueOf(fila.metode()), omplir(fila.ruta()));
		if (fila.cos() != null) {
			peticio.contentType(MediaType.APPLICATION_JSON).content(omplir(fila.cos()));
		}
		if (actor != Actor.ANONIM) {
			peticio.header(HttpHeaders.AUTHORIZATION, bearer(actors.get(actor)));
		}
		mvc.perform(peticio).andExpect(status().is(esperat));
	}

	@Test
	@DisplayName("Totes les rutes de l'API són a la matriu: una ruta nova sense fila fa fallar aquest test")
	void capRutaSenseFila() {
		Set<String> api = new TreeSet<>();
		rutes.getHandlerMethods().keySet().forEach(info -> {
			for (String patro : info.getPatternValues()) {
				if (patro.startsWith("/api/")) {
					for (RequestMethod metode : info.getMethodsCondition().getMethods()) {
						api.add(metode + " " + sensePlantilla(patro));
					}
				}
			}
		});
		Set<String> matriu = FILES.stream()
			.map(f -> f.metode() + " " + sensePlantilla(f.ruta()))
			.collect(Collectors.toCollection(TreeSet::new));

		assertThat(matriu).isEqualTo(api);
	}

	private static String sensePlantilla(String ruta) {
		return ruta.replaceAll("\\{[^}]+}", "{}");
	}

	@Test
	@DisplayName("Un token invàlid dona 401 fins i tot a les rutes públiques")
	void tokenInvalid() throws Exception {
		mvc.perform(get("/api/offers").header(HttpHeaders.AUTHORIZATION, "Bearer no-es-un-token")).andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("Les capçaleres X-User-* no donen accés a cap ruta que abans se'n fiava")
	void capcaleresSenseEfecte() throws Exception {
		String[] rutesAdmin = {"/api/pressupostos", "/api/blacklist"};
		for (String ruta : rutesAdmin) {
			mvc.perform(get(ruta).header("X-User-Rol", "admin")).andExpect(status().isUnauthorized());
			mvc.perform(getCom(client, ruta).header("X-User-Rol", "admin")).andExpect(status().isForbidden());
		}
		mvc.perform(getCom(actors.get(Actor.ALTRE_CLIENT), "/api/pressupostos/" + valors.get("pressupost"))
			.header("X-User-Id", professional.toString()).header("X-User-Rol", "admin"))
			.andExpect(status().isForbidden());
		mvc.perform(getCom(actors.get(Actor.ALTRE_CLIENT), "/api/pressupostos/incidencia/" + valors.get("incidencia"))
			.header("X-User-Id", client.toString()).header("X-User-Rol", "admin"))
			.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	@DisplayName("El cos de la petició no permet publicar en nom d'un altre")
	void autorEsElDelToken() throws Exception {
		UUID altreProfessional = actors.get(Actor.ALTRE_PROFESSIONAL);
		UUID altreClient = actors.get(Actor.ALTRE_CLIENT);

		String oferta = cos(mvc.perform(postCom(altreProfessional, "/api/offers",
			"{\"professional\":\"" + professional + "\",\"categoria\":\"Pintura\",\"price\":\"30€\",\"note\":\"Falsa\"}"))).get("id").asText();
		String incidencia = cos(mvc.perform(postCom(altreClient, "/api/incidencies",
			"{\"client\":\"" + client + "\",\"clientId\":\"" + client + "\",\"titol\":\"Falsa\"}"))).get("id").asText();
		String pressupost = cos(mvc.perform(postCom(altreProfessional, "/api/pressupostos",
			"{\"incidenciaId\":\"" + incidencia + "\",\"professionalId\":\"" + professional + "\",\"preu\":\"1€\"}"))).get("id").asText();

		assertThat(autorDe(oferta)).isEqualTo(altreProfessional.toString());
		assertThat(autorDe(incidencia)).isEqualTo(altreClient.toString());
		assertThat(jdbc.queryForObject("SELECT professional_id::text FROM pressupostos WHERE id = ?::uuid", String.class, pressupost))
			.isEqualTo(altreProfessional.toString());
	}

	private String autorDe(String publicacio) {
		return jdbc.queryForObject("SELECT autor_id::text FROM publicacions WHERE id = ?::uuid", String.class, publicacio);
	}

	@Test
	@DisplayName("Sense sessió no es crea cap publicació ni pressupost, encara que el cos indique un autor")
	void anonimNoPublica() throws Exception {
		int publicacions = files("publicacions");
		int pressupostos = files("pressupostos");

		mvc.perform(post("/api/offers").contentType(MediaType.APPLICATION_JSON)
			.content("{\"professional\":\"" + professional + "\",\"price\":\"1\",\"note\":\"n\"}")).andExpect(status().isUnauthorized());
		mvc.perform(post("/api/incidencies").contentType(MediaType.APPLICATION_JSON)
			.content("{\"client\":\"" + client + "\",\"titol\":\"t\"}")).andExpect(status().isUnauthorized());
		mvc.perform(post("/api/pressupostos").contentType(MediaType.APPLICATION_JSON)
			.content("{\"incidenciaId\":\"" + valors.get("incidenciaLliure") + "\",\"professionalId\":\"" + professional + "\",\"preu\":\"1€\"}"))
			.andExpect(status().isUnauthorized());

		assertThat(files("publicacions")).isEqualTo(publicacions);
		assertThat(files("pressupostos")).isEqualTo(pressupostos);
	}

	@Test
	@DisplayName("Els pressupostos d'una incidència: tots per al client i l'admin, el seu per al professional, cap per a la resta")
	void pressupostosSegonsQuiPregunta() throws Exception {
		String ruta = "/api/pressupostos/incidencia/" + valors.get("incidencia");
		inserir("INSERT INTO pressupostos (id, publicacio_id, professional_id, preu) VALUES (?, ?, ?, '90€')",
			UUID.fromString(valors.get("incidencia")), actors.get(Actor.ALTRE_PROFESSIONAL));

		mvc.perform(getCom(client, ruta)).andExpect(jsonPath("$.length()").value(2));
		mvc.perform(getCom(admin, ruta)).andExpect(jsonPath("$.length()").value(2));
		mvc.perform(getCom(professional, ruta)).andExpect(jsonPath("$.length()").value(1))
			.andExpect(jsonPath("$[0].id").value(valors.get("pressupost")));
		mvc.perform(getCom(actors.get(Actor.ALTRE_CLIENT), ruta)).andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	@DisplayName("El tauler no exposa les tasques de ningú més, però manté els comptadors globals")
	void taulerSenseTasquesAlienes() throws Exception {
		mvc.perform(get("/api/dashboard")).andExpect(jsonPath("$.tasks.length()").value(0)).andExpect(jsonPath("$.completedProjects").value(1));
		mvc.perform(getCom(actors.get(Actor.ALTRE_CLIENT), "/api/dashboard")).andExpect(jsonPath("$.tasks.length()").value(0));
		mvc.perform(getCom(client, "/api/dashboard")).andExpect(jsonPath("$.tasks.length()").value(3));
		mvc.perform(getCom(admin, "/api/dashboard")).andExpect(jsonPath("$.tasks.length()").value(3));
	}
}
