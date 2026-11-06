package com.avisa.imatges;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.avisa.AvisaApplication;
import com.avisa.config.AdminSeeder;
import com.avisa.config.JwtService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Pujar, servir i esborrar imatges per l'API, amb la seguretat i els tokens reals.
 * La base és una H2 pròpia amb només les columnes que toca el servei; la matriu de permisos sencera
 * i les restriccions de PostgreSQL es proven a MatriuPermisosIT i ImatgesIT.
 */
@SpringBootTest(classes = AvisaApplication.class)
@AutoConfigureMockMvc
class ImatgesApiTest {

	private static final long MAX_BYTES = 20_000;

	@TempDir
	static Path carpeta;

	@DynamicPropertySource
	static void propietats(DynamicPropertyRegistry registre) {
		registre.add("spring.datasource.url", () -> "jdbc:h2:mem:imatges-api;DB_CLOSE_DELAY=-1;MODE=PostgreSQL");
		registre.add("avisa.imatges.directori", carpeta::toString);
		registre.add("avisa.imatges.max-bytes", () -> MAX_BYTES);
	}

	/** H2 no té l'esquema de PostgreSQL; el seeder es prova a AdminSeederIT. */
	@MockBean
	AdminSeeder adminSeeder;

	@Autowired
	MockMvc mvc;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	JwtService jwt;

	private final ObjectMapper json = new ObjectMapper();

	private UUID client;
	private UUID altreClient;
	private UUID professional;
	private UUID admin;

	@BeforeEach
	void dades() throws IOException {
		// Les files s'esborren per SQL, no per l'API: els fitxers del test anterior es netegen a mà
		try (Stream<Path> anteriors = Files.list(carpeta)) {
			for (Path p : anteriors.toList()) {
				Files.delete(p);
			}
		}
		jdbc.execute("CREATE TABLE IF NOT EXISTS usuaris (id UUID PRIMARY KEY, rol VARCHAR(20), foto_url VARCHAR(200), logo_url VARCHAR(200))");
		jdbc.execute("DELETE FROM usuaris");
		client = usuari("CLIENT");
		altreClient = usuari("CLIENT");
		professional = usuari("PROFESSIONAL");
		admin = usuari("ADMIN");
	}

