import React, { useState, useEffect } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { KeyRound, Mail, ArrowRight, CheckCircle, AlertCircle, ShieldCheck } from 'lucide-react';
import AuthLayout from '../components/AuthLayout';
import api from '../services/api';
import toast from 'react-hot-toast';

const ForgotPassword = () => {
    const [searchParams] = useSearchParams();
    const [step, setStep] = useState(1); // 1: Email, 2: OTP & New Password, 3: Success
    const [email, setEmail] = useState('');
    const [otp, setOtp] = useState('');
    const [newPassword, setNewPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [emailError, setEmailError] = useState('');
    const [otpError, setOtpError] = useState('');
    const [passwordError, setPasswordError] = useState('');
    const [confirmPasswordError, setConfirmPasswordError] = useState('');
    const [loading, setLoading] = useState(false);
    const [isReady, setIsReady] = useState(false); // Controls readOnly to bypass auto-fill
    const navigate = useNavigate();

    // Handle auto-fill prevention and unmount cleanup
    useEffect(() => {
        // Small delay to allow browser's auto-fill engine to pass
        const timer = setTimeout(() => setIsReady(true), 100);

        return () => {
            clearTimeout(timer);
            setStep(1);
            setEmail('');
            setOtp('');
            setNewPassword('');
            setConfirmPassword('');
            setEmailError('');
            setOtpError('');
            setPasswordError('');
            setConfirmPasswordError('');
        };
    }, []);

    useEffect(() => {
        const urlEmail = searchParams.get('email');
        const urlOtp = searchParams.get('otp');

        if (urlEmail && urlOtp) {
            setEmail(urlEmail);
            setOtp(urlOtp);
            setStep(2);
        }
    }, [searchParams]);

    const handleSendOtp = async (e) => {
        e.preventDefault();
        setEmailError('');
        setLoading(true);
        try {
            await api.post('/auth/forgot-password', { email }, { skipToast: true });
            setNewPassword('');
            setConfirmPassword('');
            setStep(2);
        } catch (err) {
            const message = err.response?.data?.message || err.message;
            if (message.includes("registered")) {
                setEmailError("This email address is not registered on our platform.");
            }
        } finally {
            setLoading(false);
        }
    };

    const handleResetPassword = async (e) => {
        e.preventDefault();
        setOtpError('');
        setPasswordError('');
        setConfirmPasswordError('');

        if (newPassword !== confirmPassword) {
            setConfirmPasswordError('Passwords do not match');
            return;
        }

        setLoading(true);
        try {
            await api.post('/auth/reset-password', { email, otp, newPassword }, { skipToast: true });
            toast.success('Password reset successful! Please login.');
            navigate('/login');
        } catch (err) {
            const message = err.response?.data?.message || err.message;
            if (message.includes("OTP")) {
                setOtpError(message.replace("Error: ", ""));
            } else if (message.includes("not found")) {
                setEmailError("Account not found. Please try again.");
                setStep(1);
            }
        } finally {
            setLoading(false);
        }
    };

    return (
        <AuthLayout 
            title="Account Recovery" 
            subtitle="Follow the steps to securely reset your account password."
            showBackButton={true}
        >
            <div className="space-y-8">
                <div className="text-center lg:text-left">
                    <h2 className="text-3xl font-extrabold text-[#0b1629]">
                        {step === 1 && 'Forgot Password'}
                        {step === 2 && 'Reset Password'}
                        {step === 3 && 'Success!'}
                    </h2>
                    <p className="mt-2 text-sm text-gray-600">
                        {step === 1 && "Enter your registered email and we'll send you an OTP."}
                        {step === 2 && `We've sent a 6-digit code to ${email}`}
                        {step === 3 && 'Your password has been successfully reset.'}
                    </p>
                </div>
                {step === 1 && (
                    <form className="space-y-6" onSubmit={handleSendOtp} autoComplete="off">
                        {/* Dummy fields to catch initial browser auto-fill */}
                        <input type="text" style={{ display: 'none' }} />
                        <input type="password" style={{ display: 'none' }} />

                        <div className="space-y-1">
                            <label className="block text-sm font-medium text-gray-700 mb-1">Email Address</label>
                            <div className="relative">
                                <Mail className={`absolute left-3 top-1/2 -translate-y-1/2 ${emailError ? 'text-red-400' : 'text-gray-400'}`} size={18} />
                                <input
                                    type="email"
                                    required
                                    autoComplete="off"
                                    readOnly={!isReady}
                                    onFocus={(e) => e.target.readOnly = false}
                                    className={`appearance-none block w-full pl-10 pr-4 py-3 border ${emailError ? 'border-red-500 bg-red-50' : 'border-gray-300'} placeholder-gray-400 text-gray-900 rounded-xl focus:outline-none focus:ring-2 focus:ring-[#3d5afe] focus:border-transparent transition-all sm:text-sm`}
                                    placeholder="name@company.com"
                                    value={email}
                                    onChange={(e) => {
                                        setEmail(e.target.value);
                                        if (emailError) setEmailError('');
                                    }}
                                />
                            </div>
                            {emailError && <p className="text-red-500 text-xs font-medium ml-1 mt-1 animate-in fade-in slide-in-from-top-1">{emailError}</p>}
                        </div>
                        <button
                            type="submit"
                            disabled={loading}
                            className="group relative w-full flex justify-center py-3.5 px-4 border border-transparent text-sm font-bold rounded-xl text-white bg-[#3d5afe] hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-[#3d5afe] transition-all shadow-lg shadow-blue-500/20 disabled:opacity-50 gap-2 items-center"
                        >
                            {loading ? 'Sending OTP...' : (
                                <>
                                    Send OTP <ArrowRight size={18} className="group-hover:translate-x-1 transition-transform" />
                                </>
                            )}
                        </button>
                    </form>
                )}

                {step === 2 && (
                    <form className="space-y-6" onSubmit={handleResetPassword} autoComplete="off">
                        {/* Dummy fields to catch initial browser auto-fill */}
                        <input type="text" style={{ display: 'none' }} />
                        <input type="password" style={{ display: 'none' }} />

                        <div className="space-y-4">
                            <div className="space-y-1">
                                <label className="block text-sm font-medium text-gray-700 mb-1">6-Digit OTP</label>
                                <input
                                    type="text"
                                    required
                                    maxLength={6}
                                    autoComplete="one-time-code"
                                    readOnly={!isReady}
                                    onFocus={(e) => e.target.readOnly = false}
                                    className={`appearance-none block w-full px-4 py-3 border ${otpError ? 'border-red-500 bg-red-50' : 'border-gray-300'} placeholder-gray-400 text-gray-900 rounded-xl focus:outline-none focus:ring-2 focus:ring-[#3d5afe] focus:border-transparent transition-all sm:text-sm text-center tracking-[1em] font-black`}
                                    placeholder="000000"
                                    value={otp}
                                    onChange={(e) => {
                                        setOtp(e.target.value.replace(/\D/g, ''));
                                        if (otpError) setOtpError('');
                                    }}
                                />
                                {otpError && <p className="text-red-500 text-xs font-medium text-center mt-1 animate-in fade-in slide-in-from-top-1">{otpError}</p>}
                            </div>
                            <div className="space-y-1">
                                <label className="block text-sm font-medium text-gray-700 mb-1">New Password</label>
                                <div className="relative">
                                    <KeyRound className={`absolute left-3 top-1/2 -translate-y-1/2 ${passwordError ? 'text-red-400' : 'text-gray-400'}`} size={18} />
                                    <input
                                        type="password"
                                        required
                                        minLength={6}
                                        autoComplete="new-password"
                                        readOnly={!isReady}
                                        onFocus={(e) => e.target.readOnly = false}
                                        className={`appearance-none block w-full pl-10 pr-4 py-3 border ${passwordError ? 'border-red-500 bg-red-50' : 'border-gray-300'} placeholder-gray-400 text-gray-900 rounded-xl focus:outline-none focus:ring-2 focus:ring-[#3d5afe] focus:border-transparent transition-all sm:text-sm`}
                                        placeholder="••••••••"
                                        value={newPassword}
                                        onChange={(e) => {
                                            setNewPassword(e.target.value);
                                            if (passwordError) setPasswordError('');
                                        }}
                                    />
                                </div>
                                {passwordError && <p className="text-red-500 text-xs font-medium ml-1 mt-1 animate-in fade-in slide-in-from-top-1">{passwordError}</p>}
                            </div>
                            <div className="space-y-1">
                                <label className="block text-sm font-medium text-gray-700 mb-1">Confirm New Password</label>
                                <div className="relative">
                                    <ShieldCheck className={`absolute left-3 top-1/2 -translate-y-1/2 ${confirmPasswordError ? 'text-red-400' : 'text-gray-400'}`} size={18} />
                                    <input
                                        type="password"
                                        required
                                        minLength={6}
                                        autoComplete="new-password"
                                        readOnly={!isReady}
                                        onFocus={(e) => e.target.readOnly = false}
                                        className={`appearance-none block w-full pl-10 pr-4 py-3 border ${confirmPasswordError ? 'border-red-500 bg-red-50' : 'border-gray-300'} placeholder-gray-400 text-gray-900 rounded-xl focus:outline-none focus:ring-2 focus:ring-[#3d5afe] focus:border-transparent transition-all sm:text-sm`}
                                        placeholder="••••••••"
                                        value={confirmPassword}
                                        onChange={(e) => {
                                            setConfirmPassword(e.target.value);
                                            if (confirmPasswordError) setConfirmPasswordError('');
                                        }}
                                    />
                                </div>
                                {confirmPasswordError && <p className="text-red-500 text-xs font-medium ml-1 mt-1 animate-in fade-in slide-in-from-top-1">{confirmPasswordError}</p>}
                            </div>
                        </div>
                        <button
                            type="submit"
                            disabled={loading}
                            className="w-full flex justify-center py-3.5 px-4 border border-transparent text-sm font-bold rounded-xl text-white bg-[#3d5afe] hover:bg-blue-700 transition-all shadow-lg shadow-blue-500/20 disabled:opacity-50"
                        >
                            {loading ? 'Resetting Password...' : 'Reset Password'}
                        </button>
                        <button 
                            type="button"
                            onClick={() => setStep(1)}
                            className="w-full text-xs text-gray-500 hover:text-brand-accent font-medium mt-2"
                        >
                            Didn't get OTP? Try again.
                        </button>
                    </form>
                )}

                {step === 3 && (
                    <div className="text-center space-y-8 animate-in zoom-in-95 duration-500">
                        <div className="mx-auto w-20 h-20 bg-green-100 text-green-600 rounded-full flex items-center justify-center shadow-inner">
                            <CheckCircle size={40} />
                        </div>
                        <div className="space-y-4">
                            <p className="text-gray-600">Your password has been reset successfully. You can now log in with your new password.</p>
                            <button
                                onClick={() => navigate('/login')}
                                className="w-full py-3.5 bg-brand-dark hover:bg-black text-white rounded-xl font-bold transition-all shadow-lg shadow-gray-900/10"
                            >
                                Back to Login
                            </button>
                        </div>
                    </div>
                )}
            </div>
        </AuthLayout>
    );
};

export default ForgotPassword;
