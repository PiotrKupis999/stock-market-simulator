package com.example.stockmarketsimulator.service;

import com.example.stockmarketsimulator.exception.StorageOperationException;
import com.example.stockmarketsimulator.model.LogEntry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AuditService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public AuditService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public List<LogEntry> getLog() {
        List<String> entries = redisTemplate.opsForList().range("log", 0, -1);
        List<LogEntry> result = new ArrayList<>();
        if (entries == null) return result;
        for (String json : entries) {
            try {
                result.add(objectMapper.readValue(json, LogEntry.class));
            } catch (JsonProcessingException e) {
                throw new StorageOperationException("Could not deserialize audit log entry", e);
            }
        }
        return result;
    }
}
