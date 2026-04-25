package com.example.stockmarketsimulator.service;

import com.example.stockmarketsimulator.model.Stock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class WalletService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    public boolean walletExists(String walletId) {
        return redisTemplate.hasKey("wallet:" + walletId);
    }

    public List<Stock> getWalletState(String walletId) {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries("wallet:" + walletId);
        List<Stock> list = new ArrayList<>();
        for (Map.Entry<Object, Object> e : entries.entrySet()) {
            int qty = Integer.parseInt((String) e.getValue());
            if (qty > 0) {
                list.add(new Stock((String) e.getKey(), qty));
            }
        }
        return list;
    }

    public int getStockQuantity(String walletId, String stockName) {
        Object val = redisTemplate.opsForHash().get("wallet:" + walletId, stockName);
        return val != null ? Integer.parseInt((String) val) : 0;
    }
}
