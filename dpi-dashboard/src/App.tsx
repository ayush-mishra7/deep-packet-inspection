import { Activity, ShieldCheck, ShieldAlert, Zap, Globe, AlertTriangle } from 'lucide-react';
import { useDpiStats } from './hooks/useDpiStats';
import { StatsCard } from './components/StatsCard';
import { ThroughputChart } from './components/ThroughputChart';
import { DecisionPieChart } from './components/DecisionPieChart';

const WEBSOCKET_URL = 'ws://localhost:8080/ws/stats';

function App() {
    const { stats, isConnected } = useDpiStats(WEBSOCKET_URL);

    return (
        <div className="min-h-screen p-6 md:p-10 max-w-7xl mx-auto space-y-8">
            {/* Header */}
            <header className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-surfaceHighlight pb-6 mb-8">
                <div>
                    <div className="flex items-center gap-3">
                        <ShieldCheck className="w-8 h-8 text-primary" />
                        <h1 className="text-2xl font-bold tracking-tight text-textMain">System Analytics</h1>
                    </div>
                    <p className="text-textMuted text-sm mt-1 flex items-center gap-2">
                        <span className="relative flex h-2 w-2">
                            {isConnected && (
                                <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-primary opacity-75"></span>
                            )}
                            <span className={`relative inline-flex rounded-full h-2 w-2 ${isConnected ? 'bg-primary' : 'bg-danger'}`}></span>
                        </span>
                        {isConnected ? 'Streaming live from Engine' : 'Disconnected - Attempting Reconnection...'}
                    </p>
                </div>

                <div className="bg-surface border border-surfaceHighlight rounded-lg px-4 py-2 flex items-center gap-3 shadow-lg">
                    <Globe className="w-4 h-4 text-secondary" />
                    <span className="text-sm font-mono tracking-wider text-textMuted">WSS:// {WEBSOCKET_URL.replace('ws://', '')}</span>
                    <div className={`text-xs ml-2 px-2 py-0.5 rounded uppercase font-bold ${isConnected ? 'bg-primary/20 text-primary' : 'bg-danger/20 text-danger'}`}>
                        {isConnected ? 'Active' : 'Offline'}
                    </div>
                </div>
            </header>

            {/* Primary Metrics Grid */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
                <StatsCard
                    title="Total Packets"
                    value={stats.totalPackets.toLocaleString()}
                    icon={<Activity className="w-5 h-5 text-secondary" />}
                    color="text-secondary"
                />
                <StatsCard
                    title="Allowed Traffic"
                    value={stats.allowedCount.toLocaleString()}
                    icon={<ShieldCheck className="w-5 h-5 text-primary" />}
                    color="text-primary"
                />
                <StatsCard
                    title="Blocked Threats"
                    value={stats.blockedCount.toLocaleString()}
                    icon={<ShieldAlert className="w-5 h-5 text-danger" />}
                    color="text-danger"
                />
                <StatsCard
                    title="Throttled Flows"
                    value={stats.throttledCount.toLocaleString()}
                    icon={<AlertTriangle className="w-5 h-5 text-warning" />}
                    color="text-warning"
                />
            </div>

            {/* Main Content Area */}
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">

                {/* Left Column - Big Chart */}
                <div className="lg:col-span-2 space-y-6">
                    <ThroughputChart currentThroughput={stats.throughputPerSecond} />

                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                        <StatsCard
                            title="TCP Ratio"
                            value={`${(stats.tcpRatio * 100).toFixed(1)}%`}
                            icon={<Zap className="w-5 h-5 text-secondary" />}
                            color="text-secondary"
                        />
                        <StatsCard
                            title="UDP Ratio"
                            value={`${(stats.udpRatio * 100).toFixed(1)}%`}
                            icon={<Activity className="w-5 h-5 text-textMuted" />}
                            color="text-textMuted"
                        />
                    </div>
                </div>

                {/* Right Column - Side Panels */}
                <div className="space-y-6">
                    <DecisionPieChart
                        allowed={stats.allowedCount}
                        blocked={stats.blockedCount}
                        throttled={stats.throttledCount}
                    />

                    {/* Top Domains Leaderboard */}
                    <div className="bg-surface border border-surfaceHighlight rounded-xl p-5 shadow-2xl">
                        <div className="flex items-center justify-between mb-4 border-b border-surfaceHighlight pb-3">
                            <h3 className="text-textMuted text-sm font-medium tracking-wide uppercase flex items-center gap-2">
                                <Globe className="w-4 h-4" /> Top Destinations
                            </h3>
                        </div>
                        {Object.keys(stats.topDomains).length === 0 ? (
                            <div className="text-center py-8 text-textMuted text-sm">Waiting for traffic data...</div>
                        ) : (
                            <ul className="space-y-3">
                                {Object.entries(stats.topDomains).map(([domain, count], index) => (
                                    <li key={domain} className="flex items-center justify-between group">
                                        <div className="flex items-center gap-3 overflow-hidden">
                                            <span className="text-xs font-mono bg-surfaceHighlight text-textMuted px-2 py-0.5 rounded">
                                                #{index + 1}
                                            </span>
                                            <span className="text-sm text-textMain truncate transition-colors group-hover:text-primary">
                                                {domain}
                                            </span>
                                        </div>
                                        <span className="text-sm font-mono text-textMuted">
                                            {count.toLocaleString()}
                                        </span>
                                    </li>
                                ))}
                            </ul>
                        )}
                    </div>
                </div>

            </div>
        </div>
    );
}

export default App;
