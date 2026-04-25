package com.example.stockmarketsimulator.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class LogEntry {
    private final String type;
    private final String walletId;
    private final String stockName;

    @JsonCreator
    public LogEntry(
            @JsonProperty("type") String type,
            @JsonProperty("wallet_id") String walletId,
            @JsonProperty("stock_name") String stockName) {
        this.type = type;
        this.walletId = walletId;
        this.stockName = stockName;
    }

    public String getType() {
        return type;
    }

    @JsonProperty("wallet_id")
    public String getWalletId() {
        return walletId;
    }

    @JsonProperty("stock_name")
    public String getStockName() {
        return stockName;
    }
}
