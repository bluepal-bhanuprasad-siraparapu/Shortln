import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import api from '../services/api';
import {
  Bell,
  Search,
  Download,
  Filter,
  User,
  Calendar,
  Info,
  AlertTriangle,
  X,
  UserCheck,
  Zap
} from 'lucide-react';

import ExportControls from '../components/ExportControls';
import Pagination from '../components/Pagination';

const AdminNotifications = () => {
  const [typeFilter, setTypeFilter] = useState('ALL');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);

  const { data: notifications, isLoading, isFetching } = useQuery({
    queryKey: ['admin', 'notifications', typeFilter, startDate, endDate, page, pageSize],
    queryFn: () => {
      const params = new URLSearchParams();
      if (typeFilter !== 'ALL') params.append('type', typeFilter);
      if (startDate) params.append('startDate', `${startDate}T00:00:00`);
      if (endDate) params.append('endDate', `${endDate}T23:59:59`);
      params.append('page', page);
      params.append('size', pageSize);
      params.append('sort', 'createdAt,desc');
      
      return api.get(`/notifications/admin?${params.toString()}`).then(res => res.data);
    },
    placeholderData: (previousData) => previousData,
  });

  const handleExport = async (format) => {
    try {
      const params = new URLSearchParams();
      params.append('format', format);
      if (typeFilter !== 'ALL') params.append('type', typeFilter);
      if (startDate) params.append('startDate', `${startDate}T00:00:00`);
      if (endDate) params.append('endDate', `${endDate}T23:59:59`);

      const response = await api.get(`/notifications/admin/export?${params.toString()}`, {
        responseType: 'blob'
      });
      
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      const extension = format.toLowerCase();
      link.setAttribute('download', `system-alerts-report-${new Date().toISOString().split('T')[0]}.${extension}`);
      document.body.appendChild(link);
      link.click();
      link.remove();
    } catch (error) {
      console.error('Error exporting report:', error);
    }
  };

  const clearFilters = () => {
    setTypeFilter('ALL');
    setStartDate('');
    setEndDate('');
  };

  const getNotificationIcon = (type) => {
    switch (type) {
      case 'SYSTEM': return <UserCheck className="text-blue-500" size={18} />;
      case 'SUBSCRIPTION': return <Zap className="text-purple-500" size={18} />;
      case 'EXPIRY': return <AlertTriangle className="text-orange-500" size={18} />;
      default: return <Bell className="text-gray-400" size={18} />;
    }
  };

  return (
    <div className="space-y-8 animate-in fade-in duration-500">
      <div className="flex flex-col lg:flex-row justify-between items-start lg:items-center gap-6">
        <div>
          <div className="flex items-center gap-3 mb-2">
            <div className="p-2 bg-brand-accent/10 rounded-lg text-brand-accent">
              <Bell size={24} />
            </div>
            <h2 className="text-3xl font-black text-brand-dark tracking-tight">System Alerts</h2>
          </div>
          <p className="text-gray-500 font-medium">Monitor all automated events, user registrations, and plan upgrades.</p>
        </div>
        
        <div className="flex items-center gap-3">
          {isFetching && (
            <div className="flex items-center gap-2 bg-gray-50 px-3 py-2 rounded-lg border border-gray-100 animate-pulse">
              <div className="w-1.5 h-1.5 bg-brand-accent rounded-full animate-ping"></div>
              <span className="text-[10px] font-black text-brand-dark uppercase tracking-widest">Updating...</span>
            </div>
          )}
          <ExportControls 
            onExport={handleExport}
            disabled={!notifications || notifications.totalElements === 0}
            label="Export Report"
          />
        </div>
      </div>

      <div className="bg-white p-5 px-6 rounded-3xl border border-gray-100 shadow-sm">
        <div className="flex flex-col xl:flex-row items-center gap-10">
          {/* Category Filter */}
          <div className="flex items-center gap-4 min-w-[320px]">
            <span className="text-[10px] font-black text-gray-400 uppercase tracking-widest whitespace-nowrap opacity-70">Category</span>
            <div className="relative flex-1 group">
              <Filter className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400 group-hover:text-brand-accent transition-colors" size={16} />
              <select
                value={typeFilter}
                onChange={(e) => {
                  setTypeFilter(e.target.value);
                  setPage(0);
                }}
                className="w-full pl-12 pr-10 py-3.5 bg-gray-50/20 border border-gray-100 rounded-2xl focus:ring-2 focus:ring-brand-accent/20 focus:border-brand-accent transition-all text-sm font-bold appearance-none outline-none cursor-pointer hover:bg-white"
              >
                <option value="ALL">All System Events</option>
                <option value="SYSTEM">User Registrations</option>
                <option value="SUBSCRIPTION">Financial Upgrades</option>
                <option value="EXPIRY">Link Expirations</option>
              </select>
              <div className="absolute right-4 top-1/2 -translate-y-1/2 pointer-events-none text-gray-400">
                <Filter size={12} />
              </div>
            </div>
          </div>

          <div className="hidden xl:block w-px h-10 bg-gray-100"></div>

          {/* Date Range */}
          <div className="flex items-center gap-4 flex-1">
            <span className="text-[10px] font-black text-gray-400 uppercase tracking-widest whitespace-nowrap opacity-70">Duration</span>
            <div className="flex items-center gap-4 bg-gray-50/20 px-6 py-2.5 rounded-2xl border border-gray-100 hover:bg-white transition-colors flex-1 max-w-2xl group/input">
              <div className="relative flex-1 group">
                <Calendar className="absolute left-0 top-1/2 -translate-y-1/2 text-gray-400 group-hover:text-brand-accent transition-colors" size={14} />
                <input 
                  type="date"
                  value={startDate}
                  onChange={(e) => setStartDate(e.target.value)}
                  className="w-full pl-7 pr-2 py-1 bg-transparent rounded-lg text-[10px] font-black border-none focus:ring-0 outline-none cursor-pointer text-gray-700"
                />
              </div>
              <div className="text-gray-200 font-black text-sm px-2 animate-pulse line-clamp-1 select-none">→</div>
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

          {/* Reset Control */}
          <div className="flex items-center gap-6">
            {(typeFilter !== 'ALL' || startDate || endDate) && (
              <button 
                onClick={clearFilters}
                className="flex items-center gap-2 px-5 py-3 text-red-500 hover:bg-red-50 rounded-2xl transition-all font-black text-[10px] uppercase tracking-widest group"
              >
                <X size={14} className="group-hover:rotate-90 transition-transform" />
                Reset
              </button>
            )}
            <div className="flex flex-col items-end">
              <div className="flex items-center gap-2 px-4 py-2 bg-brand-dark/5 rounded-xl border border-brand-dark/10">
                <span className="text-xs font-black text-brand-dark tabular-nums">
                  {notifications?.totalElements || 0}
                </span>
                <span className="text-[10px] font-bold text-gray-400 uppercase tracking-tighter">Alerts</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div className="bg-white rounded-3xl shadow-sm border border-gray-100 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left">
            <thead>
              <tr className="bg-gray-50/50 border-b border-gray-100">
                <th className="px-8 py-5 text-xs font-black text-gray-400 uppercase tracking-widest">Event Type</th>
                <th className="px-8 py-5 text-xs font-black text-gray-400 uppercase tracking-widest">Details</th>
                <th className="px-8 py-5 text-xs font-black text-gray-400 uppercase tracking-widest">User ID</th>
                <th className="px-8 py-5 text-xs font-black text-gray-400 uppercase tracking-widest">Timestamp</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-50">
              {isLoading ? (
                <tr>
                  <td colSpan="4" className="py-20 text-center">
                    <div className="flex flex-col items-center gap-3">
                      <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-brand-accent"></div>
                      <span className="text-gray-400 font-bold">Synchronizing history...</span>
                    </div>
                  </td>
                </tr>
              ) : (notifications?.content?.length === 0 || notifications?.length === 0) ? (
                <tr>
                  <td colSpan="4" className="py-20 text-center">
                    <div className="flex flex-col items-center gap-3 text-gray-300">
                      <Info size={48} />
                      <span className="text-gray-500 font-bold text-lg">No alerts found</span>
                      <p className="text-gray-400 text-sm">Try adjusting your search criteria.</p>
                    </div>
                  </td>
                </tr>
              ) : (
                notifications?.content?.map((notification) => (
                  <tr key={notification.id} className="hover:bg-gray-50/50 transition-colors group">
                    <td className="px-8 py-6">
                      <div className="flex items-center gap-3">
                        <div className="p-2.5 bg-gray-100 rounded-xl group-hover:scale-110 transition-transform">
                          {getNotificationIcon(notification.type)}
                        </div>
                        <span className="text-xs font-black text-brand-dark uppercase tracking-wider">
                          {notification.type}
                        </span>
                      </div>
                    </td>
                    <td className="px-8 py-6">
                      <p className="text-sm font-bold text-gray-700 leading-tight max-w-md">
                        {notification.message}
                      </p>
                    </td>
                    <td className="px-8 py-6">
                      <div className="inline-flex items-center gap-2 px-3 py-1.5 bg-brand-accent/5 text-brand-accent rounded-lg border border-brand-accent/10">
                        <User size={12} className="font-bold" />
                        <span className="text-xs font-black">ID: {notification.userId}</span>
                      </div>
                    </td>
                    <td className="px-8 py-6">
                      <div className="flex flex-col">
                        <span className="text-xs font-bold text-gray-900">
                          {new Date(notification.createdAt).toLocaleDateString()}
                        </span>
                        <span className="text-[10px] font-bold text-gray-400 uppercase">
                          {new Date(notification.createdAt).toLocaleTimeString()}
                        </span>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
        {notifications && (
          <Pagination 
            currentPage={page}
            totalPages={notifications.totalPages}
            totalElements={notifications.totalElements}
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

export default AdminNotifications;
