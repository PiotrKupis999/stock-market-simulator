package com.example.stockmarketsimulator.controller;

import com.example.stockmarketsimulator.controller.dto.TradeRequest;
import com.example.stockmarketsimulator.controller.dto.WalletResponse;
import com.example.stockmarketsimulator.service.StockMarketApplicationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/wallets")
public class WalletController {

    private final StockMarketApplicationService stockMarket;

    public WalletController(StockMarketApplicationService stockMarket) {
        this.stockMarket = stockMarket;
    }

    @PostMapping("/{walletId}/stocks/{stockName}")
    public ResponseEntity<Void> buySellStock(
            @PathVariable String walletId,
            @PathVariable String stockName,
            @RequestBody(required = false) TradeRequest body) {
        stockMarket.trade(walletId, stockName, body);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{walletId}")
    public WalletResponse getWallet(@PathVariable String walletId) {
        return stockMarket.getWallet(walletId);
    }

    @GetMapping("/{walletId}/stocks/{stockName}")
    public int getStockQuantity(
            @PathVariable String walletId,
            @PathVariable String stockName) {
        return stockMarket.getWalletStockQuantity(walletId, stockName);
    }
}
