// src/components/ErrorBoundary.jsx
import React from 'react';
import { Alert, Button } from 'react-bootstrap';
import { FaExclamationTriangle } from 'react-icons/fa';
import './ErrorBoundary.css';
class ErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, error };
  }

  componentDidCatch(error, errorInfo) {
    console.error('Error caught by boundary:', error, errorInfo);
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="error-boundary text-center py-5">
          <FaExclamationTriangle size={48} className="text-danger mb-3" />
          <Alert variant="danger">
            <h4>Something went wrong</h4>
            <p>{this.state.error?.message || 'An unexpected error occurred.'}</p>
            <Button variant="outline-danger" onClick={() => window.location.reload()}>
              Reload Page
            </Button>
          </Alert>
        </div>
      );
    }
    return this.props.children;
  }
}

export default ErrorBoundary;