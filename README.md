# 🔍 Deep Packet Inspection Engine

<div align="center">

![Java](https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2.5-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-3.9-C71A36?style=for-the-badge&logo=apache-maven&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)

**A production-grade, high-performance Deep Packet Inspection engine built with Spring Boot —  
designed for real-time network traffic analysis, rule-based enforcement, and scalable packet processing.**

</div>

---

## 🌐 Overview

The **DPI Engine** captures and analyzes network packets from PCAP files or live interfaces, extracts rich metadata, builds logical connections using five-tuple tracking, applies dynamic traffic rules (ALLOW / BLOCK / THROTTLE), and exposes real-time analytics through REST APIs.

Built with clean layered architecture and designed for multi-threaded scalability from day one.

---

## 🏗️ Architecture

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
            │  Connection  │           │    Rules     │        │  Decision    │
            │   Tracker    │           │    Engine    │        │   Engine     │
            └──────┬───────┘           └──────┬───────┘        └──────┬───────┘
                   │                          │                       │
                   ▼                          ▼                       ▼
            ┌──────────────┐           ┌──────────────┐        ┌──────────────┐
            │ AI Extractor │           │ Async Events │        │  Prometheus  │
            │  (ML Ready)  │           │   (Postgres) │        │   Metrics    │
            └──────────────┘           └──────────────┘        └──────────────┘
                                              │
                                              ▼
                                    ┌──────────────────┐
                                    │ WebSocket Streams│
                                    │ & REST API Layer │
                                    └──────────────────┘
```

---

## 📦 Extracted Metadata

| Field              | Description                                |
|--------------------|--------------------------------------------|
| **Source IP**       | IPv4/IPv6 source address                   |
| **Destination IP**  | IPv4/IPv6 destination address              |
| **Source Port**     | Transport-layer source port                |
| **Destination Port**| Transport-layer destination port           |
| **Protocol**        | TCP or UDP                                 |
| **Packet Size**     | Total packet length in bytes               |
| **Timestamp**       | Capture timestamp (nanosecond precision)   |
| **TLS SNI**         | Server Name Indication (when available)    |

---

## 📁 Project Structure

```
dpi-engine/
└── src/main/java/com/ayush/dpi/
    ├── DpiEngineApplication.java    # Application entry point
    ├── config/                      # Centralized configuration & properties
    ├── capture/                     # Packet ingestion (PCAP / live interfaces)
    ├── parser/                      # Raw byte → structured metadata decoding
    ├── loadbalancer/                # Packet distribution across worker threads
    ├── worker/                      # Multi-threaded packet processing pool
    ├── connection/                  # Five-tuple connection tracking
    ├── rules/                       # Dynamic traffic rule definitions
    ├── decision/                    # Rule evaluation & action determination
    ├── stats/                       # Real-time metrics aggregation
    ├── api/                         # REST controllers & error handling
    └── util/                        # Shared helpers & constants
```

---

## ⚙️ Tech Stack

| Technology        | Purpose                                     |
|-------------------|---------------------------------------------|
| **Java 17**       | Modern LTS with records & pattern matching  |
| **Spring Boot 3.2** | Production-grade application framework   |
| **Spring Web**    | REST API layer                              |
| **Spring Actuator** | Health, metrics, info endpoints           |
| **Spring Validation** | Request payload validation              |
| **Lombok**        | Boilerplate reduction                       |
| **Logback**       | Structured logging with rolling files       |
| **JUnit 5 + MockMvc** | Integration testing                    |

---

## 🚀 Getting Started

### Prerequisites

- **Java 17+**
- **Maven 3.9+** (or use the included wrapper)

### Build

```bash
cd dpi-engine
mvn clean install
```

### Run

```bash
cd dpi-engine
mvn spring-boot:run
```

### Verify

```bash
# Custom health endpoint
curl http://localhost:8080/api/health

# Actuator health
curl http://localhost:8080/actuator/health
```

**Expected response from `/api/health`:**

```json
{
  "status": "UP",
  "timestamp": "2026-02-28T06:18:42.123Z",
  "version": "0.1.0-SNAPSHOT"
}
```

---

## 🐳 Deployment & Containerization

The DPI engine is fully containerized with a lightweight, multi-stage Dockerfile and can be deployed in production environments using Docker Compose or Kubernetes.

### Environment Configuration
The following environment variables dynamically override `application.yml`:
*   `DPI_MODE`: `server` (REST only) or `benchmark` (Load test injection)
*   `DPI_WORKER_COUNT`: Number of threads allocated
*   `DPI_PCAP_FILE`: Absolute path if processing a file
*   `LOGGING_LEVEL_ROOT`: `INFO`, `DEBUG`, or `TRACE`

### Docker Compose (Recommended Local)
A `docker-compose.yml` is included to spin up the API and a PostgreSQL database (prepared for log aggregation).
```bash
docker-compose up -d
docker-compose logs -f dpi-engine
```

### Kubernetes (K8s)
Manifests are provided in `/k8s` for cloud deployments.
```bash
kubectl apply -f k8s/deployment.yml
kubectl apply -f k8s/service.yml
kubectl get pods -w
```

---

## 🗺️ Roadmap

| Phase | Description                                       | Status       |
|-------|---------------------------------------------------|--------------|
| 1     | Project scaffold, health API, config, logging     | ✅ Complete   |
| 2     | Packet capture & parsing (PCAP + live interface)  | ✅ Complete   |
| 3     | Connection tracking with five-tuple               | ✅ Complete   |
| 4     | Rule engine with dynamic ALLOW/BLOCK/THROTTLE     | ✅ Complete   |
| 5     | Multi-threaded worker pool with load balancing    | ✅ Complete   |
| 6     | Real-time statistics & dashboard APIs             | ✅ Complete   |
| 7     | Performance tuning & production hardening         | ✅ Complete   |
| 8     | Docker, Kubernetes, and CI/CD pipelines           | ✅ Complete   |
| 9     | Extensibility, Observability, Persistence & AI    | ✅ Complete   |

---

## ⚡ Performance & Benchmarks

The DPI Engine is optimized for high-throughput packet processing, utilizing an asynchronous event-driven architecture, lock-free concurrency, and zero-allocation data structures on the hot path.

### Key Optimizations
*   **`ConnectionKey` Optimization:** Replaced string-based hashing with an immutable `record` that precomputes its `hashCode`, avoiding expensive string concatenations and drastically improving HashMap lookups.
*   **Dynamic Worker Scaling:** The `LoadBalancerService` automatically provisions worker threads matching the available CPU cores.
*   **Lock-Free Analytics:** Uses `LongAdder` and `ConcurrentHashMap` combined with periodic worker-local flushing to eliminate thread contention on global statistics.
*   **Reduced I/O Bottlenecks:** Sampled downstream logging on the hot path to prevent logger I/O blocking.

### Micro-Benchmark Results

Using the built-in `SyntheticTrafficSimulator` simulating realistic connection distributions across local threads, the following results were achieved:

*   **Total Packets Injected:** 1,000,000 packets
*   **Ingestion Rate:** ~400,000+ packets/second
*   **Processing Time:** ~2.3 seconds
*   **Throughput Success:** 94-98% ingestion success ratio under extreme continuous load without client-side backpressure.

---

## 🤝 Contributing

This is a system design portfolio project. Contributions, suggestions, and feedback are welcome.

---

## 📄 License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.

---

<div align="center">

**Built with ❤️ by [Ayush Mishra](https://github.com/ayush-mishra7)**

</div>
