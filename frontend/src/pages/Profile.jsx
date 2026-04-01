import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { Link } from 'react-router-dom';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import api from '../services/api';
import { 
  User as UserIcon, 
  Mail, 
  Shield, 
  CreditCard, 
  CheckCircle,
  AlertCircle,
  Save
} from 'lucide-react';

const Profile = () => {
  const { user, updateUser, isAdmin } = useAuth();
  const [name, setName] = useState(user?.name || '');
  const [isEditing, setIsEditing] = useState(false);
  const queryClient = useQueryClient();

  const updateMutation = useMutation({
    mutationFn: (newName) => api.put(`/user/${user.id}`, { ...user, name: newName }),
    onSuccess: (response) => {
      updateUser(response.data);
      setIsEditing(false);
    }
  });

  const handleUpdate = (e) => {
    e.preventDefault();
    updateMutation.mutate(name);
  };

  return (
    <div className="max-w-4xl mx-auto space-y-8">
      <div>
        <h2 className="text-2xl font-bold text-brand-dark">Account Settings</h2>
        <p className="text-gray-500">Manage your profile information and account preferences.</p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Left: Profile Summary */}
        <div className="lg:col-span-1 space-y-6">
          <div className="bg-white p-8 rounded-2xl shadow-sm border border-gray-100 flex flex-col items-center text-center">
             <div className="w-24 h-24 bg-brand-accent/10 rounded-full flex items-center justify-center text-brand-accent mb-4 border-4 border-brand-accent/5">
                <UserIcon size={40} />
             </div>
             <h3 className="text-xl font-bold text-brand-dark">{user?.name || 'User Name'}</h3>
             <p className="text-sm text-gray-500">{user?.email}</p>
             <div className="mt-4 flex gap-2">
                <span className="px-3 py-1 bg-brand-dark/5 text-brand-dark text-xs font-bold rounded-full uppercase tracking-wider">
                  {user?.role}
                </span>
                <span className="px-3 py-1 bg-brand-accent/10 text-brand-accent text-xs font-bold rounded-full uppercase tracking-wider">
                  {user?.plan} PLAN
                </span>
             </div>
          </div>

          <div className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100 flex items-center gap-4 hover:shadow-md transition-shadow duration-300">
             <div className="p-3 bg-green-50 text-green-600 rounded-xl group-hover:scale-110 transition-transform">
                <CheckCircle size={20} />
             </div>
             <div>
                <p className="text-xs font-bold text-gray-400 uppercase tracking-wider">Status</p>
                <p className="text-sm font-bold text-brand-dark">Account Active</p>
             </div>
          </div>

          {!isAdmin && (
            <div className="bg-white p-8 rounded-2xl shadow-sm border border-gray-100 flex flex-col hover:shadow-md transition-shadow duration-300 group">
              <h3 className="text-lg font-bold text-brand-dark mb-6 flex items-center gap-2">
                <div className="p-2 bg-red-50 rounded-lg">
                  <AlertCircle size={20} className="text-red-600" />
                </div>
                Danger Zone
              </h3>
              <div className="bg-red-50/30 p-6 rounded-xl border border-red-100 flex-grow">
                <p className="text-sm text-red-600/70 mb-6 leading-relaxed font-medium">
                  Deleting your account is permanent and cannot be undone.
                </p>
                <button className="w-full px-6 py-3 bg-brand-accent text-white rounded-xl text-sm font-bold hover:bg-blue-700 hover:-translate-y-0.5 active:translate-y-0 transition-all duration-300 shadow-lg shadow-brand-accent/25">
                  Delete Account
                </button>
              </div>
            </div>
          )}
        </div>

        {/* Right: Detailed Settings */}
        <div className="lg:col-span-2 space-y-6">
          <div className="bg-white p-8 rounded-2xl shadow-sm border border-gray-100">
             <h3 className="text-lg font-bold text-brand-dark mb-6 flex items-center gap-2">
                <Shield size={20} className="text-blue-500" />
                Personal Information
             </h3>
             <form onSubmit={handleUpdate} className="space-y-6">
                <div>
                   <label className="block text-sm font-bold text-brand-dark mb-2">Full Name</label>
                   <div className="relative">
                      <UserIcon className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={18} />
                      <input 
                        type="text" 
                        value={name}
                        onChange={(e) => setName(e.target.value)}
                        className="w-full pl-10 pr-4 py-3 bg-gray-50 border border-gray-200 rounded-xl focus:ring-2 focus:ring-brand-accent focus:border-transparent outline-none transition-all"
                        placeholder="John Doe"
                      />
                   </div>
                </div>

                <div>
                   <label className="block text-sm font-bold text-brand-dark mb-2">Email Address</label>
                   <div className="relative opacity-60">
                      <Mail className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={18} />
                      <input 
                        type="email" 
                        value={user?.email}
                        disabled
                        className="w-full pl-10 pr-4 py-3 bg-gray-100 border border-gray-200 rounded-xl cursor-not-allowed"
                      />
                   </div>
                   <p className="mt-1 text-xs text-gray-400">Email cannot be changed after registration.</p>
                </div>

                 <div className="pt-4">
                    <button 
                      type="submit"
                      disabled={updateMutation.isPending || name === user?.name}
                      className="bg-brand-accent text-white px-8 py-3 rounded-xl font-bold shadow-lg shadow-brand-accent/25 hover:bg-blue-700 hover:-translate-y-0.5 active:translate-y-0 transition-all duration-300 flex items-center gap-2 disabled:opacity-50 disabled:translate-y-0 disabled:shadow-none"
                    >
                      <Save size={18} />
                      {updateMutation.isPending ? 'Updating...' : 'Save Changes'}
                    </button>
                 </div>
             </form>
          </div>

          {!isAdmin && (
            <div className="bg-white p-8 rounded-2xl shadow-sm border border-gray-100 flex flex-col hover:shadow-md transition-shadow duration-300">
              <h3 className="text-lg font-bold text-brand-dark mb-6 flex items-center gap-2">
                <div className="p-2 bg-purple-50 rounded-lg">
                  <CreditCard size={20} className="text-purple-500" />
                </div>
                Subscription Plan
              </h3>
              <div className="bg-gradient-to-br from-brand-light to-white p-6 rounded-xl border border-brand-accent/10 flex-grow">
                <div className="flex justify-between items-start mb-4">
                  <div>
                    <h4 className="font-bold text-brand-dark text-lg uppercase tracking-tight">{user?.plan} PLAN</h4>
                    <p className="text-sm text-gray-500">Your current subscription tier.</p>
                    {user?.subscriptionExpiry && (
                      <p className="text-xs text-brand-accent font-bold mt-1 bg-brand-accent/5 px-2 py-1 rounded-md inline-block">
                        Expires: {new Date(user.subscriptionExpiry).toLocaleDateString()}
                      </p>
                    )}
                  </div>
                  {user?.plan === 'FREE' && (
                    <Link to="/pricing" className="bg-white px-3 py-1.5 rounded-lg border border-brand-accent/20 text-brand-accent text-xs font-bold hover:bg-brand-accent hover:text-white transition-all duration-300 shadow-sm">
                      Upgrade
                    </Link>
                  )}
                </div>
                <ul className="space-y-3">
                  <li className="flex items-center gap-3 text-sm text-gray-600">
                    <div className={`p-1 rounded-full ${user?.plan === 'PRO' ? "bg-green-100 text-green-600" : "bg-gray-100 text-gray-400"}`}>
                      <CheckCircle size={12} />
                    </div>
                    {user?.plan === 'PRO' ? "Unlimited active links" : "Up to 5 active links"}
                  </li>
                  <li className="flex items-center gap-3 text-sm text-gray-600">
                    <div className={`p-1 rounded-full ${user?.plan === 'PRO' ? "bg-green-100 text-green-600" : "bg-gray-100 text-gray-400"}`}>
                      <CheckCircle size={12} />
                    </div>
                    {user?.plan === 'PRO' ? "Advanced analytics" : "QR Codes"}
                  </li>
                  {user?.plan === 'PRO' && (
                    <li className="flex items-center gap-3 text-sm text-gray-600">
                      <div className="p-1 rounded-full bg-green-100 text-green-600">
                        <CheckCircle size={12} />
                      </div>
                      Priority Support
                    </li>
                  )}
                </ul>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default Profile;
