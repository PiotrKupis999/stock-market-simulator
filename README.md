# Stock Market Simulator

A simplified stock market simulation service built with Java 17 and Spring Boot. Highly available via **three** application instances, a Redis shared store, and an nginx load balancer – all orchestrated with Docker Compose.

## Prerequisites

- Docker and Docker Compose (no local Java or Maven needed; the JAR is built inside Docker)

For running tests locally:
- Java 17+
- Maven 3.6+

## Quick Start

**Linux / macOS:**
```bash
chmod +x start.sh
./start.sh 8080
```

**Windows:**
```bat
run.bat 8080
```

Port defaults to `8080` if not specified. The service will be available at `http://localhost:8080`.

To stop: `docker compose down`

## Architecture

```
Client → nginx:PORT → upstream { sim1:8080, sim2:8080, sim3:8080 }
                              ↕          ↕          ↕
                              Redis (shared state)
```

| Component | Role |
|-----------|------|
| **nginx** | Load balancer — single entry point, round-robins across 3 instances, retries on failure |
| **sim1/sim2/sim3** | Spring Boot instances — stateless, share state via Redis |
| **Redis** | Stores bank stocks (`bank` hash), wallet stocks (`wallet:{id}` hash), known stocks (`stocks:known` set), and audit log (`log` list) |

`restart: unless-stopped` on all services ensures Docker restarts any container that exits (e.g., after `/chaos`). With 3 instances, killing one still leaves 2 serving traffic while the third recovers.

## Running Tests

```bash
mvn test
```

Two test suites:

| Suite | Description | Requires |
|-------|-------------|----------|
| **Unit tests** (21) | `@WebMvcTest` with Mockito mocks — test all endpoint contracts and error cases | Nothing — runs fast (~10 s) |
| **Integration tests** (30) | `@SpringBootTest` + Testcontainers — spins up a real Redis, tests Lua scripts and full API flows | Docker daemon accessible |

Integration tests are annotated with `@Testcontainers(disabledWithoutDocker = true)` — they are **skipped** (not failed) when Docker is not available, so `mvn test` always succeeds.

To run only unit tests: `mvn test -Dgroups='!integration'`  
To run everything including integration tests: `mvn test` (requires Docker)

## API Reference

### `POST /wallets/{wallet_id}/stocks/{stock_name}`
Buy or sell a single unit of a stock.

```json
{"type": "buy"}
```
or
```json
{"type": "sell"}
```

| Response | Condition |
|----------|-----------|
| 200 | Success |
| 400 | No stock available in bank (buy) or wallet (sell) |
| 404 | Stock has never been added via `POST /stocks` |

Auto-creates the wallet on first successful buy. Only successful operations are logged.

---

### `GET /wallets/{wallet_id}`
Returns current wallet state.
```json
{"id": "wallet1", "stocks": [{"name": "AAPL", "quantity": 3}]}
```
Returns 404 if wallet does not exist.

---

### `GET /wallets/{wallet_id}/stocks/{stock_name}`
Returns quantity of a specific stock in a wallet (plain integer).
```
3
```
Returns 404 if the stock has never been added via `POST /stocks`. Returns 0 if the stock is known but the wallet holds none.

---

### `GET /stocks`
Returns current bank state.
```json
{"stocks": [{"name": "AAPL", "quantity": 99}, {"name": "GOOG", "quantity": 1}]}
```

---

### `POST /stocks`
Replaces the entire bank inventory.
```json
{"stocks": [{"name": "AAPL", "quantity": 100}, {"name": "GOOG", "quantity": 50}]}
```
Returns 200 on success. Stock names mentioned here are permanently registered in the system — they remain "known" even if subsequently replaced by a new `POST /stocks` call, so wallets can still sell them.

---

### `GET /log`
Returns the full audit log in order of occurrence. Bank operations are excluded; only successful wallet operations appear.
```json
{"log": [{"type": "buy", "wallet_id": "w1", "stock_name": "AAPL"}]}
```

---

### `POST /chaos`
Kills the instance that handled this request (returns 200 first, then calls `System.exit(1)` after 100 ms). The other two instances continue serving traffic; Docker restarts the killed container automatically.

## Design Notes

### Atomicity
Buy and sell operations are implemented as Redis Lua scripts. The entire check-and-update sequence (validate existence → check quantity → decrement/increment) runs atomically on the Redis server, preventing overselling or double-spending under concurrent load.

### Stock Existence vs Bank State
A stock "exists" if it has ever been added via `POST /stocks`. This state is tracked in a persistent `stocks:known` Redis Set that only grows — never shrinks. The bank hash (`bank`) tracks current quantities and can be completely replaced. This separation means:
- Selling a stock that was removed from the bank but is still in a wallet: **works** (the stock is still "known")
- Buying a stock that was removed from the bank (quantity = 0): fails with 400, not 404

### High Availability
Three application instances sit behind nginx. `proxy_next_upstream error timeout http_502 http_503 http_504` with `max_fails=1 fail_timeout=5s` means that after a single failed request to an instance, nginx marks it unavailable for 5 seconds and routes to the remaining two. Combined with `restart: unless-stopped`, the service is continuously available even while a chaos-killed instance is restarting.

### Cross-Platform
`eclipse-temurin:17-jre-alpine` supports both `linux/amd64` and `linux/arm64`. The multi-stage Dockerfile builds the JAR inside Docker so no local toolchain is needed on the host.
