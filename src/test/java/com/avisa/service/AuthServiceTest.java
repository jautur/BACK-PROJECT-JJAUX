package com.avisa.service;

import com.avisa.config.JwtService;
import com.avisa.dto.DemanaRecuperacioRequest;
import com.avisa.dto.LoginRequest;
import com.avisa.dto.RecuperacioResponse;
import com.avisa.dto.RegisterRequest;
import com.avisa.dto.RestablirContrasenyaRequest;
import com.avisa.model.Usuari;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private InMemoryCatalog catalog;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

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

        Optional<Usuari> result = authService.login(new LoginRequest("noexisteix@test.com", "wrong"));

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Registre d'usuari nou amb token")
    void haDeRegistrarUsuariNou() {
        RegisterRequest req = new RegisterRequest("Carles", "carles@test.com", "secret123", "professional", "600333444");
        Usuari nou = new Usuari("u-2", "Carles", "carles@test.com", "professional", "600333444", "Bio");
        when(catalog.addUser(req)).thenReturn(nou);
        when(jwtService.generateToken("u-2", "carles@test.com", "professional")).thenReturn("jwt-token-nou");

        Usuari result = authService.register(req);

        assertThat(result).isNotNull();
        assertThat(result.token()).isEqualTo("jwt-token-nou");
        assertThat(result.nom()).isEqualTo("Carles");
    }

    @Test
    @DisplayName("Sol·licitar recuperació genera codi OTP per a usuari existent")
    void haDeGenerarCodiRecuperacio() {
        Usuari u = new Usuari("u-10", "Maria", "maria@test.com", "client", null, null);
        when(catalog.findUserById("maria@test.com")).thenReturn(Optional.empty());
        when(catalog.findUserByEmail("maria@test.com")).thenReturn(Optional.of(u));

        RecuperacioResponse resp = authService.sollicitarRecuperacio(new DemanaRecuperacioRequest("maria@test.com"));

        assertThat(resp.success()).isTrue();
        assertThat(resp.codiDev()).isNotNull();
        assertThat(resp.codiDev()).hasSize(6);
    }

    @Test
    @DisplayName("Restablir contrasenya amb èxit amb codi correcte")
    void haDeRestablirContrasenyaAmbCodiCorrecte() {
        Usuari u = new Usuari("u-10", "Maria", "maria@test.com", "client", null, null);
        when(catalog.findUserById("maria@test.com")).thenReturn(Optional.empty());
        when(catalog.findUserByEmail("maria@test.com")).thenReturn(Optional.of(u));

        RecuperacioResponse demanaResp = authService.sollicitarRecuperacio(new DemanaRecuperacioRequest("maria@test.com"));
        String codi = demanaResp.codiDev();

        RecuperacioResponse restablirResp = authService.restablirContrasenya(
            new RestablirContrasenyaRequest("maria@test.com", codi, "novaContrasenya123")
        );

        assertThat(restablirResp.success()).isTrue();
        verify(catalog).changePassword("u-10", "novaContrasenya123");
    }

    @Test
    @DisplayName("Restablir contrasenya falla si el codi és invàlid")
    void haDeFallarRestablirAmbCodiInvalid() {
        RecuperacioResponse resp = authService.restablirContrasenya(
            new RestablirContrasenyaRequest("maria@test.com", "999999", "novaContrasenya123")
        );

        assertThat(resp.success()).isFalse();
    }

    @Test
    @DisplayName("Canviar contrasenya directa")
    void haDeCanviarContrasenyaDirecta() {
        authService.changePassword("u-1", "passNova");
        verify(catalog).changePassword("u-1", "passNova");
    }
}
