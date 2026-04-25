package com.example.stockmarketsimulator.controller;

import com.example.stockmarketsimulator.model.Stock;
import com.example.stockmarketsimulator.service.AuditService;
import com.example.stockmarketsimulator.service.BankService;
import com.example.stockmarketsimulator.service.TradeResult;
import com.example.stockmarketsimulator.service.TradingService;
import com.example.stockmarketsimulator.service.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/wallets")
public class WalletController {

    @Autowired
    private WalletService walletService;

    @Autowired
    private BankService bankService;

    @Autowired
    private TradingService tradingService;

    @Autowired
    private AuditService auditService;

    @PostMapping("/{walletId}/stocks/{stockName}")
    public ResponseEntity<Void> buySellStock(
            @PathVariable String walletId,
            @PathVariable String stockName,
            @RequestBody Map<String, String> body) {

        String type = body.get("type");
        if (!"buy".equals(type) && !"sell".equals(type)) {
            return ResponseEntity.badRequest().build();
        }

        TradeResult result = "buy".equals(type)
                ? tradingService.buy(walletId, stockName)
                : tradingService.sell(walletId, stockName);

        return switch (result) {
            case NOT_FOUND -> ResponseEntity.notFound().build();
            case INSUFFICIENT_BANK, INSUFFICIENT_WALLET -> ResponseEntity.badRequest().build();
            case OK -> {
                auditService.addEntry(type, walletId, stockName);
                yield ResponseEntity.ok().build();
            }
        };
    }

    @GetMapping("/{walletId}")
    public ResponseEntity<Map<String, Object>> getWallet(@PathVariable String walletId) {
        if (!walletService.walletExists(walletId)) {
            return ResponseEntity.notFound().build();
        }
        List<Stock> stocks = walletService.getWalletState(walletId);
        Map<String, Object> response = new HashMap<>();
        response.put("id", walletId);
        response.put("stocks", stocks);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{walletId}/stocks/{stockName}")
    public ResponseEntity<Integer> getStockQuantity(
            @PathVariable String walletId,
            @PathVariable String stockName) {
        if (!bankService.stockExists(stockName)) {
            return ResponseEntity.notFound().build();
        }
        int quantity = walletService.getStockQuantity(walletId, stockName);
        return ResponseEntity.ok(quantity);
    }
}
