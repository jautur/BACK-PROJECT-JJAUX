package com.avisa.controller;

import com.avisa.dto.DashboardResponse;
import com.avisa.service.DashboardService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {

    private MockMvc mockMvc;

    @Mock
    private DashboardService dashboardService;

    @InjectMocks
    private DashboardController dashboardController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(dashboardController).build();
    }

    @Test
    @DisplayName("GET /api/dashboard retorna mètriques globals")
    void haDeRetornarDashboard() throws Exception {
        DashboardResponse mockResp = new DashboardResponse(3, 7, 12, List.of(), List.of(), List.of());
        when(dashboardService.getDashboard()).thenReturn(mockResp);

        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openTasks").value(3))
                .andExpect(jsonPath("$.activeOffers").value(7))
                .andExpect(jsonPath("$.completedProjects").value(12));
    }
}

