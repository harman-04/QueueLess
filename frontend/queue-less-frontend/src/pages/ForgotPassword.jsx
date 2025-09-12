//src/pages/ForgotPassword.jsx
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import AuthFormWrapper from '../components/AuthFormWrapper';
import passwordAxios from '../utils/passwordAxios';
import { toast } from 'react-toastify';

const ForgotPassword = () => {
  const [email, setEmail] = useState('');
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
        await passwordAxios.post('/forgot', { email });
        toast.success('OTP sent to your email!');
      navigate('/verify-otp', { state: { email } });
    } catch (err) {
      toast.error(err.response?.data || 'Failed to send OTP');
    }
  };

  return (
    <AuthFormWrapper title="Forgot Password">
      <form onSubmit={handleSubmit}>
        <div className="mb-3">
          <label>Email</label>
          <input
            type="email"
            className="form-control"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />
        </div>
        <button className="btn btn-primary w-100" type="submit">Send OTP</button>
      </form>
    </AuthFormWrapper>
  );
};

export default ForgotPassword;
