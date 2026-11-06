package com.avisa.service;

import com.avisa.config.JwtService;
import com.avisa.dto.DemanaRecuperacioRequest;
import com.avisa.dto.LoginRequest;
import com.avisa.dto.RecuperacioResponse;
import com.avisa.dto.RegisterRequest;
import com.avisa.dto.RestablirContrasenyaRequest;
import com.avisa.model.Usuari;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceTest {

    private static final String CODI = "482915";

    @Mock
    private InMemoryCatalog catalog;

    @Mock
    private JwtService jwtService;

    @Mock
    private EmailService emailService;

    private final MockEnvironment entorn = new MockEnvironment();
    private final AtomicReference<Instant> ara = new AtomicReference<>(Instant.parse("2026-11-06T10:00:00Z"));
    private AuthService authService;

    private final Usuari maria = new Usuari("u-10", "Maria", "maria@test.com", "CLIENT", null, null);

    @BeforeEach
    void setUp() {
        Clock rellotge = new Clock() {
            @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
            @Override public Clock withZone(java.time.ZoneId zone) { return this; }
            @Override public Instant instant() { return ara.get(); }
        };
        authService = new AuthService(catalog, jwtService, entorn, new LimitIntentsLogin(rellotge), emailService, () -> CODI, rellotge);
        // Maria es pot trobar pel seu ID, pel nom (findUserById) o pel correu
        when(catalog.findUserById(anyString())).thenReturn(Optional.empty());
        when(catalog.findUserByEmail(anyString())).thenReturn(Optional.empty());
        when(catalog.findUserById("u-10")).thenReturn(Optional.of(maria));
        when(catalog.findUserById("Maria")).thenReturn(Optional.of(maria));
        when(catalog.findUserByEmail("maria@test.com")).thenReturn(Optional.of(maria));
    }

    private RecuperacioResponse demanar(String identificador) {
        return authService.sollicitarRecuperacio(new DemanaRecuperacioRequest(identificador));
    }

    private RecuperacioResponse restablir(String identificador, String codi) {
        return authService.restablirContrasenya(new RestablirContrasenyaRequest(identificador, codi, "novaContrasenya123"));
    }

    @Test
    @DisplayName("Login exitós retorna usuari amb token")
    void haDeFerLoginExitos() {
        Usuari user = new Usuari("u-1", "Joan", "joan@test.com", "client", "600111222", "Bio");
        when(catalog.authenticate("joan@test.com", "pass123")).thenReturn(Optional.of(user));
        when(jwtService.generateToken("u-1", "joan@test.com", "client")).thenReturn("jwt-mock-token");

        Optional<Usuari> result = authService.login(new LoginRequest("joan@test.com", "pass123"));

        assertThat(result).isPresent();
        assertThat(result.get().token()).isEqualTo("jwt-mock-token");
        assertThat(result.get().nom()).isEqualTo("Joan");
    }

    @Test
    @DisplayName("Login fallit retorna buit")
    void haDeFallarLoginAmbCredencialsIncorrectes() {
        when(catalog.authenticate("noexisteix@test.com", "wrong")).thenReturn(Optional.empty());

        assertThat(authService.login(new LoginRequest("noexisteix@test.com", "wrong"))).isEmpty();
    }

    @Test
    @DisplayName("Registre d'usuari nou amb token")
    void haDeRegistrarUsuariNou() {
        RegisterRequest req = new RegisterRequest("Carles", "carles@test.com", "secret123", "professional", "600333444");
        Usuari nou = new Usuari("u-2", "Carles", "carles@test.com", "professional", "600333444", "Bio");
        when(catalog.addUser(req)).thenReturn(nou);
        when(jwtService.generateToken("u-2", "carles@test.com", "professional")).thenReturn("jwt-token-nou");

        Usuari result = authService.register(req);

        assertThat(result.token()).isEqualTo("jwt-token-nou");
        assertThat(result.nom()).isEqualTo("Carles");
    }

    // ---------------- Recuperació de contrasenya

    @Test
    @DisplayName("La resposta és la mateixa existisca o no el compte, i no porta ni el codi ni el correu")
    void respostaIgualSensCodiNiCorreu() {
        RecuperacioResponse existent = demanar("Maria");
        RecuperacioResponse inexistent = demanar("ningu@test.com");

        assertThat(existent).isEqualTo(inexistent);
        assertThat(existent.success()).isTrue();
        assertThat(existent.toString()).doesNotContain(CODI).doesNotContain("maria@test.com");
    }

    @Test
    @DisplayName("El codi només s'escriu al log amb el perfil dev")
    void codiNomesAlLogEnDev(CapturedOutput sortida) {
        demanar("maria@test.com");
        assertThat(sortida.getAll()).doesNotContain(CODI);

        entorn.setActiveProfiles("dev");
        demanar("maria@test.com");
        assertThat(sortida.getAll()).contains(CODI).doesNotContain("maria@test.com");
    }

    @Test
    @DisplayName("Amb el codi correcte es canvia la contrasenya")
    void restablirAmbCodiCorrecte() {
        demanar("maria@test.com");

        assertThat(restablir("maria@test.com", CODI).success()).isTrue();
        verify(catalog).changePassword("u-10", "novaContrasenya123");
    }

    @Test
    @DisplayName("Després de 5 intents fallits el codi queda invalidat, encara que després s'envie el correcte")
    void cincIntentsFallitsInvaliden() {
        demanar("maria@test.com");
        for (int i = 0; i < AuthService.INTENTS_MAXIMS; i++) {
            assertThat(restablir("maria@test.com", "000000").success()).isFalse();
        }

        RecuperacioResponse correcte = restablir("maria@test.com", CODI);

        assertThat(correcte.success()).isFalse();
        assertThat(correcte.message()).isEqualTo(AuthService.MISSATGE_CODI_NO_VALID);
        verify(catalog, never()).changePassword(any(), any());
    }

    @Test
    @DisplayName("Amb 4 intents fallits el codi correcte encara val")
    void quatreIntentsFallitsNoInvaliden() {
        demanar("maria@test.com");
        for (int i = 0; i < AuthService.INTENTS_MAXIMS - 1; i++) {
            restablir("maria@test.com", "000000");
        }

        assertThat(restablir("maria@test.com", CODI).success()).isTrue();
    }

    @Test
    @DisplayName("Els intents compten per usuari, s'identifique com s'identifique")
    void intentsPerUsuari() {
        demanar("maria@test.com");
        restablir("maria@test.com", "000001");
        restablir("Maria", "000002");
        restablir("u-10", "000003");
        restablir("MARIA@test.com".toLowerCase(), "000004");
        restablir("Maria", "000005");

        assertThat(restablir("u-10", CODI).success()).isFalse();
    }

    @Test
    @DisplayName("Un codi usat s'esborra: no torna a valer ni pel correu, ni pel nom, ni per l'ID")
    void unSolUsQualsevolIdentificador() {
        demanar("maria@test.com");
        assertThat(restablir("Maria", CODI).success()).isTrue();

        assertThat(restablir("maria@test.com", CODI).success()).isFalse();
        assertThat(restablir("Maria", CODI).success()).isFalse();
        assertThat(restablir("u-10", CODI).success()).isFalse();
        verify(catalog).changePassword("u-10", "novaContrasenya123");
    }

    @Test
    @DisplayName("El codi caduca als 15 minuts")
    void caduca() {
        demanar("maria@test.com");
        ara.set(ara.get().plus(AuthService.VALIDESA_CODI).plusSeconds(1));

        assertThat(restablir("maria@test.com", CODI).success()).isFalse();
        verify(catalog, never()).changePassword(any(), any());
    }

    @Test
    @DisplayName("Amb un compte inexistent l'error és el mateix que amb un codi incorrecte")
    void errorIgualSenseCompte() {
        demanar("maria@test.com");

        RecuperacioResponse senseCompte = restablir("ningu@test.com", CODI);
        RecuperacioResponse codiDolent = restablir("maria@test.com", "000000");

        assertThat(senseCompte).isEqualTo(codiDolent);
    }

    @Test
    @DisplayName("Canviar contrasenya directa")
    void haDeCanviarContrasenyaDirecta() {
        authService.changePassword("u-1", "passNova");
        verify(catalog).changePassword("u-1", "passNova");
    }

    @Test
    @DisplayName("Registra usuari, guarda el token de verificació d'email i envia correu")
    void registreEnviaCorreuVerificacio() {
        RegisterRequest req = new RegisterRequest("Pere", "pere@test.com", "secret123", "CLIENT", null);
        Usuari pere = new Usuari("u-20", "Pere", "pere@test.com", "CLIENT", null, null);
        when(catalog.addUser(req)).thenReturn(pere);
        when(jwtService.generateToken("u-20", "pere@test.com", "CLIENT")).thenReturn("jwt-pere");

        Usuari registrat = authService.register(req);

        assertThat(registrat.token()).isEqualTo("jwt-pere");
        verify(catalog).guardarTokenVerificacioEmail(anyString(), anyString(), any());
        verify(emailService).enviarEmailVerificacio(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Verifica el compte si el token és correcte")
    void verificarEmailAmbExit() {
        when(catalog.trobarUsuariPerTokenVerificacio(anyString(), any())).thenReturn(Optional.of(maria));

        Optional<Usuari> res = authService.verificarEmail("token-valid");

        assertThat(res).isPresent();
        assertThat(res.get().emailVerificat()).isTrue();
        verify(catalog).marcarEmailComVerificat("u-10");
    }

    @Test
    @DisplayName("Falla la verificació si el token no existeix o ha caducat")
    void verificarEmailTokenInvalid() {
        when(catalog.trobarUsuariPerTokenVerificacio(anyString(), any())).thenReturn(Optional.empty());

        Optional<Usuari> res = authService.verificarEmail("token-inexistent");

        assertThat(res).isEmpty();
        verify(catalog, never()).marcarEmailComVerificat(anyString());
    }
}
