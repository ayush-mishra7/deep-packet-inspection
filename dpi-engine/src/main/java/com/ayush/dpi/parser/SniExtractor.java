package com.ayush.dpi.parser;

import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Extracts the Server Name Indication (SNI) from a TLS ClientHello message.
 * <p>
 * Performs defensive byte-level parsing with bounds checks at every step.
 * Never throws exceptions — returns {@link Optional#empty()} for any
 * non-TLS, malformed, or truncated payload.
 * </p>
 * <p>
 * This class is stateless and thread-safe.
 * </p>
 */
@Slf4j
public final class SniExtractor {

    // TLS constants
    private static final int TLS_CONTENT_TYPE_HANDSHAKE = 22;
    private static final int TLS_HANDSHAKE_TYPE_CLIENT_HELLO = 1;
    private static final int SNI_EXTENSION_TYPE = 0x0000;
    private static final int SNI_HOST_NAME_TYPE = 0;

    private SniExtractor() {
        // Utility class
    }

    /**
     * Attempt to extract SNI from a TCP payload that may contain a TLS ClientHello.
     *
     * @param tcpPayload the raw bytes of the TCP payload
     * @return the server name if a valid SNI extension is found, otherwise empty
     */
    public static Optional<String> extract(byte[] tcpPayload) {
        if (tcpPayload == null || tcpPayload.length < 6) {
            return Optional.empty();
        }

        ByteBuffer buf = ByteBuffer.wrap(tcpPayload);

        try {
            // --- TLS Record Layer ---
            int contentType = Byte.toUnsignedInt(buf.get()); // 1 byte
            if (contentType != TLS_CONTENT_TYPE_HANDSHAKE) {
                return Optional.empty();
            }

            buf.getShort(); // TLS version (2 bytes) — skip
            int recordLength = Short.toUnsignedInt(buf.getShort()); // 2 bytes

            if (buf.remaining() < recordLength || recordLength < 4) {
                return Optional.empty();
            }

            // --- Handshake Header ---
            int handshakeType = Byte.toUnsignedInt(buf.get()); // 1 byte
            if (handshakeType != TLS_HANDSHAKE_TYPE_CLIENT_HELLO) {
                return Optional.empty();
            }

            // Handshake length (3 bytes, big-endian)
            int handshakeLength = ((Byte.toUnsignedInt(buf.get()) << 16)
                    | (Byte.toUnsignedInt(buf.get()) << 8)
                    | Byte.toUnsignedInt(buf.get()));

            if (buf.remaining() < handshakeLength) {
                return Optional.empty();
            }

            // --- ClientHello Body ---
            buf.getShort(); // client version (2 bytes) — skip
            skipBytes(buf, 32); // random (32 bytes)

            // Session ID
            int sessionIdLen = Byte.toUnsignedInt(buf.get());
            if (buf.remaining() < sessionIdLen)
                return Optional.empty();
            skipBytes(buf, sessionIdLen);

            // Cipher Suites
            int cipherSuitesLen = Short.toUnsignedInt(buf.getShort());
            if (buf.remaining() < cipherSuitesLen)
                return Optional.empty();
            skipBytes(buf, cipherSuitesLen);

            // Compression Methods
            int compMethodsLen = Byte.toUnsignedInt(buf.get());
            if (buf.remaining() < compMethodsLen)
                return Optional.empty();
            skipBytes(buf, compMethodsLen);

            // --- Extensions ---
            if (buf.remaining() < 2)
                return Optional.empty();
            int extensionsLength = Short.toUnsignedInt(buf.getShort());
            if (buf.remaining() < extensionsLength)
                return Optional.empty();

            int extensionsEnd = buf.position() + extensionsLength;

            while (buf.position() + 4 <= extensionsEnd) {
                int extType = Short.toUnsignedInt(buf.getShort());
                int extLen = Short.toUnsignedInt(buf.getShort());

                if (buf.remaining() < extLen)
                    return Optional.empty();

                if (extType == SNI_EXTENSION_TYPE) {
                    return parseSniExtension(buf, extLen);
                }

                skipBytes(buf, extLen);
            }

            return Optional.empty();

        } catch (Exception e) {
            log.debug("SNI extraction failed (non-TLS or malformed): {}", e.getMessage());
            return Optional.empty();
        }
    }

    private static Optional<String> parseSniExtension(ByteBuffer buf, int extLen) {
        if (extLen < 5) {
            skipBytes(buf, extLen);
            return Optional.empty();
        }

        buf.getShort(); // SNI list length — skip
        int nameType = Byte.toUnsignedInt(buf.get());

        if (nameType != SNI_HOST_NAME_TYPE) {
            skipBytes(buf, extLen - 3);
            return Optional.empty();
        }

        int nameLen = Short.toUnsignedInt(buf.getShort());
        if (buf.remaining() < nameLen || nameLen == 0) {
            return Optional.empty();
        }

        byte[] nameBytes = new byte[nameLen];
        buf.get(nameBytes);
        String sni = new String(nameBytes, StandardCharsets.US_ASCII).trim();

        return sni.isEmpty() ? Optional.empty() : Optional.of(sni);
    }

    private static void skipBytes(ByteBuffer buf, int count) {
        buf.position(buf.position() + count);
    }
}
