package com.avisa.service;

import com.avisa.dto.DashboardResponse;
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
class DashboardServiceTest {

    @Mock
    private InMemoryCatalog catalog;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    @DisplayName("Obtenir dashboard consolidat")
    void haDObtenirDashboard() {
        DashboardResponse mockDashboard = new DashboardResponse(5, 10, 20, List.of(), List.of(), List.of());
        when(catalog.dashboard()).thenReturn(mockDashboard);

        DashboardResponse resp = dashboardService.getDashboard(null, false);
        assertThat(resp).isNotNull();
        assertThat(resp.openTasks()).isEqualTo(5);
        assertThat(resp.activeOffers()).isEqualTo(10);
        assertThat(resp.completedProjects()).isEqualTo(20);
    }

    @Test
    @DisplayName("Les tasques del dashboard són només les de qui pregunta, i totes per a un administrador")
    void lesTasquesSonPrivades() {
        com.avisa.model.Tasca meua = new com.avisa.model.Tasca("T-1", "A", "D", "C", "u-1", "Client", "u-2", "Pro", null, null, null, "1€", java.time.LocalDateTime.now(), null, null, null, null);
        com.avisa.model.Tasca aliena = new com.avisa.model.Tasca("T-2", "B", "D", "C", "u-3", "Altre", "u-4", "Altre pro", null, null, null, "1€", java.time.LocalDateTime.now(), null, null, null, null);
        when(catalog.dashboard()).thenReturn(new DashboardResponse(1, 2, 3, List.of(meua, aliena), List.of(), List.of()));

        assertThat(dashboardService.getDashboard(null, false).tasks()).isEmpty();
        assertThat(dashboardService.getDashboard("u-1", false).tasks()).containsExactly(meua);
        assertThat(dashboardService.getDashboard("u-2", false).tasks()).containsExactly(meua);
        assertThat(dashboardService.getDashboard("u-9", true).tasks()).containsExactly(meua, aliena);
        assertThat(dashboardService.getDashboard("u-1", false).completedProjects()).isEqualTo(3);
    }
}

