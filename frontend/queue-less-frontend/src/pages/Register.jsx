import { useFormik } from 'formik';
import { registerSchema } from '../validation/authSchema';
import axiosInstance from '../utils/axiosInstance';
import AuthFormWrapper from '../components/AuthFormWrapper';
import { Link, useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import { useState } from 'react';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faUser, faEnvelope, faPhone, faLock, faKey } from '@fortawesome/free-solid-svg-icons';
import '@fortawesome/fontawesome-free/css/all.min.css';

// InputWithIcon: Separated to avoid recreation on each render
const InputWithIcon = ({ label, name, type, icon, placeholder, value, onChange, touched, errors }) => (
  <div className="mb-3">
    <label className="form-label">{label}</label>
    <div className="input-group">
      <span className="input-group-text bg-light border-end-0">
        <FontAwesomeIcon icon={icon} className="text-secondary" />
      </span>
      <input
        type={type}
        name={name}
        className={`form-control border-start-0 ${touched[name] && errors[name] ? 'is-invalid' : ''}`}
        value={value}
        onChange={onChange}
        placeholder={placeholder}
        autoComplete="off"
      />
      {touched[name] && errors[name] && <div className="invalid-feedback">{errors[name]}</div>}
    </div>
  </div>
);

// SelectWithLabel: Separated for the same reason
const SelectWithLabel = ({ label, name, options, value, onChange }) => (
  <div className="mb-3">
    <label className="form-label">{label}</label>
    <select
      name={name}
      className="form-control shadow-sm"
      value={value}
      onChange={onChange}
    >
      {options.map((option) => (
        <option key={option.value} value={option.value}>
          {option.label}
        </option>
      ))}
    </select>
  </div>
);

const Register = () => {
  const navigate = useNavigate();
  const [role, setRole] = useState('USER');

  const { values, errors, touched, handleChange, handleSubmit } = useFormik({
    initialValues: {
      name: '',
      email: '',
      phoneNumber: '',
      password: '',
      role: 'USER',
      token: '',
    },
    validationSchema: registerSchema,
    onSubmit: async (values) => {
      try {
        const payload = {
          ...values,
          token: role !== 'USER' ? values.token : '',
        };
        await axiosInstance.post('register', payload);
        toast.success('Registered successfully!');
        navigate('/login');
      } catch (err) {
        toast.error(err.response?.data || 'Registration failed');
      }
    },
  });

  return (
    <AuthFormWrapper title="Create Your Account">
      <form onSubmit={handleSubmit}>
        <InputWithIcon
          label="Name"
          name="name"
          type="text"
          icon={faUser}
          placeholder="Enter your full name"
          value={values.name}
          onChange={handleChange}
          touched={touched}
          errors={errors}
        />
        <InputWithIcon
          label="Email"
          name="email"
          type="email"
          icon={faEnvelope}
          placeholder="Your email address"
          value={values.email}
          onChange={handleChange}
          touched={touched}
          errors={errors}
        />
        <InputWithIcon
          label="Phone Number"
          name="phoneNumber"
          type="tel"
          icon={faPhone}
          placeholder="+91 9876543210"
          value={values.phoneNumber}
          onChange={handleChange}
          touched={touched}
          errors={errors}
        />
        <InputWithIcon
          label="Password"
          name="password"
          type="password"
          icon={faLock}
          placeholder="Create a strong password"
          value={values.password}
          onChange={handleChange}
          touched={touched}
          errors={errors}
        />

        <SelectWithLabel
          label="Account Type"
          name="role"
          options={[
            { value: 'USER', label: 'User' },
            { value: 'PROVIDER', label: 'Service Provider' },
            { value: 'ADMIN', label: 'Admin' },
          ]}
          value={values.role}
          onChange={(e) => {
            handleChange(e);
            setRole(e.target.value);
          }}
        />

        {(role === 'ADMIN' || role === 'PROVIDER') && (
          <InputWithIcon
            label="Registration Token"
            name="token"
            type="text"
            icon={faKey}
            placeholder="Paste your secure token"
            value={values.token}
            onChange={handleChange}
            touched={touched}
            errors={errors}
          />
        )}

        <button className="btn btn-primary w-100 login-btn mb-2 shadow-sm" type="submit">
          Register
        </button>

        <Link to="/login" className="text-decoration-none create-link text-center text-muted small">
          Already have an account?
        </Link>
      </form>
    </AuthFormWrapper>
  );
};

export default Register;
