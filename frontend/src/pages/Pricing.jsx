import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import paymentService from '../services/payment';
import { useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';

const Pricing = () => {
  const { user, updateUser } = useAuth();
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const handleUpgrade = async () => {
    setLoading(true);
    try {
      // 1. Create real order on backend
      const order = await paymentService.createOrder({ plan: 'PRO' });
      
      // 2. Initialize Real Razorpay Checkout
      const options = {
        key: order.keyId, // Key ID from backend
        amount: order.amount,
        currency: order.currency,
        name: "Shortln Pro",
        description: "Upgrade to Pro Plan",
        order_id: order.id,
        handler: async function (response) {
          // This function handles the response after successful payment
          try {
            setLoading(true);
            const result = await paymentService.verifyPayment({
              razorpay_order_id: response.razorpay_order_id,
              razorpay_payment_id: response.razorpay_payment_id,
              razorpay_signature: response.razorpay_signature
            });

            if (result.status === 'success') {
              updateUser(result.user);
              toast.success("Success! You are now a PRO user.");
              navigate('/profile');
            }
          } catch (error) {
            console.error("Verification failed", error);
            toast.error("Payment verification failed. Please contact support.");
          } finally {
            setLoading(false);
          }
        },
        prefill: {
          name: user?.name,
          email: user?.email
        },
        theme: {
          color: "#6366f1"
        },
        modal: {
          ondismiss: function() {
            setLoading(false);
          }
        }
      };

      const rzp = new window.Razorpay(options);
      rzp.on('payment.failed', function (response){
          toast.error("Payment failed: " + response.error.description);
          setLoading(false);
      });
      rzp.open();

    } catch (error) {
      console.error("Order creation failed", error);
      toast.error("Could not initiate payment. Please try again later.");
      setLoading(false);
    }
  };

  const plans = [
    {
      name: 'Free',
      price: '₹0',
      features: ['Up to 5 links', 'QR Codes', 'Standard Support'],
      buttonText: 'Current Plan',
      isCurrent: user?.plan === 'FREE',
      highlight: false
    },
    {
      name: 'Pro',
      price: '₹999',
      period: '/year',
      features: ['Unlimited links', 'Advanced Analytics', 'Priority Support', 'Custom QR Codes'],
      buttonText: user?.plan === 'PRO' ? 'Current Plan' : 'Upgrade to Pro',
      isCurrent: user?.plan === 'PRO',
      highlight: true
    }
  ];

  return (
    <div className="p-6 max-w-6xl mx-auto">
      <div className="text-center mb-12">
        <h1 className="text-4xl font-extrabold text-gray-900 mb-4">Choose Your Plan</h1>
        <p className="text-lg text-gray-600">Get the tools you need to manage your links effectively.</p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-8 max-w-4xl mx-auto">
        {plans.map((plan) => (
          <div 
            key={plan.name}
            className={`relative rounded-3xl p-8 transition-all duration-300 ${
              plan.highlight 
                ? 'bg-gradient-to-br from-indigo-600 to-purple-700 text-white shadow-2xl scale-105 z-10' 
                : 'bg-white text-gray-900 shadow-xl border border-gray-100 hover:shadow-2xl'
            }`}
          >
            {plan.highlight && (
              <div className="absolute top-0 right-8 transform -translate-y-1/2 bg-yellow-400 text-yellow-900 text-xs font-bold px-3 py-1 rounded-full uppercase tracking-wider">
                Most Popular
              </div>
            )}
            
            <h3 className="text-2xl font-bold mb-2">{plan.name}</h3>
            <div className="mb-6">
              <span className="text-4xl font-extrabold">{plan.price}</span>
              {plan.period && <span className="text-lg opacity-80">{plan.period}</span>}
            </div>

            <ul className="mb-8 space-y-4">
              {plan.features.map((feature) => (
                <li key={feature} className="flex items-center">
                  <svg className={`h-5 w-5 mr-3 ${plan.highlight ? 'text-indigo-200' : 'text-indigo-500'}`} fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                  </svg>
                  <span>{feature}</span>
                </li>
              ))}
            </ul>

            <button
              onClick={plan.name === 'Pro' && !plan.isCurrent ? handleUpgrade : null}
              disabled={loading || plan.isCurrent}
              className={`w-full py-4 rounded-xl font-bold text-lg transition-all duration-300 ${
                plan.isCurrent
                  ? 'bg-gray-200 text-gray-500 cursor-default'
                  : plan.highlight
                    ? 'bg-white text-indigo-600 hover:bg-gray-100 transform hover:-translate-y-1'
                    : 'bg-indigo-600 text-white hover:bg-indigo-700 transform hover:-translate-y-1'
              } ${loading ? 'opacity-50' : ''}`}
            >
              {loading ? 'Initiating Checkout...' : plan.buttonText}
            </button>
          </div>
        ))}
      </div>

      <div className="mt-16 text-center">
        <p className="text-gray-500">Secure payment powered by Razorpay.</p>
      </div>
    </div>
  );
};

export default Pricing;
