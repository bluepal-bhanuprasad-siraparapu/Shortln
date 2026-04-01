import React from 'react';
import { useQuery } from '@tanstack/react-query';
import api from '../services/api';
import toast from 'react-hot-toast';
import { 
  MousePointer2, 
  Link2, 
  TrendingUp,
  ArrowUpRight,
  PlusCircle,
  BarChart3,
  Download,
  Users,
  Lock,
  Calendar,
  Layers
} from 'lucide-react';
import { 
  BarChart, 
  Bar, 
  XAxis, 
  YAxis, 
  CartesianGrid, 
  Tooltip, 
  ResponsiveContainer,
  AreaChart,
  Area,
  Cell
} from 'recharts';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import ExportControls from '../components/ExportControls';
import { downloadCSV, downloadPDF } from '../utils/exportUtils';

const StatCard = ({ title, value, icon, trend }) => (
  <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
    <div className="flex justify-between items-start mb-4">
      <div className="text-gray-700">
        {icon}
      </div>
      {trend && (
        <span className="flex items-center text-green-500 text-xs font-bold">
          <TrendingUp size={14} className="mr-1" /> {trend}
        </span>
      )}
    </div>
    <h3 className="text-gray-500 text-sm font-medium">{title}</h3>
    <p className="text-2xl font-bold text-brand-dark mt-1">{value}</p>
  </div>
);

