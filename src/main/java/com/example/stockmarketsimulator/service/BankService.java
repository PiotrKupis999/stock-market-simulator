package com.example.stockmarketsimulator.service;

import com.example.stockmarketsimulator.model.Stock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class BankService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    public boolean stockExists(String stockName) {
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember("stocks:known", stockName));
    }

    public List<Stock> getStocks() {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries("bank");
        List<Stock> list = new ArrayList<>();
        for (Map.Entry<Object, Object> e : entries.entrySet()) {
            list.add(new Stock((String) e.getKey(), Integer.parseInt((String) e.getValue())));
        }
        return list;
    }

    public void setStocks(List<Stock> stocks) {
        redisTemplate.delete("bank");
        for (Stock s : stocks) {
            redisTemplate.opsForHash().put("bank", s.getName(), String.valueOf(s.getQuantity()));
            redisTemplate.opsForSet().add("stocks:known", s.getName());
        }
    }

    public int getQuantity(String stockName) {
        Object val = redisTemplate.opsForHash().get("bank", stockName);
        return val != null ? Integer.parseInt((String) val) : 0;
    }
}
