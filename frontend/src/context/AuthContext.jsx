import React, { createContext, useContext, useState, useEffect } from 'react';
import api from '../services/api';

const AuthContext = createContext();

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const savedUser = localStorage.getItem('user');
    if (savedUser) {
      const parsedUser = JSON.parse(savedUser);
      // Migration: convert roles array to single role if needed, and ensure plan exists
      if (parsedUser.roles && !parsedUser.role) {
        parsedUser.role = parsedUser.roles.includes('ROLE_ADMIN') ? 'ADMIN' : 'USER';
      }
      if (!parsedUser.plan) parsedUser.plan = 'FREE';
      setUser(parsedUser);
    }
    setLoading(false);
  }, []);

  const login = async (email, password) => {
    const response = await api.post('/auth/login', { email, password }, { skipToast: true });
    const { token, id, name, email: userEmail, role, plan, subscriptionExpiry } = response.data;
    const userData = { id, name, email: userEmail, role, plan, subscriptionExpiry, token };
    localStorage.setItem('token', token);
    localStorage.setItem('user', JSON.stringify(userData));
    setUser(userData);
    return response.data;
  };

  const magicLogin = async (magicToken) => {
    const response = await api.get(`/auth/magic-login?token=${magicToken}`);
    const { token, id, name, email: userEmail, role, plan, subscriptionExpiry } = response.data;
    const userData = { id, name, email: userEmail, role, plan, subscriptionExpiry, token };
    localStorage.setItem('token', token);
    localStorage.setItem('user', JSON.stringify(userData));
    setUser(userData);
    return response.data;
  };

  const register = async (name, email, password, config = {}) => {
    return await api.post('/auth/register', { name, email, password }, config);
  };

  const logout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    setUser(null);
  };

  const updateUser = (updatedData) => {
    const newUser = { ...user, ...updatedData };
    localStorage.setItem('user', JSON.stringify(newUser));
    setUser(newUser);
  };

  const isAdmin = user?.role === 'ADMIN';

  return (
    <AuthContext.Provider value={{ user, login, magicLogin, register, logout, updateUser, loading, isAdmin }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => useContext(AuthContext);