const Dashboard = () => {
  const navigate = useNavigate();
  const { user, isAdmin } = useAuth();
  
  const { data: linksData, isLoading: loadingLinks } = useQuery({
    queryKey: ['links', isAdmin ? 'all' : 'user', 'top'],
    queryFn: () => api.get((isAdmin ? '/links/all' : '/links') + '?size=8&sort=clickCount,desc').then(res => res.data)
  });

  const links = linksData?.content || [];

  const { data: stats, isLoading: loadingStats } = useQuery({
    queryKey: ['analytics', 'stats', isAdmin ? 'all' : 'user'],
    queryFn: () => api.get('/analytics/stats').then(res => res.data)
  });
  
  const { data: history, isLoading: loadingHistory } = useQuery({
    queryKey: ['analytics', 'history', isAdmin ? 'all' : 'user'],
    queryFn: () => api.get('/analytics/history').then(res => res.data)
  });

  const totalClicks = stats?.totalClicks || 0;

  const handleExport = async (exportFormat) => {
    if (!isAdmin && user?.plan !== 'PRO') {
      toast.error("Exporting analytics is a PRO feature. Please upgrade your plan to access this feature.");
      return;
    }

    if (exportFormat === 'PDF') {
      try {
        const endpoint = isAdmin ? '/analytics/admin/export/all' : '/analytics/export';
        const response = await api.get(endpoint, { responseType: 'blob' });
        const url = window.URL.createObjectURL(new Blob([response.data], { type: 'application/pdf' }));
        const link = document.createElement('a');
        link.href = url;
        link.setAttribute('download', isAdmin ? 'all_analytics.pdf' : 'my_analytics.pdf');
        document.body.appendChild(link);
        link.click();
        link.remove();
        window.URL.revokeObjectURL(url);
      } catch (err) {
        console.error('Failed to download PDF', err);
        toast.error("Failed to download analytics report.");
      }
    } else {
      // CSV Export
      const headers = ["Section", "Date/Name", "Count/Clicks"];
      const rows = [
        ["OVERALL STATS", "", ""],
        ["Total Platform Links", "", stats?.totalLinks || 0],
        ["Total Clicks", "", stats?.totalClicks || 0],
        ["Unique Clicks", "", stats?.uniqueClicks || 0],
        ["Clicks Today", "", stats?.clicksToday || 0],
        ["", "", ""],
        ["TOP LINKS", "", ""],
        ...barData.map(d => ["Link", d.name, d.clicks]),
        ["", "", ""],
        ["30-DAY HISTORY", "", ""],
        ...history.map(h => ["History", h.date, h.count])
      ];

      downloadCSV(rows, headers, isAdmin ? 'platform_analytics' : 'my_analytics');
    }
  };

  const barData = (links || [])
    .map(link => ({
      name: link.title || link.shortUrl.split('/').pop(),
      clicks: link.clickCount || 0
    }));

  const chartColors = [
    '#3B82F6', '#8B5CF6', '#EC4899', '#F59E0B', '#10B981', '#6366F1'
  ];

  if (loadingLinks || loadingStats || loadingHistory) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-brand-accent"></div>
      </div>
    );
  }

  return (
    <div className="space-y-8">
      <div className="flex justify-between items-center">
        <div>
          <h2 className="text-2xl font-bold text-brand-dark">
            {isAdmin ? 'Platform Analytics Overview' : 'My Analytics Overview'}
          </h2>
          <p className="text-gray-500">
            {isAdmin ? 'Real-time performance metrics and traffic trends across the entire platform.' : 'Detailed performance insights and traffic distribution for your links.'}
          </p>
        </div>
        <div className="relative">
          <ExportControls 
            onExport={handleExport}
            label={(!isAdmin && user?.plan !== 'PRO') ? "Unlock Export" : "Export Analytics"}
          />
          {(!isAdmin && user?.plan !== 'PRO') && (
            <span className="absolute -top-3 -right-2 bg-gradient-to-r from-amber-400 to-orange-500 text-white text-[10px] px-2 py-0.5 rounded-full shadow-sm font-black tracking-tighter border-2 border-white animate-pulse">
              PRO
            </span>
          )}
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <StatCard 
          title={isAdmin ? "Total Global Links" : "Total Links"}
          value={stats?.totalLinks || 0} 
          icon={<Link2 size={28} strokeWidth={1.5} />} 
        />
        <StatCard 
          title={isAdmin ? "Platform Clicks" : "Total Clicks"}
          value={totalClicks} 
          icon={<MousePointer2 size={28} strokeWidth={1.5} />} 
        />
        <StatCard 
          title="Unique Clicks" 
          value={stats?.uniqueClicks || 0} 
          icon={<Users size={28} strokeWidth={1.5} />} 
        />
        <StatCard 
          title="Clicks Today" 
          value={stats?.clicksToday || 0} 
          icon={<TrendingUp size={28} strokeWidth={1.5} />} 
        />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        {/* Graph 1: Total Clicks per Link */}
        <div className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100 flex flex-col">
          <div className="flex justify-between items-center mb-8">
            <div>
              <h3 className="text-lg font-bold text-brand-dark">Clicks per Link</h3>
              <p className="text-sm text-gray-500">Distribution of clicks across your top links</p>
            </div>
            <div className="p-2 bg-blue-50 text-blue-600 rounded-lg">
              <Layers size={20} />
            </div>
          </div>
          <div className="h-[300px] w-full min-h-[300px] relative">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={barData} margin={{ top: 0, right: 0, left: -20, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f3f4f6" />
                <XAxis 
                  dataKey="name" 
                  axisLine={false} 
                  tickLine={false} 
                  tick={{ fill: '#9ca3af', fontSize: 12 }}
                  interval={0}
                />
                <YAxis 
                  axisLine={false} 
                  tickLine={false} 
                  tick={{ fill: '#9ca3af', fontSize: 12 }}
                />
                <Tooltip 
                  cursor={{ fill: '#f9fafb' }}
                  contentStyle={{ borderRadius: '12px', border: 'none', boxShadow: '0 10px 15px -3px rgb(0 0 0 / 0.1)' }}
                />
                <Bar dataKey="clicks" radius={[6, 6, 0, 0]} barSize={40}>
                  {barData.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={chartColors[index % chartColors.length]} />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Graph 2: Day-wise Click Counts */}
        <div className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100 flex flex-col">
          <div className="flex justify-between items-center mb-8">
            <div>
              <h3 className="text-lg font-bold text-brand-dark">Click History</h3>
              <p className="text-sm text-gray-500">Day-wise traffic trends for the last 30 days</p>
            </div>
            <div className="p-2 bg-purple-50 text-purple-600 rounded-lg">
              <Calendar size={20} />
            </div>
          </div>
          <div className="h-[300px] w-full min-h-[300px] relative">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={history} margin={{ top: 0, right: 0, left: -20, bottom: 0 }}>
                <defs>
                  <linearGradient id="colorCount" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#8B5CF6" stopOpacity={0.1}/>
                    <stop offset="95%" stopColor="#8B5CF6" stopOpacity={0}/>
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f3f4f6" />
                <XAxis 
                  dataKey="date" 
                  axisLine={false} 
                  tickLine={false} 
                  tick={{ fill: '#9ca3af', fontSize: 12 }}
                  tickFormatter={(str) => {
                    const date = new Date(str);
                    return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
                  }}
                />
                <YAxis 
                  axisLine={false} 
                  tickLine={false} 
                  tick={{ fill: '#9ca3af', fontSize: 12 }}
                />
                <Tooltip 
                  contentStyle={{ borderRadius: '12px', border: 'none', boxShadow: '0 10px 15px -3px rgb(0 0 0 / 0.1)' }}
                />
                <Area 
                  type="monotone" 
                  dataKey="count" 
                  stroke="#8B5CF6" 
                  strokeWidth={3}
                  fillOpacity={1} 
                  fill="url(#colorCount)" 
                />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Dashboard;
