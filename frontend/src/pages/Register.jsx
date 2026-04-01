import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { UserPlus } from 'lucide-react';
import AuthLayout from '../components/AuthLayout';
import toast from 'react-hot-toast';

const Register = () => {
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [emailError, setEmailError] = useState('');
  const [passwordError, setPasswordError] = useState('');
  const [confirmPasswordError, setConfirmPasswordError] = useState('');
  const [loading, setLoading] = useState(false);
  const [isReady, setIsReady] = useState(false); // Controls readOnly to bypass auto-fill
  const { register } = useAuth();
  const navigate = useNavigate();

  // Handle auto-fill prevention and unmount cleanup
  React.useEffect(() => {
    // Small delay to allow browser's auto-fill engine to pass
    const timer = setTimeout(() => setIsReady(true), 100);

    return () => {
      clearTimeout(timer);
      setName('');
      setEmail('');
      setPassword('');
      setConfirmPassword('');
      setEmailError('');
      setPasswordError('');
      setConfirmPasswordError('');
    };
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setEmailError('');
    setPasswordError('');
    setConfirmPasswordError('');

    if (password !== confirmPassword) {
      setConfirmPasswordError('Passwords do not match');
      return;
    }

    setLoading(true);
    try {
      await register(name, email, password, { skipToast: true });
      toast.success('Registration successful! Redirecting to login...');
      setTimeout(() => navigate('/login'), 2000);
    } catch (err) {
      const message = err.response?.data?.message || err.message;
      if (message.includes("Email")) {
        setEmailError("This email address is already registered.");
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthLayout 
      title="Join ShortenIt" 
      subtitle="Create your account and start building closer connections with every link."
    >
      <div className="space-y-8">
        <div className="text-center lg:text-left">
          <h2 className="text-3xl font-extrabold text-[#0b1629]">Create account</h2>
          <p className="mt-2 text-sm text-gray-600">
            Already have an account?{' '}
            <Link to="/login" className="font-medium text-[#3d5afe] hover:text-blue-700 transition-colors">
              Sign in here
            </Link>
          </p>
        </div>

        <form className="mt-8 space-y-6" onSubmit={handleSubmit} autoComplete="off">
          {/* Dummy fields to catch initial browser auto-fill */}
          <input type="text" style={{ display: 'none' }} />
          <input type="password" style={{ display: 'none' }} />
          
          <div className="space-y-4">
            <div className="space-y-1">
              <label className="block text-sm font-medium text-gray-700 mb-1">Full Name</label>
              <input
                type="text"
                required
                autoComplete="off"
                readOnly={!isReady}
                onFocus={(e) => e.target.readOnly = false}
                className="appearance-none block w-full px-4 py-3 border border-gray-300 placeholder-gray-400 text-gray-900 rounded-xl focus:outline-none focus:ring-2 focus:ring-[#3d5afe] focus:border-transparent transition-all sm:text-sm"
                placeholder="John Doe"
                value={name}
                onChange={(e) => setName(e.target.value)}
              />
            </div>
            <div className="space-y-1">
              <label className="block text-sm font-medium text-gray-700 mb-1">Email Address</label>
              <input
                type="email"
                required
                autoComplete="off"
                readOnly={!isReady}
                onFocus={(e) => e.target.readOnly = false}
                className={`appearance-none block w-full px-4 py-3 border ${emailError ? 'border-red-500 bg-red-50' : 'border-gray-300'} placeholder-gray-400 text-gray-900 rounded-xl focus:outline-none focus:ring-2 focus:ring-[#3d5afe] focus:border-transparent transition-all sm:text-sm`}
                placeholder="name@company.com"
                value={email}
                onChange={(e) => {
                  setEmail(e.target.value);
                  if (emailError) setEmailError('');
                }}
              />
              {emailError && <p className="text-red-500 text-xs font-medium ml-1 mt-1 animate-in fade-in slide-in-from-top-1">{emailError}</p>}
            </div>
            <div className="space-y-1">
              <label className="block text-sm font-medium text-gray-700 mb-1">Password</label>
              <input
                type="password"
                required
                minLength={6}
                autoComplete="new-password"
                readOnly={!isReady}
                onFocus={(e) => e.target.readOnly = false}
                className={`appearance-none block w-full px-4 py-3 border border-gray-300 placeholder-gray-400 text-gray-900 rounded-xl focus:outline-none focus:ring-2 focus:ring-[#3d5afe] focus:border-transparent transition-all sm:text-sm`}
                placeholder="••••••••"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
              />
            </div>
            <div className="space-y-1">
              <label className="block text-sm font-medium text-gray-700 mb-1">Confirm Password</label>
              <input
                type="password"
                required
                minLength={6}
                autoComplete="new-password"
                readOnly={!isReady}
                onFocus={(e) => e.target.readOnly = false}
                className={`appearance-none block w-full px-4 py-3 border ${confirmPasswordError ? 'border-red-500 bg-red-50' : 'border-gray-300'} placeholder-gray-400 text-gray-900 rounded-xl focus:outline-none focus:ring-2 focus:ring-[#3d5afe] focus:border-transparent transition-all sm:text-sm`}
                placeholder="••••••••"
                value={confirmPassword}
                onChange={(e) => {
                  setConfirmPassword(e.target.value);
                  if (confirmPasswordError) setConfirmPasswordError('');
                }}
              />
              {confirmPasswordError && <p className="text-red-500 text-xs font-medium ml-1 mt-1 animate-in fade-in slide-in-from-top-1">{confirmPasswordError}</p>}
            </div>
          </div>


          <div>
            <button
              type="submit"
              disabled={loading}
              className="group relative w-full flex justify-center py-3.5 px-4 border border-transparent text-sm font-bold rounded-xl text-white bg-[#3d5afe] hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-[#3d5afe] transition-all shadow-lg shadow-blue-500/20 disabled:opacity-50"
            >
              {loading ? (
                 <span className="flex items-center gap-2">
                  <svg className="animate-spin h-5 w-5 text-white" viewBox="0 0 24 24">
                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" fill="none" />
                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
                  </svg>
                  Creating account...
                </span>
              ) : 'Create account'}
            </button>
          </div>
        </form>
      </div>
    </AuthLayout>
  );
};

export default Register;
