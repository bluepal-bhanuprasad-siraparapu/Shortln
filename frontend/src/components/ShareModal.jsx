import React, { useState } from 'react';
import { 
  X, 
  Copy, 
  CheckCircle2, 
  Twitter, 
  Facebook, 
  Instagram, 
  MessageCircle, 
  Mail,
  MessagesSquare,
  Share2
} from 'lucide-react';
import toast from 'react-hot-toast';

const ShareModal = ({ link, onClose }) => {
  const [copied, setCopied] = useState(false);

  if (!link) return null;

  const url = link.shortUrl;
  const title = link.title || 'Check out this link!';

  const shareOptions = [
    {
      name: 'WhatsApp',
      icon: <MessageCircle size={24} />,
      color: 'bg-[#25D366] text-white',
      url: `https://wa.me/?text=${encodeURIComponent(title + ' ' + url)}`
    },
    {
      name: 'Facebook',
      icon: <Facebook size={24} />,
      color: 'bg-[#1877F2] text-white',
      url: `https://www.facebook.com/sharer/sharer.php?u=${encodeURIComponent(url)}`
    },
    {
      name: 'Instagram',
      icon: <Instagram size={24} />,
      color: 'bg-gradient-to-tr from-[#F58529] via-[#DD2A7B] to-[#8134AF] text-white',
      onClick: () => {
        navigator.clipboard.writeText(url);
        toast.success("Link copied! Share it on your Instagram.");
      }
    },
    {
      name: 'X (Twitter)',
      icon: <Twitter size={24} />,
      color: 'bg-black text-white',
      url: `https://twitter.com/intent/tweet?url=${encodeURIComponent(url)}&text=${encodeURIComponent(title)}`
    },
    {
      name: 'Threads',
      icon: <MessagesSquare size={24} />,
      color: 'bg-black text-white',
      url: `https://threads.net/intent/post?text=${encodeURIComponent(url)}`
    },
    {
      name: 'Email',
      icon: <Mail size={24} />,
      color: 'bg-gray-600 text-white',
      url: `mailto:?subject=${encodeURIComponent(title)}&body=${encodeURIComponent(url)}`
    }
  ];

  const handleCopy = () => {
    navigator.clipboard.writeText(url);
    setCopied(true);
    toast.success("Link copied to clipboard!");
    setTimeout(() => setCopied(false), 2000);
  };

  const handleShare = (option) => {
    if (option.url) {
      window.open(option.url, '_blank', 'width=600,height=400');
    } else if (option.onClick) {
      option.onClick();
    }
  };

  return (
    <div className="fixed inset-0 bg-black/60 backdrop-blur-md flex items-center justify-center z-[110] animate-in fade-in duration-300">
      <div 
        className="bg-white rounded-[2rem] p-8 max-w-lg w-full shadow-2xl relative animate-in zoom-in-95 slide-in-from-bottom-10 duration-500 overflow-hidden"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="absolute top-0 left-0 w-full h-1.5 bg-gradient-to-r from-brand-accent via-purple-500 to-orange-400"></div>
        
        <button
          onClick={onClose}
          className="absolute top-6 right-6 p-2 text-gray-400 hover:text-brand-dark hover:bg-gray-100 rounded-full transition-all group"
        >
          <X size={24} className="group-hover:rotate-90 transition-transform duration-300" />
        </button>

        <div className="space-y-8 mt-4">
          <div className="text-center space-y-2">
            <h3 className="text-3xl font-black text-brand-dark tracking-tight leading-none">Share your link</h3>
            <p className="text-gray-500 font-medium">Capture your audience everywhere.</p>
          </div>

          <div className="grid grid-cols-3 gap-6 py-4">
            {shareOptions.map((option) => (
              <button
                key={option.name}
                onClick={() => handleShare(option)}
                className="flex flex-col items-center gap-3 transition-all group"
              >
                <div className={`${option.color} p-5 rounded-2xl shadow-lg shadow-black/10 group-hover:scale-110 group-hover:-translate-y-1 group-active:scale-95 transition-all duration-300`}>
                  {option.icon}
                </div>
                <span className="text-[10px] font-black text-gray-400 uppercase tracking-widest group-hover:text-brand-dark transition-colors">
                  {option.name}
                </span>
              </button>
            ))}
          </div>

          <div className="space-y-5">
             <div className="relative group/input">
                <input 
                  type="text" 
                  readOnly 
                  value={url}
                  className="w-full bg-gray-50 border-2 border-gray-100 rounded-2xl pl-6 pr-28 py-5 text-sm font-bold text-gray-700 outline-none focus:border-brand-accent/30 transition-all cursor-default"
                />
                <button
                  onClick={handleCopy}
                  className="absolute right-2 top-1/2 -translate-y-1/2 bg-brand-dark text-white px-6 py-3 rounded-xl text-xs font-bold hover:bg-brand-accent shadow-lg shadow-black/10 transition-all flex items-center gap-2 group-active/input:scale-95"
                >
                  {copied ? (
                    <>
                      <CheckCircle2 size={16} className="text-green-400" />
                      COPIED
                    </>
                  ) : (
                    <>
                      <Copy size={16} />
                      COPY
                    </>
                  )}
                </button>
             </div>
             <div className="flex items-center justify-center gap-4 text-gray-300">
                <div className="h-px flex-1 bg-gray-100"></div>
                <p className="text-[9px] font-bold uppercase tracking-[0.3em] whitespace-nowrap">Global Reach</p>
                <div className="h-px flex-1 bg-gray-100"></div>
             </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ShareModal;
