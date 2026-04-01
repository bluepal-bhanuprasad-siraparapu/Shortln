import { User, Menu } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { useNavigate } from 'react-router-dom';
import NotificationBell from './NotificationBell';

const Navbar = ({ onMenuButtonClick, isSidebarOpen }) => {
  const { user } = useAuth();
  const navigate = useNavigate();

  return (
    <header className={`h-16 bg-white border-b border-gray-100 flex items-center justify-between px-4 lg:px-8 fixed top-0 right-0 z-40 transition-all duration-300 ${isSidebarOpen ? 'lg:left-64' : 'left-0'}`}>
      <div className="flex items-center gap-4 flex-1">
        {!isSidebarOpen && (
          <button 
            onClick={onMenuButtonClick}
            className="p-2 text-gray-500 hover:bg-gray-100 rounded-lg lg:block transition-all duration-300 animate-in fade-in slide-in-from-left-2"
          >
            <Menu size={20} className="transition-transform duration-300" />
          </button>
        )}
      </div>

      <div className="flex items-center gap-6">
        <NotificationBell />
        
        <div 
          onClick={() => navigate('/profile')}
          className="flex items-center gap-3 pl-6 border-l border-gray-100 cursor-pointer group hover:bg-gray-50/50 p-1 rounded-xl transition-all"
        >
          <div className="text-right hidden sm:block">
            <p className="text-sm font-semibold text-brand-dark group-hover:text-brand-accent transition-colors">
              {user?.name || user?.email?.split('@')[0]}
            </p>
            <p className="text-xs text-gray-500 capitalize">{user?.role?.toLowerCase() || 'Member'}</p>
          </div>
          <div className="w-10 h-10 bg-gray-100 rounded-full flex items-center justify-center text-brand-dark group-hover:bg-brand-accent group-hover:text-white transition-all shadow-sm">
            <User size={20} />
          </div>
        </div>
      </div>
    </header>
  );
};

export default Navbar;
