const AuthFormWrapper = ({ children, title }) => (
  <div className="d-flex justify-content-center align-items-center min-vh-100 bg-light">
    <div className="card shadow p-4" style={{ maxWidth: '400px', width: '100%' }}>
      <h3 className="text-center mb-4">{title}</h3>
      {children}
    </div>
  </div>
);

export default AuthFormWrapper;
