import { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import AuthFormWrapper from '../components/AuthFormWrapper';
import passwordAxios from '../utils/passwordAxios';
import { toast } from 'react-toastify';
import './ResetPassword.css'; // <-- import the new CSS

const ResetPassword = () => {
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const location = useLocation();
  const navigate = useNavigate();
  const email = location.state?.email;

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (newPassword !== confirmPassword) {
      toast.error("Passwords do not match");
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

  return (
    <AuthFormWrapper title="Reset Password">
      <div className="reset-password-form"> {/* <-- wrapper div */}
        <form onSubmit={handleSubmit}>
          <div className="mb-3">
            <label className="form-label">New Password</label>
            <input
              type="password"
              className="form-control"
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              placeholder='Add New Password '
              required
            />
          </div>
          <div className="mb-3">
            <label className="form-label">Confirm Password</label>
            <input
              type="password"
              className="form-control"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              placeholder='Re-Entered New Password To Confirm'
              required
            />
          </div>
          <button className="btn btn-primary w-100" type="submit">
            Reset Password
          </button>
        </form>
      </div>
    </AuthFormWrapper>
  );
};

export default ResetPassword;