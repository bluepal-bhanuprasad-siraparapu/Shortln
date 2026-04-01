import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import api from '../services/api';
import { 
  History, 
  Search, 
  User, 
  Calendar, 
  Activity,
  X,
  ShieldCheck
} from 'lucide-react';
import ExportControls from '../components/ExportControls';
import Pagination from '../components/Pagination';

const AuditLogs = () => {
  const [searchTerm, setSearchTerm] = useState('');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);

  const { data: logs, isLoading, isFetching } = useQuery({
    queryKey: ['admin', 'audit-logs', searchTerm, startDate, endDate, page, pageSize],
    queryFn: () => {
        const params = new URLSearchParams();
        if (searchTerm) params.append('query', searchTerm);
        if (startDate) params.append('startDate', `${startDate}T00:00:00`);
        if (endDate) params.append('endDate', `${endDate}T23:59:59`);
        params.append('page', page);
        params.append('size', pageSize);
        params.append('sort', 'createdAt,desc');
        
        return api.get(`/admin/audit-logs?${params.toString()}`).then(res => res.data);
    },
    placeholderData: (previousData) => previousData,
  });

  const handleExport = async (format) => {
    try {
      const params = new URLSearchParams();
      params.append('format', format);
      if (searchTerm) params.append('query', searchTerm);
      if (startDate) params.append('startDate', `${startDate}T00:00:00`);
      if (endDate) params.append('endDate', `${endDate}T23:59:59`);

      const response = await api.get(`/admin/audit-logs/export?${params.toString()}`, {
        responseType: 'blob'
      });
      
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      const extension = format.toLowerCase();
      link.setAttribute('download', `audit-log-report-${new Date().toISOString().split('T')[0]}.${extension}`);
      document.body.appendChild(link);
      link.click();
      link.remove();
    } catch (error) {
      console.error('Error exporting audit logs:', error);
    }
  };

  const getSimplifiedStatus = (action) => {
    const act = action.toUpperCase();
    if (act.includes('FAILURE')) return 'FAIL';
    if (act.includes('SUCCESS') || act.includes('CREATE') || act.includes('REGISTER')) return 'SUCCESS';
    if (act.includes('UPDATE') || act.includes('TOGGLE')) return 'UPDATE';
    if (act.includes('NOTIFICATION') || act.includes('ALERT')) return 'NOTIFICATION';
    if (act.includes('DELETE')) return 'DELETE';
    return act.split('_')[0]; // Fallback to first word
  };

  const formatAction = (action) => {
    if (!action) return '';
    return action
      .toLowerCase()
      .split('_')
      .map(word => word.charAt(0).toUpperCase() + word.slice(1))
      .join(' ')
      .replace('Service ', 'Service: ');
  };

  const getActionBadgeColor = (status) => {
    switch (status) {
      case 'SUCCESS': return 'bg-green-50 text-green-600 border-green-100';
      case 'FAIL': return 'bg-red-50 text-red-600 border-red-100';
      case 'UPDATE': return 'bg-orange-50 text-orange-600 border-orange-100';
      case 'NOTIFICATION': return 'bg-blue-50 text-blue-600 border-blue-100';
      case 'DELETE': return 'bg-rose-50 text-rose-600 border-rose-100';
      default: return 'bg-gray-100 text-gray-600 border-gray-200';
    }
  };

  const clearFilters = () => {
    setSearchTerm('');
    setStartDate('');
    setEndDate('');
  };

  return (
    <div className="space-y-8 animate-in fade-in duration-500 pb-10">
      <div className="flex flex-col lg:flex-row justify-between items-start lg:items-center gap-6">
        <div>
          <div className="flex items-center gap-3 mb-2">
            <div className="p-3 bg-brand-accent/10 rounded-2xl text-brand-accent">
              <History size={28} />
            </div>
            <h2 className="text-3xl font-black text-brand-dark tracking-tight">Audit History</h2>
          </div>
          <p className="text-gray-500 font-medium">Complete administrative log of all critical platform events and security actions.</p>
        </div>
        
        <div className="flex items-center gap-3">
          {isFetching && (
            <div className="flex items-center gap-2 bg-blue-50 px-4 py-2 rounded-xl border border-blue-100 animate-pulse text-blue-600">
              <Activity size={16} className="animate-spin" />
              <span className="text-[10px] font-black uppercase tracking-widest">Live Syncing</span>
            </div>
          )}
          <ExportControls 
            onExport={handleExport}
            disabled={!logs || logs.totalElements === 0}
            label="Export Audit Logs"
          />
        </div>
      </div>

      <div className="bg-white p-5 px-6 rounded-3xl border border-gray-100 shadow-sm">
        <div className="flex flex-col xl:flex-row items-center gap-10">
          <div className="flex items-center gap-4 flex-1 min-w-[300px]">
            <span className="text-[10px] font-black text-gray-400 uppercase tracking-widest whitespace-nowrap opacity-70">Search</span>
            <div className="relative flex-1 group">
              <Search className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400 group-hover:text-brand-accent transition-colors" size={16} />
              <input 
                type="text"
                placeholder="Name, Email, or Detail..."
                value={searchTerm}
                onChange={(e) => {
                  setSearchTerm(e.target.value);
                  setPage(0);
                }}
                className="w-full pl-12 pr-4 py-3.5 bg-gray-50/20 border border-gray-100 rounded-2xl focus:ring-2 focus:ring-brand-accent/20 focus:border-brand-accent transition-all text-sm font-bold outline-none"
              />
            </div>
          </div>

          <div className="hidden xl:block w-px h-10 bg-gray-100"></div>

          <div className="flex items-center gap-4 flex-1">
            <span className="text-[10px] font-black text-gray-400 uppercase tracking-widest whitespace-nowrap opacity-70">Duration</span>
            <div className="flex items-center gap-4 bg-gray-50/20 px-6 py-2.5 rounded-2xl border border-gray-100 hover:bg-white transition-colors flex-1 group/input">
              <div className="relative flex-1 group">
                <Calendar className="absolute left-0 top-1/2 -translate-y-1/2 text-gray-400 group-hover:text-brand-accent transition-colors" size={14} />
                <input 
                  type="date"
                  value={startDate}
                  onChange={(e) => setStartDate(e.target.value)}
                  className="w-full pl-7 pr-2 py-1 bg-transparent rounded-lg text-[10px] font-black border-none focus:ring-0 outline-none cursor-pointer text-gray-700"
                />
              </div>
              <div className="text-gray-200 font-black text-sm px-2 select-none">→</div>
              <div className="relative flex-1 group">
                <Calendar className="absolute left-0 top-1/2 -translate-y-1/2 text-gray-400 group-hover:text-brand-accent transition-colors" size={14} />
                <input 
                  type="date"
                  value={endDate}
                  onChange={(e) => setEndDate(e.target.value)}
                  className="w-full pl-7 pr-2 py-1 bg-transparent rounded-lg text-[10px] font-black border-none focus:ring-0 outline-none cursor-pointer text-gray-700"
                />
              </div>
            </div>
          </div>

          {(searchTerm || startDate || endDate) && (
            <button 
              onClick={clearFilters}
              className="flex items-center gap-2 px-5 py-3 text-red-500 hover:bg-red-50 rounded-2xl transition-all font-black text-[10px] uppercase tracking-widest group"
            >
              <X size={14} className="group-hover:rotate-90 transition-transform" />
              Reset
            </button>
          )}
        </div>
      </div>

      <div className="bg-white rounded-3xl shadow-sm border border-gray-100 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left">
            <thead>
              <tr className="bg-gray-50/50 border-b border-gray-100">
                <th className="px-8 py-5 text-[10px] font-black text-gray-400 uppercase tracking-widest">Event & Status</th>
                <th className="px-8 py-5 text-[10px] font-black text-gray-400 uppercase tracking-widest">Details</th>
                <th className="px-8 py-5 text-[10px] font-black text-gray-400 uppercase tracking-widest">User / Identity</th>
                <th className="px-8 py-5 text-[10px] font-black text-gray-400 uppercase tracking-widest">Timestamp</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-50">
              {isLoading ? (
                <tr>
                  <td colSpan="4" className="py-24 text-center">
                    <div className="flex flex-col items-center gap-4">
                      <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-brand-accent"></div>
                      <span className="text-gray-400 font-bold tracking-tight">Accessing Audit Vault...</span>
                    </div>
                  </td>
                </tr>
              ) : logs?.content?.length === 0 ? (
                <tr>
                  <td colSpan="4" className="py-24 text-center">
                    <div className="flex flex-col items-center gap-4 text-gray-300">
                      <ShieldCheck size={64} className="opacity-20" />
                      <div className="space-y-1">
                        <p className="text-gray-500 font-bold text-lg">No logs match your filter</p>
                        <p className="text-gray-400 text-sm">Every action is recorded, try broading your search.</p>
                      </div>
                    </div>
                  </td>
                </tr>
              ) : (
                logs?.content?.map((log) => {
                  const simplifiedStatus = getSimplifiedStatus(log.action);
                  return (
                    <tr key={log.id} className="hover:bg-gray-50/50 transition-colors group">
                      <td className="px-8 py-6">
                        <div className="flex flex-col gap-2">
                          <span className={`inline-flex px-3 py-1 rounded-lg text-[9px] font-black uppercase tracking-widest border ${getActionBadgeColor(simplifiedStatus)}`}>
                            {simplifiedStatus}
                          </span>
                          {log.action.startsWith('SERVICE_') && (
                            <span className="text-[8px] font-bold text-gray-400 flex items-center gap-1">
                               <Activity size={10} /> Auto-logged by Aspect
                            </span>
                          )}
                        </div>
                      </td>
                      <td className="px-8 py-6">
                        <div className="max-w-md">
                          <div className="mb-1">
                            <span className="text-[10px] font-black text-brand-accent uppercase tracking-widest bg-brand-accent/5 px-2 py-0.5 rounded">
                              {formatAction(log.action)}
                            </span>
                          </div>
                          <p className="text-sm font-bold text-gray-700 leading-snug group-hover:text-brand-dark transition-colors">
                            {log.details}
                          </p>
                        </div>
                      </td>
                      <td className="px-8 py-6">
                        <div className="flex flex-col gap-1">
                          <div className="flex items-center gap-2">
                            <div className="p-1.5 bg-gray-100 rounded-lg text-gray-500">
                              <User size={12} />
                            </div>
                            <div className="flex flex-col">
                              <span className="text-xs font-black text-brand-dark">
                                {log.userName || log.email || 'SYSTEM'}
                              </span>
                              {log.email && (
                                <span className="text-[10px] font-bold text-gray-400 italic">
                                  {log.email}
                                </span>
                              )}
                            </div>
                          </div>
                        </div>
                      </td>
                      <td className="px-8 py-6">
                        <div className="flex flex-col">
                          <span className="text-xs font-bold text-gray-900">
                            {new Date(log.createdAt).toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' })}
                          </span>
                          <span className="text-[10px] font-bold text-gray-400 uppercase tracking-tighter">
                            {new Date(log.createdAt).toLocaleTimeString()}
                          </span>
                        </div>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
        {logs && (
          <Pagination 
            currentPage={page}
            totalPages={logs.totalPages}
            totalElements={logs.totalElements}
            pageSize={pageSize}
            onPageChange={setPage}
            onPageSizeChange={(size) => {
              setPageSize(size);
              setPage(0);
            }}
          />
        )}
      </div>
    </div>
  );
};

export default AuditLogs;
