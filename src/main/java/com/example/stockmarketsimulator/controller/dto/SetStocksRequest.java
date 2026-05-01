package com.example.stockmarketsimulator.controller.dto;

import com.example.stockmarketsimulator.model.Stock;

import java.util.List;

public class SetStocksRequest {
    private List<Stock> stocks;

    public List<Stock> getStocks() {
        return stocks;
    }

    public void setStocks(List<Stock> stocks) {
        this.stocks = stocks;
    }
}
