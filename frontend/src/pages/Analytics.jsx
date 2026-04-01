import React, { useState } from 'react';
import { useParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import api from '../services/api';
import { 
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
  PieChart, Pie, Cell, Legend
} from 'recharts';
import { 
  MousePointer2, 
  Users, 
  Globe, 
  Smartphone,
  ChevronLeft,
  Download,
  Search,
  Calendar,
  AlertCircle
} from 'lucide-react';
import { format } from 'date-fns';
import html2canvas from 'html2canvas';
import jsPDF from 'jspdf';
import autoTable from 'jspdf-autotable';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Lock } from 'lucide-react';
import ExportControls from '../components/ExportControls';
import { downloadCSV } from '../utils/exportUtils';
import Pagination from '../components/Pagination';

const COLORS = ['#3d5afe', '#8e24aa', '#00bfa5', '#ffab00', '#ff5252'];

const Analytics = () => {
  const { id } = useParams();
  const { user } = useAuth();
  const navigate = useNavigate();

  const [searchTerm, setSearchTerm] = useState('');
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');

  const isPro = user?.plan === 'PRO' || user?.role === 'ADMIN';

  const { data: overall, isLoading: loadingOverall } = useQuery({
    queryKey: ['analytics', 'overall', id],
    queryFn: () => api.get(`/analytics/link/${id}/overall`).then(res => res.data)
  });

  const { data: geo, isLoading: loadingGeo } = useQuery({
    queryKey: ['analytics', 'geo', id],
    queryFn: () => api.get(`/analytics/link/${id}/geo`).then(res => res.data)
  });

  const { data: devices, isLoading: loadingDevices } = useQuery({
    queryKey: ['analytics', 'devices', id],
    queryFn: () => api.get(`/analytics/link/${id}/devices`).then(res => res.data)
  });

  const { data: eventsData, isLoading: loadingEvents } = useQuery({
    queryKey: ['analytics', 'events', id, searchTerm, page, pageSize],
    queryFn: () => api.get(`/analytics/link/${id}/events`, {
      params: { 
        query: searchTerm,
        page,
        size: pageSize,
        sort: 'clickedAt,desc'
      }
    }).then(res => res.data),
    enabled: isPro
  });

  const events = eventsData?.content || [];

  const geoData = geo ? Object.entries(geo).map(([name, value]) => ({ name, value })) : [];
  const deviceData = devices ? Object.entries(devices).map(([name, value]) => ({ name, value })) : [];

  const filteredEvents = events; // Now filtered on server for search term

  const handleSearch = (e) => {
    setSearchTerm(e.target.value);
    setPage(0);
  };

  const handleExportPdf = async () => {
    try {
      toast.loading("Generating visual report...", { id: 'report' });
      const doc = new jsPDF('p', 'mm', 'a4');
      const pageWidth = doc.internal.pageSize.getWidth();
      
      // Header
      doc.setFillColor(61, 90, 254);
      doc.rect(0, 0, pageWidth, 40, 'F');
      doc.setTextColor(255, 255, 255);
      doc.setFontSize(22);
      doc.text("Link Analytics Report", 20, 25);
      doc.setFontSize(10);
      doc.text(`Generated on ${format(new Date(), 'PPP p')}`, 20, 32);

      // Link Info
      doc.setTextColor(50, 50, 50);
      doc.setFontSize(14);
      doc.text("Performance Overview", 20, 55);
      doc.setFontSize(10);
      doc.text(`Link ID: ${id}`, 20, 62);
      doc.text(`Total Clicks: ${overall?.totalClicks || 0}`, 20, 68);
      doc.text(`Unique Visitors: ${overall?.uniqueClicks || 0}`, 20, 74);

      let currentY = 85;

      // Capture Charts
      const charts = document.querySelectorAll('.recharts-responsive-container');
      if (charts.length > 0) {
        for (let i = 0; i < charts.length; i++) {
          const canvas = await html2canvas(charts[i], { scale: 2 });
          const imgData = canvas.toDataURL('image/png');
          const imgWidth = 85;
          const imgHeight = (canvas.height * imgWidth) / canvas.width;
          
          if (i % 2 === 0 && i > 0) currentY += imgHeight + 10;
          const xPos = (i % 2 === 0) ? 20 : 105;
          
          doc.addImage(imgData, 'PNG', xPos, currentY, imgWidth, imgHeight);
          if (i === charts.length - 1) currentY += imgHeight + 20;
        }
      }

      // Detailed Table
      if (filteredEvents && filteredEvents.length > 0) {
        doc.addPage();
        doc.setFontSize(14);
        doc.text("Detailed Click History", 20, 20);
        
        const tableHeaders = [["Timestamp", "Country", "City", "Device", "Browser"]];
        const tableRows = filteredEvents.slice(0, 50).map(e => [
          format(new Date(e.clickedAt), 'MMM dd, HH:mm ss'),
          e.country || '-',
          e.city || '-',
          e.device || '-',
          e.browser || '-'
        ]);

        autoTable(doc, {
          startY: 30,
          head: tableHeaders,
          body: tableRows,
          theme: 'striped',
          headStyles: { fillColor: [61, 90, 254] },
          styles: { fontSize: 8 }
        });
      }

      doc.save(`analytics-report-${id}.pdf`);
      toast.success("Report downloaded!", { id: 'report' });
    } catch (err) {
      console.error('Failed to generate visual PDF', err);
      toast.error("Failed to generate visual report.", { id: 'report' });
    }
  };

  const handleExportCsv = () => {
    const headers = ["Metric", "Value"];
    const rows = [
      ["Total Clicks", overall?.totalClicks || 0],
      ["Unique Clicks", overall?.uniqueClicks || 0],
      ["--- GEOGRAPHIC ---", ""],
      ...geoData.map(d => [d.name, d.value]),
      ["--- DEVICES ---", ""],
      ...deviceData.map(d => [d.name, d.value])
    ];

    downloadCSV(rows, headers, `link-analytics-${id}-${new Date().getTime()}`);
  };

  const handleExport = (format) => {
    if (format === 'PDF') {
      handleExportPdf();
    } else {
      handleExportCsv();
    }
  };

  if (loadingOverall || loadingGeo || loadingDevices) {
    return <div className="text-center py-20 text-gray-400">Loading analytics...</div>;
  }

  if (!isPro) {
    return (
      <div className="max-w-4xl mx-auto py-20 px-4">
        <div className="bg-white rounded-3xl p-12 text-center shadow-xl border border-gray-100 flex flex-col items-center">
          <div className="w-20 h-20 bg-indigo-50 text-indigo-600 rounded-2xl flex items-center justify-center mb-8">
            <Lock size={40} />
          </div>
          <h2 className="text-3xl font-bold text-brand-dark mb-4">Premium Feature</h2>
          <p className="text-gray-500 text-lg mb-8 max-w-md">
            Detailed analytics are only available for members of our Pro plan. 
            Upgrade today to see geographic data, device breakdowns, and more!
          </p>
          <div className="flex gap-4">
            <button 
              onClick={() => navigate('/pricing')}
              className="bg-brand-accent text-white px-8 py-3 rounded-xl font-bold shadow-lg shadow-brand-accent/20 hover:bg-blue-700 transition-all"
            >
              Explore Pro Plans
            </button>
            <button 
              onClick={() => navigate('/links')}
              className="bg-white text-gray-700 px-8 py-3 rounded-xl font-bold border border-gray-200 hover:bg-gray-50 transition-all"
            >
              Back to My Links
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-8 max-w-6xl mx-auto">
      <div className="flex items-center gap-4">
        <Link to="/links" className="p-2 hover:bg-white rounded-lg transition-all text-gray-400 hover:text-brand-dark">
          <ChevronLeft size={24} />
        </Link>
        <div className="flex-1">
          <h2 className="text-2xl font-bold text-brand-dark">Link Analytics</h2>
          <p className="text-gray-500">Detailed performance insights for your short URL.</p>
        </div>
        <ExportControls 
          onExport={handleExport}
          label="Export Report"
        />
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100 flex items-center gap-6">
          <div className="p-4 bg-brand-accent/10 text-brand-accent rounded-xl">
            <MousePointer2 size={24} />
          </div>
          <div>
            <p className="text-sm font-bold text-gray-400 uppercase tracking-wider">Total Clicks</p>
            <p className="text-3xl font-black text-brand-dark">{overall?.totalClicks || 0}</p>
          </div>
        </div>
        <div className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100 flex items-center gap-6">
          <div className="p-4 bg-purple-100 text-purple-600 rounded-xl">
            <Users size={24} />
          </div>
          <div>
            <p className="text-sm font-bold text-gray-400 uppercase tracking-wider">Unique Clicks</p>
            <p className="text-3xl font-black text-brand-dark">{overall?.uniqueClicks || 0}</p>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        <div className="bg-white p-8 rounded-2xl shadow-sm border border-gray-100">
          <div className="flex items-center gap-3 mb-8">
             <div className="p-2 bg-green-50 text-green-600 rounded-lg">
               <Globe size={18} />
             </div>
             <h3 className="text-lg font-bold text-brand-dark">Geographic Distribution</h3>
          </div>
          <div className="h-[300px] w-full">
            {geoData.length > 0 ? (
              <ResponsiveContainer width="100%" height="100%" minWidth={0}>
                <BarChart data={geoData}>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f1f5f9" />
                  <XAxis 
                    dataKey="name" 
                    axisLine={false} 
                    tickLine={false} 
                    tick={{ fill: '#94a3b8', fontSize: 12 }} 
                  />
                  <YAxis 
                    axisLine={false} 
                    tickLine={false} 
                    tick={{ fill: '#94a3b8', fontSize: 12 }} 
                  />
                  <Tooltip 
                    cursor={{ fill: '#f8fafc' }}
                    contentStyle={{ borderRadius: '12px', border: 'none', boxShadow: '0 10px 15px -3px rgb(0 0 0 / 0.1)' }}
                  />
                  <Bar dataKey="value" fill="#3d5afe" radius={[4, 4, 0, 0]} barSize={40} />
                </BarChart>
              </ResponsiveContainer>
            ) : (
              <div className="h-full flex flex-col items-center justify-center text-gray-400">
                <Globe size={48} className="mb-4 opacity-10" />
                <p>No geographic data available yet.</p>
              </div>
            )}
          </div>
        </div>

        <div className="bg-white p-8 rounded-2xl shadow-sm border border-gray-100">
          <div className="flex items-center gap-3 mb-8">
             <div className="p-2 bg-orange-50 text-orange-600 rounded-lg">
               <Smartphone size={18} />
             </div>
             <h3 className="text-lg font-bold text-brand-dark">Device Types</h3>
          </div>
          <div className="h-[300px] w-full">
            {deviceData.length > 0 ? (
              <ResponsiveContainer width="100%" height="100%" minWidth={0}>
                <PieChart>
                  <Pie
                    data={deviceData}
                    cx="50%"
                    cy="50%"
                    innerRadius={60}
                    outerRadius={80}
                    paddingAngle={8}
                    dataKey="value"
                  >
                    {deviceData.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                    ))}
                  </Pie>
                  <Tooltip 
                    contentStyle={{ borderRadius: '12px', border: 'none', boxShadow: '0 10px 15px -3px rgb(0 0 0 / 0.1)' }}
                  />
                  <Legend verticalAlign="bottom" height={36} />
                </PieChart>
              </ResponsiveContainer>
            ) : (
              <div className="h-full flex flex-col items-center justify-center text-gray-400">
                <Smartphone size={48} className="mb-4 opacity-10" />
                <p>No device data available yet.</p>
              </div>
            )}
          </div>
        </div>
      </div>

      <div className="bg-white rounded-3xl shadow-sm border border-gray-100 overflow-hidden">
        <div className="p-6 border-b border-gray-50 flex flex-col md:flex-row gap-6 justify-between items-center bg-gray-50/30">
          <div className="flex flex-col md:flex-row gap-4 w-full md:w-auto">
            <div className="relative w-full md:w-64">
              <Search className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400" size={16} />
              <input 
                type="text"
                placeholder="Search logs..."
                value={searchTerm}
                onChange={handleSearch}
                className="w-full pl-10 pr-4 py-2.5 rounded-xl border-none ring-1 ring-gray-200 focus:ring-2 focus:ring-brand-accent transition-all text-sm bg-white"
              />
            </div>
            
            <div className="flex items-center gap-2">
              <div className="relative">
                <Calendar className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={12} />
                <input 
                  type="date"
                  value={startDate}
                  onChange={(e) => setStartDate(e.target.value)}
                  className="pl-8 pr-2 py-2 rounded-lg border-none ring-1 ring-gray-200 focus:ring-2 focus:ring-brand-accent text-[10px] font-bold bg-white"
                />
              </div>
              <span className="text-gray-400 text-xs">to</span>
              <div className="relative">
                <Calendar className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={12} />
                <input 
                  type="date"
                  value={endDate}
                  onChange={(e) => setEndDate(e.target.value)}
                  className="pl-8 pr-2 py-2 rounded-lg border-none ring-1 ring-gray-200 focus:ring-2 focus:ring-brand-accent text-[10px] font-bold bg-white"
                />
              </div>
            </div>
          </div>
          <div className="text-sm font-bold text-gray-400">
            {eventsData?.totalElements || 0} total clicks found
          </div>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left">
            <thead>
              <tr className="bg-white border-b border-gray-50">
                <th className="px-6 py-4 text-[10px] font-black text-gray-400 uppercase tracking-wider">Timestamp</th>
                <th className="px-6 py-4 text-[10px] font-black text-gray-400 uppercase tracking-wider">Location</th>
                <th className="px-6 py-4 text-[10px] font-black text-gray-400 uppercase tracking-wider">Device</th>
                <th className="px-6 py-4 text-[10px] font-black text-gray-400 uppercase tracking-wider">Browser</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-50">
              {loadingEvents ? (
                Array.from({ length: 3 }).map((_, i) => (
                  <tr key={i} className="animate-pulse">
                    <td colSpan="4" className="px-6 py-4"><div className="h-4 bg-gray-50 rounded w-full"></div></td>
                  </tr>
                ))
              ) : filteredEvents?.length === 0 ? (
                <tr>
                  <td colSpan="4" className="px-6 py-12 text-center text-gray-400 text-sm">
                    No matching activity found.
                  </td>
                </tr>
              ) : (
                filteredEvents?.map((event) => (
                  <tr key={event.id} className="hover:bg-gray-50/50 transition-colors">
                    <td className="px-6 py-4">
                      <div className="flex flex-col">
                        <span className="text-sm font-bold text-gray-700">{format(new Date(event.clickedAt), 'HH:mm:ss')}</span>
                        <span className="text-[10px] text-gray-400">{format(new Date(event.clickedAt), 'MMM dd, yyyy')}</span>
                      </div>
                    </td>
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-2 text-sm text-gray-600 font-medium">
                        <Globe size={14} className="text-gray-300" />
                        {event.city ? `${event.city}, ` : ''}{event.country || 'Unknown'}
                      </div>
                    </td>
                    <td className="px-6 py-4 text-sm text-gray-600">
                      <div className="flex items-center gap-2">
                        <Smartphone size={14} className="text-gray-300" />
                        {event.device || 'Unknown'}
                      </div>
                    </td>
                    <td className="px-6 py-4 text-sm text-gray-600 font-bold">
                      {event.browser || 'Unknown'}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
        
        {eventsData?.totalPages > 1 && (
          <div className="p-6 bg-gray-50/30 border-t border-gray-50">
            <Pagination 
              currentPage={page}
              totalPages={eventsData.totalPages}
              onPageChange={setPage}
              pageSize={pageSize}
              onPageSizeChange={(newSize) => {
                setPageSize(newSize);
                setPage(0);
              }}
              totalElements={eventsData.totalElements}
            />
          </div>
        )}
      </div>
    </div>
  );
};

export default Analytics;
