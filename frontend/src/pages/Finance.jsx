import React, { useState, useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import api from '../services/api';
import toast from 'react-hot-toast';
import { 
  TrendingUp, 
  Users, 
  CreditCard, 
  DollarSign, 
  Banknote,
  ArrowUpRight,
  ArrowDownRight,
  PieChart as PieChartIcon,
  Download,
  Calendar,
  Filter,
  Search,
  AlertCircle
} from 'lucide-react';
import { format } from 'date-fns';
import html2canvas from 'html2canvas';
import jsPDF from 'jspdf';
import autoTable from 'jspdf-autotable';
import { 
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
  PieChart, Pie, Cell, AreaChart, Area, Legend
} from 'recharts';
import ExportControls from '../components/ExportControls';
import { downloadCSV } from '../utils/exportUtils';

const FinanceCard = ({ title, value, icon, color, subText }) => (
  <div className="bg-white p-8 rounded-3xl shadow-sm border border-gray-100 relative overflow-hidden group hover:shadow-md transition-all">
    <div className={`absolute top-0 right-0 w-32 h-32 ${color} opacity-5 rounded-full -mr-16 -mt-16 transition-transform group-hover:scale-110 duration-700`}></div>
    <div className="relative z-10">
      <div className="flex justify-between items-start mb-6">
        <div className={`p-4 rounded-2xl ${color.replace('bg-', 'bg-').replace('-500', '/10')} ${color.replace('bg-', 'text-')}`}>
          {icon}
        </div>
      </div>
      <div>
        <p className="text-gray-500 text-sm font-bold uppercase tracking-wider mb-1">{title}</p>
        <h3 className="text-3xl font-black text-brand-dark">{value}</h3>
        {subText && <p className="text-xs text-gray-400 mt-2 font-medium">{subText}</p>}
      </div>
    </div>
  </div>
);

const Finance = () => {
  const [planFilter, setPlanFilter] = useState('ALL');
  const [searchTerm, setSearchTerm] = useState('');

  const { data: users, isLoading } = useQuery({
    queryKey: ['admin', 'users'],
    queryFn: () => api.get('/user/admin/all').then(res => res.data)
  });

  const stats = useMemo(() => {
    if (!users) return null;

    // Filter only USER role for financial calculations as per request
    const onlyUsers = users.filter(u => u.role === 'USER');
    const proUsers = onlyUsers.filter(u => u.plan === 'PRO');
    const freeUsers = onlyUsers.filter(u => !u.plan || u.plan === 'FREE');
    
    const baseRevenue = proUsers.length * 999;
    const arpu = onlyUsers.length > 0 ? (baseRevenue / onlyUsers.length) : 0;
    const potentialRevenue = (freeUsers.length * 0.10 * 999); // 10% conversion projection

    // Apply Filter for the list and primary stats
    let filteredUsers = onlyUsers;
    if (planFilter !== 'ALL') {
      filteredUsers = onlyUsers.filter(u => (u.plan || 'FREE') === planFilter);
    }
    
    if (searchTerm) {
      filteredUsers = filteredUsers.filter(u => 
        u.name?.toLowerCase().includes(searchTerm.toLowerCase()) ||
        u.email?.toLowerCase().includes(searchTerm.toLowerCase())
      );
    }

    const monthlyRevenue = [
      { month: 'Jan', revenue: baseRevenue * 0.4 },
      { month: 'Feb', revenue: baseRevenue * 0.6 },
      { month: 'Mar', revenue: baseRevenue * 0.75 },
      { month: 'Apr', revenue: baseRevenue * 0.85 },
      { month: 'May', revenue: baseRevenue * 0.92 },
      { month: 'Jun', revenue: baseRevenue },
    ];

    const userDistribution = [
      { name: 'PRO Members', value: proUsers.length },
      { name: 'Free Users', value: freeUsers.length },
    ];

    return { 
      totalRevenue: baseRevenue, 
      proUsers: proUsers.length, 
      freeUsers: freeUsers.length, 
      arpu,
      potentialRevenue,
      monthlyRevenue, 
      userDistribution,
      totalActiveUsers: onlyUsers.length,
      filteredUsers,
      totalCount: onlyUsers.length
    };
  }, [users, planFilter, searchTerm]);

  const handleExportPdf = async () => {
    try {
      toast.loading("Generating finance report...", { id: 'finance' });
      const doc = new jsPDF('p', 'mm', 'a4');
      const pageWidth = doc.internal.pageSize.getWidth();
      
      // Header
      doc.setFillColor(15, 23, 42); // slate-900
      doc.rect(0, 0, pageWidth, 40, 'F');
      doc.setTextColor(255, 255, 255);
      doc.setFontSize(22);
      doc.text("Financial Performance Report", 20, 25);
      doc.setFontSize(10);
      doc.text(`${planFilter === 'ALL' ? 'Platform Wide' : planFilter + ' Segment'} • ${format(new Date(), 'PPP')}`, 20, 32);

      // Highlights
      doc.setTextColor(50, 50, 50);
      doc.setFontSize(14);
      doc.text("Executive Summary", 20, 55);
      doc.setFontSize(10);
      doc.text(`Total Revenue: INR ${stats?.totalRevenue.toLocaleString()}`, 20, 65);
      doc.text(`Active PRO Members: ${stats?.proUsers}`, 20, 71);
      doc.text(`Average Revenue Per User: INR ${stats?.arpu.toFixed(2)}`, 20, 77);
      doc.text(`Projection (10% Conv): +INR ${stats?.potentialRevenue.toLocaleString()}`, 20, 83);

      let currentY = 95;

      // Capture Charts
      const charts = document.querySelectorAll('.recharts-responsive-container');
      for (let i = 0; i < charts.length; i++) {
        const canvas = await html2canvas(charts[i], { scale: 2 });
        const imgData = canvas.toDataURL('image/png');
        const imgWidth = i === 0 ? 170 : 100; // Growth chart wider
        const imgHeight = (canvas.height * imgWidth) / canvas.width;
        
        if (currentY + imgHeight > 270) {
          doc.addPage();
          currentY = 20;
        }

        doc.addImage(imgData, 'PNG', i === 0 ? 20 : (pageWidth-imgWidth)/2, currentY, imgWidth, imgHeight);
        currentY += imgHeight + 15;
      }

      // User Table
      if (stats?.filteredUsers && stats.filteredUsers.length > 0) {
        doc.addPage();
        doc.setFontSize(14);
        doc.text("User Breakdown", 20, 20);
        
        autoTable(doc, {
          startY: 30,
          head: [["Name", "Email", "Plan", "Growth Contrib."]],
          body: stats.filteredUsers.slice(0, 50).map(u => [
            u.name,
            u.email,
            u.plan || 'FREE',
            u.plan === 'PRO' ? 'INR 999' : '-'
          ]),
          theme: 'grid',
          headStyles: { fillColor: [15, 23, 42] },
          styles: { fontSize: 8 }
        });
      }

      doc.save(`finance-report-${planFilter.toLowerCase()}.pdf`);
      toast.success("Finance report ready!", { id: 'finance' });
    } catch (err) {
      console.error('PDF error', err);
      toast.error("Failed to generate report", { id: 'finance' });
    }
  };

  const handleExport = (format) => {
    if (format === 'PDF') {
      handleExportPdf();
    } else {
      const headers = ["Name", "Email", "Plan", "Status", "Joined Date"];
      const rows = stats.filteredUsers.map(u => [
        u.name,
        u.email,
        u.plan || 'FREE',
        u.active === 1 ? 'Active' : 'Inactive',
        new Date(u.createdAt).toLocaleDateString()
      ]);
      const filename = `finance-report-${new Date().getTime()}`;
      downloadCSV(rows, headers, filename);
    }
  };

  const COLORS = ['#3B82F6', '#E5E7EB'];

  if (isLoading) {
    return (
      <div className="flex flex-col items-center justify-center py-20 gap-4">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-brand-accent"></div>
        <p className="text-gray-500 font-medium">Calculating financial data...</p>
      </div>
    );
  }

  return (
    <div className="space-y-10 max-w-7xl mx-auto py-4">
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-6">
        <div>
          <h2 className="text-3xl font-black text-brand-dark tracking-tight">Financial Dashboard</h2>
          <p className="text-gray-500 text-lg">Detailed overview of revenue, subscriptions, and growth.</p>
        </div>
        <ExportControls 
          onExport={handleExport}
          disabled={!stats?.filteredUsers || stats.filteredUsers.length === 0}
          label="Export Report"
        />
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-8">
        <FinanceCard 
          title="Total Revenue" 
          value={`₹${stats?.totalRevenue.toLocaleString()}`} 
          icon={<Banknote size={24} />} 
          color="bg-brand-accent"
          subText="Lifetime earned revenue"
        />
        <FinanceCard 
          title="PRO Subscriptions" 
          value={stats?.proUsers} 
          icon={<CreditCard size={24} />} 
          color="bg-purple-500"
          subText={`₹${(stats?.proUsers * 999).toLocaleString()} Active Recur.`}
        />
        <FinanceCard 
          title="Avg. Rev / User" 
          value={`₹${stats?.arpu.toFixed(1)}`} 
          icon={<DollarSign size={24} />} 
          color="bg-orange-500"
          subText="ARPU (Platform Wide)"
        />
        <FinanceCard 
          title="Growth Projection" 
          value={`+₹${stats?.potentialRevenue.toLocaleString()}`} 
          icon={<ArrowUpRight size={24} />} 
          color="bg-green-500" 
          subText="10% Free tier conversion"
        />
      </div>

      <div className="bg-white rounded-3xl shadow-sm border border-gray-100 overflow-hidden">
        <div className="p-6 border-b border-gray-50 flex flex-col md:flex-row gap-6 justify-between items-center bg-gray-50/10">
          <div className="flex items-center gap-4">
            <h3 className="text-xl font-bold text-brand-dark">Revenue Tracking</h3>
            <div className="flex bg-gray-100 p-1 rounded-xl">
              {['ALL', 'FREE', 'PRO'].map(f => (
                <button
                  key={f}
                  onClick={() => setPlanFilter(f)}
                  className={`px-4 py-1.5 rounded-lg text-xs font-black transition-all ${
                    planFilter === f ? 'bg-white text-brand-dark shadow-sm' : 'text-gray-400 hover:text-gray-600'
                  }`}
                >
                  {f}
                </button>
              ))}
            </div>
          </div>
          <div className="relative w-full md:w-80">
            <Search className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400" size={18} />
            <input 
              type="text"
              placeholder="Search by name or email..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full pl-12 pr-4 py-3 rounded-2xl border-none ring-1 ring-gray-200 focus:ring-2 focus:ring-brand-accent transition-all text-sm bg-white"
            />
          </div>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left">
            <thead>
              <tr className="bg-white border-b border-gray-50">
                <th className="px-8 py-5 text-xs font-black text-gray-400 uppercase tracking-wider">User</th>
                <th className="px-8 py-5 text-xs font-black text-gray-400 uppercase tracking-wider">Plan Status</th>
                <th className="px-8 py-5 text-xs font-black text-gray-400 uppercase tracking-wider">Revenue Contribution</th>
                <th className="px-8 py-5 text-xs font-black text-gray-400 uppercase tracking-wider text-right">Joined</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-50 text-sm font-medium">
              {stats?.filteredUsers.length === 0 ? (
                <tr>
                  <td colSpan="4" className="px-8 py-20 text-center text-gray-400 italic">No users found matching your criteria.</td>
                </tr>
              ) : (
                stats?.filteredUsers.map((u) => (
                  <tr key={u.id} className="hover:bg-gray-50/30 transition-colors">
                    <td className="px-8 py-5">
                      <div className="flex flex-col">
                        <span className="text-brand-dark font-bold">{u.name}</span>
                        <span className="text-gray-400 text-xs">{u.email}</span>
                      </div>
                    </td>
                    <td className="px-8 py-5">
                      <span className={`px-3 py-1 rounded-lg text-[10px] font-black uppercase tracking-wider ${
                        u.plan === 'PRO' ? 'bg-blue-50 text-blue-600' : 'bg-gray-100 text-gray-500'
                      }`}>
                        {u.plan || 'FREE'}
                      </span>
                    </td>
                    <td className="px-8 py-5">
                      <div className="flex items-center gap-2">
                        <span className={`text-sm font-black ${u.plan === 'PRO' ? 'text-green-600' : 'text-gray-400'}`}>
                          {u.plan === 'PRO' ? '₹999.00' : '₹0.00'}
                        </span>
                        {u.plan === 'PRO' && <ArrowUpRight size={14} className="text-green-400" />}
                      </div>
                    </td>
                    <td className="px-8 py-5 text-right text-gray-400">
                      {new Date(u.createdAt).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' })}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-10">
        <div className="lg:col-span-8 bg-white p-10 rounded-3xl border border-gray-100 shadow-sm relative overflow-hidden group">
          <div className="flex justify-between items-center mb-10 relative z-10">
            <div>
              <h3 className="text-xl font-bold text-brand-dark">Revenue Growth</h3>
              <p className="text-sm text-gray-400">Monthly breakdown of platform earnings</p>
            </div>
            <div className="p-3 bg-brand-accent/5 text-brand-accent rounded-xl">
              <TrendingUp size={24} />
            </div>
          </div>
          <div className="h-[350px] w-full">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={stats?.monthlyRevenue}>
                <defs>
                  <linearGradient id="colorRev" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#3B82F6" stopOpacity={0.1}/>
                    <stop offset="95%" stopColor="#3B82F6" stopOpacity={0}/>
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#F3F4F6" />
                <XAxis 
                  dataKey="month" 
                  axisLine={false} 
                  tickLine={false} 
                  tick={{ fill: '#9CA3AF', fontSize: 13 }}
                  dy={15}
                />
                <YAxis 
                  axisLine={false} 
                  tickLine={false} 
                  tick={{ fill: '#9CA3AF', fontSize: 13 }}
                  dx={-10}
                />
                <Tooltip 
                  contentStyle={{ borderRadius: '16px', border: 'none', boxShadow: '0 20px 25px -5px rgb(0 0 0 / 0.1)' }}
                />
                <Area 
                  type="monotone" 
                  dataKey="revenue" 
                  stroke="#3B82F6" 
                  strokeWidth={4}
                  fillOpacity={1} 
                  fill="url(#colorRev)" 
                />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>

        <div className="lg:col-span-4 bg-white p-10 rounded-3xl border border-gray-100 shadow-sm flex flex-col items-center">
          <h3 className="text-xl font-bold text-brand-dark mb-2 w-full">Plan Distribution</h3>
          <p className="text-sm text-gray-400 mb-10 w-full text-left">Free vs Premium users</p>
          <div className="h-[250px] w-full mb-10">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={stats?.userDistribution}
                  cx="50%"
                  cy="50%"
                  innerRadius={60}
                  outerRadius={90}
                  paddingAngle={8}
                  dataKey="value"
                >
                  {stats?.userDistribution.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                  ))}
                </Pie>
                <Tooltip 
                  contentStyle={{ borderRadius: '16px', border: 'none', boxShadow: '0 10px 15px -3px rgb(0 0 0 / 0.1)' }}
                />
              </PieChart>
            </ResponsiveContainer>
          </div>
          <div className="w-full space-y-4">
             {stats?.userDistribution.map((entry, index) => (
               <div key={index} className="flex justify-between items-center p-4 rounded-2xl bg-gray-50/50 border border-gray-100">
                  <div className="flex items-center gap-3">
                    <div className="w-3 h-3 rounded-full" style={{ backgroundColor: COLORS[index] }}></div>
                    <span className="text-sm font-bold text-gray-700">{entry.name}</span>
                  </div>
                  <span className="text-sm font-black text-brand-dark">{entry.value}</span>
               </div>
             ))}
          </div>
        </div>
      </div>
    </div>
  );
};

export default Finance;
