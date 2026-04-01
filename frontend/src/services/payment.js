import api from './api';

export const paymentService = {
  createOrder: async (data) => {
    const response = await api.post('/payments/create-order', data);
    return response.data;
  },

  verifyPayment: async (paymentData) => {
    const response = await api.post('/payments/verify', paymentData);
    return response.data;
  }
};

export default paymentService;
