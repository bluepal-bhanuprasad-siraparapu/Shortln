import React, { useState, useRef, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useAuth } from '../context/AuthContext';
import api from '../services/api';
import { 
  BarChart2, 
  Trash2, 
  Copy, 
  Calendar,
  MoreVertical,
  QrCode,
  Download,
  X,
  Search,
  Edit2,
  CheckCircle2,
  Lock,
  Share2
} from 'lucide-react';
import ShareModal from '../components/ShareModal';
import QrModal from '../components/QrModal';
import { Link, useNavigate } from 'react-router-dom';
import ExportControls from '../components/ExportControls';
import Pagination from '../components/Pagination';
import { downloadCSV, downloadPDF } from '../utils/exportUtils';



const ActionMenu = ({ link, onToggleStatus, onShowQr, onDelete, isLast, userPlan }) => {
  const [isOpen, setIsOpen] = useState(false);
  const menuRef = useRef(null);
  const navigate = useNavigate();

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
              onClick={() => { navigate(`/edit/${link.id}`); setIsOpen(false); }}
              className="w-full px-4 py-2.5 text-left text-sm flex items-center gap-3 hover:bg-gray-50 transition-colors"
            >
              <div className="p-1.5 bg-brand-accent/10 text-brand-accent rounded-lg">
                <Edit2 size={14} />
              </div>
              <span className="text-gray-700 font-medium font-sans">Update Link</span>
            </button>

            {userPlan === 'PRO' ? (
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
            ) : (
              <Link
                to="/pricing"
                className="w-full px-4 py-2.5 text-left text-sm flex items-center gap-3 hover:bg-gray-50 transition-colors group/analytics"
                onClick={() => setIsOpen(false)}
              >
                <div className="p-1.5 bg-gray-100 text-gray-400 rounded-lg group-hover/analytics:bg-purple-50 group-hover/analytics:text-purple-600 transition-colors">
                  <Lock size={14} />
                </div>
                <span className="text-gray-400 group-hover/analytics:text-gray-600 font-medium font-sans">Analytics (PRO)</span>
              </Link>
            )}

            <button
              onClick={() => { onShowQr(link); setIsOpen(false); }}
              className="w-full px-4 py-2.5 text-left text-sm flex items-center gap-3 hover:bg-gray-50 transition-colors"
            >
              <div className="p-1.5 bg-orange-50 text-orange-500 rounded-lg">
                <QrCode size={14} />
              </div>
              <span className="text-gray-700 font-medium font-sans">Show QR Code</span>
            </button>

            <button
              onClick={() => { onDelete(link.id); setIsOpen(false); }}
              className="w-full px-4 py-2.5 text-left text-sm flex items-center gap-3 hover:bg-brand-accent/5 transition-colors group/delete"
            >
              <div className="p-1.5 bg-brand-accent/10 text-brand-accent rounded-lg group-hover/delete:bg-brand-accent/20">
                <Trash2 size={14} />
              </div>
              <span className="text-brand-accent font-medium font-sans">Delete Link</span>
            </button>
          </div>
        </div>
      )}
    </div>
  );
};

