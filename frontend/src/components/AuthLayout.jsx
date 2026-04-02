import React from 'react';
import { Link } from 'react-router-dom';
import { Link as LinkIcon, Globe, Zap, QrCode, ShieldCheck, ArrowLeft } from 'lucide-react';

const AuthLayout = ({ children, title, subtitle, showBackButton }) => {
  return (
    <div className="min-h-screen grid grid-cols-1 lg:grid-cols-2 bg-white overflow-hidden">
      {/* Left Panel: Animated Design */}
      <div className="hidden lg:flex relative bg-[#0b1629] flex-col items-center justify-center p-12 overflow-hidden">
        {/* Background Decorative Elements */}
        <div className="absolute top-0 left-0 w-full h-full overflow-hidden pointer-events-none">
          <div className="absolute -top-1/4 -left-1/4 w-1/2 h-1/2 bg-[#3d5afe]/20 rounded-full blur-[120px]" />
          <div className="absolute -bottom-1/4 -right-1/4 w-1/2 h-1/2 bg-purple-600/10 rounded-full blur-[120px]" />
        </div>

        {/* Animated Link Connection Visual (Simplified) */}
        <div className="relative w-full max-w-lg aspect-square flex items-center justify-center scale-90">
          <svg width="100%" height="100%" viewBox="0 0 400 400" className="opacity-40">
            <path d="M200,200 Q100,50 20,150" stroke="#3d5afe" strokeWidth="2" fill="none" strokeDasharray="5 5">
              <animate attributeName="stroke-dashoffset" from="50" to="0" dur="5s" repeatCount="indefinite" />
            </path>
            <path d="M200,200 Q300,50 380,150" stroke="#3d5afe" strokeWidth="2" fill="none" strokeDasharray="5 5">
              <animate attributeName="stroke-dashoffset" from="50" to="0" dur="7s" repeatCount="indefinite" />
            </path>
            <path d="M200,200 Q100,350 20,250" stroke="#3d5afe" strokeWidth="2" fill="none" strokeDasharray="5 5">
              <animate attributeName="stroke-dashoffset" from="50" to="0" dur="6s" repeatCount="indefinite" />
            </path>
            <path d="M200,200 Q300,350 380,250" stroke="#3d5afe" strokeWidth="2" fill="none" strokeDasharray="5 5">
              <animate attributeName="stroke-dashoffset" from="50" to="0" dur="8s" repeatCount="indefinite" />
            </path>
          </svg>

          {/* Central Icon */}
          <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2">
            <div className="w-24 h-24 bg-[#3d5afe] rounded-3xl shadow-2xl shadow-[#3d5afe]/40 flex items-center justify-center text-white transform hover:scale-110 transition-transform duration-500">
              <LinkIcon size={48} />
            </div>
            <div className="absolute -inset-4 bg-[#3d5afe]/20 rounded-[2.5rem] animate-ping opacity-30" />
          </div>

          {/* Satellite Icons */}
          <div className="absolute top-[15%] left-[15%] animate-float">
             <Globe className="text-blue-400/40" size={32} />
          </div>
          <div className="absolute top-[20%] right-[15%] animate-float-delayed">
             <Zap className="text-purple-400/40" size={28} />
          </div>
          <div className="absolute bottom-[20%] left-[20%] animate-float-delayed">
             <QrCode className="text-[#3d5afe]/40" size={36} />
          </div>
          <div className="absolute bottom-[15%] right-[20%] animate-float">
             <ShieldCheck className="text-white/40" size={30} />
          </div>
        </div>

        {/* Content */}
        <div className="relative z-10 text-center mt-12 space-y-4 max-w-sm">
          <Link to="/" className="inline-flex items-center gap-2 mb-8 group">
            <LinkIcon className="text-[#3d5afe] group-hover:rotate-12 transition-transform" size={24} />
            <span className="text-xl font-black text-white tracking-tight">Shortln</span>
          </Link>
          <h1 className="text-3xl font-bold text-white leading-tight">{title}</h1>
          <p className="text-gray-400 text-lg leading-relaxed">{subtitle}</p>
        </div>
      </div>

      {/* Right Panel: Form */}
      <div className="flex items-center justify-center p-8 lg:p-12 bg-white relative">
        {showBackButton && (
          <Link 
            to="/" 
            className="absolute top-8 left-8 flex items-center gap-2 text-gray-500 hover:text-[#3d5afe] transition-colors group z-20"
          >
            <ArrowLeft size={20} className="group-hover:-translate-x-1 transition-transform" />
            <span className="font-medium">Back to Home</span>
          </Link>
        )}

        {/* Mobile Header (hidden if back button is shown) */}
        {!showBackButton && (
          <div className="lg:hidden absolute top-8 left-8">
             <Link to="/" className="flex items-center gap-2">
              <LinkIcon className="text-[#3d5afe]" size={24} />
              <span className="text-xl font-black text-[#0b1629] tracking-tight">Shortln</span>
            </Link>
          </div>
        )}

        <div className="w-full max-w-md animate-in fade-in slide-in-from-bottom-4 duration-700">
          {children}
        </div>
      </div>
    </div>
  );
};

export default AuthLayout;
