// src/pages/VerifyEmail.jsx
import React, { useState, useEffect, useCallback } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import AuthFormWrapper from '../components/AuthFormWrapper';
import OtpInput from '../components/OtpInput';
import { toast } from 'react-toastify';
import axiosInstance from '../utils/axiosInstance';
import './VerifyEmail.css';

const VerifyEmail = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const email = location.state?.email;
  const [otp, setOtp] = useState(['', '', '', '', '', '']);
  const [timer, setTimer] = useState(60);
  const [resending, setResending] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (!email) {
      navigate('/register');
    }
  }, [email, navigate]);

  useEffect(() => {
    const countdown = setInterval(() => {
      setTimer((t) => {
        if (t <= 1) {
          clearInterval(countdown);
          return 0;
        }
        return t - 1;
      });
    }, 1000);
    return () => clearInterval(countdown);
  }, []);

  const handleVerify = useCallback(async (otpValue) => {
    if (submitting) return;
    setSubmitting(true);
    try {
      await axiosInstance.post('/auth/verify-email', { email, otp: otpValue });
      toast.success('Email verified! You can now log in.');
      navigate('/login');
    } catch (err) {
      toast.error(err.response?.data || 'Verification failed');
      setSubmitting(false); // allow retry on failure
    }
  }, [email, navigate, submitting]);

  const handleResend = async () => {
    setResending(true);
    try {
      await axiosInstance.post(`/auth/resend-verification?email=${encodeURIComponent(email)}`);
      toast.success('OTP resent!');
      setOtp(['', '', '', '', '', '']);
      setTimer(60);
    } catch (err) {
      toast.error(err.response?.data || 'Resend failed');
    } finally {
      setResending(false);
    }
  };

  return (
    <AuthFormWrapper title="Verify Your Email">
      <div className="verify-email-container">
        <p>
          We've sent a 6‑digit code to <strong>{email}</strong>. Enter it below.
        </p>
        <OtpInput otp={otp} setOtp={setOtp} autoSubmit={handleVerify} />
        <div className="text-center mt-3">
          {timer > 0 ? (
            <span className="text-muted">Resend OTP in {timer}s</span>
          ) : (
            <button
              className="btn btn-link p-0"
              onClick={handleResend}
              disabled={resending}
            >
              {resending ? 'Resending...' : 'Resend OTP'}
            </button>
          )}
        </div>
      </div>
    </AuthFormWrapper>
  );
};

export default VerifyEmail;