package com.example.stockmarketsimulator.controller;

import com.example.stockmarketsimulator.controller.dto.WalletResponse;
import com.example.stockmarketsimulator.exception.BadRequestException;
import com.example.stockmarketsimulator.exception.NotFoundException;
import com.example.stockmarketsimulator.model.Stock;
import com.example.stockmarketsimulator.service.StockMarketApplicationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WalletController.class)
class WalletControllerTest {

    private static final String MALFORMED_JSON = "{";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StockMarketApplicationService stockMarket;

    @Test
    void buy_stockNotFound_returns404() throws Exception {
        doThrow(new NotFoundException("stock not found"))
                .when(stockMarket).trade(eq("w1"), eq("AAPL"), any());

        mockMvc.perform(post("/wallets/w1/stocks/AAPL")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"buy\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void buy_insufficientBank_returns400() throws Exception {
        doThrow(new BadRequestException("bank has no stock available"))
                .when(stockMarket).trade(eq("w1"), eq("AAPL"), any());

        mockMvc.perform(post("/wallets/w1/stocks/AAPL")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"buy\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void buy_success_returns200() throws Exception {
        mockMvc.perform(post("/wallets/w1/stocks/AAPL")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"buy\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void sell_stockNotFound_returns404() throws Exception {
        doThrow(new NotFoundException("stock not found"))
                .when(stockMarket).trade(eq("w1"), eq("AAPL"), any());

        mockMvc.perform(post("/wallets/w1/stocks/AAPL")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"sell\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void sell_insufficientWallet_returns400() throws Exception {
        doThrow(new BadRequestException("wallet has no stock available"))
                .when(stockMarket).trade(eq("w1"), eq("AAPL"), any());

        mockMvc.perform(post("/wallets/w1/stocks/AAPL")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"sell\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sell_success_returns200() throws Exception {
        mockMvc.perform(post("/wallets/w1/stocks/AAPL")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"sell\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void buySell_invalidType_returns400() throws Exception {
        doThrow(new BadRequestException("invalid type"))
                .when(stockMarket).trade(eq("w1"), eq("AAPL"), any());

        mockMvc.perform(post("/wallets/w1/stocks/AAPL")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"transfer\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void buySell_missingBody_returns400() throws Exception {
        doThrow(new BadRequestException("request body is required"))
                .when(stockMarket).trade(eq("w1"), eq("AAPL"), isNull());

        mockMvc.perform(post("/wallets/w1/stocks/AAPL")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void buySell_malformedJson_returns400() throws Exception {
        mockMvc.perform(post("/wallets/w1/stocks/AAPL")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MALFORMED_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getWallet_notFound_returns404() throws Exception {
        when(stockMarket.getWallet("w1")).thenThrow(new NotFoundException("wallet not found"));

        mockMvc.perform(get("/wallets/w1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getWallet_found_returnsIdAndStocks() throws Exception {
        when(stockMarket.getWallet("w1")).thenReturn(new WalletResponse("w1", List.of(new Stock("AAPL", 3))));

        mockMvc.perform(get("/wallets/w1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("w1"))
                .andExpect(jsonPath("$.stocks[0].name").value("AAPL"))
                .andExpect(jsonPath("$.stocks[0].quantity").value(3));
    }

    @Test
    void getWalletStock_stockNotInSystem_returns404() throws Exception {
        when(stockMarket.getWalletStockQuantity("w1", "AAPL")).thenThrow(new NotFoundException("stock not found"));

        mockMvc.perform(get("/wallets/w1/stocks/AAPL"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getWalletStock_stockExists_returnsQuantity() throws Exception {
        when(stockMarket.getWalletStockQuantity("w1", "AAPL")).thenReturn(5);

        mockMvc.perform(get("/wallets/w1/stocks/AAPL"))
                .andExpect(status().isOk())
                .andExpect(content().string("5"));
    }

    @Test
    void getWalletStock_walletHasNone_returnsZero() throws Exception {
        when(stockMarket.getWalletStockQuantity("w1", "AAPL")).thenReturn(0);

        mockMvc.perform(get("/wallets/w1/stocks/AAPL"))
                .andExpect(status().isOk())
                .andExpect(content().string("0"));
    }
}
