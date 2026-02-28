# DPI Engine — Module

> Core Spring Boot application module for the Deep Packet Inspection engine.

For full documentation, architecture, and getting started guide, see the [project README](../README.md).

## Quick Reference

```bash
# Build
mvn clean install

# Run (server mode)
mvn spring-boot:run

# Run (pcap mode)
mvn spring-boot:run -Dspring-boot.run.arguments="--dpi.mode=pcap --dpi.capture.pcap-file-path=path/to/file.pcap"

# Test
mvn test
```
