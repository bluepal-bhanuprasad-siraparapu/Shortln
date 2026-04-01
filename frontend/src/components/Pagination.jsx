import React from 'react';
import { ChevronLeft, ChevronRight, ChevronsLeft, ChevronsRight } from 'lucide-react';

/**
 * Modern, Premium Pagination Component
 * 
 * @param {number} currentPage - Current page index (0-indexed)
 * @param {number} totalPages - Total number of pages
 * @param {number} totalElements - Total number of items
 * @param {number} pageSize - Number of items per page
 * @param {function} onPageChange - Callback for page change
 * @param {function} onPageSizeChange - Callback for page size change
 */
const Pagination = ({ 
  currentPage, 
  totalPages, 
  totalElements, 
  pageSize, 
  onPageChange, 
  onPageSizeChange 
}) => {
  if (totalPages <= 1 && totalElements <= pageSize) return null;

  const startElement = currentPage * pageSize + 1;
  const endElement = Math.min((currentPage + 1) * pageSize, totalElements);

  const getPageNumbers = () => {
    const pages = [];
    const maxVisible = 5;
    
    if (totalPages <= maxVisible) {
      for (let i = 0; i < totalPages; i++) pages.push(i);
    } else {
      let start = Math.max(0, currentPage - 2);
      let end = Math.min(totalPages - 1, start + maxVisible - 1);
      
      if (end === totalPages - 1) {
        start = Math.max(0, end - maxVisible + 1);
      }
      
      for (let i = start; i <= end; i++) pages.push(i);
    }
    return pages;
  };

  return (
    <div className="flex flex-col sm:flex-row items-center justify-between gap-6 px-8 py-6 bg-white border-t border-gray-50 rounded-b-3xl">
      <div className="flex items-center gap-4 order-2 sm:order-1">
        <span className="text-[10px] font-black text-gray-400 uppercase tracking-widest whitespace-nowrap">
          Show
        </span>
        <select
          value={pageSize}
          onChange={(e) => onPageSizeChange(Number(e.target.value))}
          className="bg-gray-50 border border-gray-100 text-brand-dark text-[10px] font-black rounded-xl focus:ring-2 focus:ring-brand-accent/20 focus:border-brand-accent px-3 py-2 outline-none cursor-pointer transition-all hover:bg-white"
        >
          {[5, 10, 20, 50].map((size) => (
            <option key={size} value={size}>
              {size} Items
            </option>
          ))}
        </select>
        <span className="text-[10px] font-bold text-gray-400 italic">
          {totalElements > 0 ? (
             <>Displaying {startElement}-{endElement} of {totalElements}</>
          ) : (
            'No items to display'
          )}
        </span>
      </div>

      <div className="flex items-center gap-2 order-1 sm:order-2">
        <div className="flex items-center bg-gray-50/50 p-1 rounded-2xl border border-gray-100">
          <button
            onClick={() => onPageChange(0)}
            disabled={currentPage === 0}
            className="p-2 text-gray-400 hover:text-brand-accent hover:bg-white disabled:opacity-30 disabled:hover:bg-transparent rounded-xl transition-all"
            title="First Page"
          >
            <ChevronsLeft size={16} />
          </button>
          <button
            onClick={() => onPageChange(currentPage - 1)}
            disabled={currentPage === 0}
            className="p-2 text-gray-400 hover:text-brand-accent hover:bg-white disabled:opacity-30 disabled:hover:bg-transparent rounded-xl transition-all"
            title="Previous Page"
          >
            <ChevronLeft size={16} />
          </button>

          <div className="flex items-center px-1">
            {getPageNumbers().map((page) => (
              <button
                key={page}
                onClick={() => onPageChange(page)}
                className={`w-9 h-9 flex items-center justify-center text-[11px] font-black rounded-xl transition-all ${
                  currentPage === page
                    ? 'bg-brand-accent text-white shadow-lg shadow-brand-accent/20 scale-110'
                    : 'text-gray-500 hover:text-brand-accent hover:bg-white'
                }`}
              >
                {page + 1}
              </button>
            ))}
          </div>

          <button
            onClick={() => onPageChange(currentPage + 1)}
            disabled={currentPage >= totalPages - 1}
            className="p-2 text-gray-400 hover:text-brand-accent hover:bg-white disabled:opacity-30 disabled:hover:bg-transparent rounded-xl transition-all"
            title="Next Page"
          >
            <ChevronRight size={16} />
          </button>
          <button
            onClick={() => onPageChange(totalPages - 1)}
            disabled={currentPage >= totalPages - 1}
            className="p-2 text-gray-400 hover:text-brand-accent hover:bg-white disabled:opacity-30 disabled:hover:bg-transparent rounded-xl transition-all"
            title="Last Page"
          >
            <ChevronsRight size={16} />
          </button>
        </div>
      </div>
    </div>
  );
};

export default Pagination;
