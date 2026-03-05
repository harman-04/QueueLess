import React, { useState } from 'react';
import { Card, Button, Container, Row, Col, Form } from 'react-bootstrap';
import Swal from 'sweetalert2';
import { useFormik } from 'formik';
import * as Yup from 'yup';
import { Link } from 'react-router-dom';
import 'bootstrap-icons/font/bootstrap-icons.css';
import './PricingPage.css';
import { useSelector } from 'react-redux';
import axiosInstance from '../utils/axiosInstance'; // Use your existing axiosInstance

const RAZORPAY_KEY = import.meta.env.VITE_RAZORPAY_KEY_ID;

const plans = [
  { name: "Basic Admin", tokenType: "1_MONTH", price: 100, description: "Ideal for foundational admin tasks." },
  { name: "Standard Admin", tokenType: "1_YEAR", price: 500, description: "Enhanced tools for efficient management." },
  { name: "Enterprise Admin", tokenType: "LIFETIME", price: 1000, description: "Comprehensive control for platform leadership." },
];

const TRUSTED_DOMAINS = ['gmail.com', 'yahoo.com', 'outlook.com', 'hotmail.com'];

const PricingPage = () => {
  const [planLoading, setPlanLoading] = useState({});
  const [generatedToken, setGeneratedToken] = useState('');
  const { id: adminId } = useSelector((state) => state.auth || {});

  // Helper for theme-aware SweetAlert2
  const customSwal = (options) => {
    const isDark = document.body.classList.contains('dark-mode');
    return Swal.fire({
      background: isDark ? '#2d2d2d' : '#fff',
      color: isDark ? '#f8f9fa' : '#212529',
      confirmButtonColor: '#6610f2',
      ...options
    });
  };

  // Sleek Toast for Copy action
  const showCopyToast = () => {
    const isDark = document.body.classList.contains('dark-mode');
    const Toast = Swal.mixin({
      toast: true,
      position: 'top-end',
      showConfirmButton: false,
      timer: 2000,
      timerProgressBar: true,
      background: isDark ? '#2d2d2d' : '#fff',
      color: isDark ? '#f8f9fa' : '#212529',
    });
    Toast.fire({ icon: 'success', title: 'Admin Token copied!' });
  };

  const formik = useFormik({
    initialValues: { email: '' },
    validationSchema: Yup.object({
      email: Yup.string()
        .email('Invalid email address')
        .required('Required')
        .test('is-trusted-domain', 'Only trusted providers allowed (Gmail, Yahoo, etc).', (value) => {
          if (!value) return false;
          const domain = value.split('@')[1];
          return TRUSTED_DOMAINS.includes(domain);
        }),
    }),
    onSubmit: () => { },
  });

  const handleBuy = async (tokenType) => {
    formik.handleSubmit();

    if (!formik.isValid || !formik.values.email) {
      customSwal({ icon: 'error', title: 'Error', text: 'Please enter a valid administrative email.' });
      return;
    }

    setPlanLoading(prev => ({ ...prev, [tokenType]: true }));

    try {
      const res = await axiosInstance.post('/payment/create-order', null, {
        params: {
          email: formik.values.email,
          role: 'ADMIN',
          tokenType,
          adminId // Passing adminId if available
        }
      });

      const { amount, orderId, currency } = res.data;

      const options = {
        key: RAZORPAY_KEY,
        amount: amount.toString(),
        currency,
        name: "QueueLess Admin Access",
        description: "Purchase Admin Access Token",
        order_id: orderId,
        handler: async (response) => {
          try {
            const confirmRes = await axiosInstance.post('/payment/confirm', null, {
              params: {
                orderId: orderId,
                paymentId: response.razorpay_payment_id,
                email: formik.values.email,
                tokenType,
              }
            });

            setGeneratedToken(confirmRes.data.tokenValue);
            customSwal({ icon: 'success', title: 'Success', text: 'Admin Access Granted! Token Generated.' });
          } catch (error) {
            customSwal({ icon: 'error', title: 'Error', text: error?.response?.data?.message || 'Confirmation failed.' });
          }
        },
        prefill: { email: formik.values.email },
        theme: { color: "#6610f2" },
      };

      const rzp = new window.Razorpay(options);
      rzp.open();
    } catch (error) {
      customSwal({ icon: 'error', title: 'Error', text: error?.response?.data?.message || 'Order creation failed.' });
    } finally {
      setPlanLoading(prev => ({ ...prev, [tokenType]: false }));
    }
  };

  const handleCopy = () => {
    navigator.clipboard.writeText(generatedToken);
    showCopyToast();
  };

  return (
    <Container className="py-5 pricing-page-container">
      <div className="pricing-page-header">
        <h2>Elevate Your Control</h2>
        <p className="lead">Choose the perfect admin access plan for seamless management.</p>
      </div>

      <Form className="mb-5 pricing-form" onSubmit={formik.handleSubmit}>
        <Row className="justify-content-center">
          <Col md={6}>
            <Form.Group className="mb-4" controlId="formAdminEmail">
              <Form.Label className="fw-semibold">Administrative Email</Form.Label>
              <Form.Control
                type="email"
                name="email"
                placeholder="admin@yourdomain.com"
                value={formik.values.email}
                onChange={formik.handleChange}
                onBlur={formik.handleBlur}
                isInvalid={formik.touched.email && formik.errors.email}
              />
              <Form.Control.Feedback type="invalid">{formik.errors.email}</Form.Control.Feedback>
              <Form.Text className="text-muted small">This email will be associated with your admin privileges.</Form.Text>
            </Form.Group>
          </Col>
        </Row>
      </Form>

      <Row className="justify-content-center">
        {plans.map((plan) => (
          <Col lg={4} md={6} key={plan.tokenType} className="mb-4">
            <Card className="pricing-card">
              <Card.Body className="text-center">
                <Card.Title className="h5 fw-bold">{plan.name}</Card.Title>
                <Card.Subtitle className="h2 my-4">
                  <span className="price-currency">₹</span>{plan.price}
                </Card.Subtitle>
                <Card.Text className="text-secondary">{plan.description}</Card.Text>
                <Button
                  variant="primary"
                  disabled={planLoading[plan.tokenType] || !formik.isValid || !formik.values.email}
                  onClick={() => handleBuy(plan.tokenType)}
                  className="rounded-pill mt-3"
                >
                  {planLoading[plan.tokenType] ? (
                    <span className="spinner-border spinner-border-sm" role="status"></span>
                  ) : (
                    <><i className="bi bi-key-fill me-2"></i> Get Admin Access</>
                  )}
                </Button>
              </Card.Body>
            </Card>
          </Col>
        ))}
      </Row>

      {generatedToken && (
        <div className="text-center mt-5 p-4 token-success-box">
          <h4 className="fw-bold mb-3"><i className="bi bi-check-circle-fill me-2"></i> Admin Token Ready!</h4>
          <div className="selectable token-display mb-4">{generatedToken}</div>
          
          <div className="d-flex flex-wrap justify-content-center gap-2">
            <Button variant="outline-primary" onClick={handleCopy} className="rounded-pill">
              <i className="bi bi-clipboard-check me-2"></i> Copy Token
            </Button>
            <Link to="/register">
              <Button variant="primary" className="rounded-pill">
                <i className="bi bi-person-plus-fill me-2"></i> Proceed to Registration
              </Button>
            </Link>
          </div>
          <p className="mt-4 text-muted small">
            <i className="bi bi-info-circle-fill me-2"></i>
            Securely copy your admin token and proceed to registration.
          </p>
        </div>
      )}
    </Container>
  );
};

export default PricingPage;