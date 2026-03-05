import React from 'react';
import { Container, Row, Col, Button } from 'react-bootstrap';
import { FaHome, FaSadTear } from 'react-icons/fa';
import { useNavigate } from 'react-router-dom';
import 'animate.css';
import './NotFound.css';

const NotFound = () => {
  const navigate = useNavigate();

  return (
    <Container className="not-found-container animate__animated animate__fadeIn">
      <Row className="justify-content-center align-items-center min-vh-100">
        <Col md={10} lg={8} xl={6} className="text-center">
          <div className="not-found-content">
            <div className="not-found-icon-wrapper">
              <FaSadTear className="not-found-icon" />
            </div>
            <h1 className="not-found-title">404</h1>
            <h2 className="not-found-subtitle">Page Not Found</h2>
            <p className="not-found-text">
              Oops! The page you're looking for doesn't exist or has been moved.
            </p>
            <Button
              variant="primary"
              size="lg"
              className="not-found-button mt-4"
              onClick={() => navigate('/')}
            >
              <FaHome className="me-2" /> Back to Home
            </Button>
          </div>
        </Col>
      </Row>
    </Container>
  );
};

export default NotFound;