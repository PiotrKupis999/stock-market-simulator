package com.example.stockmarketsimulator.controller.dto;

import com.example.stockmarketsimulator.model.Stock;

import java.util.List;

public record StocksResponse(List<Stock> stocks) {
}
