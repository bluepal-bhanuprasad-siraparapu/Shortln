import React from 'react';
import { Link } from 'react-router-dom';
import { 
  Link as LinkIcon, 
  BarChart3, 
  QrCode, 
  ShieldCheck, 
  Zap, 
  Globe,
  ArrowRight
} from 'lucide-react';

const FeatureCard = ({ icon, title, description }) => (
  <div className="bg-white p-8 rounded-2xl shadow-sm border border-gray-100 hover:shadow-md transition-shadow">
    <div className="w-12 h-12 bg-brand-accent/10 text-brand-accent rounded-xl flex items-center justify-center mb-6">
      {icon}
    </div>
    <h3 className="text-xl font-bold text-brand-dark mb-3">{title}</h3>
    <p className="text-gray-500 leading-relaxed text-sm">{description}</p>
  </div>
);

const HeroVisual = () => (
  <div className="relative lg:block hidden animate-in fade-in slide-in-from-right-8 duration-700 delay-200">
    {/* Decorative background blobs */}
    <div className="absolute -top-20 -right-20 w-96 h-96 bg-brand-accent/10 rounded-full blur-3xl opacity-50" />
    <div className="absolute -bottom-20 -left-20 w-72 h-72 bg-purple-400/10 rounded-full blur-3xl opacity-50" />
    
    <div className="relative h-[600px] w-full flex items-center justify-center overflow-visible">
      {/* Connection Lines Container */}
      <div className="absolute inset-0 z-0">
        <svg width="100%" height="100%" viewBox="0 0 600 600" className="opacity-40">
          <defs>
            <linearGradient id="line-grad" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" stopColor="#3d5afe" stopOpacity="0" />
              <stop offset="50%" stopColor="#3d5afe" stopOpacity="0.5" />
              <stop offset="100%" stopColor="#3d5afe" stopOpacity="0" />
            </linearGradient>
          </defs>
          
          <path d="M300,300 Q150,100 50,250" stroke="url(#line-grad)" strokeWidth="2" fill="none" strokeDasharray="10 10">
            <animate attributeName="stroke-dashoffset" from="100" to="0" dur="10s" repeatCount="indefinite" />
          </path>
          <path d="M300,300 Q450,100 550,250" stroke="url(#line-grad)" strokeWidth="2" fill="none" strokeDasharray="10 10">
            <animate attributeName="stroke-dashoffset" from="100" to="0" dur="12s" repeatCount="indefinite" />
          </path>
          <path d="M300,300 Q150,500 50,350" stroke="url(#line-grad)" strokeWidth="2" fill="none" strokeDasharray="10 10">
            <animate attributeName="stroke-dashoffset" from="100" to="0" dur="8s" repeatCount="indefinite" />
          </path>
          <path d="M300,300 Q450,500 550,350" stroke="url(#line-grad)" strokeWidth="2" fill="none" strokeDasharray="10 10">
            <animate attributeName="stroke-dashoffset" from="100" to="0" dur="15s" repeatCount="indefinite" />
          </path>
        </svg>
      </div>

      {/* Floating Elements */}
      <div className="relative z-10 w-full h-full">
        {/* Central Node */}
        <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2">
          <div className="w-32 h-32 bg-white rounded-[2.5rem] shadow-2xl flex items-center justify-center border border-gray-100 transform hover:scale-110 transition-transform duration-500 group cursor-default">
            <div className="w-20 h-20 bg-brand-accent/10 rounded-3xl flex items-center justify-center text-brand-accent group-hover:bg-brand-accent group-hover:text-white transition-all duration-500">
              <LinkIcon size={40} />
            </div>
            <div className="absolute -inset-4 bg-brand-accent/5 rounded-[3rem] animate-ping opacity-20" />
          </div>
        </div>

        {/* Satellite Nodes */}
        <div className="absolute top-[10%] left-[20%] animate-float">
          <div className="p-4 bg-white rounded-2xl shadow-lg border border-gray-100 flex items-center gap-3 transform -rotate-12">
            <div className="w-8 h-8 bg-blue-50 rounded-lg flex items-center justify-center text-blue-500">
              <Globe size={18} />
            </div>
            <div className="h-2 w-16 bg-gray-100 rounded-full" />
          </div>
        </div>

        <div className="absolute top-[15%] right-[15%] animate-float-delayed">
          <div className="p-4 bg-white rounded-2xl shadow-lg border border-gray-100 flex items-center gap-3 transform rotate-6">
            <div className="w-8 h-8 bg-purple-50 rounded-lg flex items-center justify-center text-purple-500">
              <Zap size={18} />
            </div>
            <div className="h-2 w-20 bg-gray-100 rounded-full" />
          </div>
        </div>

        <div className="absolute bottom-[20%] left-[15%] animate-float">
          <div className="p-4 bg-white rounded-2xl shadow-lg border border-gray-100 flex items-center gap-3 transform rotate-12">
            <div className="w-8 h-8 bg-green-50 rounded-lg flex items-center justify-center text-green-500">
              <QrCode size={18} />
            </div>
            <div className="h-2 w-12 bg-gray-100 rounded-full" />
          </div>
        </div>

        <div className="absolute bottom-[10%] right-[20%] animate-float-delayed">
          <div className="p-4 bg-white rounded-2xl shadow-lg border border-gray-100 flex items-center gap-3 transform -rotate-6">
            <div className="w-8 h-8 bg-orange-50 rounded-lg flex items-center justify-center text-orange-500">
              <ShieldCheck size={18} />
            </div>
            <div className="h-2 w-24 bg-gray-100 rounded-full" />
          </div>
        </div>
      </div>
    </div>
  </div>
);

