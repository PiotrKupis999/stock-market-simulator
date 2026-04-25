package com.example.stockmarketsimulator.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TradingService {

    private static final String BUY_SCRIPT =
            """
                    if redis.call('SISMEMBER', KEYS[3], ARGV[1]) == 0 then return 'NOT_FOUND' end
                    local qty = tonumber(redis.call('HGET', KEYS[1], ARGV[1]) or '0')
                    if qty == nil or qty <= 0 then return 'INSUFFICIENT_BANK' end
                    redis.call('HINCRBY', KEYS[1], ARGV[1], -1)
                    redis.call('HINCRBY', KEYS[2], ARGV[1], 1)
                    return 'OK'""";

    private static final String SELL_SCRIPT =
            """
                    if redis.call('SISMEMBER', KEYS[3], ARGV[1]) == 0 then return 'NOT_FOUND' end
                    local qty = tonumber(redis.call('HGET', KEYS[2], ARGV[1]) or '0')
                    if qty == nil or qty <= 0 then return 'INSUFFICIENT_WALLET' end
                    redis.call('HINCRBY', KEYS[2], ARGV[1], -1)
                    redis.call('HINCRBY', KEYS[1], ARGV[1], 1)
                    return 'OK'""";

    @Autowired
    private StringRedisTemplate redisTemplate;

    public TradeResult buy(String walletId, String stockName) {
        DefaultRedisScript<String> script = new DefaultRedisScript<>(BUY_SCRIPT, String.class);
        String result = redisTemplate.execute(script, List.of("bank", "wallet:" + walletId, "stocks:known"), stockName);
        return TradeResult.valueOf(result);
    }

    public TradeResult sell(String walletId, String stockName) {
        DefaultRedisScript<String> script = new DefaultRedisScript<>(SELL_SCRIPT, String.class);
        String result = redisTemplate.execute(script, List.of("bank", "wallet:" + walletId, "stocks:known"), stockName);
        return TradeResult.valueOf(result);
    }
}
