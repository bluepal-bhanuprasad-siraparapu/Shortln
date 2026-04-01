import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { Toaster } from 'react-hot-toast';
import { AuthProvider, useAuth } from './context/AuthContext';
import Login from './pages/Login';
import Register from './pages/Register';
import Dashboard from './pages/Dashboard';
import Home from './pages/Home';
import DashboardLayout from './layouts/DashboardLayout';
import LinkList from './pages/LinkList';
import CreateLink from './pages/CreateLink';
import Analytics from './pages/Analytics';
import QRCodePage from './pages/QRCodePage';
import UserManagement from './pages/UserManagement';
import AdminLinks from './pages/AdminLinks';
import Profile from './pages/Profile';
import Pricing from './pages/Pricing';
import ForgotPassword from './pages/ForgotPassword';
import AutoLogin from './pages/AutoLogin';
import Finance from './pages/Finance';
import Reports from './pages/Reports';
import AdminNotifications from './pages/AdminNotifications';
import AuditLogs from './pages/AuditLogs';

const queryClient = new QueryClient();

const DashboardHome = () => {
  const { isAdmin } = useAuth();
  return isAdmin ? <Dashboard /> : <CreateLink />;
};

const PrivateRoute = ({ children }) => {
  const { user, loading } = useAuth();
  
  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-brand-light">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-brand-accent"></div>
      </div>
    );
  }
  
  return user ? children : <Navigate to="/login" />;
};

const AdminRoute = ({ children }) => {
  const { user, loading, isAdmin } = useAuth();
  
  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-brand-light">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-brand-accent"></div>
      </div>
    );
  }
  
  return (user && isAdmin) ? children : <Navigate to="/dashboard" />;
};

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <Toaster position="top-center" />
      <AuthProvider>
        <Router>
          <Routes>
            <Route path="/" element={<Home />} />
            <Route path="/login" element={<Login />} />
            <Route path="/auto-login" element={<AutoLogin />} />
            <Route path="/register" element={<Register />} />
            <Route path="/forgot-password" element={<ForgotPassword />} />
            <Route 
              path="/dashboard" 
              element={
                <PrivateRoute>
                  <DashboardLayout>
                    <DashboardHome />
                  </DashboardLayout>
                </PrivateRoute>
              } 
            />
            <Route 
              path="/links" 
              element={
                <PrivateRoute>
                  <DashboardLayout>
                    <LinkList />
                  </DashboardLayout>
                </PrivateRoute>
              } 
            />
            <Route 
              path="/qr-codes" 
              element={
                <PrivateRoute>
                  <DashboardLayout>
                    <QRCodePage />
                  </DashboardLayout>
                </PrivateRoute>
              } 
            />
            <Route 
              path="/analytics" 
              element={
                <PrivateRoute>
                  <DashboardLayout>
                    <Dashboard />
                  </DashboardLayout>
                </PrivateRoute>
              } 
            />
             <Route 
              path="/create" 
              element={
                <PrivateRoute>
                  <DashboardLayout>
                    <CreateLink />
                  </DashboardLayout>
                </PrivateRoute>
              } 
            />
            <Route 
              path="/edit/:id" 
              element={
                <PrivateRoute>
                  <DashboardLayout>
                    <CreateLink />
                  </DashboardLayout>
                </PrivateRoute>
              } 
            />
             <Route 
              path="/analytics/:id" 
              element={
                <PrivateRoute>
                  <DashboardLayout>
                    <Analytics />
                  </DashboardLayout>
                </PrivateRoute>
              } 
            />
            <Route 
              path="/profile" 
              element={
                <PrivateRoute>
                  <DashboardLayout>
                    <Profile />
                  </DashboardLayout>
                </PrivateRoute>
              } 
            />
            <Route 
              path="/pricing" 
              element={
                <PrivateRoute>
                  <DashboardLayout>
                    <Pricing />
                  </DashboardLayout>
                </PrivateRoute>
              } 
            />
            {/* Admin Routes */}
            <Route 
              path="/admin/users" 
              element={
                <AdminRoute>
                  <DashboardLayout>
                    <UserManagement />
                  </DashboardLayout>
                </AdminRoute>
              } 
            />
            <Route 
              path="/admin/links" 
              element={
                <AdminRoute>
                  <DashboardLayout>
                    <AdminLinks />
                  </DashboardLayout>
                </AdminRoute>
              } 
            />
            <Route 
              path="/admin/reports" 
              element={
                <AdminRoute>
                  <DashboardLayout>
                    <Reports />
                  </DashboardLayout>
                </AdminRoute>
              } 
            />
            <Route 
              path="/finance" 
              element={
                <AdminRoute>
                  <DashboardLayout>
                    <Finance />
                  </DashboardLayout>
                </AdminRoute>
              } 
            />
            <Route 
              path="/admin/notifications" 
              element={
                <AdminRoute>
                  <DashboardLayout>
                    <AdminNotifications />
                  </DashboardLayout>
                </AdminRoute>
              } 
            />
            <Route 
              path="/admin/audit-logs" 
              element={
                <AdminRoute>
                  <DashboardLayout>
                    <AuditLogs />
                  </DashboardLayout>
                </AdminRoute>
              } 
            />
            <Route path="*" element={<Navigate to="/" />} />
          </Routes>
        </Router>
      </AuthProvider>
    </QueryClientProvider>
  );
}

export default App;
