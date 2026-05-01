package com.example.stockmarketsimulator.service;

import com.example.stockmarketsimulator.model.Stock;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class BankService {

    private static final String SET_STOCKS_SCRIPT =
            """
                    redis.call('DEL', KEYS[1])
                    for i = 1, #ARGV, 2 do
                        redis.call('HSET', KEYS[1], ARGV[i], ARGV[i + 1])
                        redis.call('SADD', KEYS[2], ARGV[i])
                    end
                    return 'OK'""";

    private static final DefaultRedisScript<String> SET_STOCKS_REDIS_SCRIPT =
            new DefaultRedisScript<>(SET_STOCKS_SCRIPT, String.class);

    private final StringRedisTemplate redisTemplate;

    public BankService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean stockExists(String stockName) {
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember("stocks:known", stockName));
    }

    public List<Stock> getStocks() {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries("bank");
        List<Stock> list = new ArrayList<>();
        for (Map.Entry<Object, Object> e : entries.entrySet()) {
            list.add(new Stock((String) e.getKey(), Integer.parseInt((String) e.getValue())));
        }
        list.sort(Comparator.comparing(Stock::getName));
        return list;
    }

    public void setStocks(List<Stock> stocks) {
        validateStocks(stocks);

        List<String> args = new ArrayList<>(stocks.size() * 2);
        for (Stock s : stocks) {
            args.add(s.getName());
            args.add(String.valueOf(s.getQuantity()));
        }

        redisTemplate.execute(SET_STOCKS_REDIS_SCRIPT, List.of("bank", "stocks:known"), args.toArray());
    }

    public int getQuantity(String stockName) {
        Object val = redisTemplate.opsForHash().get("bank", stockName);
        return val != null ? Integer.parseInt((String) val) : 0;
    }

    private void validateStocks(List<Stock> stocks) {
        if (stocks == null) {
            throw new IllegalArgumentException("stocks must not be null");
        }

        Set<String> names = new HashSet<>();
        for (Stock stock : stocks) {
            if (stock == null) {
                throw new IllegalArgumentException("stocks must not contain null entries");
            }
            if (isBlank(stock.getName())) {
                throw new IllegalArgumentException("stock name must not be blank");
            }
            if (stock.getQuantity() < 0) {
                throw new IllegalArgumentException("stock quantity must not be negative");
            }
            if (!names.add(stock.getName())) {
                throw new IllegalArgumentException("stock names must be unique");
            }
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
