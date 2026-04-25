package com.example.stockmarketsimulator.controller;

import com.example.stockmarketsimulator.model.Stock;
import com.example.stockmarketsimulator.service.BankService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StockController.class)
class StockControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BankService bankService;

    @Test
    void getStocks_empty_returnsEmptyList() throws Exception {
        when(bankService.getStocks()).thenReturn(List.of());

        mockMvc.perform(get("/stocks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stocks").isArray())
                .andExpect(jsonPath("$.stocks").isEmpty());
    }

    @Test
    void getStocks_withStocks_returnsList() throws Exception {
        when(bankService.getStocks()).thenReturn(List.of(
                new Stock("AAPL", 100),
                new Stock("GOOG", 50)
        ));

        mockMvc.perform(get("/stocks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stocks[0].name").value("AAPL"))
                .andExpect(jsonPath("$.stocks[0].quantity").value(100))
                .andExpect(jsonPath("$.stocks[1].name").value("GOOG"))
                .andExpect(jsonPath("$.stocks[1].quantity").value(50));
    }

    @Test
    void setStocks_validBody_returns200() throws Exception {
        mockMvc.perform(post("/stocks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stocks\":[{\"name\":\"AAPL\",\"quantity\":100}]}"))
                .andExpect(status().isOk());

        verify(bankService).setStocks(anyList());
    }

    @Test
    void setStocks_missingStocksKey_returns400() throws Exception {
        mockMvc.perform(post("/stocks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verify(bankService, never()).setStocks(anyList());
    }

    @Test
    void setStocks_emptyList_clearsBank() throws Exception {
        mockMvc.perform(post("/stocks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stocks\":[]}"))
                .andExpect(status().isOk());

        verify(bankService).setStocks(List.of());
    }
}
