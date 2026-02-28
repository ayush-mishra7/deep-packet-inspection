import { useState, useRef, useCallback, useEffect } from 'react';

export interface DpiStats {
    totalPackets: number;
    throughputPerSecond: number;
    tcpRatio: number;
    udpRatio: number;
    allowedCount: number;
    blockedCount: number;
    throttledCount: number;
    topDomains: Record<string, number>;
}

const DEFAULT_STATS: DpiStats = {
    totalPackets: 0,
    throughputPerSecond: 0,
    tcpRatio: 0,
    udpRatio: 0,
    allowedCount: 0,
    blockedCount: 0,
    throttledCount: 0,
    topDomains: {}
};

export function useDpiStats(url: string) {
    const [stats, setStats] = useState<DpiStats>(DEFAULT_STATS);
    const [isConnected, setIsConnected] = useState(false);
    const ws = useRef<WebSocket | null>(null);
    const reconnectTimeout = useRef<number | null>(null);

    const connect = useCallback(() => {
        try {
            ws.current = new WebSocket(url);

            ws.current.onopen = () => {
                setIsConnected(true);
                console.log('Connected to DPI Engine WebSocket');
            };

            ws.current.onmessage = (event: MessageEvent) => {
                try {
                    const data = JSON.parse(event.data);
                    setStats((prev) => ({ ...prev, ...data }));
                } catch (err) {
                    console.error('Failed to parse stats payload', err);
                }
            };

            ws.current.onclose = () => {
                setIsConnected(false);
                console.log('Disconnected from DPI Engine WebSocket, reconnecting in 3s...');
                // Auto-reconnect after 3 seconds
                reconnectTimeout.current = window.setTimeout(connect, 3000);
            };

            ws.current.onerror = (err: Event) => {
                console.error('WebSocket Error ->', err);
                ws.current?.close();
            };
        } catch (error) {
            console.error('WebSocket Connection Initialization Error:', error);
            setIsConnected(false);
        }
    }, [url]);

    useEffect(() => {
        connect();

        return () => {
            if (reconnectTimeout.current) clearTimeout(reconnectTimeout.current);
            if (ws.current) {
                ws.current.onclose = null; // Prevent reconnect loop on intentional unmount
                ws.current.close();
            }
        };
    }, [connect]);

    return { stats, isConnected };
}
