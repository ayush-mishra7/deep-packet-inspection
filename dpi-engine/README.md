# DPI Engine

> **High-Performance Deep Packet Inspection Engine with Rule-Based Traffic Enforcement**

A production-grade, scalable DPI system built with Spring Boot that processes network packets from PCAP files or live interfaces, extracts rich metadata, builds logical connections, applies dynamic traffic rules, and serves real-time analytics via REST APIs.

---

## Architecture

```
┌─────────────┐     ┌─────────────┐     ┌───────────────┐
│   Capture   │────▶│   Parser    │────▶│ Load Balancer │
│  (PCAP/NIC) │     │ (IP/TCP/TLS)│     │ (Hash/RR)     │
└─────────────┘     └─────────────┘     └───────┬───────┘
                                                │
                    ┌───────────────────────────┼───────────────────────┐
                    ▼                           ▼                       ▼
            ┌──────────────┐           ┌──────────────┐        ┌──────────────┐
            │   Worker 1   │           │   Worker 2   │  ...   │   Worker N   │
            └──────┬───────┘           └──────┬───────┘        └──────┬───────┘
                   │                          │                       │
                   ▼                          ▼                       ▼
            ┌──────────────┐           ┌──────────────┐        ┌──────────────┐
            │  Connection  │           │    Rules      │        │  Decision    │
            │   Tracker    │           │    Engine     │        │   Engine     │
            └──────────────┘           └──────────────┘        └──────────────┘
                                              │
                                              ▼
                                    ┌──────────────────┐
                                    │   Statistics &    │
                                    │   REST API Layer  │
                                    └──────────────────┘
```

## Extracted Metadata

| Field           | Description                              |
|-----------------|------------------------------------------|
| Source IP        | IPv4/IPv6 source address                 |
| Destination IP   | IPv4/IPv6 destination address            |
| Source Port      | Transport-layer source port              |
| Destination Port | Transport-layer destination port         |
| Protocol         | TCP or UDP                               |
| Packet Size      | Total packet length in bytes             |
| Timestamp        | Capture timestamp (nanosecond precision) |
| TLS SNI          | Server Name Indication (when available)  |

## Project Structure

```
com.ayush.dpi
├── config/          # Centralized configuration & properties
├── capture/         # Packet ingestion from PCAP / live interfaces
├── parser/          # Raw byte → structured metadata decoding
├── loadbalancer/    # Packet distribution across worker threads
├── worker/          # Multi-threaded packet processing pool
├── connection/      # Five-tuple connection tracking
├── rules/           # Dynamic traffic rule definitions
├── decision/        # Rule evaluation & action determination
├── stats/           # Real-time metrics aggregation
├── api/             # REST controllers & error handling
└── util/            # Shared helpers & constants
```

## Tech Stack

- **Java 17** — modern LTS with records, sealed classes, pattern matching
- **Spring Boot 3.2** — production-grade framework
- **Spring Web** — REST API layer
- **Spring Actuator** — health, metrics, info endpoints
- **Spring Validation** — request payload validation
- **Lombok** — boilerplate reduction
- **Logback** — structured logging with rolling files
- **JUnit 5 + MockMvc** — integration testing

## Getting Started

### Prerequisites

- Java 17+
- Maven 3.9+ (or use the included Maven wrapper)

### Build

```bash
cd dpi-engine
mvnw.cmd clean install        # Windows
./mvnw clean install           # macOS / Linux
```

### Run

```bash
mvnw.cmd spring-boot:run      # Windows
./mvnw spring-boot:run         # macOS / Linux
```

### Verify

```bash
# Health endpoint
curl http://localhost:8080/api/health

# Actuator
curl http://localhost:8080/actuator/health
```

Expected health response:

```json
{
  "status": "UP",
  "timestamp": "2026-02-28T06:18:42.123Z",
  "version": "0.1.0-SNAPSHOT"
}
```

## Roadmap

| Phase | Description                                       | Status      |
|-------|---------------------------------------------------|-------------|
| 1     | Project scaffold, health API, config, logging     | ✅ Complete  |
| 2     | Packet capture & parsing (PCAP + live interface)  | 🔜 Next     |
| 3     | Connection tracking with five-tuple               | ⏳ Planned   |
| 4     | Rule engine with dynamic ALLOW/BLOCK/THROTTLE     | ⏳ Planned   |
| 5     | Multi-threaded worker pool with load balancing    | ⏳ Planned   |
| 6     | Real-time statistics & dashboard APIs             | ⏳ Planned   |
| 7     | Performance tuning & production hardening         | ⏳ Planned   |

## License

Proprietary — © 2026 Ayush Mishra. All rights reserved.
