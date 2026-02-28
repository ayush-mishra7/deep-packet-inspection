package com.ayush.dpi.parser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link SniExtractor}.
 * <p>
 * Validates SNI extraction from crafted TLS ClientHello payloads
 * and safe handling of non-TLS, malformed, and truncated data.
 * </p>
 */
class SniExtractorTest {

    @Test
    @DisplayName("Extracts SNI from a valid TLS ClientHello")
    void extractsSniFromValidClientHello() {
        byte[] clientHello = buildTlsClientHelloWithSni("www.example.com");
        Optional<String> sni = SniExtractor.extract(clientHello);
        assertThat(sni).isPresent().hasValue("www.example.com");
    }

    @Test
    @DisplayName("Extracts SNI with longer domain name")
    void extractsSniLongerDomain() {
        byte[] clientHello = buildTlsClientHelloWithSni("api.subdomain.example.org");
        Optional<String> sni = SniExtractor.extract(clientHello);
        assertThat(sni).isPresent().hasValue("api.subdomain.example.org");
    }

    @Test
    @DisplayName("Returns empty for null payload")
    void returnsEmptyForNull() {
        assertThat(SniExtractor.extract(null)).isEmpty();
    }

    @Test
    @DisplayName("Returns empty for empty payload")
    void returnsEmptyForEmpty() {
        assertThat(SniExtractor.extract(new byte[0])).isEmpty();
    }

    @Test
    @DisplayName("Returns empty for non-TLS data")
    void returnsEmptyForNonTls() {
        assertThat(SniExtractor.extract(new byte[] { 0x47, 0x45, 0x54, 0x20, 0x2F })).isEmpty();
    }

    @Test
    @DisplayName("Returns empty for truncated TLS record")
    void returnsEmptyForTruncatedRecord() {
        byte[] truncated = new byte[] { 0x16, 0x03, 0x01, 0x00, 0x05, 0x01 };
        assertThat(SniExtractor.extract(truncated)).isEmpty();
    }

    @Test
    @DisplayName("Returns empty for TLS record that is not a ClientHello")
    void returnsEmptyForNonClientHello() {
        // ServerHello has handshake type 0x02
        byte[] serverHello = buildMinimalTlsRecord(0x02);
        assertThat(SniExtractor.extract(serverHello)).isEmpty();
    }

    // =========================================================================
    // Helpers — build TLS ClientHello payloads
    // =========================================================================

    /**
     * Builds a minimal valid TLS ClientHello with a single SNI extension.
     */
    private byte[] buildTlsClientHelloWithSni(String hostname) {
        byte[] hostnameBytes = hostname.getBytes(StandardCharsets.US_ASCII);

        // SNI extension payload: list_len(2) + type(1) + name_len(2) + name
        int sniDataLen = 2 + 1 + 2 + hostnameBytes.length;
        // Extension: type(2) + length(2) + data
        int sniExtLen = 4 + sniDataLen;
        // Extensions block: total_length(2) + extension
        int extensionsBlockLen = 2 + sniExtLen;

        // ClientHello body (minimal):
        // client_version(2) + random(32) + session_id_len(1) + session_id(0)
        // + cipher_suites_len(2) + one_suite(2) + comp_methods_len(1) + one_method(1)
        // + extensions
        int clientHelloBodyLen = 2 + 32 + 1 + 2 + 2 + 1 + 1 + extensionsBlockLen;

        // Handshake: type(1) + length(3) + body
        int handshakeLen = 4 + clientHelloBodyLen;

        // TLS record: content_type(1) + version(2) + length(2) + handshake
        int totalLen = 5 + handshakeLen;

        ByteBuffer buf = ByteBuffer.allocate(totalLen);

        // --- TLS Record Header ---
        buf.put((byte) 0x16); // content type: Handshake
        buf.putShort((short) 0x0301); // TLS 1.0
        buf.putShort((short) handshakeLen); // record length

        // --- Handshake Header ---
        buf.put((byte) 0x01); // ClientHello
        // Handshake length (3 bytes big-endian)
        buf.put((byte) ((clientHelloBodyLen >> 16) & 0xFF));
        buf.put((byte) ((clientHelloBodyLen >> 8) & 0xFF));
        buf.put((byte) (clientHelloBodyLen & 0xFF));

        // --- ClientHello Body ---
        buf.putShort((short) 0x0303); // client version: TLS 1.2
        buf.put(new byte[32]); // random
        buf.put((byte) 0); // session ID length = 0
        buf.putShort((short) 2); // cipher suites length
        buf.putShort((short) 0x002F); // TLS_RSA_WITH_AES_128_CBC_SHA
        buf.put((byte) 1); // compression methods length
        buf.put((byte) 0); // null compression

        // --- Extensions ---
        buf.putShort((short) sniExtLen); // total extensions length

        // SNI extension
        buf.putShort((short) 0x0000); // extension type: SNI
        buf.putShort((short) sniDataLen); // extension data length
        buf.putShort((short) (sniDataLen - 2)); // SNI list length
        buf.put((byte) 0); // host name type
        buf.putShort((short) hostnameBytes.length); // host name length
        buf.put(hostnameBytes); // host name

        return buf.array();
    }

    /**
     * Builds a minimal TLS record with a given handshake type (non-ClientHello).
     */
    private byte[] buildMinimalTlsRecord(int handshakeType) {
        ByteBuffer buf = ByteBuffer.allocate(10);
        buf.put((byte) 0x16); // Handshake
        buf.putShort((short) 0x0301); // TLS 1.0
        buf.putShort((short) 5); // record length
        buf.put((byte) handshakeType); // handshake type
        buf.put((byte) 0); // length (3 bytes)
        buf.put((byte) 0);
        buf.put((byte) 1);
        buf.put((byte) 0); // 1 byte of body
        return buf.array();
    }
}
