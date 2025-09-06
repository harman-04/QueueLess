//src/components/OtpInput.jsx
import { useEffect, useRef } from 'react';

const OtpInput = ({ otp, setOtp, autoSubmit }) => {
  const inputs = useRef([]);

  const handleChange = (value, index) => {
    if (!/^\d?$/.test(value)) return;
    const newOtp = [...otp];
    newOtp[index] = value;
    setOtp(newOtp);

    if (value && index < 5) inputs.current[index + 1].focus();
  };

  const handleKeyDown = (e, index) => {
    if (e.key === 'Backspace' && !otp[index] && index > 0) {
      inputs.current[index - 1].focus();
    }
  };

  useEffect(() => {
    inputs.current[0]?.focus();
  }, []);

  useEffect(() => {
    if (otp.every((digit) => digit !== '')) {
      autoSubmit(otp.join(''));
    }
  }, [otp, autoSubmit]); // ✅ Included autoSubmit in dependency array

  return (
    <div className="d-flex gap-2 justify-content-center">
      {otp.map((value, i) => (
        <input
          key={i}
          maxLength="1"
          value={value}
          type="text"
          ref={(el) => (inputs.current[i] = el)}
          onChange={(e) => handleChange(e.target.value, i)}
          onKeyDown={(e) => handleKeyDown(e, i)}
          className="form-control text-center"
          style={{ width: '3rem', height: '3rem', fontSize: '1.5rem' }}
        />
      ))}
    </div>
  );
};

export default OtpInput;
