import React, { useState, useRef, useEffect } from 'react';
import { Bell, Check, Trash2, Clock, AlertCircle } from 'lucide-react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '../services/api';
import { formatDistanceToNow } from 'date-fns';
import toast from 'react-hot-toast';

const NotificationBell = () => {
  const [isOpen, setIsOpen] = useState(false);
  const dropdownRef = useRef(null);
  const queryClient = useQueryClient();

  const { data: notificationsData, isLoading } = useQuery({
    queryKey: ['notifications', 'bell'],
    queryFn: () => api.get('/notifications', { params: { size: 10, sort: 'createdAt,desc' } }).then(res => res.data),
    refetchInterval: 30000 // Poll every 30 seconds
  });

  const notifications = notificationsData?.content || [];

  const { data: unreadCount = 0 } = useQuery({
    queryKey: ['notifications', 'unread', 'count'],
    queryFn: () => api.get('/notifications/unread/count').then(res => res.data),
    refetchInterval: 30000
  });

  const markAsReadMutation = useMutation({
    mutationFn: (id) => api.put(`/notifications/${id}/read`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications'] });
    }
  });

  const markAllAsReadMutation = useMutation({
    mutationFn: () => api.put('/notifications/read-all'),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications'] });
      toast.success('All notifications marked as read');
    }
  });

  useEffect(() => {
    const handleClickOutside = (event) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
        setIsOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  return (
    <div className="relative" ref={dropdownRef}>
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="relative p-2.5 text-gray-400 hover:text-brand-accent hover:bg-brand-accent/5 rounded-xl transition-all duration-300 group"
      >
        <Bell size={22} className="group-hover:scale-110 transition-transform" />
        {unreadCount > 0 && (
          <span className="absolute top-1.5 right-1.5 w-4.5 h-4.5 bg-red-500 text-white text-[10px] font-black flex items-center justify-center rounded-full border-2 border-white animate-bounce-suble">
            {unreadCount > 9 ? '9+' : unreadCount}
          </span>
        )}
      </button>

      {isOpen && (
        <div className="absolute right-0 mt-3 w-80 bg-white rounded-2xl shadow-[0_20px_50px_rgba(0,0,0,0.1)] border border-gray-100 overflow-hidden z-50 animate-in fade-in slide-in-from-top-2 duration-200">
          <div className="px-5 py-4 border-b border-gray-50 flex items-center justify-between bg-gray-50/30">
            <h3 className="font-bold text-brand-dark flex items-center gap-2">
              Notifications
              {unreadCount > 0 && <span className="px-2 py-0.5 bg-brand-accent/10 text-brand-accent text-[10px] rounded-full uppercase tracking-wider">{unreadCount} New</span>}
            </h3>
            {notifications.length > 0 && (
              <button 
                onClick={() => markAllAsReadMutation.mutate()}
                className="text-[10px] font-black text-brand-accent hover:text-brand-dark uppercase tracking-widest transition-colors"
                disabled={unreadCount === 0}
              >
                Mark all as read
              </button>
            )}
          </div>

          <div className="max-h-[380px] overflow-y-auto overscroll-contain">
            {isLoading ? (
              <div className="p-10 text-center space-y-3">
                <div className="w-8 h-8 border-2 border-brand-accent border-t-transparent rounded-full animate-spin mx-auto"></div>
                <p className="text-xs text-gray-400 font-medium tracking-wide">Syncing alerts...</p>
              </div>
            ) : notifications.length === 0 ? (
              <div className="py-12 px-6 text-center space-y-4">
                <div className="w-16 h-16 bg-gray-50 rounded-full flex items-center justify-center mx-auto mb-2">
                  <Bell size={24} className="text-gray-200" />
                </div>
                <p className="text-sm text-gray-400 font-medium">All caught up! No notifications yet.</p>
              </div>
            ) : (
              <div className="divide-y divide-gray-50">
                {notifications.map((n) => (
                  <div 
                    key={n.id} 
                    className={`px-5 py-4 hover:bg-gray-50/80 transition-all cursor-pointer relative group ${!n.read ? 'bg-brand-accent/[0.02]' : ''}`}
                    onClick={() => !n.read && markAsReadMutation.mutate(n.id)}
                  >
                    <div className="flex gap-4">
                      <div className={`mt-1 h-8 w-8 rounded-lg flex items-center justify-center shrink-0 ${
                        n.type === 'EXPIRY' ? 'bg-orange-50 text-orange-500' : 'bg-brand-accent/10 text-brand-accent'
                      }`}>
                        {n.type === 'EXPIRY' ? <Clock size={16} /> : <AlertCircle size={16} />}
                      </div>
                      <div className="flex-1 min-w-0">
                        <p className={`text-xs leading-relaxed ${!n.read ? 'text-brand-dark font-bold' : 'text-gray-500'}`}>
                          {n.message}
                        </p>
                        <span className="text-[10px] text-gray-400 mt-2 block font-medium">
                          {formatDistanceToNow(new Date(n.createdAt), { addSuffix: true })}
                        </span>
                      </div>
                      {!n.read && (
                        <div className="w-2 h-2 bg-brand-accent rounded-full mt-2 ring-4 ring-brand-accent/10"></div>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
};

export default NotificationBell;
