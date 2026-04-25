package com.example.stockmarketsimulator.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/chaos")
public class ChaosController {

    private final Runnable shutdown;

    public ChaosController() {
        this.shutdown = () -> System.exit(1);
    }

    ChaosController(Runnable shutdown) {
        this.shutdown = shutdown;
    }

    @PostMapping
    public void chaos(HttpServletResponse response) throws IOException {
        response.setStatus(200);
        response.flushBuffer();
        new Thread(() -> {
            try { Thread.sleep(100); } catch (InterruptedException ignored) {}
            shutdown.run();
        }).start();
    }
}
