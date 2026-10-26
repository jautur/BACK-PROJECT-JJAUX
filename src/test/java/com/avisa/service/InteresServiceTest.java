package com.avisa.service;

import com.avisa.dto.CrearInteresRequest;
import com.avisa.model.Interes;
import com.avisa.model.Tasca;
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
class InteresServiceTest {

    @Mock
    private InMemoryCatalog catalog;

    @InjectMocks
    private InteresService interesService;

    @Test
    @DisplayName("Crear interès nou")
    void haDeCrearInteres() {
        CrearInteresRequest req = new CrearInteresRequest("PUB-1", "u-client", "Client A", "Estic interessat");
        Interes interes = new Interes("INT-1", "PUB-1", "u-client", "Client A", "Estic interessat", null, LocalDateTime.now());
        when(catalog.addInteres(req)).thenReturn(interes);

        Interes result = interesService.create(req);
        assertThat(result).isNotNull();
        assertThat(result.missatge()).isEqualTo("Estic interessat");
    }

    @Test
    @DisplayName("Llistar interessos per professional")
    void haDeLlistarPerProfessional() {
        Interes interes = new Interes("INT-1", "PUB-1", "u-client", "Client A", "Estic interessat", null, LocalDateTime.now());
        when(catalog.findInteressosByProfessional("pro1")).thenReturn(List.of(interes));

        List<Interes> result = interesService.listByProfessional("pro1");
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("Acceptar interès genera tasca")
    void haDAcceptarInteres() {
        Tasca tasca = new Tasca("T-1", "Servei", "Desc", "Cat", "u-client", "Client A", "u-pro", "Pro 1", null, null, null, "50€", LocalDateTime.now(), null, null, null, null);
        when(catalog.acceptarInteres("INT-1")).thenReturn(Optional.of(tasca));

        Optional<Tasca> result = interesService.accept("INT-1");
        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo("T-1");
    }

    @Test
    @DisplayName("Rebutjar interès")
    void haDeRebutjarInteres() {
        when(catalog.rebutjarInteres("INT-1")).thenReturn(true);

        boolean rebutjat = interesService.reject("INT-1");
        assertThat(rebutjat).isTrue();
        verify(catalog).rebutjarInteres("INT-1");
    }
}

