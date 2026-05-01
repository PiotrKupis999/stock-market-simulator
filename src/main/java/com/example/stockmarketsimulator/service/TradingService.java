package com.example.stockmarketsimulator.service;

import com.example.stockmarketsimulator.exception.StorageOperationException;
import com.example.stockmarketsimulator.model.LogEntry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class TradingService {

    private static final String BUY_SCRIPT =
            """
                    if redis.call('SISMEMBER', KEYS[3], ARGV[1]) == 0 then return 'NOT_FOUND' end
                    local qty = tonumber(redis.call('HGET', KEYS[1], ARGV[1]) or '0')
                    if qty == nil or qty <= 0 then return 'INSUFFICIENT_BANK' end
                    redis.call('HINCRBY', KEYS[1], ARGV[1], -1)
                    redis.call('HINCRBY', KEYS[2], ARGV[1], 1)
                    redis.call('RPUSH', KEYS[4], ARGV[2])
                    return 'OK'""";

    private static final String SELL_SCRIPT =
            """
                    if redis.call('SISMEMBER', KEYS[3], ARGV[1]) == 0 then return 'NOT_FOUND' end
                    local qty = tonumber(redis.call('HGET', KEYS[2], ARGV[1]) or '0')
                    if qty == nil or qty <= 0 then return 'INSUFFICIENT_WALLET' end
                    redis.call('HINCRBY', KEYS[2], ARGV[1], -1)
                    redis.call('HINCRBY', KEYS[1], ARGV[1], 1)
                    redis.call('RPUSH', KEYS[4], ARGV[2])
                    return 'OK'""";

    private static final DefaultRedisScript<String> BUY_REDIS_SCRIPT = new DefaultRedisScript<>(BUY_SCRIPT, String.class);
    private static final DefaultRedisScript<String> SELL_REDIS_SCRIPT = new DefaultRedisScript<>(SELL_SCRIPT, String.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public TradingService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public TradeResult buy(String walletId, String stockName) {
        return executeTrade(BUY_REDIS_SCRIPT, "buy", walletId, stockName);
    }

    public TradeResult sell(String walletId, String stockName) {
        return executeTrade(SELL_REDIS_SCRIPT, "sell", walletId, stockName);
    }

    private TradeResult executeTrade(DefaultRedisScript<String> script, String type, String walletId, String stockName) {
        String result = redisTemplate.execute(
                script,
                List.of("bank", walletKey(walletId), "stocks:known", "log"),
                stockName,
                logEntryJson(type, walletId, stockName));

        return TradeResult.valueOf(Objects.requireNonNull(result, "Redis trade script returned no result"));
    }

    private String logEntryJson(String type, String walletId, String stockName) {
        try {
            return objectMapper.writeValueAsString(new LogEntry(type, walletId, stockName));
        } catch (JsonProcessingException e) {
            throw new StorageOperationException("Could not serialize audit log entry", e);
        }
    }

    private String walletKey(String walletId) {
        return "wallet:" + walletId;
    }
}
