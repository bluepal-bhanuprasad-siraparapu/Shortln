import React, { useState, useRef, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '../services/api';
import {
  BarChart2,
  Trash2,
  Copy,
  Calendar,
  Globe,
  Filter,
  ExternalLink,
  Power,
  MoreVertical,
  QrCode,
  X,
  Download,
  Search,
  ChevronDown,
  FileText
} from 'lucide-react';
import { Link } from 'react-router-dom';
import ExportControls from '../components/ExportControls';
import Pagination from '../components/Pagination';
import { downloadCSV, downloadPDF } from '../utils/exportUtils';

const QrModal = ({ link, onClose }) => {
  const [qrUrl, setQrUrl] = useState(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!link) {
      setQrUrl(null);
      return;
    }

    const fetchQr = async () => {
      setLoading(true);
      try {
        const response = await api.get(`/links/${link.id}/qr`, { responseType: 'blob' });
        const url = URL.createObjectURL(response.data);
        setQrUrl(url);
      } catch (error) {
        console.error('Error fetching QR:', error);
      } finally {
        setLoading(false);
      }
    };

    fetchQr();

    return () => {
      if (qrUrl) URL.revokeObjectURL(qrUrl);
    };
  }, [link]);

  if (!link) return null;

  const downloadQr = () => {
    if (!qrUrl) return;
    const linkComp = document.createElement('a');
    linkComp.href = qrUrl;
    linkComp.download = `qr-${link.shortCode}.png`;
    document.body.appendChild(linkComp);
    linkComp.click();
    document.body.removeChild(linkComp);
  };

  return (
    <div className="fixed inset-0 bg-black/50 backdrop-blur-sm flex items-center justify-center z-[100] animate-in fade-in duration-300">
      <div className="bg-white rounded-2xl p-8 max-w-sm w-full shadow-2xl relative animate-in zoom-in-95 duration-200">
        <button
          onClick={onClose}
          className="absolute top-4 right-4 p-2 text-gray-400 hover:text-gray-600 hover:bg-gray-100 rounded-full transition-colors"
        >
          <X size={20} />
        </button>

        <div className="text-center space-y-6">
          <div>
            <h3 className="text-xl font-bold text-brand-dark">QR Code</h3>
            <p className="text-sm text-gray-500">{link.title || link.shortCode}</p>
          </div>

          <div className="bg-gray-50 p-6 rounded-2xl flex justify-center border border-gray-100 min-h-[200px] items-center">
            {loading ? (
              <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-brand-accent"></div>
            ) : qrUrl ? (
              <img
                src={qrUrl}
                alt="QR Code"
                className="w-48 h-48 rounded-lg shadow-sm"
              />
            ) : (
              <p className="text-red-500 text-sm font-bold">Failed to load QR</p>
            )}
          </div>

          <div className="flex flex-col gap-3">
            <button
              onClick={downloadQr}
              disabled={!qrUrl}
              className="w-full bg-brand-accent text-white py-3 rounded-xl font-bold hover:bg-brand-accent/90 transition-all flex items-center justify-center gap-2 disabled:opacity-50"
            >
              <Download size={18} />
              Download PNG
            </button>
            <p className="text-[10px] text-gray-400 uppercase tracking-widest font-bold">Scan to open link</p>
          </div>
        </div>
      </div>
    </div>
  );
};

