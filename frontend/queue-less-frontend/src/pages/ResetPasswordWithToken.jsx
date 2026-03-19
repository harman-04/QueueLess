import React, { useState, useEffect } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { Container, Card, Form, Button, Alert, Spinner } from 'react-bootstrap';
import { FaKey, FaEye, FaEyeSlash, FaCheckCircle, FaTimesCircle } from 'react-icons/fa';
import axiosInstance from '../utils/axiosInstance';
import { toast } from 'react-toastify';
import AuthFormWrapper from '../components/AuthFormWrapper';
import './ResetPasswordWithToken.css'; // Premium custom styles

const ResetPasswordWithToken = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const token = searchParams.get('token');

  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showNew, setShowNew] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [tokenValid, setTokenValid] = useState(null);
  const [checking, setChecking] = useState(true);
  const [passwordStrength, setPasswordStrength] = useState(0); // 0-4

  // Password strength checker
  useEffect(() => {
    if (!newPassword) {
      setPasswordStrength(0);
      return;
    }
    let strength = 0;
    if (newPassword.length >= 8) strength++;
    if (/[a-z]/.test(newPassword)) strength++;
    if (/[A-Z]/.test(newPassword)) strength++;
    if (/[0-9]/.test(newPassword)) strength++;
    if (/[^a-zA-Z0-9]/.test(newPassword)) strength++;
    setPasswordStrength(Math.min(4, strength));
  }, [newPassword]);

  useEffect(() => {
    if (!token) {
      setTokenValid(false);
      setChecking(false);
      return;
    }
    validateToken();
  }, [token]);

  const validateToken = async () => {
    try {
      await axiosInstance.post('/password-reset-token/validate', null, {
        params: { token }
      });
      setTokenValid(true);
    } catch (err) {
      setTokenValid(false);
    } finally {
      setChecking(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (newPassword !== confirmPassword) {
      toast.error('Passwords do not match');
      return;
    }
    if (newPassword.length < 8) {
      toast.error('Password must be at least 8 characters');
      return;
    }
    setIsSubmitting(true);
    try {
      await axiosInstance.post('/password-reset-token/reset', {
        token,
        newPassword
      });
      toast.success('Password reset successful! You can now log in.');
      navigate('/login');
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to reset password');
    } finally {
      setIsSubmitting(false);
    }
  };

  // Strength bar color and label
  const strengthMap = ['Too weak', 'Weak', 'Fair', 'Good', 'Strong'];
  const strengthColor = ['#e74c3c', '#e67e22', '#f1c40f', '#2ecc71', '#27ae60'];

  if (checking) {
    return (
      <Container className="text-center py-5">
        <Spinner animation="border" variant="primary" />
        <p className="mt-3 text-secondary">Validating reset link...</p>
      </Container>
    );
  }

  if (!tokenValid) {
    return (
      <AuthFormWrapper title="Invalid Reset Link">
        <Alert variant="danger" className="text-center">
          This password reset link is invalid or has expired.
        </Alert>
        <Button variant="primary" onClick={() => navigate('/forgot-password')} className="w-100 premium-btn">
          Request New Reset
        </Button>
      </AuthFormWrapper>
    );
  }

  return (
    <AuthFormWrapper title="Set New Password">
      <Form onSubmit={handleSubmit} className="reset-password-form">
        {/* New Password Field */}
        <Form.Group className="mb-4" controlId="newPassword">
          <Form.Label className="fw-semibold">New Password</Form.Label>
          <div className="password-input-wrapper">
            <Form.Control
              type={showNew ? 'text' : 'password'}
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              placeholder="Enter new password"
              required
              className="premium-input"
            />
            <span
              className="password-toggle"
              onClick={() => setShowNew(!showNew)}
            >
              {showNew ? <FaEyeSlash /> : <FaEye />}
            </span>
          </div>
          {/* Password Strength Meter */}
          {newPassword && (
            <div className="mt-2">
              <div className="strength-bars">
                {[...Array(4)].map((_, i) => (
                  <div
                    key={i}
                    className="strength-bar"
                    style={{
                      backgroundColor: i < passwordStrength ? strengthColor[passwordStrength] : 'var(--border-color)',
                      width: '25%'
                    }}
                  />
                ))}
              </div>
              <small className="text-secondary" style={{ color: strengthColor[passwordStrength] }}>
                {strengthMap[passwordStrength]}
              </small>
            </div>
          )}
        </Form.Group>

        {/* Confirm Password Field */}
        <Form.Group className="mb-4" controlId="confirmPassword">
          <Form.Label className="fw-semibold">Confirm Password</Form.Label>
          <div className="password-input-wrapper">
            <Form.Control
              type={showConfirm ? 'text' : 'password'}
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              placeholder="Confirm new password"
              required
              className="premium-input"
            />
            <span
              className="password-toggle"
              onClick={() => setShowConfirm(!showConfirm)}
            >
              {showConfirm ? <FaEyeSlash /> : <FaEye />}
            </span>
          </div>
          {/* Match indicator */}
          {confirmPassword && (
            <div className="mt-1">
              {newPassword === confirmPassword ? (
                <small className="text-success">
                  <FaCheckCircle className="me-1" /> Passwords match
                </small>
              ) : (
                <small className="text-danger">
                  <FaTimesCircle className="me-1" /> Passwords do not match
                </small>
              )}
            </div>
          )}
        </Form.Group>

        <Button
          type="submit"
          variant="primary"
          className="w-100 premium-btn"
          disabled={isSubmitting}
        >
          {isSubmitting ? (
            <Spinner animation="border" size="sm" />
          ) : (
            <>
              <FaKey className="me-2" />
              Reset Password
            </>
          )}
        </Button>
      </Form>
    </AuthFormWrapper>
  );
};

export default ResetPasswordWithToken;