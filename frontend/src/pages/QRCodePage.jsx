import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useAuth } from '../context/AuthContext';
import api from '../services/api';
import { 
  QrCode, 
  Download, 
  Search, 
  ExternalLink,
  Copy,
  Calendar,
  Share2,
  CheckCircle2
} from 'lucide-react';
import toast from 'react-hot-toast';
import QrModal from '../components/QrModal';
import { Link } from 'react-router-dom';
import Pagination from '../components/Pagination';

const QrCodeCard = ({ link, onShowQr }) => {
  const [qrUrl, setQrUrl] = useState(null);
  const [loading, setLoading] = useState(false);
  const [copied, setCopied] = useState(false);

  React.useEffect(() => {
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
  }, [link.id]);

  const downloadQr = () => {
    if (!qrUrl) return;
    const linkComp = document.createElement('a');
    linkComp.href = qrUrl;
    linkComp.download = `qr-${link.shortCode}.png`;
    document.body.appendChild(linkComp);
    linkComp.click();
    document.body.removeChild(linkComp);
  };

  const copyLink = () => {
    navigator.clipboard.writeText(link.shortUrl);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const shareQr = async () => {
    if (!qrUrl) return;
    try {
      const response = await fetch(qrUrl);
      const blob = await response.blob();
      const file = new File([blob], `qr-${link.shortCode}.png`, { type: blob.type });

      if (navigator.canShare && navigator.share && navigator.canShare({ files: [file] })) {
        await navigator.share({
          files: [file],
          title: `QR Code for ${link.shortCode}`,
          text: `Scan this QR code to visit: ${link.shortUrl}`
        });
      } else {
        toast.error("Sharing files is not supported on this browser.");
      }
    } catch (error) {
      console.error('Error sharing QR:', error);
      toast.error("An error occurred while sharing.");
    }
  };

  return (
    <div className="bg-white rounded-2xl p-6 shadow-sm border border-gray-100 flex flex-col items-center group hover:shadow-md transition-all">
      <div className="w-full mb-4 flex justify-between items-start">
        <div className="flex-1">
          <h3 className="font-bold text-gray-900 truncate" title={link.title || link.shortCode}>
            {link.title || link.shortCode}
          </h3>
          <p className="text-xs text-gray-400 truncate max-w-[150px]">{link.originalUrl}</p>
        </div>
        <div className="flex items-center gap-2">
          <button 
            onClick={shareQr}
            className="p-2 bg-blue-50 text-brand-accent rounded-lg hover:bg-brand-accent hover:text-white transition-all shadow-sm shadow-brand-accent/10"
            title="Share QR Image"
          >
            <Share2 size={16} />
          </button>
        </div>
      </div>

      <div 
        onClick={() => onShowQr(link)}
        className="bg-gray-50 p-4 rounded-xl flex justify-center items-center w-full aspect-square mb-6 border border-gray-100 group-hover:bg-white group-hover:border-brand-accent/30 transition-all cursor-pointer relative"
      >
        {loading ? (
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-brand-accent"></div>
        ) : qrUrl ? (
          <img
            src={qrUrl}
            alt="QR Code"
            className="w-full h-full object-contain rounded-lg"
          />
        ) : (
          <p className="text-red-500 text-xs font-bold text-center">Failed to load QR</p>
        )}
      </div>

      <div className="w-full space-y-3">
        <div className="flex items-center gap-2 p-2.5 bg-gray-50 rounded-xl border border-gray-100 text-xs font-medium text-brand-accent overflow-hidden">
          <span className="truncate flex-1">{link.shortUrl}</span>
          <button onClick={copyLink} className="text-gray-400 hover:text-brand-accent transition-colors">
            {copied ? <CheckCircle2 size={14} className="text-green-500" /> : <Copy size={14} />}
          </button>
        </div>

        <button
          onClick={downloadQr}
          disabled={!qrUrl}
          className="w-full bg-brand-dark text-white py-3 rounded-xl font-bold hover:bg-black transition-all flex items-center justify-center gap-2 disabled:opacity-50 text-sm shadow-sm"
        >
          <Download size={16} />
          Download PNG
        </button>
      </div>
    </div>
  );
};

const QRCodePage = () => {
  const [searchQuery, setSearchQuery] = useState('');
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(8);
  const [selectedLink, setSelectedLink] = useState(null);
  
  const { data: linksData, isLoading } = useQuery({
    queryKey: ['links', searchQuery, page, pageSize],
    queryFn: () => api.get('/links', {
      params: {
        query: searchQuery,
        page,
        size: pageSize
      }
    }).then(res => res.data)
  });

  const filteredLinks = linksData?.content || [];

  return (
    <div className="space-y-8">
      {selectedLink && (
        <QrModal 
          link={selectedLink} 
          onClose={() => setSelectedLink(null)} 
        />
      )}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
        <div>
          <h2 className="text-2xl font-bold text-brand-dark">QR Codes</h2>
          <p className="text-gray-500">View and download QR codes for your short links.</p>
        </div>
      </div>

      <div className="bg-white p-4 rounded-2xl border border-gray-100 shadow-sm flex items-center gap-4">
        <div className="relative flex-1">
          <Search className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400" size={20} />
          <input 
            type="text" 
            placeholder="Search by title or URL..." 
            value={searchQuery}
            onChange={(e) => {
              setSearchQuery(e.target.value);
              setPage(0);
            }}
            className="w-full pl-12 pr-4 py-3.5 bg-gray-50/50 border border-gray-100 rounded-xl focus:ring-2 focus:ring-brand-accent/20 focus:border-brand-accent transition-all text-sm outline-none"
          />
        </div>
      </div>

      {isLoading ? (
        <div className="flex flex-col items-center justify-center py-20 gap-4">
           <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-brand-accent"></div>
           <p className="text-gray-500 font-medium">Loading your codes...</p>
        </div>
      ) : filteredLinks?.length > 0 ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
          {filteredLinks.map(link => (
            <QrCodeCard key={link.id} link={link} onShowQr={setSelectedLink} />
          ))}
        </div>
      ) : (
        <div className="bg-white rounded-3xl p-20 text-center border border-gray-100 flex flex-col items-center">
          <div className="w-20 h-20 bg-gray-50 text-gray-300 rounded-3xl flex items-center justify-center mb-6">
            <QrCode size={40} />
          </div>
          <h3 className="text-xl font-bold text-brand-dark mb-2">No QR Codes Found</h3>
          <p className="text-gray-500 max-w-sm">
            {searchQuery ? "No links match your search criteria. Try a different term." : "You haven't created any links yet. Start by creating your first short link!"}
          </p>
          {!searchQuery && (
            <Link to="/dashboard" className="mt-8 bg-brand-accent text-white px-8 py-3 rounded-xl font-bold shadow-lg shadow-brand-accent/20 hover:bg-blue-700 transition-all">
              Create New Link
            </Link>
          )}
        </div>
      )}

      {linksData?.totalPages > 1 && (
        <div className="pt-4">
          <Pagination 
            currentPage={page}
            totalPages={linksData.totalPages}
            onPageChange={setPage}
            pageSize={pageSize}
            onPageSizeChange={(newSize) => {
              setPageSize(newSize);
              setPage(0);
            }}
            totalElements={linksData.totalElements}
          />
        </div>
      )}
    </div>
  );
};

export default QRCodePage;
