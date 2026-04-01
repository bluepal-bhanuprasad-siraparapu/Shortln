import React, { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import api from '../services/api';
import { useAuth } from '../context/AuthContext';
import { Link2, Copy, Download, CheckCircle, Calendar, Tag, Type, Lock, AlertCircle, Rocket, Sparkles, Save, Edit2 } from 'lucide-react';
import { Link, useParams, useNavigate } from 'react-router-dom';

const CreateLink = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const isEditMode = !!id;
  
  const [originalUrl, setOriginalUrl] = useState('');
  const [title, setTitle] = useState('');
  const [customAlias, setCustomAlias] = useState('');
  const [expiresAt, setExpiresAt] = useState('');
  const [createdLink, setCreatedLink] = useState(null);
  const [copied, setCopied] = useState(false);
  const queryClient = useQueryClient();
  const { user } = useAuth();

  // Fetch link data if in edit mode
  const { data: linkToEdit, isLoading: isLoadingLink } = useQuery({
    queryKey: ['link', id],
    queryFn: () => api.get(`/links/${id}`).then(res => res.data),
    enabled: isEditMode
  });

  // Pre-fill form when data is fetched
  React.useEffect(() => {
    if (linkToEdit && isEditMode) {
      setOriginalUrl(linkToEdit.originalUrl);
      setTitle(linkToEdit.title || '');
      setCustomAlias(linkToEdit.customAlias || linkToEdit.shortCode);
      setExpiresAt(linkToEdit.expiresAt ? linkToEdit.expiresAt.split('T')[0] : '');
    }
  }, [linkToEdit, isEditMode]);
  
  const minDate = new Date();
  minDate.setDate(minDate.getDate() + 1);
  const minDateStr = minDate.toISOString().split('T')[0];

  const { data: linksData } = useQuery({
    queryKey: ['links', 'count'],
    queryFn: () => api.get('/links', { params: { size: 1 } }).then(res => res.data),
    enabled: user?.plan === 'FREE'
  });

  const isLimitReached = user?.plan === 'FREE' && (linksData?.totalElements || 0) >= 5;

  const mutation = useMutation({
    mutationFn: (newLink) => {
      if (isEditMode) {
        return api.put(`/links/${id}`, newLink).then(res => res.data);
      }
      return api.post('/links', newLink).then(res => res.data);
    },
    onSuccess: (data) => {
      if (isEditMode) {
        queryClient.invalidateQueries(['links']);
        navigate('/links'); // Redirect to list after update
      } else {
        setCreatedLink(data);
        queryClient.invalidateQueries(['links']);
      }
    }
  });

  const handleSubmit = (e) => {
    e.preventDefault();
    mutation.mutate({ 
      originalUrl, 
      title: title || null,
      customAlias: customAlias || null, 
      expiresAt: expiresAt ? `${expiresAt}T23:59:59` : null 
    });
  };

  const copyToClipboard = () => {
    navigator.clipboard.writeText(createdLink.shortUrl);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const downloadQr = async () => {
    try {
      const response = await api.get(`/links/${createdLink.id}/qr`, { responseType: 'blob' });
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `qr-${createdLink.shortCode}.png`);
      document.body.appendChild(link);
      link.click();
      link.remove();
    } catch (err) {
      console.error('Failed to download QR code', err);
    }
  };

  return (
    <div className="max-w-5xl mx-auto space-y-10 py-6">
      <div className="flex flex-col md:flex-row md:items-end justify-between gap-4">
        <div className="space-y-2">
          <div className="flex items-center gap-3">
             <div className="p-2 bg-brand-accent/10 rounded-lg text-brand-accent">
                {isEditMode ? <Edit2 size={24} /> : <Rocket size={24} />}
             </div>
             <h2 className="text-3xl font-extrabold text-brand-dark tracking-tight">
               {isEditMode ? 'Update Link' : 'Create New Link'}
             </h2>
          </div>
          <p className="text-gray-500 text-lg max-w-xl">
            {isEditMode 
              ? 'Update your existing link details and configuration.' 
              : 'Transform your long URLs into powerful, branded short links in seconds.'}
          </p>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
        <div className="lg:col-span-7 bg-white p-8 md:p-10 rounded-3xl shadow-xl shadow-gray-200/50 border border-gray-100 relative overflow-hidden group">
          <div className="absolute top-0 right-0 w-32 h-32 bg-brand-accent/5 rounded-full -mr-16 -mt-16 transition-transform group-hover:scale-110 duration-700"></div>
          
          <form onSubmit={handleSubmit} className="space-y-8 relative z-10" autoComplete="off">
            <div className="space-y-6">
              <div className="group/input">
                <label className="block text-sm font-bold text-gray-700 mb-2 transition-colors group-focus-within/input:text-brand-accent italic">Original Link</label>
                <div className="relative">
                  <Link2 className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400 group-focus-within/input:text-brand-accent transition-colors" size={20} />
                  <input 
                    type="url" 
                    required
                    placeholder="https://example.com/very-long-url-to-shorten"
                    className="w-full pl-12 pr-4 py-4 bg-gray-50/50 border border-gray-100 rounded-2xl focus:ring-4 focus:ring-brand-accent/5 focus:border-brand-accent focus:bg-white outline-none transition-all text-gray-800 placeholder-gray-400 font-medium shadow-sm"
                    value={originalUrl}
                    autoComplete="off"
                    onChange={(e) => setOriginalUrl(e.target.value)}
                  />
                </div>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div className="group/input">
                  <label className="block text-sm font-bold text-gray-700 mb-2 transition-colors group-focus-within/input:text-brand-accent italic">Link Title</label>
                  <div className="relative">
                    <Type className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400 group-focus-within/input:text-brand-accent transition-colors" size={20} />
                    <input 
                      type="text" 
                      required
                      placeholder="e.g. My Website"
                      className="w-full pl-12 pr-4 py-4 bg-gray-50/50 border border-gray-100 rounded-2xl focus:ring-4 focus:ring-brand-accent/5 focus:border-brand-accent focus:bg-white outline-none transition-all text-gray-800 placeholder-gray-400 font-medium shadow-sm"
                      value={title}
                      autoComplete="off"
                      onChange={(e) => setTitle(e.target.value)}
                    />
                  </div>
                </div>

                <div className="group/input">
                  <label className="block text-sm font-bold text-gray-700 mb-2 transition-colors group-focus-within/input:text-brand-accent italic">Custom Alias</label>
                  <div className="relative">
                    <Tag className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400 group-focus-within/input:text-brand-accent transition-colors" size={20} />
                    <input 
                      type="text" 
                      pattern="[a-zA-Z0-9-]+"
                      title="Only letters, numbers, and hyphens"
                      placeholder="e.g. custom-name"
                      className="w-full pl-12 pr-4 py-4 bg-gray-50/50 border border-gray-100 rounded-2xl focus:ring-4 focus:ring-brand-accent/5 focus:border-brand-accent focus:bg-white outline-none transition-all text-gray-800 placeholder-gray-400 font-medium shadow-sm"
                      value={customAlias}
                      autoComplete="off"
                      onChange={(e) => setCustomAlias(e.target.value)}
                    />
                  </div>
                </div>
              </div>

              <div className="group/input">
                <label className="block text-sm font-bold text-gray-700 mb-2 transition-colors group-focus-within/input:text-brand-accent italic">Expiry Date</label>
                <div className="relative">
                  <Calendar className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400 group-focus-within/input:text-brand-accent transition-colors" size={20} />
                  <input 
                    type="date" 
                    required
                    min={minDateStr}
                    className="w-full pl-12 pr-4 py-4 bg-gray-50/50 border border-gray-100 rounded-2xl focus:ring-4 focus:ring-brand-accent/5 focus:border-brand-accent focus:bg-white outline-none transition-all text-gray-800 font-medium shadow-sm"
                    value={expiresAt}
                    onChange={(e) => setExpiresAt(e.target.value)}
                  />
                </div>
              </div>
            </div>

            {isLimitReached && (
              <div className="bg-red-50 border border-red-100 p-5 rounded-2xl flex items-start gap-4 animate-in shake duration-500">
                <div className="p-2 bg-red-100 rounded-lg text-red-600">
                   <AlertCircle size={20} />
                </div>
                <div>
                  <p className="text-red-800 font-bold mb-1">Limit Reached</p>
                  <p className="text-sm text-red-600 leading-relaxed">
                    You've reached the limit of 5 links on the Free plan. 
                    <Link to="/pricing" className="ml-1 font-bold underline hover:text-red-700 transition-colors">Upgrade to Pro</Link> to create unlimited links.
                  </p>
                </div>
              </div>
            )}

            <button 
              type="submit"
              disabled={mutation.isPending || isLimitReached}
              className="group w-full py-4.5 bg-brand-accent hover:bg-blue-700 text-white font-extrabold rounded-2xl shadow-xl shadow-brand-accent/25 hover:shadow-brand-accent/40 transition-all disabled:opacity-50 disabled:bg-gray-400 disabled:shadow-none active:scale-[0.98] flex items-center justify-center gap-3 text-lg"
            >
              {mutation.isPending ? (
                <>
                  <div className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin"></div>
                  {isEditMode ? 'Updating Link...' : 'Generating Link...'}
                </>
              ) : (
                <>
                  {isEditMode ? <Save size={20} /> : <Sparkles size={20} className="group-hover:rotate-12 transition-transform" />}
                  {isLimitReached ? 'Upgrade Required' : (isEditMode ? 'Save Changes' : 'Generate Short Link')}
                </>
              )}
            </button>
          </form>
        </div>

        <div className="lg:col-span-12 xl:col-span-5 h-full">
          {createdLink ? (
            <div className="bg-white p-10 rounded-3xl shadow-2xl shadow-gray-200 border border-brand-accent/10 h-full flex flex-col items-center justify-center text-center animate-in zoom-in-95 duration-500 relative overflow-hidden">
               <div className="absolute top-0 right-0 w-40 h-40 bg-green-50 rounded-full -mr-20 -mt-20 opacity-50"></div>
               <div className="absolute bottom-0 left-0 w-40 h-40 bg-brand-accent/5 rounded-full -ml-20 -mb-20 opacity-50"></div>

              <div className="h-20 w-20 bg-green-100 text-green-600 rounded-3xl flex items-center justify-center mb-8 rotate-3 shadow-lg shadow-green-100/50">
                <CheckCircle size={40} />
              </div>
              <h3 className="text-2xl font-black text-brand-dark mb-3">Link Ready!</h3>
              <p className="text-gray-500 mb-10 px-6 leading-relaxed">Your brand new short link is ready to be shared with the world.</p>
              
              <div className="w-full bg-gray-50/80 p-5 rounded-2xl border-2 border-dashed border-brand-accent/20 mb-10 flex items-center justify-between group transition-all hover:bg-white hover:border-brand-accent/50">
                <span className="font-bold text-brand-accent text-lg truncate mr-4">{createdLink.shortUrl}</span>
                <button 
                  onClick={copyToClipboard}
                  className="p-3 bg-white shadow-sm border border-gray-100 rounded-xl transition-all text-gray-500 hover:text-brand-accent hover:scale-110 active:scale-95"
                  title="Copy Link"
                >
                  {copied ? <CheckCircle size={24} className="text-green-500" /> : <Copy size={24} />}
                </button>
              </div>

              <div className="flex flex-col sm:flex-row gap-4 w-full relative z-10">
                <button 
                  onClick={downloadQr}
                  className="flex-1 py-4 bg-gray-900 hover:bg-black text-white rounded-2xl font-bold flex items-center justify-center gap-3 transition-all hover:-translate-y-1 shadow-lg shadow-gray-900/10 active:translate-y-0"
                >
                  <Download size={20} /> QR Code
                </button>
                <button 
                  onClick={() => setCreatedLink(null)}
                  className="flex-1 py-4 bg-white border border-gray-200 hover:bg-gray-50 text-gray-700 rounded-2xl font-bold transition-all hover:border-brand-accent/30 active:scale-95"
                >
                  Create New
                </button>
              </div>
            </div>
          ) : (
            <div className="bg-gray-50/50 p-12 rounded-3xl border-2 border-dashed border-gray-200 h-full min-h-[500px] flex flex-col items-center justify-center text-center text-gray-400 group">
              <div className="w-24 h-24 bg-white rounded-3xl flex items-center justify-center mb-8 shadow-sm group-hover:scale-110 transition-transform duration-500">
                <Link2 size={48} className="opacity-20 group-hover:opacity-40 transition-opacity" />
              </div>
              <h4 className="text-xl font-bold text-gray-500 mb-3">Instant Preview</h4>
              <p className="max-w-xs text-sm leading-relaxed">Complete the form to see your generated link and QR code here instantly.</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default CreateLink;
