//src/pages/AdminDashboard.jsx
import React, { useState, useEffect } from 'react';
import { Button } from 'react-bootstrap';
import { toast } from 'react-toastify';
import axiosInstance from '../utils/axiosInstance';

const AdminDashboard = () => {
  const [payments, setPayments] = useState([]);

  useEffect(() => {
    const fetchPayments = async () => {
      try {
        const response = await axiosInstance.get('/admin/payments'); // Endpoint to get payment history
        setPayments(response.data);
      } catch (error) {
        toast.error('Failed to fetch payment history.');
      }
    };

    fetchPayments();
  }, []);

  const handleCreateToken = async (payment) => {
    try {
      const response = await axiosInstance.post('/admin/create-token', { paymentId: payment.id });
      toast.success('Token created successfully!');
    } catch (error) {
      toast.error('Failed to create token.');
    }
  };

  return (
    <div className="admin-dashboard">
      <h1>Admin Dashboard</h1>
      <div>
        {payments.map((payment) => (
          <div key={payment.id} className="payment-item">
            <p>{payment.details}</p>
            <Button onClick={() => handleCreateToken(payment)}>Create Token</Button>
          </div>
        ))}
      </div>
    </div>
  );
};

export default AdminDashboard;
