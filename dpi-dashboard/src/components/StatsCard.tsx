import type { ReactNode } from 'react';

interface StatsCardProps {
    title: string;
    value: string | number;
    icon: ReactNode;
    trend?: string;
    color?: string;
}

export function StatsCard({ title, value, icon, trend, color = 'text-primary' }: StatsCardProps) {
    return (
        <div className="bg-surface border border-surfaceHighlight rounded-xl p-5 shadow-2xl transition-all hover:border-textMuted/30">
            <div className="flex justify-between items-start mb-4">
                <h3 className="text-textMuted text-sm font-medium tracking-wide uppercase">{title}</h3>
                <div className={`p-2 rounded-lg bg-surfaceHighlight/50 ${color}`}>
                    {icon}
                </div>
            </div>

            <div className="flex items-baseline space-x-2">
                <h2 className="text-3xl font-bold font-mono tracking-tight text-textMain">{value}</h2>
                {trend && <span className="text-sm font-medium text-textMuted tracking-wider">{trend}</span>}
            </div>
        </div>
    );
}