const Home = () => {
  return (
    <div className="min-h-screen bg-[#f8f9fa] selection:bg-brand-accent selection:text-white flex flex-col overflow-x-hidden">
      {/* Navigation */}
      <nav className="max-w-7xl mx-auto px-6 py-6 flex items-center justify-between w-full relative z-50">
        <div className="flex items-center gap-2">
          <LinkIcon className="text-brand-accent" size={32} />
          <span className="text-2xl font-black text-brand-dark tracking-tight">ShortenIt</span>
        </div>
        <div className="flex items-center gap-4">
          <Link 
            to="/login" 
            className="px-6 py-2.5 bg-brand-dark text-white font-bold rounded-full hover:bg-gray-800 transition-all shadow-lg shadow-brand-dark/10"
          >
            Log in
          </Link>
        </div>
      </nav>

      {/* Hero Section */}
      <section className="max-w-7xl mx-auto px-6 flex-1 grid grid-cols-1 lg:grid-cols-2 gap-12 items-center w-full pb-10">
        <div className="space-y-6 lg:space-y-8 animate-in fade-in slide-in-from-left-8 duration-700">
          <h1 className="text-5xl lg:text-7xl font-black text-brand-dark leading-[1.05] tracking-tight">
            Build closer <br />
            <span className="text-brand-accent text-glow">connections</span> with <br />
            every link.
          </h1>
          <p className="text-lg lg:text-xl text-gray-500 max-w-lg leading-relaxed">
            Create short links, share them anywhere, and track how they perform. Our platform gives you more than just a short URL.
          </p>
          <div className="flex flex-col sm:flex-row gap-4 pt-2 lg:pt-4">
            <Link 
              to="/register" 
              className="px-8 lg:px-10 py-4 lg:py-5 bg-brand-accent text-white font-bold rounded-2xl hover:bg-blue-700 transition-all shadow-xl shadow-brand-accent/20 flex items-center justify-center gap-2 text-lg group"
            >
              Get Started for Free <ArrowRight className="group-hover:translate-x-1 transition-transform" />
            </Link>
            <Link 
              to="/login" 
              className="px-8 lg:px-10 py-4 lg:py-5 bg-white text-brand-dark font-bold rounded-2xl border border-gray-200 hover:bg-gray-50 transition-all flex items-center justify-center text-lg"
            >
              View Dashboard
            </Link>
          </div>
        </div>

        <HeroVisual />
      </section>

      {/* Features Grid */}
      <section className="bg-white py-32">
        <div className="max-w-7xl mx-auto px-6">
          <div className="text-center max-w-3xl mx-auto mb-20">
            <h2 className="text-4xl font-black text-brand-dark mb-6">Everything you need to grow.</h2>
            <p className="text-lg text-gray-500">Powerful features to help you create, manage, and understand your links in one unified platform.</p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
            <FeatureCard 
              icon={<LinkIcon />}
              title="Branded Links"
              description="Customize your links with your own alias to build trust and increase click-through rates by up to 34%."
            />
            <FeatureCard 
              icon={<BarChart3 />}
              title="Advanced Analytics"
              description="Track clicks, geographic data, and device types in real-time. Understand exactly who is engaging with your content."
            />
            <FeatureCard 
              icon={<QrCode />}
              title="QR Code Generator"
              description="Download high-quality QR codes for your short links instantly. Perfect for print and outdoor marketing."
            />
            <FeatureCard 
              icon={<ShieldCheck />}
              title="Secure Links"
              description="Set expiry dates for your links to keep them private or temporary. Automated DDOS protection for every URL."
            />
            <FeatureCard 
              icon={<Globe />}
              title="Global Reach"
              description="Our infrastructure is built on a global edge network, ensuring your redirects are lightning fast anywhere in the world."
            />
            <FeatureCard 
              icon={<Zap />}
              title="API Access"
              description="Integrate our shortening service directly into your workflow with our developer-friendly REST API."
            />
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="bg-brand-dark text-white py-20">
        <div className="max-w-7xl mx-auto px-6 flex flex-col items-center text-center">
          <div className="flex items-center gap-2 mb-8">
            <LinkIcon className="text-brand-accent" size={40} />
            <span className="text-3xl font-black tracking-tight">ShortenIt</span>
          </div>
          <p className="text-gray-400 max-w-lg mb-12 leading-relaxed">
            The world's most advanced link management platform. Trusted by thousands of creators and businesses worldwide.
          </p>
          <div className="flex gap-8 mb-12">
            <a href="#" className="text-gray-400 hover:text-white transition-colors">Twitter</a>
            <a href="#" className="text-gray-400 hover:text-white transition-colors">LinkedIn</a>
            <a href="#" className="text-gray-400 hover:text-white transition-colors">GitHub</a>
          </div>
          <div className="text-sm text-gray-500 border-t border-white/5 pt-12 w-full">
            © 2026 ShortenIt Inc. All rights reserved. Built with precision.
          </div>
        </div>
      </footer>
    </div>
  );
};

export default Home;
