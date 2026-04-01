import axios from 'axios';
import toast from 'react-hot-toast';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api',
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 400 && error.response?.data?.details) {
      // Backend validation error map
      const details = error.response.data.details;
      Object.keys(details).forEach(field => {
        if (!error.config?.skipToast) {
          toast.error(`${field}: ${details[field]}`);
        }
      });
    } else if (error.response?.data?.message && error.response?.status !== 401) {
      // General backend error message (avoid double-toasting 401s since we redirect)
      if (!error.config?.skipToast) {
        toast.error(error.response.data.message);
      }
    }

    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default api;
