//src/components/AuthFormWrapper.jsx
import './AuthFormWrapper.css';

const AuthFormWrapper = ({ children, title }) => (
  <div className="auth-form-wrapper">
    <div className="card shadow p-4">
      <h3 className="text-center mb-4">{title}</h3>
      {children}
    </div>
  </div>
);
export default AuthFormWrapper;
