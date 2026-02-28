package com.ayush.dpi.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Centralized DPI engine properties bound from {@code application.yml}.
 * <p>
 * All custom configuration lives under the {@code dpi.*} namespace.
 * </p>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "dpi")
public class DpiProperties {

    /** Display name of the application. */
    private String appName = "DPI Engine";

    /** Current application version. */
    private String version = "0.1.0-SNAPSHOT";

    /** Maximum number of worker threads for packet processing. */
    private int maxWorkers = 4;

    /** Default traffic action when no rule matches (ALLOW | BLOCK | THROTTLE). */
    private String defaultAction = "ALLOW";

    /** Packet capture configuration. */
    private Capture capture = new Capture();

    @Getter
    @Setter
    public static class Capture {
        /** Kernel buffer size in bytes for live capture. */
        private int bufferSize = 65536;

        /** Maximum bytes to capture per packet. */
        private int snapshotLength = 65535;
    }
}