const ActionMenu = ({ link, onToggleStatus, onCopy, onShowQr, isLast }) => {
  const [isOpen, setIsOpen] = useState(false);
  const menuRef = useRef(null);

  useEffect(() => {
    const handleClickOutside = (event) => {
      if (menuRef.current && !menuRef.current.contains(event.target)) {
        setIsOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  return (
    <div className="relative" ref={menuRef}>
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="p-2 text-gray-400 hover:text-brand-dark hover:bg-gray-100 rounded-lg transition-colors"
      >
        <MoreVertical size={20} />
      </button>

      {isOpen && (
        <div className={`absolute right-0 w-48 bg-white rounded-xl shadow-xl border border-gray-100 py-2 z-50 animate-in fade-in duration-200 ${isLast ? 'bottom-full mb-2 slide-in-from-bottom-2' : 'mt-2 slide-in-from-top-2'}`}>
          <div className="px-4 py-3.5 flex items-center justify-between border-b border-gray-50 bg-gray-50/50">
            <span className="text-[10px] font-bold text-gray-400 uppercase tracking-widest leading-none">Status</span>
            <button 
              onClick={() => onToggleStatus(link.id)}
              className={`relative inline-flex h-4.5 w-8 items-center rounded-full transition-all duration-300 focus:outline-none ${link.active === 1 ? 'bg-brand-accent shadow-sm shadow-brand-accent/20' : 'bg-gray-300'}`}
            >
              <span className={`inline-block h-3 w-3 transform rounded-full bg-white transition-transform duration-300 ${link.active === 1 ? 'translate-x-4' : 'translate-x-1'}`} />
            </button>
          </div>

          <div className="py-1">
            <button
              onClick={() => { onCopy(link.shortUrl); setIsOpen(false); }}
              className="w-full px-4 py-2.5 text-left text-sm flex items-center gap-3 hover:bg-gray-50 transition-colors"
            >
              <div className="p-1.5 bg-brand-accent/10 text-brand-accent rounded-lg">
                <Copy size={14} />
              </div>
              <span className="text-gray-700 font-medium font-sans">Copy Short Link</span>
            </button>

            <Link
              to={`/analytics/${link.id}`}
              className="w-full px-4 py-2.5 text-left text-sm flex items-center gap-3 hover:bg-gray-50 transition-colors"
              onClick={() => setIsOpen(false)}
            >
              <div className="p-1.5 bg-purple-50 text-purple-600 rounded-lg">
                <BarChart2 size={14} />
              </div>
              <span className="text-gray-700 font-medium font-sans">View Analytics</span>
            </Link>

            <button
              onClick={() => { onShowQr(link); setIsOpen(false); }}
              className="w-full px-4 py-2.5 text-left text-sm flex items-center gap-3 hover:bg-gray-50 transition-colors"
            >
              <div className="p-1.5 bg-orange-50 text-orange-500 rounded-lg">
                <QrCode size={14} />
              </div>
              <span className="text-gray-700 font-medium font-sans">Show QR Code</span>
            </button>
          </div>
        </div>
      )}
    </div>
  );
};

const AdminLinks = () => {
  const queryClient = useQueryClient();
  const [selectedLinkForQr, setSelectedLinkForQr] = useState(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);

  const { data: links, isLoading } = useQuery({
    queryKey: ['admin', 'links', searchQuery, statusFilter, startDate, endDate, page, pageSize],
    queryFn: () => {
      const params = new URLSearchParams();
      if (searchQuery) params.append('query', searchQuery);
      if (statusFilter === 'ACTIVE') params.append('active', '1');
      if (statusFilter === 'INACTIVE') params.append('active', '0');
      if (startDate) params.append('startDate', `${startDate}T00:00:00`);
      if (endDate) params.append('endDate', `${endDate}T23:59:59`);
      params.append('page', page);
      params.append('size', pageSize);
      params.append('sort', 'createdAt,desc');
      return api.get('/links/all?' + params.toString()).then(res => res.data);
    },
    placeholderData: (previousData) => previousData
  });

  const getLinkStatus = (link) => {
    const isExpired = link.expiresAt && new Date(link.expiresAt) < new Date();
    if (isExpired) return 'EXPIRED';
    if (link.active === 0) return 'INACTIVE';
    return 'ACTIVE';
  };

  const filteredLinks = links?.content || [];

  const handleExport = (format) => {
    if (!filteredLinks || filteredLinks.length === 0) return;

    const headers = ["Title", "Short Code", "Original URL", "Clicks", "Created By", "Status"];
    const rows = filteredLinks.map(link => [
      link.title || 'Untitled',
      link.shortCode,
      link.originalUrl,
      link.clickCount,
      link.username || 'System',
      getLinkStatus(link)
    ]);

    const filename = `admin-links-report-${statusFilter.toLowerCase()}-${new Date().getTime()}`;

    if (format === 'PDF') {
      downloadPDF('Global Links Report', headers, rows, filename);
    } else {
      downloadCSV(rows, headers, filename);
    }
  };

  const deleteMutation = useMutation({
    mutationFn: (id) => api.delete(`/links/${id}`),
    onSuccess: () => {
      queryClient.invalidateQueries(['admin', 'links']);
      queryClient.invalidateQueries(['links']);
    }
  });

  const toggleStatusMutation = useMutation({
    mutationFn: (id) => api.patch(`/links/${id}/status`),
    onSuccess: () => {
      queryClient.invalidateQueries(['admin', 'links']);
      queryClient.invalidateQueries(['links']);
    }
  });

  const copyToClipboard = (url) => {
    navigator.clipboard.writeText(url);
  };

  if (isLoading) return <div className="text-center py-20">Loading global link data...</div>;

  return (
    <div className="space-y-8">
      <div className="flex flex-col lg:flex-row justify-between items-start lg:items-center gap-6">
        <div>
          <h2 className="text-3xl font-black text-brand-dark tracking-tight">Global Link Overview</h2>
          <p className="text-gray-500 font-medium">View and manage every link created on the platform.</p>
        </div>
        
        <div className="flex flex-wrap items-center gap-4 w-full lg:w-auto">
           {/* Stats Summary */}
          <div className="bg-white px-5 py-3 rounded-2xl border border-gray-100 flex items-center gap-4 shadow-sm">
            <div className="p-2.5 bg-brand-accent/10 text-brand-accent rounded-xl">
              <Globe size={20} />
            </div>
            <div>
              <p className="text-[10px] font-bold text-gray-400 uppercase tracking-wider leading-none mb-1">Total Items</p>
              <p className="text-xl font-black text-brand-dark leading-none">
                {links?.totalElements || 0}
              </p>
            </div>
          </div>

          <ExportControls 
            onExport={handleExport}
            disabled={!filteredLinks || filteredLinks.length === 0}
            label="Export Report"
          />
        </div>
      </div>

      <div className="bg-white p-4 rounded-2xl border border-gray-100 shadow-sm flex flex-col xl:flex-row gap-4 items-center">
        {/* Search */}
        <div className="relative flex-1 w-full xl:w-80">
          <Search className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400" size={20} />
          <input 
            type="text" 
            placeholder="Search by title, URL or owner..." 
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-12 pr-4 py-3.5 bg-gray-50/50 border border-gray-100 rounded-xl focus:ring-2 focus:ring-brand-accent/20 focus:border-brand-accent transition-all text-sm outline-none"
          />
        </div>

        {/* Date Filters */}
        <div className="flex items-center gap-2 w-full xl:w-auto bg-gray-50/50 p-1.5 rounded-xl border border-gray-100">
           <div className="relative flex-1 md:w-36">
              <Calendar className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={14} />
              <input 
                type="date"
                value={startDate}
                onChange={(e) => setStartDate(e.target.value)}
                className="w-full pl-9 pr-3 py-2 rounded-lg border-none focus:ring-2 focus:ring-brand-accent transition-all text-[10px] bg-white uppercase font-bold"
                title="Start Date"
              />
            </div>
            <span className="text-gray-400 font-bold text-[10px]">to</span>
            <div className="relative flex-1 md:w-36">
              <Calendar className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={14} />
              <input 
                type="date"
                value={endDate}
                onChange={(e) => setEndDate(e.target.value)}
                className="w-full pl-9 pr-3 py-2 rounded-lg border-none focus:ring-2 focus:ring-brand-accent transition-all text-[10px] bg-white uppercase font-bold"
                title="End Date"
              />
            </div>
        </div>

        {/* Status Filter */}
        <div className="flex items-center gap-1 bg-gray-50/50 p-1.5 rounded-xl border border-gray-100 w-full xl:w-auto whitespace-nowrap overflow-x-auto no-scrollbar">
          {['ALL', 'ACTIVE', 'INACTIVE', 'EXPIRED'].map((status) => (
            <button
              key={status}
              onClick={() => setStatusFilter(status)}
              className={`px-3 py-2 rounded-lg text-[10px] font-bold transition-all ${
                statusFilter === status 
                  ? 'bg-white text-brand-accent shadow-sm' 
                  : 'text-gray-500 hover:text-brand-dark'
              }`}
            >
              {status.charAt(0) + status.slice(1).toLowerCase()}
            </button>
          ))}
        </div>

        {(searchQuery || statusFilter !== 'ALL' || startDate || endDate) && (
          <button 
            onClick={() => { setSearchQuery(''); setStatusFilter('ALL'); setStartDate(''); setEndDate(''); }}
            className="px-4 py-2 text-xs font-bold text-red-500 hover:bg-red-50 rounded-xl transition-all"
          >
            Clear
          </button>
        )}
      </div>

      <div className="bg-white rounded-2xl shadow-sm border border-gray-100">
        <div className="overflow-x-auto min-h-[400px] pb-10">
          <table className="w-full text-left font-sans">
          <thead>
            <tr className="bg-gray-50 border-b border-gray-100">
              <th className="px-6 py-4 font-bold text-xs text-gray-500 uppercase tracking-wider">Title</th>
              <th className="px-6 py-4 font-bold text-xs text-gray-500 uppercase tracking-wider">Link & Alias</th>
              <th className="px-6 py-4 font-bold text-xs text-gray-500 uppercase tracking-wider">Original URL</th>
              <th className="px-6 py-4 font-bold text-xs text-gray-500 uppercase tracking-wider text-center">Clicks</th>
              <th className="px-6 py-4 font-bold text-xs text-gray-500 uppercase tracking-wider text-center">Created By</th>
              <th className="px-6 py-4 font-bold text-xs text-gray-500 uppercase tracking-wider text-center">Status</th>
              <th className="px-6 py-4 font-bold text-xs text-gray-500 uppercase tracking-wider text-right">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-50">
            {filteredLinks?.map((link, index) => (
              <tr key={link.id} className="hover:bg-gray-50 transition-colors group">
                <td className="px-6 py-5 font-bold text-gray-900 border-r border-gray-50">
                  <div className="flex items-center gap-2">
                    {link.title || <span className="text-gray-400 italic font-normal text-xs">No title</span>}
                  </div>
                </td>
                <td className="px-6 py-5">
                  <div className="flex flex-col space-y-1">
                    <span className="font-bold text-brand-dark text-[10px] uppercase tracking-wider">{link.shortCode}</span>
                    <a 
                      href={link.shortUrl}
                      target="_blank" 
                      rel="noopener noreferrer"
                      className="text-sm text-brand-accent font-bold hover:text-blue-700 transition-colors truncate max-w-[200px]"
                    >
                      {link.shortUrl}
                    </a>
                  </div>
                </td>
                <td className="px-6 py-5">
                  <p className="text-xs text-gray-500 truncate max-w-[150px]" title={link.originalUrl}>{link.originalUrl}</p>
                </td>
                <td className="px-6 py-5 text-center">
                  <div className="flex flex-col items-center">
                    <span className="font-black text-brand-dark text-lg leading-none">{link.clickCount}</span>
                    <span className="text-[8px] font-bold text-gray-400 uppercase tracking-tighter mt-1">Total Views</span>
                  </div>
                </td>
                <td className="px-6 py-5 text-center">
                   <div className="inline-flex items-center gap-2 px-3 py-1 bg-gray-50 rounded-full border border-gray-100">
                    <div className="w-1.5 h-1.5 bg-brand-accent rounded-full animate-pulse"></div>
                    <span className="text-[10px] font-bold text-gray-700">{link.username || 'System'}</span>
                  </div>
                </td>
                <td className="px-6 py-5 text-center">
                  {(() => {
                    const status = getLinkStatus(link);
                    if (status === 'EXPIRED') {
                      return (
                        <span className="text-[9px] font-black bg-red-50 text-red-500 px-2.5 py-1.5 rounded-lg uppercase tracking-widest border border-red-100/50">
                          Expired
                        </span>
                      );
                    }
                    if (status === 'INACTIVE') {
                      return (
                        <span className="text-[9px] font-black bg-gray-50 text-gray-400 px-2.5 py-1.5 rounded-lg uppercase tracking-widest border border-gray-100/50">
                          Inactive
                        </span>
                      );
                    }
                    return (
                      <span className="text-[9px] font-black bg-green-50 text-green-500 px-2.5 py-1.5 rounded-lg uppercase tracking-widest border border-green-100/50">
                        Active
                      </span>
                    );
                  })()}
                </td>
                <td className="px-6 py-5 text-right flex justify-end">
                  <ActionMenu
                    link={link}
                    onToggleStatus={(id) => toggleStatusMutation.mutate(id)}
                    onCopy={copyToClipboard}
                    onShowQr={setSelectedLinkForQr}
                    isLast={index > 0 && index >= (filteredLinks.length - (filteredLinks.length > 5 ? 3 : 2))}
                  />
                </td>
              </tr>
            ))}
            {filteredLinks?.length === 0 && (
              <tr>
                <td colSpan="7" className="py-24 text-center">
                  <div className="flex flex-col items-center justify-center space-y-3">
                    <div className="p-4 bg-gray-50 rounded-full text-gray-300">
                       <Search size={40} />
                    </div>
                    <div className="space-y-1">
                      <p className="text-gray-900 font-bold">No links found</p>
                      <p className="text-gray-400 text-sm">Try adjusting your filters or search terms.</p>
                    </div>
                  </div>
                </td>
              </tr>
            )}
          </tbody>
        </table>
        </div>
        {links && (
          <Pagination 
            currentPage={page}
            totalPages={links.totalPages}
            totalElements={links.totalElements}
            pageSize={pageSize}
            onPageChange={setPage}
            onPageSizeChange={(size) => {
              setPageSize(size);
              setPage(0);
            }}
          />
        )}
      </div>

      <QrModal link={selectedLinkForQr} onClose={() => setSelectedLinkForQr(null)} />
    </div>
  );
};

export default AdminLinks;
