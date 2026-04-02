import React from 'react';
import { NavLink } from 'react-router-dom';
import { 
  BarChart3, 
  Link as LinkIcon, 
  PlusCircle, 
  LayoutDashboard, 
  QrCode,
  LogOut,
  Settings,
  CreditCard,
  User,
  Banknote,
  Activity,
  Bell,
  X,
  History
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';

const Sidebar = ({ isOpen, onClose }) => {
  const { logout, isAdmin } = useAuth();

  const userNavItems = [
    { to: '/dashboard', icon: <LayoutDashboard size={20} />, label: 'Home' },
    { to: '/links', icon: <LinkIcon size={20} />, label: 'My Links' },
    { to: '/qr-codes', icon: <QrCode size={20} />, label: 'QR Codes' },
    { to: '/analytics', icon: <BarChart3 size={20} />, label: 'Analytics' },
    { to: '/pricing', icon: <CreditCard size={20} />, label: 'Pricing' },
    { to: '/profile', icon: <Settings size={20} />, label: 'Profile' },
  ];

  const adminNavItems = [
    { to: '/dashboard', icon: <LayoutDashboard size={20} />, label: 'Dashboard' },
    { to: '/admin/links', icon: <LinkIcon size={20} />, label: 'All Links' },
    { to: '/admin/reports', icon: <Activity size={20} />, label: 'Reports' },
    { to: '/profile', icon: <Settings size={20} />, label: 'Profile' },
    { to: '/admin/users', icon: <User size={20} />, label: 'User Management' },
    { to: '/finance', icon: <Banknote size={20} />, label: 'Finance' },
    { to: '/admin/notifications', icon: <Bell size={20} />, label: 'System Alerts' },
    { to: '/admin/audit-logs', icon: <History size={20} />, label: 'Audit Logs' },
  ];

  const navItems = isAdmin ? adminNavItems : userNavItems;

  return (
    <>
      {/* Mobile Overlay */}
      {isOpen && (
        <div 
          className="fixed inset-0 bg-black/50 z-40 lg:hidden"
          onClick={onClose}
        />
      )}
      
      <div className={`w-64 bg-brand-dark text-white h-screen flex flex-col fixed left-0 top-0 z-50 transition-transform duration-300 ${isOpen ? 'translate-x-0' : '-translate-x-full'}`}>
        <div className="p-6 flex justify-between items-center">
          <div className="flex items-center gap-2">
            <LinkIcon className="text-brand-accent" size={28} />
            <span className="text-xl font-black tracking-tight">Shortln</span>
          </div>
          <button onClick={onClose} className="text-gray-400 hover:text-white transition-colors duration-200">
            <X size={24} />
          </button>
        </div>

        <nav className="flex-1 px-4 py-4 space-y-2">
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              onClick={() => { if (window.innerWidth < 1024) onClose(); }}
              className={({ isActive }) =>
                `flex items-center gap-3 px-4 py-3 rounded-lg transition-all ${
                  isActive
                    ? 'bg-brand-accent text-white shadow-lg shadow-brand-accent/20'
                    : 'text-gray-400 hover:bg-white/5 hover:text-white'
                }`
              }
            >
              {item.icon}
              <span className="font-medium">{item.label}</span>
            </NavLink>
          ))}
        </nav>

        <div className="p-4 border-t border-white/10 space-y-2">
          <button
            onClick={logout}
            className="flex items-center gap-3 w-full px-4 py-3 text-red-400 hover:bg-red-500/10 rounded-lg transition-all"
          >
            <LogOut size={20} />
            <span className="font-medium">Logout</span>
          </button>
        </div>
      </div>
    </>
  );
};

export default Sidebar;
