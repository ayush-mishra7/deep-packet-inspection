import { useState, useEffect } from 'react';
import {
    XAxis, YAxis, CartesianGrid,
    Tooltip, ResponsiveContainer, Area, AreaChart
} from 'recharts';

interface ThroughputChartProps {
    currentThroughput: number;
}

export function ThroughputChart({ currentThroughput }: ThroughputChartProps) {
    const [data, setData] = useState<{ time: string, pps: number }[]>([]);

    useEffect(() => {
        setData(prev => {
            const now = new Date();
            const timeStr = `${now.getHours()}:${now.getMinutes()}:${now.getSeconds()}`;

            const newData = [...prev, { time: timeStr, pps: currentThroughput }];
            // Keep only last 30 seconds of history
            if (newData.length > 30) {
                return newData.slice(newData.length - 30);
            }
            return newData;
        });
    }, [currentThroughput]);

    return (
        <div className="bg-surface border border-surfaceHighlight rounded-xl p-5 shadow-2xl transition-all hover:border-textMuted/30">
            <div className="mb-4">
                <h3 className="text-textMuted text-sm font-medium tracking-wide uppercase">Live Throughput (PPS)</h3>
            </div>

            <div className="h-64 w-full">
                <ResponsiveContainer width="100%" height="100%">
                    <AreaChart data={data} margin={{ top: 5, right: 0, left: 0, bottom: 0 }}>
                        <defs>
                            <linearGradient id="colorPps" x1="0" y1="0" x2="0" y2="1">
                                <stop offset="5%" stopColor="#4ade80" stopOpacity={0.3} />
                                <stop offset="95%" stopColor="#4ade80" stopOpacity={0} />
                            </linearGradient>
                        </defs>
                        <CartesianGrid strokeDasharray="3 3" stroke="#222226" vertical={false} />
                        <XAxis
                            dataKey="time"
                            stroke="#94a3b8"
                            fontSize={12}
                            tickMargin={10}
                            minTickGap={30}
                        />
                        <YAxis
                            stroke="#94a3b8"
                            fontSize={12}
                            tickFormatter={(val) => val > 1000 ? `${(val / 1000).toFixed(1)}k` : val}
                        />
                        <Tooltip
                            contentStyle={{ backgroundColor: '#151518', borderColor: '#222226', color: '#f8fafc' }}
                            itemStyle={{ color: '#4ade80' }}
                        />
                        <Area
                            type="monotone"
                            dataKey="pps"
                            stroke="#4ade80"
                            strokeWidth={3}
                            fillOpacity={1}
                            fill="url(#colorPps)"
                            isAnimationActive={false}
                        />
                    </AreaChart>
                </ResponsiveContainer>
            </div>
        </div>
    );
}
