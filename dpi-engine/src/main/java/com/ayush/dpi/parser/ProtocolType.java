package com.ayush.dpi.parser;

/**
 * Supported transport/network protocol types identified during packet parsing.
 */
public enum ProtocolType {
    TCP,
    UDP,
    ICMP,
    OTHER;

    /**
     * Maps an IP protocol number to the corresponding enum value.
     *
     * @param protocolNumber the IP protocol field value
     * @return the matching {@link ProtocolType}
     */
    public static ProtocolType fromIpProtocolNumber(int protocolNumber) {
        return switch (protocolNumber) {
            case 6 -> TCP;
            case 17 -> UDP;
            case 1 -> ICMP;
            default -> OTHER;
        };
    }
}