const LinkList = () => {
  const queryClient = useQueryClient();
  const { user } = useAuth();
  const [selectedLinkForQr, setSelectedLinkForQr] = useState(null);
  const [selectedLinkForShare, setSelectedLinkForShare] = useState(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [copiedIndex, setCopiedIndex] = useState(null);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const navigate = useNavigate();

  const getLinkStatus = (link) => {
    const isExpired = link.expiresAt && new Date(link.expiresAt) < new Date();
    if (isExpired) return 'EXPIRED';
    if (link.active === 0) return 'INACTIVE';
    return 'ACTIVE';
  };

  const { data: links, isLoading } = useQuery({
    queryKey: ['links', searchQuery, statusFilter, page, pageSize],
    queryFn: () => {
      const params = new URLSearchParams();
      if (searchQuery) params.append('query', searchQuery);
      if (statusFilter === 'ACTIVE') params.append('active', '1');
      if (statusFilter === 'INACTIVE') params.append('active', '0');
      params.append('page', page);
      params.append('size', pageSize);
      params.append('sort', 'createdAt,desc');
      return api.get('/links?' + params.toString()).then(res => res.data);
    },
    placeholderData: (previousData) => previousData
  });

  const deleteMutation = useMutation({
    mutationFn: (id) => api.delete(`/links/${id}`),
    onSuccess: () => {
      queryClient.invalidateQueries(['links']);
    }
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }) => api.put(`/links/${id}`, data),
    onSuccess: () => {
      queryClient.invalidateQueries(['links']);
    }
  });

  const toggleStatusMutation = useMutation({
    mutationFn: (id) => api.patch(`/links/${id}/status`),
    onSuccess: () => {
      queryClient.invalidateQueries(['links']);
    }
  });

  const handleDelete = (id) => {
    if (window.confirm('Are you sure you want to delete this link?')) {
      deleteMutation.mutate(id);
    }
  };

  const copyToClipboard = (url, index) => {
    navigator.clipboard.writeText(url);
    setCopiedIndex(index);
    setTimeout(() => setCopiedIndex(null), 2000);
  };

  const filteredLinks = links?.content || [];

  const handleExport = (format) => {
    if (!filteredLinks || filteredLinks.length === 0) return;

    const headers = ["Title", "Short URL", "Original URL", "Clicks", "Created At", "Status"];
    const rows = filteredLinks.map(link => [
      link.title || 'Untitled',
      link.shortUrl,
      link.originalUrl,
      link.clickCount,
      new Date(link.createdAt).toLocaleDateString(),
      getLinkStatus(link)
    ]);

    const filename = `my-links-report-${new Date().getTime()}`;

    if (format === 'PDF') {
      downloadPDF('My Links Report', headers, rows, filename);
    } else {
      downloadCSV(rows, headers, filename);
    }
  };

  if (isLoading) return <div className="text-center py-20">Loading your links...</div>;

  return (
    <div className="space-y-8">
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
        <div>
          <h2 className="text-2xl font-bold text-brand-dark">My Links</h2>
          <p className="text-gray-500">Manage and monitor all your shortened URLs.</p>
        </div>
        
        <div className="flex items-center gap-4 w-full md:w-auto">
          <ExportControls 
            onExport={handleExport}
            disabled={!filteredLinks || filteredLinks.length === 0}
            label="Export Report"
          />
          <Link 
            to="/dashboard" 
            className="bg-brand-accent text-white px-6 py-3 rounded-xl font-bold shadow-lg shadow-brand-accent/20 hover:bg-blue-700 transition-all whitespace-nowrap"
          >
            Create New Link
          </Link>
        </div>
      </div>

      <div className="bg-white p-4 rounded-2xl border border-gray-100 shadow-sm flex flex-col md:flex-row gap-4 items-center">
        {/* Search */}
        <div className="relative flex-1 w-full">
          <Search className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400" size={20} />
          <input 
            type="text" 
            placeholder="Search links..." 
            value={searchQuery}
            onChange={(e) => {
              setSearchQuery(e.target.value);
              setPage(0);
            }}
            className="w-full pl-12 pr-4 py-3.5 bg-gray-50/50 border border-gray-100 rounded-xl focus:ring-2 focus:ring-brand-accent/20 focus:border-brand-accent transition-all text-sm outline-none"
          />
        </div>

        {/* Status Filter */}
        <div className="flex items-center gap-2 bg-gray-50/50 p-1.5 rounded-xl border border-gray-100 w-full md:w-auto whitespace-nowrap overflow-x-auto no-scrollbar">
          {['ALL', 'ACTIVE', 'INACTIVE', 'EXPIRED'].map((status) => (
            <button
              key={status}
              onClick={() => setStatusFilter(status)}
              className={`px-4 py-2 rounded-lg text-xs font-bold transition-all ${
                statusFilter === status 
                  ? 'bg-white text-brand-accent shadow-sm' 
                  : 'text-gray-500 hover:text-brand-dark'
              }`}
            >
              {status.charAt(0) + status.slice(1).toLowerCase()}
            </button>
          ))}
        </div>
      </div>

      <div className="bg-white rounded-2xl shadow-sm border border-gray-100">
        <div className="overflow-x-auto min-h-[400px] pb-10">
          <table className="w-full text-left">
            <thead>
              <tr className="bg-gray-50/50 border-b border-gray-100">
                <th className="px-6 py-4 font-bold text-xs text-gray-500 uppercase tracking-wider">Title</th>
                <th className="px-6 py-4 font-bold text-xs text-gray-500 uppercase tracking-wider">Link Details</th>
                <th className="px-6 py-4 font-bold text-xs text-gray-500 uppercase tracking-wider">Clicks</th>
                <th className="px-6 py-4 font-bold text-xs text-gray-500 uppercase tracking-wider text-center">Created At</th>
                <th className="px-6 py-4 font-bold text-xs text-gray-500 uppercase tracking-wider text-center">Expire At</th>
                <th className="px-6 py-4 font-bold text-xs text-gray-500 uppercase tracking-wider text-center">Status</th>
                <th className="px-6 py-4 font-bold text-xs text-gray-500 uppercase tracking-wider text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-50">
              {filteredLinks?.map((link, index) => (
                <tr key={link.id} className="hover:bg-gray-50/30 transition-colors">
                  <td className="px-6 py-6 font-bold text-gray-900 border-r border-gray-50">
                    <div className="flex items-center gap-2">
                       {link.title || <span className="text-gray-400 italic font-normal text-xs">No title</span>}
                    </div>
                  </td>
                  <td className="px-6 py-6">
                    <div className="flex flex-col space-y-1 max-w-xs overflow-hidden">
                      <div className="flex items-center gap-2 group/copy">
                        <a 
                          href={link.shortUrl} 
                          target="_blank" 
                          rel="noopener noreferrer"
                          className="font-bold text-brand-accent text-lg hover:text-brand-accent/80 transition-colors"
                        >
                          {link.shortUrl}
                        </a>
                        <button 
                          onClick={() => copyToClipboard(link.shortUrl, index)}
                          className="p-1.5 text-gray-400 hover:text-brand-accent hover:bg-brand-accent/5 rounded-md transition-all focus:opacity-100"
                          title="Copy Link"
                        >
                          {copiedIndex === index ? <CheckCircle2 size={14} className="text-green-500" /> : <Copy size={14} />}
                        </button>
                        <button 
                          onClick={() => setSelectedLinkForShare(link)}
                          className="p-1.5 text-gray-400 hover:text-brand-accent hover:bg-brand-accent/5 rounded-md transition-all focus:opacity-100"
                          title="Share Link"
                        >
                          <Share2 size={14} />
                        </button>
                      </div>
                      <span className="text-gray-400 text-xs truncate" title={link.originalUrl}>{link.originalUrl}</span>
                    </div>
                  </td>
                  <td className="px-6 py-6">
                    <div className="inline-flex flex-col items-center">
                      <span className="text-xl font-bold text-brand-dark">{link.clickCount}</span>
                      <span className="text-[10px] font-bold text-gray-400 uppercase tracking-tighter mt-1">Total clicks</span>
                    </div>
                  </td>
                  <td className="px-6 py-6 text-center">
                    <div className="flex flex-col items-center">
                       <span className="text-sm text-gray-600 font-medium">{new Date(link.createdAt).toLocaleDateString()}</span>
                       <span className="text-[10px] text-gray-400 uppercase font-bold mt-0.5">Registration</span>
                    </div>
                  </td>
                  <td className="px-6 py-6 text-center">
                    {link.expiresAt ? (
                      <div className="flex items-center justify-center gap-2 text-sm text-gray-500 font-medium">
                        <Calendar size={14} className="text-orange-400" />
                        {new Date(link.expiresAt).toLocaleDateString()}
                      </div>
                    ) : (
                      <span className="text-xs font-bold text-green-500 bg-green-50 px-2 py-1 rounded">Never Expire</span>
                    )}
                  </td>
                  <td className="px-6 py-6 text-center">
                    {(() => {
                      const isExpired = link.expiresAt && new Date(link.expiresAt) < new Date();
                      if (isExpired) {
                        return (
                          <span className="text-[10px] font-bold bg-red-100 text-red-600 px-2 py-1 rounded-full uppercase tracking-wider">
                            Expired
                          </span>
                        );
                      }
                      if (link.active === 0) {
                        return (
                          <span className="text-[10px] font-bold bg-gray-100 text-gray-400 px-2 py-1 rounded-full uppercase tracking-wider">
                            Inactive
                          </span>
                        );
                      }
                      return (
                        <span className="text-[10px] font-bold bg-green-100 text-green-600 px-2 py-1 rounded-full uppercase tracking-wider">
                          Active
                        </span>
                      );
                    })()}
                  </td>
                  <td className="px-6 py-6 text-right">
                    <div className="flex justify-end">
                      <ActionMenu
                        link={link}
                        onToggleStatus={(id) => toggleStatusMutation.mutate(id)}
                        onShowQr={setSelectedLinkForQr}
                        onDelete={handleDelete}
                        isLast={index > 0 && index >= (filteredLinks.length - 2)}
                        userPlan={user?.plan}
                      />
                    </div>
                  </td>
                </tr>
              ))}
              {filteredLinks?.length === 0 && (
                <tr>
                  <td colSpan="7" className="py-20 text-center text-gray-400">
                    {searchQuery ? 'No links match your search.' : 'No links found. Create your first link!'}
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
      <ShareModal link={selectedLinkForShare} onClose={() => setSelectedLinkForShare(null)} />
    </div>
  );
};

export default LinkList;
