package com.example.stockmarketsimulator.controller;

import com.example.stockmarketsimulator.controller.dto.SetStocksRequest;
import com.example.stockmarketsimulator.controller.dto.StocksResponse;
import com.example.stockmarketsimulator.service.StockMarketApplicationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/stocks")
public class StockController {

    private final StockMarketApplicationService stockMarket;

    public StockController(StockMarketApplicationService stockMarket) {
        this.stockMarket = stockMarket;
    }

    @GetMapping
    public StocksResponse getStocks() {
        return stockMarket.getStocks();
    }

    @PostMapping
    public ResponseEntity<Void> setStocks(@RequestBody(required = false) SetStocksRequest body) {
        stockMarket.setStocks(body);
        return ResponseEntity.ok().build();
    }
}
