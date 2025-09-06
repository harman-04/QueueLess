// src/components/ProtectedRoute.jsx
import React from 'react';
import { useSelector } from 'react-redux';
import { Navigate, useLocation } from 'react-router-dom';
import toast from 'react-hot-toast';

const ProtectedRoute = ({ children, allowedRoles = [] }) => {
  const { token, role, isVerified } = useSelector((state) => state.auth);
  const location = useLocation();

  // If no token, redirect to login
  if (!token) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  // Check if user has the required role
  if (allowedRoles.length > 0 && !allowedRoles.includes(role)) {
    toast.error("You don't have permission to access this page");
    
    // Redirect to appropriate dashboard based on role
    switch (role) {
      case 'ADMIN':
        return <Navigate to="/admin/dashboard" replace />;
      case 'PROVIDER':
        return <Navigate to="/provider/queues" replace />;
      case 'USER':
        return <Navigate to="/user/dashboard" replace />;
      default:
        return <Navigate to="/" replace />;
    }
  }

  // For providers and admins, check if account is verified
  if (role !== 'USER' && !isVerified) {
    toast.warning("Your account is pending verification. Some features may be limited.");
  }

  return children;
};
// Add this line at the end of the file
export default ProtectedRoute;