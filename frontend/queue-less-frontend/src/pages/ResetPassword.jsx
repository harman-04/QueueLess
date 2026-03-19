import { useState, useEffect } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import AuthFormWrapper from '../components/AuthFormWrapper';
import passwordAxios from '../utils/passwordAxios';
import { toast } from 'react-toastify';
import { FaEye, FaEyeSlash, FaCheckCircle, FaTimesCircle } from 'react-icons/fa';
import './ResetPassword.css';

const ResetPassword = () => {
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showNew, setShowNew] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);
  const [passwordStrength, setPasswordStrength] = useState(0);
  const location = useLocation();
  const navigate = useNavigate();
  const email = location.state?.email;

  // Password strength calculation
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

    try {
      await passwordAxios.post('/reset', {
        email,
        newPassword,
        confirmPassword
      });
      toast.success('Password reset successful!');
      navigate('/login');
    } catch (err) {
      toast.error(err.response?.data || 'Password reset failed');
    }
  };

  const strengthLabels = ['Too weak', 'Weak', 'Fair', 'Good', 'Strong'];
  const strengthColors = ['#e74c3c', '#e67e22', '#f1c40f', '#2ecc71', '#27ae60'];

  return (
    <AuthFormWrapper title="Reset Password">
      <div className="reset-password-form">
        <form onSubmit={handleSubmit}>
          {/* New Password Field */}
          <div className="mb-4">
            <label className="form-label">New Password</label>
            <div className="password-input-wrapper">
              <input
                type={showNew ? 'text' : 'password'}
                className="form-control premium-input"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                placeholder="Enter new password"
                required
              />
              <span
                className="password-toggle"
                onClick={() => setShowNew(!showNew)}
              >
                {showNew ? <FaEyeSlash /> : <FaEye />}
              </span>
            </div>
            {/* Password strength meter */}
            {newPassword && (
              <div className="mt-2">
                <div className="strength-bars">
                  {[...Array(4)].map((_, i) => (
                    <div
                      key={i}
                      className="strength-bar"
                      style={{
                        backgroundColor: i < passwordStrength ? strengthColors[passwordStrength] : 'var(--border-color)',
                        width: '25%'
                      }}
                    />
                  ))}
                </div>
                <small className="strength-label" style={{ color: strengthColors[passwordStrength] }}>
                  {strengthLabels[passwordStrength]}
                </small>
              </div>
            )}
          </div>

          {/* Confirm Password Field */}
          <div className="mb-4">
            <label className="form-label">Confirm Password</label>
            <div className="password-input-wrapper">
              <input
                type={showConfirm ? 'text' : 'password'}
                className="form-control premium-input"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                placeholder="Re-enter new password"
                required
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
                  <small className="match-success">
                    <FaCheckCircle className="me-1" /> Passwords match
                  </small>
                ) : (
                  <small className="match-error">
                    <FaTimesCircle className="me-1" /> Passwords do not match
                  </small>
                )}
              </div>
            )}
          </div>

          <button className="btn btn-primary premium-btn w-100" type="submit">
            Reset Password
          </button>
        </form>
      </div>
    </AuthFormWrapper>
  );
};

export default ResetPassword;