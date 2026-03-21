// src/pages/AboutUs.jsx
import React from 'react';
import { Container, Row, Col, Card, Button } from 'react-bootstrap';
import { Link } from 'react-router-dom';
import {
  FaHeart, FaRocket, FaUsers, FaShieldAlt, FaMobileAlt, FaChartLine,
  FaQrcode, FaClock, FaCheckCircle, FaGithub, FaLinkedin, FaEnvelope
} from 'react-icons/fa';
import './AboutUs.css';

const AboutUs = () => {
  return (
    <div className="about-us-page">
      <Container className="py-5">
        {/* Hero Section */}
        <Row className="mb-5 text-center">
          <Col>
            <h1 className="display-3 fw-bold gradient-text">About QueueLess</h1>
            <p className="lead text-muted">Revolutionizing the way you wait</p>
          </Col>
        </Row>

        {/* Mission Statement */}
        <Row className="mb-5">
          <Col>
            <Card className="mission-card">
              <Card.Body className="text-center p-5">
                <FaRocket size={48} className="mission-icon mb-3" />
                <h2>Our Mission</h2>
                <p className="lead">
                  To eliminate the frustration of waiting by providing smart, real‑time queue management
                  that empowers businesses and delights customers.
                </p>
              </Card.Body>
            </Card>
          </Col>
        </Row>

        {/* Problem & Solution */}
        <Row className="mb-5 align-items-center">
          <Col md={6} className="mb-4">
            <div className="problem-card p-4 h-100">
              <h3 className="text-primary mb-3">The Problem</h3>
              <p>
                Long, unpredictable queues waste time, cause stress, and hurt business productivity.
                Traditional queue systems offer no transparency – customers are left guessing when their turn will come,
                and businesses lack insight into wait times or customer satisfaction.
              </p>
            </div>
          </Col>
          <Col md={6} className="mb-4">
            <div className="solution-card p-4 h-100">
              <h3 className="text-success mb-3">Our Solution</h3>
              <p>
                QueueLess provides a digital queue management system that works for any business.
                Customers can join queues remotely, track their position, and get notified when it's almost their turn.
                Businesses gain real‑time analytics, customer feedback, and the ability to manage multiple locations effortlessly.
              </p>
            </div>
          </Col>
        </Row>

        {/* Key Features */}
        <Row className="mb-5">
          <Col>
            <h2 className="text-center mb-4">What Makes QueueLess Different</h2>
            <Row className="g-4">
              <Col md={4}>
                <div className="feature-card text-center p-4 h-100">
                  <FaMobileAlt size={36} className="feature-icon mb-3" />
                  <h5>Join from Anywhere</h5>
                  <p>No more standing in line. Join queues using your smartphone, whether you're at home or on the way.</p>
                </div>
              </Col>
              <Col md={4}>
                <div className="feature-card text-center p-4 h-100">
                  <FaClock size={36} className="feature-icon mb-3" />
                  <h5>Real‑time Updates</h5>
                  <p>Live position, estimated wait time, and instant notifications when your turn approaches.</p>
                </div>
              </Col>
              <Col md={4}>
                <div className="feature-card text-center p-4 h-100">
                  <FaChartLine size={36} className="feature-icon mb-3" />
                  <h5>Powerful Analytics</h5>
                  <p>Understand peak hours, average wait times, and customer satisfaction with detailed dashboards.</p>
                </div>
              </Col>
              <Col md={4}>
                <div className="feature-card text-center p-4 h-100">
                  <FaQrcode size={36} className="feature-icon mb-3" />
                  <h5>QR Code Integration</h5>
                  <p>Scan a QR code at any location to join the queue instantly – no app download required.</p>
                </div>
              </Col>
              <Col md={4}>
                <div className="feature-card text-center p-4 h-100">
                  <FaUsers size={36} className="feature-icon mb-3" />
                  <h5>Multi‑Tenant Design</h5>
                  <p>One platform serves hospitals, banks, shops, restaurants, and more. Each business manages its own places and queues.</p>
                </div>
              </Col>
              <Col md={4}>
                <div className="feature-card text-center p-4 h-100">
                  <FaShieldAlt size={36} className="feature-icon mb-3" />
                  <h5>Secure & Reliable</h5>
                  <p>Enterprise‑grade security, role‑based access, and audit logging ensure your data is safe.</p>
                </div>
              </Col>
            </Row>
          </Col>
        </Row>

        {/* Technology Stack */}
        <Row className="mb-5">
          <Col>
            <h2 className="text-center mb-4">Built with Modern Technology</h2>
            <Row className="text-center">
              <Col md={3} sm={6} className="mb-3">
                <div className="tech-badge">
                  <strong>Backend</strong><br />
                  Java 25, Spring Boot 3.5, MongoDB, Redis
                </div>
              </Col>
              <Col md={3} sm={6} className="mb-3">
                <div className="tech-badge">
                  <strong>Frontend</strong><br />
                  React 18, Redux Toolkit, Bootstrap
                </div>
              </Col>
              <Col md={3} sm={6} className="mb-3">
                <div className="tech-badge">
                  <strong>Real‑time</strong><br />
                  WebSocket, STOMP, SockJS
                </div>
              </Col>
              <Col md={3} sm={6} className="mb-3">
                <div className="tech-badge">
                  <strong>DevOps</strong><br />
                  Docker, Prometheus, Grafana, Loki
                </div>
              </Col>
            </Row>
          </Col>
        </Row>

        {/* Philosophy */}
        <Row className="mb-5">
          <Col>
            <h2 className="text-center mb-4">Our Philosophy</h2>
            <div className="philosophy-card text-center p-5">
              <p className="lead">
                We believe that technology should serve people, not the other way around.
                QueueLess was born from the frustration of waiting in lines and the desire to create a seamless,
                respectful experience for everyone.
              </p>
              <p>
                Our team is passionate about open source, transparency, and building tools that make a real difference.
                We invite you to join our community – whether as a user, a contributor, or a partner.
              </p>
              <FaHeart size={32} className="text-danger mt-3" />
            </div>
          </Col>
        </Row>

        {/* Call to Action */}
        <Row>
          <Col>
            <Card className="cta-card text-center">
              <Card.Body className="p-5">
                <h3>Ready to transform your waiting experience?</h3>
                <p className="mb-4">
                  Join the thousands of businesses and customers who trust QueueLess.
                </p>
                <div className="d-flex justify-content-center gap-3 flex-wrap">
                  <Link to="/register" className="btn btn-primary btn-lg px-4">Get Started</Link>
                </div>
              </Card.Body>
            </Card>
          </Col>
        </Row>
      </Container>
    </div>
  );
};

export default AboutUs;