package com.avisa.service;

import com.avisa.dto.CrearPressupostRequest;
import com.avisa.model.Pressupost;
import com.avisa.model.Tasca;
import com.avisa.model.Usuari;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PressupostServiceTest {

    @Mock
    private InMemoryCatalog catalog;

    @InjectMocks
    private PressupostService pressupostService;

    @Test
    @DisplayName("Crear un pressupost")
    void haDeCrearPressupost() {
        CrearPressupostRequest req = new CrearPressupostRequest("I-1", "u-pro", "Pro Manolo", "120€", "Pintar habitació", "2 dies");
        Pressupost p = new Pressupost("P-1", "I-1", "u-pro", "Pro Manolo", "120€", "Pintar habitació", "2 dies", LocalDateTime.now());
        when(catalog.addPressupost(req)).thenReturn(p);

        Pressupost creat = pressupostService.create(req);
        assertThat(creat).isNotNull();
        assertThat(creat.preu()).isEqualTo("120€");
    }

    @Test
    @DisplayName("Llistar pressupostos segurs per incidència")
    void haDeLlistarSegurPerIncidencia() {
        Pressupost p = new Pressupost("P-1", "I-1", "u-pro", "Pro Manolo", "120€", "Pintura", "1 dia", LocalDateTime.now());
        when(catalog.findPressupostosByIncidenciaSecure("I-1", "u-client", "Client Joan", "client"))
            .thenReturn(List.of(p));

        List<Pressupost> list = pressupostService.listByIncidenciaSecure("I-1", "u-client", "Client Joan", "client");
        assertThat(list).hasSize(1);
        assertThat(list.get(0).id()).isEqualTo("P-1");
    }

    @Test
    @DisplayName("Acceptar pressupost crea la tasca acordada")
    void haDAcceptarPressupost() {
        Tasca tasca = new Tasca("T-1", "Pintura", "Desc", "Pintura", "u-client", "Joan", "u-pro", "Manolo", null, null, null, "120€", LocalDateTime.now(), null, null, null, null);
        when(catalog.acceptarPressupost("P-1")).thenReturn(Optional.of(tasca));

        Optional<Tasca> result = pressupostService.accept("P-1");
        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo("T-1");
    }

    @Test
    @DisplayName("Rebutjar pressupost")
    void haDeRebutjarPressupost() {
        when(catalog.rebutjarPressupost("P-1")).thenReturn(true);

        boolean rebutjat = pressupostService.reject("P-1");
        assertThat(rebutjat).isTrue();
        verify(catalog).rebutjarPressupost("P-1");
    }

    @Test
    @DisplayName("Comprovar elegibilitat del professional")
    void haDeComprovarElegibilitatProfessional() {
        Usuari client = new Usuari("u-client", "Client", "c@test.com", "client", null, null);
        Usuari pro = new Usuari("u-pro", "Professional", "p@test.com", "professional", null, null);
        when(catalog.findUserById("u-client")).thenReturn(Optional.of(client));
        when(catalog.findUserById("u-pro")).thenReturn(Optional.of(pro));

        assertThat(pressupostService.isProfessionalEligible("u-client")).isFalse();
        assertThat(pressupostService.isProfessionalEligible("u-pro")).isTrue();
    }
}
