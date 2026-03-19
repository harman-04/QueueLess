import React, { useState } from 'react';
import { Card, Button, Container, Row, Col, Form } from 'react-bootstrap';
import Swal from 'sweetalert2';
import { useFormik } from 'formik';
import * as Yup from 'yup';
import { Link } from 'react-router-dom';
import 'bootstrap-icons/font/bootstrap-icons.css';
import './ProviderPricingPage.css';
import { useSelector } from 'react-redux';
import axiosInstance from '../utils/axiosInstance';

const RAZORPAY_KEY = import.meta.env.VITE_RAZORPAY_KEY_ID;

const plans = [
  { name: "Basic", tokenType: "1_MONTH", price: 100, description: "Perfect for new providers." },
  { name: "Standard", tokenType: "1_YEAR", price: 500, description: "Great value for committed providers." },
  { name: "Premium", tokenType: "LIFETIME", price: 1000, description: "Full access for established providers." },
];

const trustedDomains = [
  'gmail.com', 'outlook.com', 'yahoo.com', 'protonmail.com',
  'zoho.com', 'icloud.com', 'yourcompany.com',
];

const ProviderPricingPage = () => {
  const [planLoading, setPlanLoading] = useState({});
  const [generatedToken, setGeneratedToken] = useState('');
  const { id: adminId } = useSelector((state) => state.auth);

  /**
   * Helper to handle SweetAlert2 Dark Mode automatically
   */
  const customSwal = (options) => {
    const isDark = document.body.classList.contains('dark-mode');
    return Swal.fire({
      background: isDark ? '#2d2d2d' : '#fff',
      color: isDark ? '#f8f9fa' : '#212529',
      confirmButtonColor: '#667eea',
      ...options
    });
  };

  /**
   * Specialized Toast for Copy action
   */
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
    Toast.fire({ icon: 'success', title: 'Token copied to clipboard!' });
  };

  const formik = useFormik({
    initialValues: { email: '' },
    validationSchema: Yup.object({
      email: Yup.string()
        .email('Invalid email address')
        .required('Required')
        .test('trusted-domain', 'Please use a trusted email provider (e.g., Gmail, Outlook)', (value) => {
          if (!value) return false;
          const domain = value.split('@')[1];
          return trustedDomains.includes(domain);
        }),
    }),
    onSubmit: () => {},
  });

  const handleBuy = async (tokenType) => {
    formik.handleSubmit();
    const providerEmail = formik.values.email;

    if (formik.errors.email || !providerEmail) {
      customSwal({ icon: 'error', title: 'Error', text: 'Please enter a valid and trusted email address' });
      return;
    }

    if (!adminId) {
      customSwal({ icon: 'error', title: 'Error', text: 'Authentication error: Admin ID not found. Please log in again.' });
      return;
    }

    setPlanLoading(prev => ({ ...prev, [tokenType]: true }));

    try {
      const res = await axiosInstance.post('/payment/create-order', null, {
        params: { email: providerEmail, role: 'PROVIDER', tokenType, adminId }
      });

      const { amount, orderId, currency } = res.data;

      const options = {
        key: RAZORPAY_KEY,
        amount: amount.toString(),
        currency,
        name: "QueueLess Provider Token",
        description: "Purchase Provider Token",
        order_id: orderId,
        handler: async (response) => {
          try {
            const confirmRes = await axiosInstance.post('/payment/confirm-provider', null, {
              params: {
                orderId,
                paymentId: response.razorpay_payment_id,
                providerEmail,
                tokenType,
                adminId,
              }
            });
            setGeneratedToken(confirmRes.data.tokenValue);
            customSwal({ icon: 'success', title: 'Success', text: 'Payment Successful! Token Generated.' });
          } catch (error) {
            customSwal({ icon: 'error', title: 'Error', text: error?.response?.data?.message || 'Payment confirmation failed.' });
          }
        },
        prefill: { email: providerEmail },
        theme: { color: "#667eea" },
      };

      const rzp = new window.Razorpay(options);
      rzp.open();
    } catch (error) {
      customSwal({ icon: 'error', title: 'Error', text: error?.response?.data?.message || 'Something went wrong!' });
    } finally {
      setPlanLoading(prev => ({ ...prev, [tokenType]: false }));
    }
  };

  const handleCopy = () => {
    navigator.clipboard.writeText(generatedToken);
    showCopyToast();
  };

  const domain = formik.values.email.split('@')[1];
  const isTrusted = trustedDomains.includes(domain);
  const isEmailValid = formik.values.email && !formik.errors.email && isTrusted;

  return (
    <Container className="py-5 pricing-page">
      <div className="text-center mb-5">
        <h2 className="fw-bold">Become a Valued Provider</h2>
        <p className="lead text-muted">Select a plan to unlock provider features.</p>
      </div>

      <Form className="mb-5" onSubmit={formik.handleSubmit}>
        <Row className="justify-content-center">
          <Col md={6}>
            <Form.Group className="form-floating mb-3">
              <Form.Control
                type="email"
                name="email"
                id="floatingEmail"
                placeholder="professional@email.com"
                value={formik.values.email}
                onChange={formik.handleChange}
                isInvalid={formik.touched.email && formik.errors.email}
              />
              <Form.Control.Feedback type="invalid">
                {formik.errors.email}
              </Form.Control.Feedback>
            </Form.Group>
          </Col>
        </Row>
      </Form>

      <Row className="justify-content-center">
        {plans.map((plan) => (
          <Col lg={4} md={6} key={plan.tokenType} className="mb-4">
            <Card className="pricing-card">
              <Card.Body className="text-center">
                <Card.Title className="h5 fw-bold">{plan.name} Plan</Card.Title>
                <Card.Subtitle className="h2 my-4">
                  <span className="price-currency">₹</span>{plan.price}
                </Card.Subtitle>
                <Card.Text className="text-secondary">{plan.description}</Card.Text>
                <Button
                  variant="primary"
                  disabled={planLoading[plan.tokenType] || !isEmailValid}
                  onClick={() => handleBuy(plan.tokenType)}
                  className="animated-button rounded-pill mt-3"
                >
                  {planLoading[plan.tokenType] ? (
                    <span className="spinner-border spinner-border-sm" role="status"></span>
                  ) : (
                    <><i className="bi bi-briefcase-fill me-2"></i> Buy Now</>
                  )}
                </Button>
              </Card.Body>
            </Card>
          </Col>
        ))}
      </Row>

      {generatedToken && (
        <div className="text-center mt-5 p-4 token-box">
          <h4 className="fw-bold mb-3"><i className="bi bi-key-fill me-2"></i> Your Provider Token is Ready!</h4>
          <div className="selectable mb-4">{generatedToken}</div>
          
          <div className="d-flex flex-wrap justify-content-center gap-2">
            <Button variant="outline-success" onClick={handleCopy} className="rounded-pill">
              <i className="bi bi-clipboard-check me-2"></i> Copy Token
            </Button>
            <Link to="/register">
              <Button variant="success" className="rounded-pill">
                <i className="bi bi-person-plus-fill me-2"></i> Proceed to Registration
              </Button>
            </Link>
          </div>
          
          <p className="mt-4 text-info small">
            <i className="bi bi-info-circle-fill me-2"></i>
            Copy your token and click <strong>Proceed to Registration</strong>.
          </p>
        </div>
      )}
    </Container>
  );
};

export default ProviderPricingPage;