package com.example.stockmarketsimulator.controller;

import com.example.stockmarketsimulator.model.LogEntry;
import com.example.stockmarketsimulator.service.AuditService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LogController.class)
class LogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuditService auditService;

    @Test
    void getLog_empty_returnsEmptyList() throws Exception {
        when(auditService.getLog()).thenReturn(List.of());

        mockMvc.perform(get("/log"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.log").isArray())
                .andExpect(jsonPath("$.log").isEmpty());
    }

    @Test
    void getLog_withEntries_returnsOrderedList() throws Exception {
        when(auditService.getLog()).thenReturn(List.of(
                new LogEntry("buy", "w1", "AAPL"),
                new LogEntry("sell", "w2", "GOOG"),
                new LogEntry("buy", "w1", "GOOG")
        ));

        mockMvc.perform(get("/log"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.log[0].type").value("buy"))
                .andExpect(jsonPath("$.log[0].wallet_id").value("w1"))
                .andExpect(jsonPath("$.log[0].stock_name").value("AAPL"))
                .andExpect(jsonPath("$.log[1].type").value("sell"))
                .andExpect(jsonPath("$.log[1].wallet_id").value("w2"))
                .andExpect(jsonPath("$.log[1].stock_name").value("GOOG"))
                .andExpect(jsonPath("$.log[2].type").value("buy"))
                .andExpect(jsonPath("$.log[2].wallet_id").value("w1"))
                .andExpect(jsonPath("$.log[2].stock_name").value("GOOG"));
    }

    @Test
    void getLog_verifiesSnakeCaseFieldNames() throws Exception {
        when(auditService.getLog()).thenReturn(List.of(
                new LogEntry("buy", "wallet-123", "TSLA")
        ));

        mockMvc.perform(get("/log"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.log[0].wallet_id").value("wallet-123"))
                .andExpect(jsonPath("$.log[0].stock_name").value("TSLA"));
    }
}
