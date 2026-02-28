import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip, Legend } from 'recharts';

interface DecisionPieChartProps {
    allowed: number;
    blocked: number;
    throttled: number;
}

const COLORS = {
    allow: '#4ade80',    // Green
    block: '#fb7185',    // Red-ish
    throttle: '#fbbf24'  // Yellow
};

export function DecisionPieChart({ allowed, blocked, throttled }: DecisionPieChartProps) {
    const data = [
        { name: 'Allowed', value: allowed, color: COLORS.allow },
        { name: 'Blocked', value: blocked, color: COLORS.block },
        { name: 'Throttled', value: throttled, color: COLORS.throttle },
    ];

    // Prevent empty pie render errors
    if (allowed === 0 && blocked === 0 && throttled === 0) {
        return (
            <div className="bg-surface border border-surfaceHighlight rounded-xl p-5 shadow-2xl h-80 flex flex-col items-center justify-center">
                <h3 className="text-textMuted text-sm font-medium tracking-wide uppercase mb-2">Decisions</h3>
                <span className="text-textMuted">Waiting for traffic...</span>
            </div>
        );
    }

    return (
        <div className="bg-surface border border-surfaceHighlight rounded-xl p-5 shadow-2xl transition-all hover:border-textMuted/30 h-80 flex flex-col">
            <div className="mb-2">
                <h3 className="text-textMuted text-sm font-medium tracking-wide uppercase">Distribution</h3>
            </div>

            <div className="flex-1 w-full min-h-0">
                <ResponsiveContainer width="100%" height="100%">
                    <PieChart>
                        <Pie
                            data={data}
                            cx="50%"
                            cy="50%"
                            innerRadius={60}
                            outerRadius={80}
                            paddingAngle={5}
                            dataKey="value"
                            stroke="none"
                            isAnimationActive={true}
                        >
                            {data.map((entry, index) => (
                                <Cell key={`cell-${index}`} fill={entry.color} />
                            ))}
                        </Pie>
                        <Tooltip
                            contentStyle={{ backgroundColor: '#151518', borderColor: '#222226', color: '#f8fafc', borderRadius: '8px' }}
                            itemStyle={{ color: '#f8fafc' }}
                        />
                        <Legend verticalAlign="bottom" height={36} iconType="circle" />
                    </PieChart>
                </ResponsiveContainer>
            </div>
        </div>
    );
}
