import { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import AuthFormWrapper from '../components/AuthFormWrapper';
import passwordAxios from '../utils/passwordAxios';
import { toast } from 'react-toastify';
import OtpInput from '../components/OtpInput';

const VerifyOtp = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const email = location.state?.email;
  const [otp, setOtp] = useState(['', '', '', '', '', '']);
  const [timer, setTimer] = useState(60);
  const [resending, setResending] = useState(false);

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

  const handleResend = async () => {
    try {
      setResending(true);
      await passwordAxios.post('/forgot', { email });
      toast.success('OTP resent!');
      setOtp(['', '', '', '', '', '']);
      setTimer(60);
    } catch (err) {
      toast.error('Resend failed');
    } finally {
      setResending(false);
    }
  };

  const handleAutoSubmit = async (finalOtp) => {
    try {
        await passwordAxios.post('/verify-otp', { email, otp: finalOtp });
      toast.success('OTP verified!');
      navigate('/reset-password', { state: { email } });
    } catch (err) {
      toast.error(err.response?.data || 'Invalid OTP');
    }
  };

  return (
    <AuthFormWrapper title="Verify OTP">
      <p className="text-center text-muted">Enter the OTP sent to your email</p>
      <OtpInput otp={otp} setOtp={setOtp} autoSubmit={handleAutoSubmit} />
      <div className="text-center mt-3">
        {timer > 0 ? (
          <span className="text-muted">Resend OTP in {timer}s</span>
        ) : (
          <button className="btn btn-link p-0" onClick={handleResend} disabled={resending}>
            {resending ? 'Resending...' : 'Resend OTP'}
          </button>
        )}
      </div>
    </AuthFormWrapper>
  );
};

export default VerifyOtp;
