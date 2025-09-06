//src/pages/Register.jsx
import { useFormik } from 'formik';
import { registerSchema } from '../validation/authSchema';
import { authService } from '../services/authService';
import AuthFormWrapper from '../components/AuthFormWrapper';
import { Link, useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import { useState } from 'react';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faUser, faEnvelope, faPhone, faLock, faKey, faBell, faGlobe, faPalette } from '@fortawesome/free-solid-svg-icons';
import '@fortawesome/fontawesome-free/css/all.min.css';

const Register = () => {
  const navigate = useNavigate();
  const [role, setRole] = useState('USER');
  const [showPreferences, setShowPreferences] = useState(false);

  const { values, errors, touched, handleChange, handleSubmit } = useFormik({
    initialValues: {
      name: '',
      email: '',
      phoneNumber: '',
      password: '',
      role: 'USER',
      token: '',
      placeId: '',
      preferences: {
        emailNotifications: true,
        smsNotifications: false,
        language: 'en',
        defaultSearchRadius: 5,
        darkMode: false
      }
    },
    validationSchema: registerSchema,
    onSubmit: async (values) => {
      try {
        // Only include token and placeId if needed
        const payload = {
          name: values.name,
          email: values.email,
          phoneNumber: values.phoneNumber,
          password: values.password,
          role: values.role,
          token: role !== 'USER' ? values.token : undefined,
          placeId: role === 'PROVIDER' ? values.placeId : undefined,
          preferences: values.preferences
        };
        
        await authService.register(payload);
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
        <div className="mb-3">
          <label className="form-label">Name</label>
          <div className="input-group">
            <span className="input-group-text bg-light border-end-0">
              <FontAwesomeIcon icon={faUser} className="text-secondary" />
            </span>
            <input
              type="text"
              name="name"
              className={`form-control border-start-0 ${touched.name && errors.name ? 'is-invalid' : ''}`}
              value={values.name}
              onChange={handleChange}
              placeholder="Enter your full name"
              autoComplete="off"
            />
            {touched.name && errors.name && <div className="invalid-feedback">{errors.name}</div>}
          </div>
        </div>

        <div className="mb-3">
          <label className="form-label">Email</label>
          <div className="input-group">
            <span className="input-group-text bg-light border-end-0">
              <FontAwesomeIcon icon={faEnvelope} className="text-secondary" />
            </span>
            <input
              type="email"
              name="email"
              className={`form-control border-start-0 ${touched.email && errors.email ? 'is-invalid' : ''}`}
              value={values.email}
              onChange={handleChange}
              placeholder="Your email address"
              autoComplete="off"
            />
            {touched.email && errors.email && <div className="invalid-feedback">{errors.email}</div>}
          </div>
        </div>

        <div className="mb-3">
          <label className="form-label">Phone Number</label>
          <div className="input-group">
            <span className="input-group-text bg-light border-end-0">
              <FontAwesomeIcon icon={faPhone} className="text-secondary" />
            </span>
            <input
              type="tel"
              name="phoneNumber"
              className={`form-control border-start-0 ${touched.phoneNumber && errors.phoneNumber ? 'is-invalid' : ''}`}
              value={values.phoneNumber}
              onChange={handleChange}
              placeholder="+91 9876543210"
              autoComplete="off"
            />
            {touched.phoneNumber && errors.phoneNumber && <div className="invalid-feedback">{errors.phoneNumber}</div>}
          </div>
        </div>

        <div className="mb-3">
          <label className="form-label">Password</label>
          <div className="input-group">
            <span className="input-group-text bg-light border-end-0">
              <FontAwesomeIcon icon={faLock} className="text-secondary" />
            </span>
            <input
              type="password"
              name="password"
              className={`form-control border-start-0 ${touched.password && errors.password ? 'is-invalid' : ''}`}
              value={values.password}
              onChange={handleChange}
              placeholder="Create a strong password"
              autoComplete="off"
            />
            {touched.password && errors.password && <div className="invalid-feedback">{errors.password}</div>}
          </div>
        </div>

        <div className="mb-3">
          <label className="form-label">Account Type</label>
          <select
            name="role"
            className="form-control shadow-sm"
            value={values.role}
            onChange={(e) => {
              handleChange(e);
              setRole(e.target.value);
            }}
          >
            <option value="USER">User</option>
            <option value="PROVIDER">Service Provider</option>
            <option value="ADMIN">Admin</option>
          </select>
        </div>

        {(role === 'ADMIN' || role === 'PROVIDER') && (
          <div className="mb-3">
            <label className="form-label">Registration Token</label>
            <div className="input-group">
              <span className="input-group-text bg-light border-end-0">
                <FontAwesomeIcon icon={faKey} className="text-secondary" />
              </span>
              <input
                type="text"
                name="token"
                className={`form-control border-start-0 ${touched.token && errors.token ? 'is-invalid' : ''}`}
                value={values.token}
                onChange={handleChange}
                placeholder="Paste your secure token"
                autoComplete="off"
              />
              {touched.token && errors.token && <div className="invalid-feedback">{errors.token}</div>}
            </div>
          </div>
        )}

        {role === 'PROVIDER' && (
          <div className="mb-3">
            <label className="form-label">Place ID (Optional)</label>
            <div className="input-group">
              <span className="input-group-text bg-light border-end-0">
                <FontAwesomeIcon icon={faUser} className="text-secondary" />
              </span>
              <input
                type="text"
                name="placeId"
                className={`form-control border-start-0 ${touched.placeId && errors.placeId ? 'is-invalid' : ''}`}
                value={values.placeId}
                onChange={handleChange}
                placeholder="Enter your place ID if applicable"
                autoComplete="off"
              />
              {touched.placeId && errors.placeId && <div className="invalid-feedback">{errors.placeId}</div>}
            </div>
          </div>
        )}

        <div className="mb-3">
          <button
            type="button"
            className="btn btn-outline-secondary w-100"
            onClick={() => setShowPreferences(!showPreferences)}
          >
            <FontAwesomeIcon icon={faPalette} className="me-2" />
            {showPreferences ? 'Hide Preferences' : 'Show Preferences'}
          </button>
        </div>

        {showPreferences && (
          <div className="border p-3 rounded mb-3">
            <h6 className="mb-3">Preferences</h6>
            
            <div className="form-check mb-2">
              <input
                type="checkbox"
                name="preferences.emailNotifications"
                className="form-check-input"
                checked={values.preferences.emailNotifications}
                onChange={handleChange}
                id="emailNotifications"
              />
              <label className="form-check-label" htmlFor="emailNotifications">
                <FontAwesomeIcon icon={faEnvelope} className="me-2" />
                Email Notifications
              </label>
            </div>
            
            <div className="form-check mb-2">
              <input
                type="checkbox"
                name="preferences.smsNotifications"
                className="form-check-input"
                checked={values.preferences.smsNotifications}
                onChange={handleChange}
                id="smsNotifications"
              />
              <label className="form-check-label" htmlFor="smsNotifications">
                <FontAwesomeIcon icon={faBell} className="me-2" />
                SMS Notifications
              </label>
            </div>
            
            <div className="form-check mb-2">
              <input
                type="checkbox"
                name="preferences.darkMode"
                className="form-check-input"
                checked={values.preferences.darkMode}
                onChange={handleChange}
                id="darkMode"
              />
              <label className="form-check-label" htmlFor="darkMode">
                <FontAwesomeIcon icon={faPalette} className="me-2" />
                Dark Mode
              </label>
            </div>
            
            <div className="mb-2">
              <label className="form-label">Language</label>
              <select
                name="preferences.language"
                className="form-control"
                value={values.preferences.language}
                onChange={handleChange}
              >
                <option value="en">English</option>
                <option value="es">Spanish</option>
                <option value="fr">French</option>
                <option value="de">German</option>
              </select>
            </div>
            
            <div className="mb-2">
              <label className="form-label">Default Search Radius (km)</label>
              <input
                type="number"
                name="preferences.defaultSearchRadius"
                className="form-control"
                value={values.preferences.defaultSearchRadius}
                onChange={handleChange}
                min="1"
                max="100"
              />
            </div>
          </div>
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
