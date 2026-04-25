package com.example.stockmarketsimulator.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChaosController.class)
class ChaosControllerTest {

    @TestConfiguration
    static class Config {
        @Bean
        ChaosController chaosController() {
            return new ChaosController(() -> {});
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void chaos_returns200() throws Exception {
        mockMvc.perform(post("/chaos"))
                .andExpect(status().isOk());
    }
}
