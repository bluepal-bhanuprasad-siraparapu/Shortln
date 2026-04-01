import React from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '../services/api';
import { 
  Users, 
  FileText, 
  CheckCircle, 
  XCircle,
  Activity,
  UserCheck,
  UserMinus,
  Star,
  Zap
} from 'lucide-react';
import ExportControls from '../components/ExportControls';
import { downloadCSV, downloadPDF } from '../utils/exportUtils';

const UserManagement = () => {
  const queryClient = useQueryClient();
  const [planFilter, setPlanFilter] = React.useState('ALL');

  const { data: users, isLoading } = useQuery({
    queryKey: ['admin', 'users'],
    queryFn: () => api.get('/user/admin/all').then(res => res.data)
  });

  const statusMutation = useMutation({
    mutationFn: ({ id, active }) => api.put(`/user/admin/${id}/status?active=${active}`),
    onSuccess: () => {
      queryClient.invalidateQueries(['admin', 'users']);
    }
  });

  const handleExport = (format) => {
    if (!filteredUsers || filteredUsers.length === 0) return;

    const headers = ["Name", "Email", "Role", "Plan", "Status"];
    const rows = filteredUsers.map(u => [
      u.name,
      u.email,
      u.role || 'User',
      u.plan || 'FREE',
      u.active === 1 ? 'Active' : 'Inactive'
    ]);

    const filename = `users-report-${planFilter.toLowerCase()}-${new Date().getTime()}`;

    if (format === 'PDF') {
      downloadPDF('User Management Report', headers, rows, filename);
    } else {
      downloadCSV(rows, headers, filename);
    }
  };

  const filteredUsers = users?.filter(u => {
    if (planFilter === 'ALL') return true;
    return (u.plan || 'FREE').toUpperCase() === planFilter;
  });

  const toggleStatus = (id, currentStatus) => {
    const newStatus = currentStatus === 1 ? 0 : 1;
    statusMutation.mutate({ id, active: newStatus });
  };

  if (isLoading) return <div className="text-center py-20">Loading platform users...</div>;

  return (
    <div className="space-y-8">
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
        <div>
          <h2 className="text-3xl font-black text-brand-dark tracking-tight">User Management</h2>
          <p className="text-gray-500 font-medium">Monitor and manage all platform users and their access.</p>
        </div>
        
        <div className="flex items-center gap-3 w-full md:w-auto">
          {/* Plan Filter dropdown/selection */}
          <div className="flex bg-gray-50 p-1 rounded-xl border border-gray-100">
            {['ALL', 'PRO', 'FREE'].map((p) => (
              <button
                key={p}
                onClick={() => setPlanFilter(p)}
                className={`px-4 py-2 rounded-lg text-xs font-bold transition-all ${
                  planFilter === p 
                    ? 'bg-white text-brand-accent shadow-sm' 
                    : 'text-gray-500 hover:text-brand-dark'
                }`}
              >
                {p}
              </button>
            ))}
          </div>

          <ExportControls 
            onExport={handleExport}
            disabled={!filteredUsers || filteredUsers.length === 0}
            label="Export Report"
          />
        </div>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-4">
        <div className="bg-white p-5 rounded-2xl shadow-sm border border-gray-100 flex items-center gap-4">
          <div className="p-3 bg-blue-50 text-blue-600 rounded-xl">
             <Users size={20} />
          </div>
          <div>
             <p className="text-[10px] font-bold text-gray-400 uppercase tracking-wider">Total</p>
             <p className="text-xl font-black text-brand-dark">{users?.length || 0}</p>
          </div>
        </div>
        <div className="bg-white p-5 rounded-2xl shadow-sm border border-gray-100 flex items-center gap-4">
          <div className="p-3 bg-green-50 text-green-600 rounded-xl">
             <UserCheck size={20} />
          </div>
          <div>
             <p className="text-[10px] font-bold text-gray-400 uppercase tracking-wider">Active</p>
             <p className="text-xl font-black text-brand-dark">{users?.filter(u => u.active === 1).length || 0}</p>
          </div>
        </div>
        <div className="bg-white p-5 rounded-2xl shadow-sm border border-gray-100 flex items-center gap-4">
          <div className="p-3 bg-red-50 text-red-600 rounded-xl">
             <UserMinus size={20} />
          </div>
          <div>
             <p className="text-[10px] font-bold text-gray-400 uppercase tracking-wider">Inactive</p>
             <p className="text-xl font-black text-brand-dark">{users?.filter(u => u.active === 0).length || 0}</p>
          </div>
        </div>
        <div className="bg-white p-5 rounded-2xl shadow-sm border border-gray-100 flex items-center gap-4">
          <div className="p-3 bg-purple-50 text-purple-600 rounded-xl">
             <Star size={20} />
          </div>
          <div>
             <p className="text-[10px] font-bold text-gray-400 uppercase tracking-wider">PRO</p>
             <p className="text-xl font-black text-brand-dark">
               {users?.filter(u => (u.plan || 'FREE').toUpperCase() === 'PRO').length || 0}
             </p>
          </div>
        </div>
        <div className="bg-white p-5 rounded-2xl shadow-sm border border-gray-100 flex items-center gap-4">
          <div className="p-3 bg-gray-50 text-gray-500 rounded-xl">
             <Zap size={20} />
          </div>
          <div>
             <p className="text-[10px] font-bold text-gray-400 uppercase tracking-wider">FREE</p>
             <p className="text-xl font-black text-brand-dark">
               {users?.filter(u => (u.plan || 'FREE').toUpperCase() === 'FREE').length || 0}
             </p>
          </div>
        </div>
      </div>

      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left">
            <thead>
              <tr className="bg-gray-50 border-b border-gray-100">
                <th className="px-6 py-4 font-bold text-xs text-gray-500 uppercase">User Info</th>
                <th className="px-6 py-4 font-bold text-xs text-gray-500 uppercase">Role / Plan</th>
                <th className="px-6 py-4 font-bold text-xs text-gray-500 uppercase text-center">Status</th>
                <th className="px-6 py-4 font-bold text-xs text-gray-500 uppercase text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-50">
               {filteredUsers?.map((u) => (
                <tr key={u.id} className="hover:bg-gray-50/50 transition-colors group">
                  <td className="px-6 py-5">
                    <div className="flex flex-col">
                      <span className="font-bold text-brand-dark">{u.name}</span>
                      <span className="text-xs text-gray-400 font-medium">{u.email}</span>
                    </div>
                  </td>
                  <td className="px-6 py-5">
                    <div className="flex flex-col gap-1">
                      <span className="text-[10px] font-black px-2 py-0.5 bg-gray-100 text-gray-600 rounded-md uppercase w-fit tracking-wider">
                        {u.role || 'User'}
                      </span>
                      <span className={`text-[10px] font-black px-2 py-0.5 rounded-md uppercase w-fit tracking-wider ${
                        (u.plan || 'FREE').toUpperCase() === 'PRO' 
                          ? 'bg-purple-100 text-purple-700' 
                          : 'bg-blue-100 text-blue-700'
                      }`}>
                        {u.plan || 'FREE'}
                      </span>
                    </div>
                  </td>
                  <td className="px-6 py-5 text-center">
                    {u.active === 1 ? (
                      <span className="inline-flex items-center gap-1.5 text-[10px] font-black text-green-600 bg-green-50 px-3 py-1.5 rounded-lg border border-green-100/50 uppercase tracking-widest">
                        <div className="w-1.5 h-1.5 bg-green-600 rounded-full animate-pulse" />
                        Active
                      </span>
                    ) : (
                      <span className="inline-flex items-center gap-1.5 text-[10px] font-black text-red-600 bg-red-50 px-3 py-1.5 rounded-lg border border-red-100/50 uppercase tracking-widest">
                        <div className="w-1.5 h-1.5 bg-red-600 rounded-full" />
                        Inactive
                      </span>
                    )}
                  </td>
                  <td className="px-6 py-5 text-right">
                    <button 
                      onClick={() => toggleStatus(u.id, u.active)}
                      disabled={statusMutation.isPending}
                      className="text-[10px] font-black px-4 py-2 rounded-xl transition-all uppercase tracking-widest text-brand-accent hover:bg-brand-accent/10 border border-brand-accent/20"
                    >
                      {u.active === 1 ? 'Deactivate' : 'Activate'}
                    </button>
                  </td>
                </tr>
              ))}
              {filteredUsers?.length === 0 && (
                <tr>
                  <td colSpan="4" className="py-24 text-center">
                    <div className="flex flex-col items-center justify-center space-y-3">
                      <div className="p-4 bg-gray-50 rounded-full text-gray-300">
                         <Users size={40} />
                      </div>
                      <div className="space-y-1">
                        <p className="text-gray-900 font-bold">No users match this filter</p>
                        <p className="text-gray-400 text-sm">Try selecting a different user plan.</p>
                      </div>
                    </div>
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};

export default UserManagement;
