package com.example.stockmarketsimulator.controller;

import com.example.stockmarketsimulator.model.Stock;
import com.example.stockmarketsimulator.service.BankService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/stocks")
public class StockController {

    @Autowired
    private BankService bankService;

    @GetMapping
    public Map<String, List<Stock>> getStocks() {
        Map<String, List<Stock>> response = new HashMap<>();
        response.put("stocks", bankService.getStocks());
        return response;
    }

    public static class SetStocksRequest {
        private List<Stock> stocks;
        public List<Stock> getStocks() { return stocks; }
        public void setStocks(List<Stock> stocks) { this.stocks = stocks; }
    }

    @PostMapping
    public ResponseEntity<Void> setStocks(@RequestBody SetStocksRequest body) {
        if (body.getStocks() == null) {
            return ResponseEntity.badRequest().build();
        }
        bankService.setStocks(body.getStocks());
        return ResponseEntity.ok().build();
    }
}
