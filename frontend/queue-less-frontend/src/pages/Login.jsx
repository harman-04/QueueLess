// src/pages/Login.jsx

import { useFormik } from 'formik';
import { loginSchema } from '../validation/authSchema';
import axiosInstance from '../utils/axiosInstance';
import AuthFormWrapper from '../components/AuthFormWrapper';
import { useNavigate, Link } from 'react-router-dom';
import { toast } from 'react-toastify';
import { FaLock, FaEnvelope } from 'react-icons/fa';
import { useDispatch } from 'react-redux';
import { loginSuccess } from '../redux/authSlice';
import './Login.css';

const Login = () => {
  const navigate = useNavigate();
  const dispatch = useDispatch();

  const { values, errors, touched, handleChange, handleSubmit } = useFormik({
    initialValues: {
      email: '',
      password: '',
    },
    validationSchema: loginSchema,
    onSubmit: async (values) => {
      try {
        const response = await axiosInstance.post('/login', values);
        const { token, role, name, userId } = response.data; // ✅ Now getting 'name' from the response

        // ✅ Dispatch Redux action with the 'name'
        dispatch(loginSuccess({ token, role, name, userId }));

        toast.success(`Welcome back, ${name}!`);

        switch (role) {
          case 'USER':
            navigate('/user/dashboard');
            break;
          case 'PROVIDER':
            navigate('/provider/dashboard');
            break;
          case 'ADMIN':
            navigate('/admin/dashboard');
            break;
          default:
            navigate('/');
        }
      } catch (err) {
        toast.error(err.response?.data || 'Login failed');
      }
    },
  });

  return (
    <AuthFormWrapper title="Welcome Back 👋">
      <form onSubmit={handleSubmit} className="login-form">
        <div className="mb-4">
          <label className="form-label"><FaEnvelope className="icon" /> Email</label>
          <input
            type="email"
            name="email"
            className={`form-control ${touched.email && errors.email ? 'is-invalid' : ''}`}
            value={values.email}
            onChange={handleChange}
            placeholder="Enter your email"
          />
          {touched.email && errors.email && (
            <div className="invalid-feedback">{errors.email}</div>
          )}
        </div>

        <div className="mb-4">
          <label className="form-label"><FaLock className="icon" /> Password</label>
          <input
            type="password"
            name="password"
            className={`form-control ${touched.password && errors.password ? 'is-invalid' : ''}`}
            value={values.password}
            onChange={handleChange}
            placeholder="Enter your password"
          />
          {touched.password && errors.password && (
            <div className="invalid-feedback">{errors.password}</div>
          )}
        </div>

        <div className="d-flex justify-content-between align-items-center mb-3">
          <Link to="/forgot-password" className="text-decoration-none forgot-link">Forgot Password?</Link>
          <Link to="/register" className="text-decoration-none create-link">Don't have an account? Register</Link>
        </div>

        <button className="btn btn-primary w-100 login-btn" type="submit">
          Login
        </button>
      </form>
    </AuthFormWrapper>
  );
};

export default Login;