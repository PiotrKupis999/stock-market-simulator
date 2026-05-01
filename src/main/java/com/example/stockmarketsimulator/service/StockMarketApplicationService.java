package com.example.stockmarketsimulator.service;

import com.example.stockmarketsimulator.controller.dto.SetStocksRequest;
import com.example.stockmarketsimulator.controller.dto.StocksResponse;
import com.example.stockmarketsimulator.controller.dto.TradeRequest;
import com.example.stockmarketsimulator.controller.dto.WalletResponse;
import com.example.stockmarketsimulator.exception.BadRequestException;
import com.example.stockmarketsimulator.exception.NotFoundException;
import com.example.stockmarketsimulator.model.Stock;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StockMarketApplicationService {

    private final WalletService walletService;
    private final BankService bankService;
    private final TradingService tradingService;

    public StockMarketApplicationService(
            WalletService walletService,
            BankService bankService,
            TradingService tradingService) {
        this.walletService = walletService;
        this.bankService = bankService;
        this.tradingService = tradingService;
    }

    public void trade(String walletId, String stockName, TradeRequest request) {
        requirePresent(walletId, "walletId");
        requirePresent(stockName, "stockName");
        if (request == null) {
            throw new BadRequestException("request body is required");
        }

        TradeResult result = executeTrade(request.getType(), walletId, stockName);

        switch (result) {
            case OK -> {
            }
            case NOT_FOUND -> throw new NotFoundException("stock not found");
            case INSUFFICIENT_BANK -> throw new BadRequestException("bank has no stock available");
            case INSUFFICIENT_WALLET -> throw new BadRequestException("wallet has no stock available");
        }
    }

    public WalletResponse getWallet(String walletId) {
        requirePresent(walletId, "walletId");
        if (!walletService.walletExists(walletId)) {
            throw new NotFoundException("wallet not found");
        }

        return new WalletResponse(walletId, walletService.getWalletState(walletId));
    }

    public int getWalletStockQuantity(String walletId, String stockName) {
        requirePresent(walletId, "walletId");
        requirePresent(stockName, "stockName");
        if (!bankService.stockExists(stockName)) {
            throw new NotFoundException("stock not found");
        }

        return walletService.getStockQuantity(walletId, stockName);
    }

    public StocksResponse getStocks() {
        return new StocksResponse(bankService.getStocks());
    }

    public void setStocks(SetStocksRequest request) {
        if (request == null || request.getStocks() == null) {
            throw new BadRequestException("stocks are required");
        }

        List<Stock> stocks = request.getStocks();
        bankService.setStocks(stocks);
    }

    private void requirePresent(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new BadRequestException(fieldName + " must not be blank");
        }
    }

    private TradeResult executeTrade(String type, String walletId, String stockName) {
        if ("buy".equals(type)) {
            return tradingService.buy(walletId, stockName);
        }
        if ("sell".equals(type)) {
            return tradingService.sell(walletId, stockName);
        }

        throw new BadRequestException("trade type must be buy or sell");
    }
}
