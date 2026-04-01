import React, { useEffect, useState, useRef } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import toast from 'react-hot-toast';

const AutoLogin = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { magicLogin } = useAuth();
  const [error, setError] = useState('');
  const hasAttemptedLogin = useRef(false);

  useEffect(() => {
    if (hasAttemptedLogin.current) return;
    hasAttemptedLogin.current = true;

    const token = searchParams.get('token');

    if (!token) {
      toast.error('Invalid authentication link.');
      navigate('/login');
      return;
    }

    const performMagicLogin = async () => {
      try {
        await magicLogin(token);
        toast.success('Successfully logged in!');
        navigate('/dashboard');
      } catch (err) {
        const message = err.response?.data?.message || 'Login link expired or invalid.';
        setError(message);
        toast.error(message);
        setTimeout(() => {
          navigate('/login');
        }, 3000); // Wait 3 seconds before redirecting
      }
    };

    performMagicLogin();
  }, [searchParams, magicLogin, navigate]);

  return (
    <div className="min-h-screen flex items-center justify-center bg-[#f8faff]">
      <div className="bg-white p-8 rounded-2xl shadow-xl w-full max-w-md text-center">
        {!error ? (
          <div className="space-y-4">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-[#3d5afe] mx-auto"></div>
            <h2 className="text-xl font-bold text-gray-800">Authenticating...</h2>
            <p className="text-sm text-gray-500">Please wait while we log you into your account.</p>
          </div>
        ) : (
          <div className="space-y-4">
            <div className="mx-auto flex items-center justify-center h-12 w-12 rounded-full bg-red-100 mb-4">
              <svg className="h-6 w-6 text-red-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </div>
            <h2 className="text-xl font-bold text-gray-800">Authentication Failed</h2>
            <p className="text-sm text-gray-500">{error}</p>
            <p className="text-sm text-gray-400 mt-4">Redirecting you to the login page...</p>
          </div>
        )}
      </div>
    </div>
  );
};

export default AutoLogin;
