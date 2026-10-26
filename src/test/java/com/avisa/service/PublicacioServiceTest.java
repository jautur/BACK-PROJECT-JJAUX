package com.avisa.service;

import com.avisa.model.Publicacio;
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
class PublicacioServiceTest {

    @Mock
    private InMemoryCatalog catalog;

    @InjectMocks
    private PublicacioService publicacioService;

    @Test
    @DisplayName("Llistar totes les publicacions")
    void haDeLlistarPublicacions() {
        Publicacio p = new Publicacio("PUB-1", "u-1", "Joan", "OFERTA", "Pintura", "Desc", "Pintura", "50€", LocalDateTime.now());
        when(catalog.publicacions()).thenReturn(List.of(p));

        List<Publicacio> result = publicacioService.list();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).titol()).isEqualTo("Pintura");
    }

    @Test
    @DisplayName("Llistar publicacions per tipus")
    void haDeLlistarPerTipus() {
        Publicacio p = new Publicacio("PUB-1", "u-1", "Joan", "INCIDENCIA", "Fuita d'aigua", "Desc", "Fontaneria", "70€", LocalDateTime.now());
        when(catalog.publicacionsByTipus("INCIDENCIA")).thenReturn(List.of(p));

        List<Publicacio> result = publicacioService.listByTipus("INCIDENCIA");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).tipus()).isEqualTo("INCIDENCIA");
    }

    @Test
    @DisplayName("Contractació directa d'oferta")
    void haDeContractarDirectament() {
        Tasca tasca = new Tasca("T-1", "Pintura", "Desc", "Pintura", "u-client", "Maria", "u-pro", "Joan", null, null, null, "50€", LocalDateTime.now(), null, null, null, null);
        when(catalog.convertOfferToTask("PUB-1", "Maria")).thenReturn(Optional.of(tasca));

        Optional<Tasca> result = publicacioService.contractDirect("PUB-1", "Maria");
        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo("T-1");
    }

    @Test
    @DisplayName("Eliminar publicació")
    void haDEliminarPublicacio() {
        publicacioService.delete("PUB-1");
        verify(catalog).deletePublicacio("PUB-1");
    }
}

