import { useFormik } from 'formik';
import { loginSchema } from '../validation/authSchema';
import { authService } from '../services/authService';
import AuthFormWrapper from '../components/AuthFormWrapper';
import { useNavigate, Link } from 'react-router-dom';
import { toast } from 'react-toastify';
import { FaLock, FaEnvelope } from 'react-icons/fa';
import { useDispatch } from 'react-redux';
import { loginSuccess } from '../redux/authSlice';
import './Login.css';
import { useState } from 'react';

const Login = () => {
  const navigate = useNavigate();
  const dispatch = useDispatch();
  const [isSubmitting, setIsSubmitting] = useState(false);

  const formik = useFormik({
    initialValues: {
      email: '',
      password: '',
    },
    validationSchema: loginSchema,
    onSubmit: async (values) => {
      setIsSubmitting(true);
      try {
        const response = await authService.login(values);

        if (response?.data) {
          const {
            token,
            role,
            userId,
            name,
            profileImageUrl,
            placeId,
            isVerified,
            preferences,
            ownedPlaceIds,
          } = response.data;

          dispatch(
            loginSuccess({
              token,
              role,
              userId,
              name,
              profileImageUrl: profileImageUrl || null,
              placeId: placeId || null,
              isVerified: isVerified || false,
              preferences: preferences || {
                emailNotifications: true,
                smsNotifications: false,
                language: 'en',
                defaultSearchRadius: 5,
                darkMode: false,
                favoritePlaceIds: [],
              },
              ownedPlaceIds: ownedPlaceIds || [],
            })
          );

          toast.success(`Welcome back, ${name}!`);

          switch (role) {
            case 'USER':
              navigate('/user/dashboard');
              break;
            case 'PROVIDER':
              navigate('/provider/queues');
              break;
            case 'ADMIN':
              navigate('/admin/dashboard');
              break;
            default:
              navigate('/');
          }
        } else {
          toast.error('Login failed: No response from server');
        }
      } catch (err) {
        const errorMessage =
          err.response?.data?.message || err.message || 'Login failed';
        toast.error(errorMessage);
        console.error('Login error:', errorMessage);
      } finally {
        setIsSubmitting(false);
      }
    },
  });

  const { values, errors, touched, handleChange, handleSubmit } = formik;

  return (
    <AuthFormWrapper title="Welcome Back 👋">
      <form onSubmit={handleSubmit} className="login-form" noValidate>
        <div className="mb-4">
          <label htmlFor="email" className="form-label">
            <FaEnvelope className="icon" /> Email
          </label>
          <input
            id="email"
            type="email"
            name="email"
            className={`form-control ${touched.email && errors.email ? 'is-invalid' : ''}`}
            value={values.email}
            onChange={handleChange}
            placeholder="Enter your email"
            autoComplete="email"
            required
          />
          {touched.email && errors.email && (
            <div className="invalid-feedback">{errors.email}</div>
          )}
        </div>

        <div className="mb-4">
          <label htmlFor="password" className="form-label">
            <FaLock className="icon" /> Password
          </label>
          <input
            id="password"
            type="password"
            name="password"
            className={`form-control ${touched.password && errors.password ? 'is-invalid' : ''}`}
            value={values.password}
            onChange={handleChange}
            placeholder="Enter your password"
            autoComplete="current-password"
            required
          />
          {touched.password && errors.password && (
            <div className="invalid-feedback">{errors.password}</div>
          )}
        </div>

        <div className="d-flex justify-content-between align-items-center mb-3">
          <Link to="/forgot-password" className="text-decoration-none forgot-link">
            Forgot Password?
          </Link>
          <Link to="/register" className="text-decoration-none create-link">
            Don't have an account? Register
          </Link>
        </div>

        <button
          className="btn btn-primary w-100 login-btn"
          type="submit"
          disabled={isSubmitting}
        >
          {isSubmitting ? (
            <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
          ) : null}
          {isSubmitting ? 'Logging in...' : 'Login'}
        </button>
      </form>
    </AuthFormWrapper>
  );
};

export default Login;