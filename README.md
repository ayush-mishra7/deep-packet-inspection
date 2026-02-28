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

## 🗺️ Roadmap

| Phase | Description                                       | Status       |
|-------|---------------------------------------------------|--------------|
| 1     | Project scaffold, health API, config, logging     | ✅ Complete   |
| 2     | Packet capture & parsing (PCAP + live interface)  | 🔜 Next      |
| 3     | Connection tracking with five-tuple               | ⏳ Planned    |
| 4     | Rule engine with dynamic ALLOW/BLOCK/THROTTLE     | ⏳ Planned    |
| 5     | Multi-threaded worker pool with load balancing    | ⏳ Planned    |
| 6     | Real-time statistics & dashboard APIs             | ⏳ Planned    |
| 7     | Performance tuning & production hardening         | ⏳ Planned    |

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
