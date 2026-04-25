package com.example.stockmarketsimulator.controller;

import com.example.stockmarketsimulator.model.LogEntry;
import com.example.stockmarketsimulator.service.AuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/log")
public class LogController {

    @Autowired
    private AuditService auditService;

    @GetMapping
    public Map<String, List<LogEntry>> getLog() {
        Map<String, List<LogEntry>> response = new HashMap<>();
        response.put("log", auditService.getLog());
        return response;
    }
}
