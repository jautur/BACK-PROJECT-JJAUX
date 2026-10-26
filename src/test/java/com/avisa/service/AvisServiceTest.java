package com.avisa.service;

import com.avisa.model.Avis;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AvisServiceTest {

    @Mock
    private InMemoryCatalog catalog;

    @InjectMocks
    private AvisService avisService;

    @Test
    @DisplayName("Llistar tots els avisos")
    void haDeLlistarAvisos() {
        Avis avis = new Avis("A-1", "u-1", "Tens un pressupost nou", false, LocalDate.now());
        when(catalog.avisos()).thenReturn(List.of(avis));

        List<Avis> list = avisService.list();
        assertThat(list).hasSize(1);
        assertThat(list.get(0).missatge()).isEqualTo("Tens un pressupost nou");
    }

    @Test
    @DisplayName("Llistar avisos per usuari")
    void haDeLlistarPerUsuari() {
        Avis avis = new Avis("A-1", "u-1", "Tens un pressupost nou", false, LocalDate.now());
        when(catalog.findAvisosByUsuari("u-1")).thenReturn(List.of(avis));

        List<Avis> list = avisService.listByUsuari("u-1");
        assertThat(list).hasSize(1);
        assertThat(list.get(0).usuariId()).isEqualTo("u-1");
    }

    @Test
    @DisplayName("Crear un avís")
    void haDeCrearAvis() {
        Avis avis = new Avis("A-2", "u-2", "Feina completada", false, LocalDate.now());
        when(catalog.addAvis("u-2", "Feina completada")).thenReturn(avis);

        Avis creat = avisService.create("u-2", "Feina completada");
        assertThat(creat).isNotNull();
        assertThat(creat.id()).isEqualTo("A-2");
    }
}

