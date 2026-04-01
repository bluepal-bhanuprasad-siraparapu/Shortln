import React, { useState, useEffect } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import api from '../services/api';
import {
  Activity,
  ExternalLink,
  Globe,
  Monitor,
  Clock,
  RefreshCw,
  Search,
  Filter,
  Download,
  AlertCircle,
  Calendar
} from 'lucide-react';
import { format } from 'date-fns';
import ExportControls from '../components/ExportControls';
import Pagination from '../components/Pagination';
import { downloadCSV, downloadPDF } from '../utils/exportUtils';

const Reports = () => {
  const [searchTerm, setSearchTerm] = useState('');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [isAutoRefresh, setIsAutoRefresh] = useState(true);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const navigate = useNavigate();

  const { data: events, isLoading, error, refetch } = useQuery({
    queryKey: ['admin', 'latest-events', searchTerm, page, pageSize],
    queryFn: () => {
        const params = new URLSearchParams();
        if (searchTerm) params.append('query', searchTerm);
        params.append('page', page);
        params.append('size', pageSize);
        params.append('sort', 'clickedAt,desc');
        return api.get('/analytics/admin/latest?' + params.toString()).then(res => res.data);
    },
    refetchInterval: isAutoRefresh ? 5000 : false,
    placeholderData: (previousData) => previousData
  });

  const filteredEvents = events?.content || [];

  const handleExport = (exportFormat) => {
    if (!filteredEvents || filteredEvents.length === 0) return;
    const headers = ['ID', 'Link ID', 'Timestamp', 'Country', 'City', 'Device', 'Browser', 'Referrer'];
    const rows = filteredEvents.map(e => [
      e.id,
      e.shortLinkId,
      format(new Date(e.clickedAt), 'yyyy-MM-dd HH:mm:ss'),
      e.country || '-',
      e.city || '-',
      e.device || '-',
      e.browser || '-',
      e.referrer || '-'
    ]);

    let filename = `live-reports-${new Date().getTime()}`;
    if (startDate || endDate) {
      filename = `live-reports-${startDate || 'start'}-to-${endDate || 'end'}`;
    }

    if (exportFormat === 'PDF') {
      downloadPDF('Real-time Click Reports', headers, rows, filename);
    } else {
      downloadCSV(rows, headers, filename);
    }
  };

  return (
    <div className="space-y-8 max-w-7xl mx-auto py-4">
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-6">
        <div>
          <h2 className="text-3xl font-black text-brand-dark tracking-tight flex items-center gap-3">
            Real-time Reports
            {isAutoRefresh && (
              <span className="flex h-3 w-3 relative">
                <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-green-400 opacity-75"></span>
                <span className="relative inline-flex rounded-full h-3 w-3 bg-green-500"></span>
              </span>
            )}
          </h2>
          <p className="text-gray-500 text-lg">Monitoring live platform activity and click events.</p>
        </div>
        <div className="flex items-center gap-4">
          <button
            onClick={() => setIsAutoRefresh(!isAutoRefresh)}
            className={`flex items-center gap-2 px-4 py-2 rounded-xl font-bold transition-all ${isAutoRefresh
                ? 'bg-green-50 text-green-600 border border-green-100'
                : 'bg-gray-50 text-gray-500 border border-gray-100'
              }`}
          >
            <RefreshCw size={18} className={isAutoRefresh ? 'animate-spin-slow' : ''} />
            {isAutoRefresh ? 'Auto-Refresh ON' : 'Auto-Refresh OFF'}
          </button>
          <ExportControls
            onExport={handleExport}
            disabled={!events || events.totalElements === 0}
            label="Export Report"
          />
        </div>
      </div>

      <div className="bg-white rounded-3xl shadow-sm border border-gray-100 overflow-hidden">
        <div className="p-6 border-b border-gray-50 flex flex-col xl:flex-row gap-6 justify-between items-start xl:items-center bg-gray-50/30">
          <div className="flex flex-col md:flex-row gap-4 w-full xl:w-auto">
            <div className="relative w-full md:w-80">
              <Search className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400" size={18} />
              <input
                type="text"
                placeholder="Search ID, location, device..."
                value={searchTerm}
                onChange={(e) => {
                  setSearchTerm(e.target.value);
                  setPage(0);
                }}
                className="w-full pl-12 pr-4 py-3 rounded-2xl border-none ring-1 ring-gray-200 focus:ring-2 focus:ring-brand-accent transition-all text-sm bg-white"
              />
            </div>

            <div className="flex items-center gap-2 w-full md:w-auto">
              <div className="relative flex-1 md:w-40">
                <Calendar className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={14} />
                <input
                  type="date"
                  value={startDate}
                  onChange={(e) => setStartDate(e.target.value)}
                  className="w-full pl-9 pr-3 py-2.5 rounded-xl border-none ring-1 ring-gray-200 focus:ring-2 focus:ring-brand-accent transition-all text-xs bg-white uppercase font-bold"
                  title="Start Date"
                />
              </div>
              <span className="text-gray-400 font-bold text-xs">to</span>
              <div className="relative flex-1 md:w-40">
                <Calendar className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={14} />
                <input
                  type="date"
                  value={endDate}
                  onChange={(e) => setEndDate(e.target.value)}
                  className="w-full pl-9 pr-3 py-2.5 rounded-xl border-none ring-1 ring-gray-200 focus:ring-2 focus:ring-brand-accent transition-all text-xs bg-white uppercase font-bold"
                  title="End Date"
                />
              </div>
            </div>

            {(searchTerm || startDate || endDate) && (
              <button
                onClick={() => { setSearchTerm(''); setStartDate(''); setEndDate(''); setPage(0); }}
                className="px-4 py-2 text-xs font-bold text-red-500 hover:bg-red-50 rounded-xl transition-all border border-transparent hover:border-red-100"
              >
                Clear
              </button>
            )}
          </div>

          <div className="text-sm font-bold text-gray-400 whitespace-nowrap">
            Showing {filteredEvents?.length || 0} of {events?.totalElements || 0} events
          </div>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left">
            <thead>
              <tr className="bg-white border-b border-gray-50">
                <th className="px-6 py-5 text-xs font-black text-gray-400 uppercase tracking-wider">Timestamp</th>
                <th className="px-6 py-5 text-xs font-black text-gray-400 uppercase tracking-wider">Link ID</th>
                <th className="px-6 py-5 text-xs font-black text-gray-400 uppercase tracking-wider">Location</th>
                <th className="px-6 py-5 text-xs font-black text-gray-400 uppercase tracking-wider">Device / Browser</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-50">
              {isLoading ? (
                Array.from({ length: 5 }).map((_, i) => (
                  <tr key={i} className="animate-pulse">
                    <td colSpan="5" className="px-6 py-8">
                      <div className="h-4 bg-gray-100 rounded-full w-full"></div>
                    </td>
                  </tr>
                ))
              ) : error ? (
                <tr>
                  <td colSpan="5" className="px-6 py-20 text-center">
                    <div className="flex flex-col items-center gap-3 text-red-500">
                      <AlertCircle size={40} />
                      <p className="font-bold">Failed to load live reports</p>
                      <button onClick={() => refetch()} className="text-sm underline">Try again</button>
                    </div>
                  </td>
                </tr>
              ) : filteredEvents?.length === 0 ? (
                <tr>
                  <td colSpan="5" className="px-6 py-20 text-center text-gray-400">
                    No activity found matching your search.
                  </td>
                </tr>
              ) : (
                filteredEvents?.map((event) => (
                  <tr key={event.id} className="hover:bg-gray-50/50 transition-colors group">
                    <td className="px-6 py-5">
                      <div className="flex flex-col">
                        <span className="text-sm font-bold text-gray-700">
                          {format(new Date(event.clickedAt), 'HH:mm:ss')}
                        </span>
                        <span className="text-[10px] text-gray-400 font-medium">
                          {format(new Date(event.clickedAt), 'MMM dd, yyyy')}
                        </span>
                      </div>
                    </td>
                    <td className="px-6 py-5">
                      <div className="flex items-center gap-2">
                        <button
                          onClick={() => setSearchTerm(event.shortLinkId.toString())}
                          className="px-2 py-1 bg-blue-50 text-blue-600 rounded-lg text-xs font-black hover:bg-blue-100 transition-colors"
                          title="Click to filter by this ID"
                        >
                          ID: {event.shortLinkId}
                        </button>
                      </div>
                    </td>
                    <td className="px-6 py-5">
                      <div className="flex items-center gap-2">
                        <Globe size={14} className="text-gray-400" />
                        <span className="text-sm text-gray-600 font-medium">
                          {event.city ? `${event.city}, ` : ''}{event.country || 'Unknown'}
                        </span>
                      </div>
                    </td>
                    <td className="px-6 py-5">
                      <div className="flex flex-col gap-1">
                        <div className="flex items-center gap-2 text-sm text-gray-700 font-bold">
                          <Monitor size={14} className="text-gray-400" />
                          {event.device || 'Unknown'}
                        </div>
                        <span className="text-xs text-gray-400 font-medium pl-5">
                          {event.browser || 'Unknown Browser'}
                        </span>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
        {events && (
          <Pagination 
            currentPage={page}
            totalPages={events.totalPages}
            totalElements={events.totalElements}
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

export default Reports;
