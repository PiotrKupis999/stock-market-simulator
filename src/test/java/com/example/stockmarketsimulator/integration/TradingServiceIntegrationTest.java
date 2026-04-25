package com.example.stockmarketsimulator.integration;

import com.example.stockmarketsimulator.service.BankService;
import com.example.stockmarketsimulator.service.TradeResult;
import com.example.stockmarketsimulator.service.TradingService;
import com.example.stockmarketsimulator.model.Stock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@Tag("integration")
class TradingServiceIntegrationTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.url",
                () -> "redis://localhost:" + redis.getMappedPort(6379));
    }

    @Autowired
    private TradingService tradingService;

    @Autowired
    private BankService bankService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void setUp() {
        redisTemplate.execute((RedisCallback<Void>) connection -> {
            connection.serverCommands().flushAll();
            return null;
        });
    }

    @Test
    void buy_stockNeverAdded_returnsNotFound() {
        assertThat(tradingService.buy("wallet1", "AAPL")).isEqualTo(TradeResult.NOT_FOUND);
    }

    @Test
    void buy_stockKnownButBankEmpty_returnsInsufficientBank() {
        bankService.setStocks(List.of(new Stock("AAPL", 0)));

        assertThat(tradingService.buy("wallet1", "AAPL")).isEqualTo(TradeResult.INSUFFICIENT_BANK);
    }

    @Test
    void buy_success_returnsOk() {
        bankService.setStocks(List.of(new Stock("AAPL", 5)));

        assertThat(tradingService.buy("wallet1", "AAPL")).isEqualTo(TradeResult.OK);
    }

    @Test
    void buy_success_decrementsBankAndIncrementsWallet() {
        bankService.setStocks(List.of(new Stock("AAPL", 5)));

        tradingService.buy("wallet1", "AAPL");
        tradingService.buy("wallet1", "AAPL");

        assertThat(bankService.getQuantity("AAPL")).isEqualTo(3);
        assertThat(redisTemplate.opsForHash().get("wallet:wallet1", "AAPL")).isEqualTo("2");
    }

    @Test
    void buy_stockRemovedFromBankThenReAdded_stillKnown() {
        bankService.setStocks(List.of(new Stock("AAPL", 1)));
        tradingService.buy("wallet1", "AAPL");

        // Reset bank to different stocks - AAPL removed but should still be "known"
        bankService.setStocks(List.of(new Stock("GOOG", 5)));

        // AAPL is known (added before) but bank has 0 — should be INSUFFICIENT_BANK, not NOT_FOUND
        assertThat(tradingService.buy("wallet1", "AAPL")).isEqualTo(TradeResult.INSUFFICIENT_BANK);
    }

    @Test
    void sell_stockNeverAdded_returnsNotFound() {
        assertThat(tradingService.sell("wallet1", "AAPL")).isEqualTo(TradeResult.NOT_FOUND);
    }

    @Test
    void sell_walletHasNoStock_returnsInsufficientWallet() {
        bankService.setStocks(List.of(new Stock("AAPL", 5)));

        assertThat(tradingService.sell("wallet1", "AAPL")).isEqualTo(TradeResult.INSUFFICIENT_WALLET);
    }

    @Test
    void sell_success_returnsOk() {
        bankService.setStocks(List.of(new Stock("AAPL", 5)));
        tradingService.buy("wallet1", "AAPL");

        assertThat(tradingService.sell("wallet1", "AAPL")).isEqualTo(TradeResult.OK);
    }

    @Test
    void sell_success_decrementsWalletAndIncrementsBank() {
        bankService.setStocks(List.of(new Stock("AAPL", 5)));
        tradingService.buy("wallet1", "AAPL");
        tradingService.buy("wallet1", "AAPL");

        tradingService.sell("wallet1", "AAPL");

        assertThat(bankService.getQuantity("AAPL")).isEqualTo(4);
        assertThat(redisTemplate.opsForHash().get("wallet:wallet1", "AAPL")).isEqualTo("1");
    }

    @Test
    void sell_stockRemovedFromBank_walletCanStillSell() {
        bankService.setStocks(List.of(new Stock("AAPL", 5)));
        tradingService.buy("wallet1", "AAPL");

        // Reset bank - AAPL removed
        bankService.setStocks(List.of(new Stock("GOOG", 5)));

        // Wallet still has AAPL and should be able to sell it
        assertThat(tradingService.sell("wallet1", "AAPL")).isEqualTo(TradeResult.OK);
    }

    @Test
    void buy_exactlyDrainsBank_nextBuyFails() {
        bankService.setStocks(List.of(new Stock("AAPL", 1)));
        assertThat(tradingService.buy("wallet1", "AAPL")).isEqualTo(TradeResult.OK);
        assertThat(tradingService.buy("wallet2", "AAPL")).isEqualTo(TradeResult.INSUFFICIENT_BANK);
    }
}
