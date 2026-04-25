package com.example.stockmarketsimulator.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
@Tag("integration")
class StockMarketSimulatorIntegrationTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.url",
                () -> "redis://localhost:" + redis.getMappedPort(6379));
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void setUp() {
        redisTemplate.execute((RedisCallback<Void>) connection -> {
            connection.serverCommands().flushAll();
            return null;
        });
    }

    // ── /stocks ───────────────────────────────────────────────────────────────

    @Test
    void getStocks_initiallyEmpty() {
        ResponseEntity<Map> res = restTemplate.getForEntity("/stocks", Map.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((List<?>) res.getBody().get("stocks")).isEmpty();
    }

    @Test
    void postStocks_setsBank_getStocksReturnsIt() {
        restTemplate.postForEntity("/stocks",
                Map.of("stocks", List.of(Map.of("name", "AAPL", "quantity", 100))), Void.class);

        ResponseEntity<Map> res = restTemplate.getForEntity("/stocks", Map.class);
        List<Map<String, Object>> stocks = (List<Map<String, Object>>) res.getBody().get("stocks");
        assertThat(stocks).hasSize(1);
        assertThat(stocks.get(0).get("name")).isEqualTo("AAPL");
        assertThat(stocks.get(0).get("quantity")).isEqualTo(100);
    }

    @Test
    void postStocks_missingKey_returns400() {
        ResponseEntity<Void> res = restTemplate.postForEntity("/stocks", Map.of(), Void.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ── buy/sell ──────────────────────────────────────────────────────────────

    @Test
    void buy_unknownStock_returns404() {
        ResponseEntity<Void> res = restTemplate.postForEntity(
                "/wallets/w1/stocks/AAPL", Map.of("type", "buy"), Void.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void buy_bankHasNoStock_returns400() {
        restTemplate.postForEntity("/stocks",
                Map.of("stocks", List.of(Map.of("name", "AAPL", "quantity", 0))), Void.class);

        ResponseEntity<Void> res = restTemplate.postForEntity(
                "/wallets/w1/stocks/AAPL", Map.of("type", "buy"), Void.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void buy_success_returns200_decrementsBankAndIncrementsWallet() {
        restTemplate.postForEntity("/stocks",
                Map.of("stocks", List.of(Map.of("name", "AAPL", "quantity", 5))), Void.class);

        ResponseEntity<Void> buyRes = restTemplate.postForEntity(
                "/wallets/w1/stocks/AAPL", Map.of("type", "buy"), Void.class);
        assertThat(buyRes.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Bank decremented
        ResponseEntity<Map> bankRes = restTemplate.getForEntity("/stocks", Map.class);
        List<Map<String, Object>> bankStocks = (List<Map<String, Object>>) bankRes.getBody().get("stocks");
        assertThat(bankStocks.get(0).get("quantity")).isEqualTo(4);

        // Wallet incremented
        ResponseEntity<Integer> qtyRes = restTemplate.getForEntity(
                "/wallets/w1/stocks/AAPL", Integer.class);
        assertThat(qtyRes.getBody()).isEqualTo(1);
    }

    @Test
    void buy_createsWalletIfNotExists() {
        restTemplate.postForEntity("/stocks",
                Map.of("stocks", List.of(Map.of("name", "AAPL", "quantity", 5))), Void.class);
        restTemplate.postForEntity("/wallets/new-wallet/stocks/AAPL", Map.of("type", "buy"), Void.class);

        ResponseEntity<Map> res = restTemplate.getForEntity("/wallets/new-wallet", Map.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody().get("id")).isEqualTo("new-wallet");
    }

    @Test
    void sell_unknownStock_returns404() {
        ResponseEntity<Void> res = restTemplate.postForEntity(
                "/wallets/w1/stocks/AAPL", Map.of("type", "sell"), Void.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void sell_walletHasNoStock_returns400() {
        restTemplate.postForEntity("/stocks",
                Map.of("stocks", List.of(Map.of("name", "AAPL", "quantity", 5))), Void.class);

        ResponseEntity<Void> res = restTemplate.postForEntity(
                "/wallets/w1/stocks/AAPL", Map.of("type", "sell"), Void.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void sell_success_returns200_incrementsBankAndDecrementsWallet() {
        restTemplate.postForEntity("/stocks",
                Map.of("stocks", List.of(Map.of("name", "AAPL", "quantity", 5))), Void.class);
        restTemplate.postForEntity("/wallets/w1/stocks/AAPL", Map.of("type", "buy"), Void.class);
        restTemplate.postForEntity("/wallets/w1/stocks/AAPL", Map.of("type", "buy"), Void.class);

        ResponseEntity<Void> sellRes = restTemplate.postForEntity(
                "/wallets/w1/stocks/AAPL", Map.of("type", "sell"), Void.class);
        assertThat(sellRes.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Bank back to 4 (5-2+1)
        ResponseEntity<Map> bankRes = restTemplate.getForEntity("/stocks", Map.class);
        List<Map<String, Object>> bankStocks = (List<Map<String, Object>>) bankRes.getBody().get("stocks");
        assertThat(bankStocks.get(0).get("quantity")).isEqualTo(4);
    }

    @Test
    void stockKnownAfterBankReset_walletCanStillSell() {
        restTemplate.postForEntity("/stocks",
                Map.of("stocks", List.of(Map.of("name", "AAPL", "quantity", 5))), Void.class);
        restTemplate.postForEntity("/wallets/w1/stocks/AAPL", Map.of("type", "buy"), Void.class);

        // Reset bank to different stock — AAPL is no longer in bank
        restTemplate.postForEntity("/stocks",
                Map.of("stocks", List.of(Map.of("name", "GOOG", "quantity", 5))), Void.class);

        // Wallet still has AAPL and should be able to sell — must be 200, not 404
        ResponseEntity<Void> sellRes = restTemplate.postForEntity(
                "/wallets/w1/stocks/AAPL", Map.of("type", "sell"), Void.class);
        assertThat(sellRes.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // ── /wallets ──────────────────────────────────────────────────────────────

    @Test
    void getWallet_notFound_returns404() {
        ResponseEntity<Map> res = restTemplate.getForEntity("/wallets/nobody", Map.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getWallet_returnsIdAndStocks() {
        restTemplate.postForEntity("/stocks",
                Map.of("stocks", List.of(
                        Map.of("name", "AAPL", "quantity", 5),
                        Map.of("name", "GOOG", "quantity", 3))), Void.class);
        restTemplate.postForEntity("/wallets/w1/stocks/AAPL", Map.of("type", "buy"), Void.class);
        restTemplate.postForEntity("/wallets/w1/stocks/AAPL", Map.of("type", "buy"), Void.class);
        restTemplate.postForEntity("/wallets/w1/stocks/GOOG", Map.of("type", "buy"), Void.class);

        ResponseEntity<Map> res = restTemplate.getForEntity("/wallets/w1", Map.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody().get("id")).isEqualTo("w1");

        List<Map<String, Object>> stocks = (List<Map<String, Object>>) res.getBody().get("stocks");
        assertThat(stocks).hasSize(2);
    }

    @Test
    void getWalletStock_unknownStock_returns404() {
        ResponseEntity<Void> res = restTemplate.getForEntity("/wallets/w1/stocks/UNKNOWN", Void.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getWalletStock_knownStockNotInWallet_returnsZero() {
        restTemplate.postForEntity("/stocks",
                Map.of("stocks", List.of(Map.of("name", "AAPL", "quantity", 5))), Void.class);

        ResponseEntity<Integer> res = restTemplate.getForEntity("/wallets/w1/stocks/AAPL", Integer.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(0);
    }

    // ── /log ──────────────────────────────────────────────────────────────────

    @Test
    void getLog_initiallyEmpty() {
        ResponseEntity<Map> res = restTemplate.getForEntity("/log", Map.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((List<?>) res.getBody().get("log")).isEmpty();
    }

    @Test
    void log_recordsSuccessfulBuysAndSells_inOrder() {
        restTemplate.postForEntity("/stocks",
                Map.of("stocks", List.of(Map.of("name", "AAPL", "quantity", 5))), Void.class);

        restTemplate.postForEntity("/wallets/w1/stocks/AAPL", Map.of("type", "buy"), Void.class);
        restTemplate.postForEntity("/wallets/w2/stocks/AAPL", Map.of("type", "buy"), Void.class);
        restTemplate.postForEntity("/wallets/w1/stocks/AAPL", Map.of("type", "sell"), Void.class);

        ResponseEntity<Map> res = restTemplate.getForEntity("/log", Map.class);
        List<Map<String, Object>> log = (List<Map<String, Object>>) res.getBody().get("log");

        assertThat(log).hasSize(3);
        assertThat(log.get(0)).containsEntry("type", "buy").containsEntry("wallet_id", "w1").containsEntry("stock_name", "AAPL");
        assertThat(log.get(1)).containsEntry("type", "buy").containsEntry("wallet_id", "w2");
        assertThat(log.get(2)).containsEntry("type", "sell").containsEntry("wallet_id", "w1");
    }

    @Test
    void log_doesNotRecordFailedOperations() {
        // buy unknown stock — should fail and not be logged
        restTemplate.postForEntity("/wallets/w1/stocks/UNKNOWN", Map.of("type", "buy"), Void.class);

        ResponseEntity<Map> res = restTemplate.getForEntity("/log", Map.class);
        assertThat((List<?>) res.getBody().get("log")).isEmpty();
    }

    @Test
    void fullFlow_multiplWalletsAndStocks() {
        // Set up bank
        restTemplate.postForEntity("/stocks", Map.of("stocks", List.of(
                Map.of("name", "AAPL", "quantity", 10),
                Map.of("name", "GOOG", "quantity", 5))), Void.class);

        // Multiple wallets trade
        for (int i = 0; i < 3; i++) {
            restTemplate.postForEntity("/wallets/alice/stocks/AAPL", Map.of("type", "buy"), Void.class);
        }
        restTemplate.postForEntity("/wallets/bob/stocks/GOOG", Map.of("type", "buy"), Void.class);
        restTemplate.postForEntity("/wallets/alice/stocks/AAPL", Map.of("type", "sell"), Void.class);

        // Bank: AAPL=10-3+1=8, GOOG=5-1=4
        ResponseEntity<Map> bankRes = restTemplate.getForEntity("/stocks", Map.class);
        List<Map<String, Object>> bankStocks = (List<Map<String, Object>>) bankRes.getBody().get("stocks");
        Map<String, Integer> bankMap = new java.util.HashMap<>();
        bankStocks.forEach(s -> bankMap.put((String) s.get("name"), (Integer) s.get("quantity")));
        assertThat(bankMap.get("AAPL")).isEqualTo(8);
        assertThat(bankMap.get("GOOG")).isEqualTo(4);

        // Alice: 2 AAPL (bought 3, sold 1)
        ResponseEntity<Integer> aliceAapl = restTemplate.getForEntity("/wallets/alice/stocks/AAPL", Integer.class);
        assertThat(aliceAapl.getBody()).isEqualTo(2);

        // Log: 4 buy + 1 sell = 5 entries
        ResponseEntity<Map> logRes = restTemplate.getForEntity("/log", Map.class);
        assertThat((List<?>) logRes.getBody().get("log")).hasSize(5);
    }
}