	@Test
	@DisplayName("Un client puja un WebP com a foto i es pot llegir sense sessió, en les dues mides")
	void pujarIServir() throws Exception {
		JsonNode r = cos(pujar(client, "foto", webp()).andExpect(status().isOk()));
		String url = r.get("url").asText();
		String urlPetita = r.get("urlPetita").asText();

		assertThat(url).matches("^/api/imatges/[0-9a-f-]{36}\\.jpg$");
		assertThat(urlPetita).isEqualTo(url.replace(".jpg", "-petita.jpg"));
		assertThat(columna(client, "foto_url")).isEqualTo(url);

		for (String ruta : new String[] {url, urlPetita}) {
			byte[] imatge = mvc.perform(get(ruta))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.CONTENT_TYPE, "image/jpeg"))
				.andExpect(header().string(HttpHeaders.CACHE_CONTROL, "max-age=31536000, public, immutable"))
				.andExpect(header().string("X-Content-Type-Options", "nosniff"))
				.andExpect(header().string("Content-Security-Policy", "default-src 'none'; sandbox"))
				.andReturn().getResponse().getContentAsByteArray();
			assertThat(FormatImatge.detectar(imatge)).contains(FormatImatge.JPEG);
		}
	}

	@Test
	@DisplayName("Una foto nova esborra del disc els dos fitxers de l'anterior")
	void substituirEsborraLAnterior() throws Exception {
		String primera = cos(pujar(client, "foto", webp())).get("url").asText();
		String segona = cos(pujar(client, "foto", webp())).get("url").asText();

		assertThat(segona).isNotEqualTo(primera);
		mvc.perform(get(primera)).andExpect(status().isNotFound());
		mvc.perform(get(ServeiImatges.urlPetita(primera))).andExpect(status().isNotFound());
		mvc.perform(get(segona)).andExpect(status().isOk());
		assertThat(fitxers()).isEqualTo(2);
	}

	@Test
	@DisplayName("Ningú puja imatges en nom d'un altre, tampoc l'administrador; sense sessió, 401")
	void nomesElMateix() throws Exception {
		mvc.perform(multipart("/api/users/" + client + "/foto").file(fitxer(webp()))).andExpect(status().isUnauthorized());
		pujar(altreClient, client, "foto", webp()).andExpect(status().isForbidden());
		pujar(professional, client, "foto", webp()).andExpect(status().isForbidden());
		pujar(admin, client, "foto", webp()).andExpect(status().isForbidden());

		assertThat(columna(client, "foto_url")).isNull();
		assertThat(fitxers()).isZero();
	}

	@Test
	@DisplayName("El logotip només el pot pujar un professional, per a ell mateix")
	void logoNomesProfessional() throws Exception {
		pujar(client, "logo", webp()).andExpect(status().isForbidden());
		pujar(admin, "logo", webp()).andExpect(status().isForbidden());
		pujar(professional, "logo", webp()).andExpect(status().isOk());

		assertThat(columna(client, "logo_url")).isNull();
		assertThat(columna(professional, "logo_url")).startsWith(ServeiImatges.PREFIX_URL);
	}

	@Test
	@DisplayName("Esborrar: el mateix usuari o l'admin (moderació), i els fitxers desapareixen")
	void esborrar() throws Exception {
		String foto = cos(pujar(client, "foto", webp())).get("url").asText();

		mvc.perform(delete("/api/users/" + client + "/foto")).andExpect(status().isUnauthorized());
		mvc.perform(com(delete("/api/users/" + client + "/foto"), altreClient)).andExpect(status().isForbidden());
		mvc.perform(com(delete("/api/users/" + client + "/foto"), professional)).andExpect(status().isForbidden());
		mvc.perform(get(foto)).andExpect(status().isOk());

		mvc.perform(com(delete("/api/users/" + client + "/foto"), admin)).andExpect(status().isNoContent());
		assertThat(columna(client, "foto_url")).isNull();
		mvc.perform(get(foto)).andExpect(status().isNotFound());
		assertThat(fitxers()).isZero();

		cos(pujar(professional, "logo", webp()));
		mvc.perform(com(delete("/api/users/" + professional + "/logo"), professional)).andExpect(status().isNoContent());
		assertThat(fitxers()).isZero();

		// Esborrar el que no hi és no és cap error
		mvc.perform(com(delete("/api/users/" + client + "/foto"), client)).andExpect(status().isNoContent());
	}

	@Test
	@DisplayName("Un fitxer que no és imatge (SVG) dona 400; un de massa gran, 413; i cap dels dos toca res")
	void rebuigs() throws Exception {
		byte[] svg = "<svg xmlns=\"http://www.w3.org/2000/svg\"><script>alert(1)</script></svg>".getBytes(StandardCharsets.UTF_8);
		pujar(client, "foto", svg).andExpect(status().isBadRequest());

		byte[] gran = soroll(120, 120);
		assertThat(gran.length).isGreaterThan((int) MAX_BYTES);
		pujar(client, "foto", gran).andExpect(status().isPayloadTooLarge());

		mvc.perform(com(multipart("/api/users/" + client + "/foto"), client)).andExpect(status().isBadRequest());
		assertThat(columna(client, "foto_url")).isNull();
		assertThat(fitxers()).isZero();
	}

	@Test
	@DisplayName("Un token d'un usuari que ja no existeix dona 404 i no deixa fitxers orfes")
	void usuariInexistent() throws Exception {
		UUID esborrat = usuari("CLIENT");
		String token = bearer(esborrat);
		jdbc.update("DELETE FROM usuaris WHERE id = ?", esborrat);

		mvc.perform(multipart("/api/users/" + esborrat + "/foto").file(fitxer(webp())).header(HttpHeaders.AUTHORIZATION, token))
			.andExpect(status().isNotFound());
		assertThat(fitxers()).isZero();
	}

	@Test
	@DisplayName("GET /api/imatges només serveix noms generats pel backend")
	void nomsEstranys() throws Exception {
		Files.writeString(carpeta.resolve("secret.txt"), "no");
		for (String nom : new String[] {"secret.txt", UUID.randomUUID() + ".svg", UUID.randomUUID() + ".jpg", "..%2Fsecret.txt"}) {
			mvc.perform(get("/api/imatges/" + nom)).andExpect(r -> assertThat(r.getResponse().getStatus()).isIn(400, 404));
		}
		Files.delete(carpeta.resolve("secret.txt"));
	}

	// ---------- Ajudes ----------

	private UUID usuari(String rol) {
		UUID id = UUID.randomUUID();
		jdbc.update("INSERT INTO usuaris (id, rol) VALUES (?, ?)", id, rol);
		return id;
	}

	private String bearer(UUID usuari) {
		String rol = jdbc.queryForObject("SELECT rol FROM usuaris WHERE id = ?", String.class, usuari);
		return "Bearer " + jwt.generateToken(usuari.toString(), usuari + "@test.es", rol);
	}

	private MockHttpServletRequestBuilder com(MockHttpServletRequestBuilder peticio, UUID usuari) {
		return peticio.header(HttpHeaders.AUTHORIZATION, bearer(usuari));
	}

	private ResultActions pujar(UUID usuari, String tipus, byte[] dades) throws Exception {
		return pujar(usuari, usuari, tipus, dades);
	}

	private ResultActions pujar(UUID qui, UUID perA, String tipus, byte[] dades) throws Exception {
		return mvc.perform(com(multipart("/api/users/" + perA + "/" + tipus).file(fitxer(dades)), qui));
	}

	private static MockMultipartFile fitxer(byte[] dades) {
		// El nom i el tipus els tria qui puja: el backend no se'n fia
		return new MockMultipartFile("fitxer", "foto.png", MediaType.IMAGE_PNG_VALUE, dades);
	}

	private String columna(UUID usuari, String columna) {
		return jdbc.queryForObject("SELECT " + columna + " FROM usuaris WHERE id = ?", String.class, usuari);
	}

	private JsonNode cos(ResultActions resposta) throws Exception {
		return json.readTree(resposta.andReturn().getResponse().getContentAsString());
	}

	private static long fitxers() throws IOException {
		try (Stream<Path> tots = Files.list(carpeta)) {
			return tots.count();
		}
	}

	private static byte[] webp() throws IOException {
		try (InputStream in = ImatgesApiTest.class.getResourceAsStream("/imatges/girada-6.webp")) {
			return in.readAllBytes();
		}
	}

	private static byte[] soroll(int w, int h) throws IOException {
		BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		Random r = new Random(7);
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				img.setRGB(x, y, r.nextInt());
			}
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ImageIO.write(img, "png", out);
		return out.toByteArray();
	}
}
