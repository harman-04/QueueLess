// src/pages/Login.jsx
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
        const response = await authService.login(values);
        console.log('Login response:', response); // Add this to debug
        
        if (response.data) {
          const { token, role, userId, name, profileImageUrl, placeId, isVerified, preferences, ownedPlaceIds } = response.data;

          // ✅ Dispatch Redux action with all user data
          dispatch(loginSuccess({ 
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
              favoritePlaceIds: []
            },
            ownedPlaceIds: ownedPlaceIds || []
          }));

          toast.success(`Welcome back, ${name}!`);

          // Redirect based on role
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
          console.error('No response data received');
          toast.error('Login failed: No response from server');
        }
      } catch (err) {
        console.error('Login error details:', err);
        console.error('Response data:', err.response?.data);
        console.error('Response status:', err.response?.status);
        console.error('Response headers:', err.response?.headers);
        toast.error(err.response?.data?.message || 'Login failed');

         const errorMessage = err.response?.data?.message || err.message || 'Login failed';
        toast.error(errorMessage);
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