import React, { useState, useEffect } from 'react';
import { X, Download } from 'lucide-react';
import api from '../services/api';

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

export default QrModal;
