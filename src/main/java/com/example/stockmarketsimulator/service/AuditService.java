package com.example.stockmarketsimulator.service;

import com.example.stockmarketsimulator.model.LogEntry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AuditService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void addEntry(String type, String walletId, String stockName) {
        try {
            String json = objectMapper.writeValueAsString(new LogEntry(type, walletId, stockName));
            redisTemplate.opsForList().rightPush("log", json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public List<LogEntry> getLog() {
        List<String> entries = redisTemplate.opsForList().range("log", 0, -1);
        List<LogEntry> result = new ArrayList<>();
        if (entries == null) return result;
        for (String json : entries) {
            try {
                result.add(objectMapper.readValue(json, LogEntry.class));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        return result;
    }
}
