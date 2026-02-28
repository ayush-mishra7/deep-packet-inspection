# DPI Engine — Backend Module

> Core Spring Boot 3.2.5 application module for the Deep Packet Inspection engine.

For full project documentation, architecture diagrams, and deployment guides, see the [root README](../README.md).

---

## Quick Start

```bash
# Build (includes running all tests)
mvn clean install

# Run in server mode (default — REST API + WebSocket on port 8080)
mvn spring-boot:run

# Run in benchmark mode (injects 1M synthetic packets, then exits)
mvn spring-boot:run -Dspring-boot.run.arguments="--dpi.mode=benchmark"

# Run in PCAP mode (processes a .pcap file, then exits)
mvn spring-boot:run -Dspring-boot.run.arguments="--dpi.mode=pcap --dpi.capture.pcap-file-path=/path/to/file.pcap"

# Run tests only
mvn test
```

---

## Module Structure

```
src/main/java/com/ayush/dpi/
├── DpiEngineApplication.java        # Entry point — @SpringBootApplication, @EnableScheduling, @EnableAsync
├── api/
│   ├── HealthController.java        # GET /api/health — custom health with version & timestamp
│   ├── StatsController.java         # GET /api/stats — aggregated packet statistics
│   ├── RuleController.java          # CRUD /api/rules — dynamic rule management
│   ├── GlobalExceptionHandler.java  # Centralized error handling
│   └── dto/                         # Request/Response DTOs (RuleRequest, RuleResponse, etc.)
├── capture/
│   ├── PacketIngestionService.java  # Orchestrates packet reading from any source
│   ├── PacketSource.java            # Interface for pluggable packet sources
│   └── PcapFilePacketSource.java    # PCAP file reader (pcap4j)
├── parser/
│   ├── PacketParserService.java     # Raw bytes → ParsedPacket (IP, ports, protocol, size)
│   ├── ParsedPacket.java            # Immutable packet metadata (Lombok @Builder)
│   ├── SniExtractor.java            # TLS ClientHello → SNI hostname extraction
│   └── ProtocolType.java            # Enum: TCP, UDP, OTHER
├── loadbalancer/
│   ├── LoadBalancerService.java     # Dispatches packets to workers via five-tuple hash
│   └── FiveTupleHasher.java         # Deterministic hashing for connection affinity
├── worker/
│   └── WorkerService.java           # Isolated worker thread — queue, conn map, rule eval, local stats
├── connection/
│   ├── ConnectionKey.java           # Java record — precomputed hashCode, O(1) lookups
│   └── Connection.java              # Stateful connection tracking (packets, bytes, last decision)
├── rules/
│   ├── Rule.java                    # Interface: evaluate(packet, connection) → Decision
│   ├── IpBlockRule.java             # Blocks traffic from/to specified IP addresses
│   ├── DomainBlockRule.java         # Blocks traffic by SNI domain (wildcard support)
│   ├── DataCapRule.java             # Throttles at threshold, blocks at 2× threshold
│   └── RuleRegistry.java           # Thread-safe rule store (CopyOnWriteArrayList)
├── decision/
│   └── Decision.java                # Enum: ALLOW, BLOCK, THROTTLE
├── stats/
│   ├── StatsService.java            # Global aggregator (LongAdder — lock-free)
│   └── WorkerLocalStats.java        # Per-worker counters flushed every 1000 packets
├── websocket/
│   └── StatsWebSocketHandler.java   # Broadcasts live stats JSON every 1 second via /ws/stats
├── analytics/
│   └── AIFeatureExtractorService.java # Periodic feature extraction (ML pipeline ready)
├── persistence/
│   └── AuditEventPublisher.java     # Async event publishing for rule match auditing
└── config/
    ├── DpiProperties.java           # @ConfigurationProperties for dpi.* namespace
    ├── WebSocketConfig.java         # Registers /ws/stats endpoint
    ├── PcapModeRunner.java          # ApplicationRunner — handles pcap/benchmark modes
    └── BenchmarkRunner.java         # SyntheticTrafficSimulator — 1M packet load test
```

---

## Configuration

All properties are defined in `src/main/resources/application.yml` and can be overridden via environment variables:

| Property | Env Variable | Default | Description |
|----------|-------------|---------|-------------|
| `dpi.mode` | `DPI_MODE` | `server` | Operating mode: `server`, `benchmark`, or `pcap` |
| `dpi.worker.count` | `DPI_WORKER_COUNT` | `4` | Number of parallel worker threads |
| `dpi.worker.queue-capacity` | — | `10000` | Max packets per worker queue (backpressure) |
| `dpi.capture.pcap-file-path` | `DPI_PCAP_FILE` | _(empty)_ | Path to .pcap file for `pcap` mode |
| `dpi.capture.buffer-size` | — | `65536` | Capture buffer size in bytes |
| `dpi.default-action` | — | `ALLOW` | Default decision when no rules match |

---

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/health` | `{"status":"UP","timestamp":"...","version":"0.1.0-SNAPSHOT"}` |
| `GET` | `/api/stats` | Aggregated stats: packets, bytes, protocols, decisions, top domains |
| `GET` | `/api/rules` | List all active rules |
| `POST` | `/api/rules` | Create rule (body: `{name, type, values?, threshold?}`) |
| `DELETE` | `/api/rules/{name}` | Delete a rule by name |
| `WS` | `/ws/stats` | WebSocket — live stats broadcast every 1s |
| `GET` | `/actuator/health` | Spring actuator health |
| `GET` | `/actuator/info` | Application metadata |
| `GET` | `/actuator/prometheus` | Prometheus metrics |

---

## Testing

**54 tests** | **0 failures** | 3 skipped (PCAP native library — expected on systems without Npcap/libpcap)

```bash
mvn test
```

Tests cover: packet parsing, SNI extraction, five-tuple hashing, load balancing, connection tracking, all three rule types, stats aggregation (including 10-thread concurrency), worker lifecycle, queue backpressure, Spring context loading, health endpoint integration, and a 1M-packet performance benchmark (asserts >90% success ratio).

---

## Dependencies

| Dependency | Version | Scope | Purpose |
|-----------|---------|-------|---------|
| `spring-boot-starter-web` | 3.2.5 | compile | REST API |
| `spring-boot-starter-actuator` | 3.2.5 | compile | Health, metrics, info |
| `spring-boot-starter-validation` | 3.2.5 | compile | Request validation |
| `spring-boot-starter-websocket` | 3.2.5 | compile | Live stats WebSocket |
| `spring-boot-starter-data-jpa` | 3.2.5 | compile | Persistence layer |
| `pcap4j-core` | 1.8.2 | compile | PCAP file reading |
| `pcap4j-packetfactory-static` | 1.8.2 | compile | Packet factory |
| `postgresql` | — | runtime | Production database |
| `h2` | — | runtime | In-memory dev database |
| `micrometer-registry-prometheus` | — | runtime | Prometheus metrics |
| `resilience4j-spring-boot3` | 2.2.0 | compile | Circuit breakers, retries |
| `lombok` | — | compile (optional) | Boilerplate reduction |
| `spring-boot-starter-test` | 3.2.5 | test | JUnit 5, MockMvc, AssertJ |
