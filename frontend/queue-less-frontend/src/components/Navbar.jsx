// src/components/Navbar.jsx
import React from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useSelector, useDispatch } from 'react-redux';
import { Button } from 'react-bootstrap';
import { logout } from '../redux/authSlice';

const Navbar = () => {
  const navigate = useNavigate();
  const dispatch = useDispatch();
  const { token, role } = useSelector((state) => state.auth);

  const isAuthenticated = !!token;

  const handleLogout = () => {
    dispatch(logout());
    navigate('/');
  };

  return (
    <nav className="navbar navbar-expand-lg navbar-dark bg-primary shadow-sm">
      <div className="container">
        <Link to="/" className="navbar-brand fw-bold fs-3">
          QueueLess
        </Link>

        <div className="d-flex gap-2">
          {!isAuthenticated ? (
            <>
              <Button variant="light" onClick={() => navigate('/login')}>
                Login
              </Button>
              <Button variant="outline-light" onClick={() => navigate('/register')}>
                Sign Up
              </Button>
              <Button variant="warning" onClick={() => navigate('/pricing')}>
                Be an Admin
              </Button>
            </>
          ) : (
            <>
              {role === 'ADMIN' && (
                <Button variant="info" onClick={() => navigate('/provider-pricing')}>
                  Make Providers
                </Button>
              )}
              <Button variant="outline-light" onClick={handleLogout}>
                Logout
              </Button>
            </>
          )}
        </div>
      </div>
    </nav>
  );
};

export default Navbar;