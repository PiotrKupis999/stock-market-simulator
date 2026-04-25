package com.example.stockmarketsimulator.controller;

import com.example.stockmarketsimulator.model.Stock;
import com.example.stockmarketsimulator.service.AuditService;
import com.example.stockmarketsimulator.service.BankService;
import com.example.stockmarketsimulator.service.TradeResult;
import com.example.stockmarketsimulator.service.TradingService;
import com.example.stockmarketsimulator.service.WalletService;
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

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TradingService tradingService;

    @MockBean
    private WalletService walletService;

    @MockBean
    private BankService bankService;

    @MockBean
    private AuditService auditService;

    @Test
    void buy_stockNotFound_returns404() throws Exception {
        when(tradingService.buy("w1", "AAPL")).thenReturn(TradeResult.NOT_FOUND);

        mockMvc.perform(post("/wallets/w1/stocks/AAPL")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"buy\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void buy_insufficientBank_returns400() throws Exception {
        when(tradingService.buy("w1", "AAPL")).thenReturn(TradeResult.INSUFFICIENT_BANK);

        mockMvc.perform(post("/wallets/w1/stocks/AAPL")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"buy\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void buy_success_returns200AndLogsEntry() throws Exception {
        when(tradingService.buy("w1", "AAPL")).thenReturn(TradeResult.OK);

        mockMvc.perform(post("/wallets/w1/stocks/AAPL")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"buy\"}"))
                .andExpect(status().isOk());

        verify(auditService).addEntry("buy", "w1", "AAPL");
    }

    @Test
    void sell_stockNotFound_returns404() throws Exception {
        when(tradingService.sell("w1", "AAPL")).thenReturn(TradeResult.NOT_FOUND);

        mockMvc.perform(post("/wallets/w1/stocks/AAPL")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"sell\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void sell_insufficientWallet_returns400() throws Exception {
        when(tradingService.sell("w1", "AAPL")).thenReturn(TradeResult.INSUFFICIENT_WALLET);

        mockMvc.perform(post("/wallets/w1/stocks/AAPL")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"sell\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sell_success_returns200AndLogsEntry() throws Exception {
        when(tradingService.sell("w1", "AAPL")).thenReturn(TradeResult.OK);

        mockMvc.perform(post("/wallets/w1/stocks/AAPL")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"sell\"}"))
                .andExpect(status().isOk());

        verify(auditService).addEntry("sell", "w1", "AAPL");
    }

    @Test
    void buySell_invalidType_returns400() throws Exception {
        mockMvc.perform(post("/wallets/w1/stocks/AAPL")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"transfer\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(tradingService);
    }

    @Test
    void getWallet_notFound_returns404() throws Exception {
        when(walletService.walletExists("w1")).thenReturn(false);

        mockMvc.perform(get("/wallets/w1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getWallet_found_returnsIdAndStocks() throws Exception {
        when(walletService.walletExists("w1")).thenReturn(true);
        when(walletService.getWalletState("w1")).thenReturn(List.of(new Stock("AAPL", 3)));

        mockMvc.perform(get("/wallets/w1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("w1"))
                .andExpect(jsonPath("$.stocks[0].name").value("AAPL"))
                .andExpect(jsonPath("$.stocks[0].quantity").value(3));
    }

    @Test
    void getWalletStock_stockNotInSystem_returns404() throws Exception {
        when(bankService.stockExists("AAPL")).thenReturn(false);

        mockMvc.perform(get("/wallets/w1/stocks/AAPL"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getWalletStock_stockExists_returnsQuantity() throws Exception {
        when(bankService.stockExists("AAPL")).thenReturn(true);
        when(walletService.getStockQuantity("w1", "AAPL")).thenReturn(5);

        mockMvc.perform(get("/wallets/w1/stocks/AAPL"))
                .andExpect(status().isOk())
                .andExpect(content().string("5"));
    }

    @Test
    void getWalletStock_walletHasNone_returnsZero() throws Exception {
        when(bankService.stockExists("AAPL")).thenReturn(true);
        when(walletService.getStockQuantity("w1", "AAPL")).thenReturn(0);

        mockMvc.perform(get("/wallets/w1/stocks/AAPL"))
                .andExpect(status().isOk())
                .andExpect(content().string("0"));
    }
}
