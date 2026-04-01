import React, { useState } from 'react';
import { Download, ChevronDown, Check } from 'lucide-react';

/**
 * Reusable export controls component for reports.
 * @param {Object} props
 * @param {Function} props.onExport - Callback function with signature (format).
 * @param {boolean} props.isLoading - Loading state for the export action.
 * @param {string} [props.label="Export Report"] - Button label.
 * @param {boolean} [props.disabled=false] - Disabled state.
 */
const ExportControls = ({ onExport, isLoading, label = "Export Report", disabled = false }) => {
  const [format, setFormat] = useState('PDF');
  const [isOpen, setIsOpen] = useState(false);

  const handleExport = () => {
    onExport(format);
    setIsOpen(false);
  };

  return (
    <div className="relative inline-block">
      <div className="flex bg-white border border-gray-200 rounded-xl shadow-sm overflow-hidden divide-x divide-gray-100">
        <button
          onClick={handleExport}
          disabled={disabled || isLoading}
          className="flex items-center gap-2 px-6 py-3 bg-brand-dark text-white font-bold hover:bg-black transition-all disabled:opacity-50 disabled:cursor-not-allowed group whitespace-nowrap"
        >
          {isLoading ? (
            <div className="animate-spin rounded-full h-4 w-4 border-2 border-white border-b-transparent" />
          ) : (
            <Download size={18} className="group-hover:scale-110 transition-transform" />
          )}
          <span>{label}</span>
        </button>
        
        <button
          onClick={(e) => {
            e.stopPropagation();
            setIsOpen(!isOpen);
          }}
          disabled={disabled || isLoading}
          className="px-3 py-3 bg-white text-gray-500 hover:text-brand-dark hover:bg-gray-50 transition-all flex items-center h-full disabled:opacity-50"
        >
          <span className="text-[10px] font-black uppercase tracking-widest mr-1">{format}</span>
          <ChevronDown size={14} className={`transition-transform duration-200 ${isOpen ? 'rotate-180' : ''}`} />
        </button>
      </div>

      {isOpen && (
        <div className="absolute right-0 top-full mt-2 w-32 bg-white rounded-xl shadow-xl border border-gray-100 py-2 z-[100] animate-in fade-in slide-in-from-top-2">
          <div className="px-3 py-1 mb-1 border-b border-gray-50">
            <p className="text-[10px] font-black text-gray-400 uppercase tracking-widest leading-none">Select Format</p>
          </div>
          <button
            onClick={() => { setFormat('PDF'); setIsOpen(false); }}
            className="w-full px-4 py-2 text-left text-sm flex items-center justify-between hover:bg-gray-50 transition-colors"
          >
            <span className={format === 'PDF' ? 'font-bold text-brand-dark' : 'text-gray-600'}>PDF</span>
            {format === 'PDF' && <Check size={14} className="text-brand-accent" />}
          </button>
          <button
            onClick={() => { setFormat('CSV'); setIsOpen(false); }}
            className="w-full px-4 py-2 text-left text-sm flex items-center justify-between hover:bg-gray-50 transition-colors"
          >
            <span className={format === 'CSV' ? 'font-bold text-brand-dark' : 'text-gray-600'}>CSV</span>
            {format === 'CSV' && <Check size={14} className="text-brand-accent" />}
          </button>
        </div>
      )}
    </div>
  );
};

export default ExportControls;
