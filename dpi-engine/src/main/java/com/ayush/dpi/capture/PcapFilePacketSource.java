package com.ayush.dpi.capture;

import lombok.extern.slf4j.Slf4j;
import org.pcap4j.core.NotOpenException;
import org.pcap4j.core.PcapHandle;
import org.pcap4j.core.PcapNativeException;
import org.pcap4j.core.Pcaps;
import org.pcap4j.packet.Packet;

import java.io.File;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * {@link PacketSource} implementation that reads packets sequentially from a
 * PCAP file.
 * <p>
 * Packets are streamed one-by-one via {@code pcap4j}'s {@link PcapHandle},
 * ensuring
 * constant memory usage regardless of file size. Malformed or unsupported
 * packets
 * are logged and skipped without crashing.
 * </p>
 */
@Slf4j
public class PcapFilePacketSource implements PacketSource {

    private final String pcapFilePath;
    private final AtomicBoolean active = new AtomicBoolean(false);
    private final AtomicLong sequenceCounter = new AtomicLong(0);
    private volatile PcapHandle handle;

    public PcapFilePacketSource(String pcapFilePath) {
        this.pcapFilePath = pcapFilePath;
    }

    @Override
    public void start(PacketHandler handler) {
        File pcapFile = new File(pcapFilePath);
        if (!pcapFile.exists() || !pcapFile.isFile()) {
            log.error("PCAP file not found: {}", pcapFilePath);
            throw new IllegalArgumentException("PCAP file does not exist: " + pcapFilePath);
        }

        log.info("Opening PCAP file: {} ({} bytes)", pcapFilePath, pcapFile.length());
        active.set(true);

        try {
            handle = Pcaps.openOffline(pcapFilePath);
            log.info("PCAP file opened successfully. Starting packet ingestion...");

            while (active.get()) {
                try {
                    Packet packet = handle.getNextPacket();
                    if (packet == null) {
                        log.info("End of PCAP file reached");
                        break;
                    }

                    byte[] rawData = packet.getRawData();
                    long seq = sequenceCounter.incrementAndGet();

                    RawPacket rawPacket = RawPacket.builder()
                            .data(rawData)
                            .length(rawData.length)
                            .timestamp(handle.getTimestamp().toInstant())
                            .sequenceNumber(seq)
                            .build();

                    handler.handle(rawPacket);

                } catch (NotOpenException e) {
                    log.warn("PCAP handle closed unexpectedly, stopping capture");
                    break;
                } catch (Exception e) {
                    long currentSeq = sequenceCounter.get();
                    log.warn("Skipping malformed packet #{}: {}", currentSeq, e.getMessage());
                }
            }
        } catch (PcapNativeException e) {
            log.error("Failed to open PCAP file: {}", e.getMessage(), e);
            throw new RuntimeException("Cannot open PCAP file: " + pcapFilePath, e);
        } finally {
            stop();
        }
    }

    @Override
    public void stop() {
        active.set(false);
        if (handle != null && handle.isOpen()) {
            handle.close();
            log.info("PCAP handle closed. Total packets read: {}", sequenceCounter.get());
        }
    }

    @Override
    public boolean isActive() {
        return active.get();
    }

    /**
     * @return total number of packets read so far
     */
    public long getPacketCount() {
        return sequenceCounter.get();
    }
}
