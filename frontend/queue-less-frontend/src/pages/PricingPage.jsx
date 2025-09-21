//src/pages/PricingPage.jsx
import React, { useState } from 'react';
import { Card, Button, Container, Row, Col, Form } from 'react-bootstrap';
import axios from 'axios';
import Swal from 'sweetalert2';
import { useFormik } from 'formik';
import * as Yup from 'yup';
import { Link } from 'react-router-dom';
import 'bootstrap-icons/font/bootstrap-icons.css';

const plans = [
  { name: "Basic Admin", tokenType: "1_MONTH", price: 100, description: "Ideal for foundational admin tasks." },
  { name: "Standard Admin", tokenType: "1_YEAR", price: 500, description: "Enhanced tools for efficient management." },
  { name: "Enterprise Admin", tokenType: "LIFETIME", price: 1000, description: "Comprehensive control for platform leadership." },
];

// Define a list of trusted email domains for validation
const TRUSTED_DOMAINS = ['gmail.com', 'yahoo.com', 'outlook.com', 'hotmail.com'];

const PricingPage = () => {
  const [planLoading, setPlanLoading] = useState({});
  const [generatedToken, setGeneratedToken] = useState('');

  const formik = useFormik({
    initialValues: { email: '' },
    validationSchema: Yup.object({
      email: Yup.string()
        .email('Invalid email address')
        .required('Required')
        .test('is-trusted-domain', 'Only trusted email providers are allowed (e.g., gmail.com, yahoo.com).', (value) => {
          if (!value) return false;
          const domain = value.split('@')[1];
          return TRUSTED_DOMAINS.includes(domain);
        }),
    }),
    onSubmit: () => {},
  });

  const handleBuy = async (tokenType) => {
    formik.handleSubmit();

    // Check if there are any validation errors before proceeding
    if (!formik.isValid) {
      Swal.fire('Error', 'Please enter a valid email from a trusted domain.', 'error');
      return;
    }

    setPlanLoading(prevState => ({ ...prevState, [tokenType]: true }));

    try {
      const res = await axios.post('https://localhost:8443/api/payment/create-order', null, {
        params: {
          email: formik.values.email,
          role: 'ADMIN',
          tokenType,
        }
      });

      const { amount, orderId, currency } = res.data;

      const options = {
        key: 'rzp_test_qye1jaTP5mlc26',
        amount: amount.toString(),
        currency,
        name: "QueueLess Admin Access",
        description: "Purchase Admin Access Token",
        order_id: orderId,
        handler: async (response) => {
          const confirmRes = await axios.post('https://localhost:8443/api/payment/confirm', null, {
            params: {
              orderId: orderId,
              paymentId: response.razorpay_payment_id,
              email: formik.values.email,
              tokenType,
            }
          });

          setGeneratedToken(confirmRes.data.tokenValue);
          Swal.fire('Success', 'Admin Access Granted! Token Generated.', 'success');
        },
        prefill: {
          email: formik.values.email,
        },
        theme: {
          color: "#6610f2", // A more sophisticated purple
        },
      };

      const rzp = new window.Razorpay(options);
      rzp.open();

    } catch (error) {
      Swal.fire('Error', error?.response?.data?.message || 'Something went wrong!', 'error');
    } finally {
      setPlanLoading(prevState => ({ ...prevState, [tokenType]: false }));
    }
  };

  const handleCopy = () => {
    navigator.clipboard.writeText(generatedToken);
    Swal.fire('Copied!', 'Admin Token copied to clipboard.', 'success');
  };

  return (
    <Container className="py-5" style={{ backgroundColor: '#f0f8ff' }}> {/* Soft lavender background */}
      <div className="text-center mb-5">
        <h2 className="text-center mb-3 fw-bold" style={{ color: '#6610f2' }}>Elevate Your Control</h2> {/* Elegant title */}
        <p className="lead text-muted">Choose the perfect admin access plan for seamless management.</p>
      </div>

      <Form className="mb-5" onSubmit={formik.handleSubmit}>
        <Row className="justify-content-center">
          <Col md={6}>
            <Form.Group className="mb-4" controlId="formAdminEmail">
              <Form.Label className="fw-semibold" style={{ color: '#495057' }}>Enter your administrative email:</Form.Label>
              <Form.Control
                type="email"
                name="email"
                placeholder="admin@yourdomain.com"
                value={formik.values.email}
                onChange={formik.handleChange}
                onBlur={formik.handleBlur} // Add onBlur to trigger validation on field exit
                isInvalid={formik.touched.email && formik.errors.email}
                className="shadow-sm" // Subtle shadow on input
              />
              <Form.Control.Feedback type="invalid" className="text-danger">{formik.errors.email}</Form.Control.Feedback>
              <Form.Text className="text-muted small">This email will be associated with your admin privileges.</Form.Text>
            </Form.Group>
          </Col>
        </Row>
      </Form>

      <Row className="justify-content-center">
        {plans.map((plan) => (
          <Col md={4} key={plan.tokenType} className="mb-4">
            <Card className="shadow-lg border-0 rounded-lg h-100 d-flex flex-column" style={{ backgroundColor: '#ffffff' }}> {/* White cards */}
              <Card.Body className="text-center py-4 d-flex flex-column justify-content-between">
                <div>
                  <Card.Title className="h5 fw-bold mb-3" style={{ color: '#6610f2' }}>{plan.name}</Card.Title>
                  <Card.Subtitle className="h6 mb-3 text-muted"><span className="fw-bold" style={{ color: '#6c757d' }}>₹{plan.price}</span></Card.Subtitle>
                  <Card.Text className="mb-3 text-secondary">{plan.description}</Card.Text>
                </div>
                <Button
                  variant="primary" // Using primary for a consistent feel
                  disabled={planLoading[plan.tokenType] || !formik.isValid || !formik.values.email} // Disable if form is invalid or email is empty
                  onClick={() => handleBuy(plan.tokenType)}
                  className="mt-3 rounded-pill shadow-sm"
                  style={{ backgroundColor: '#6610f2', borderColor: '#6610f2' }}
                >
                  {planLoading[plan.tokenType] ? <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span> : <i className="bi bi-key-fill me-2"></i>} {/* Key icon for admin */}
                  {planLoading[plan.tokenType] ? 'Processing...' : 'Get Admin Access'}
                </Button>
              </Card.Body>
            </Card>
          </Col>
        ))}
      </Row>

      {generatedToken && (
        <div className="text-center mt-5 p-4 bg-white rounded-lg shadow-sm border border-primary"> {/* White background for token */}
          <h4 className="text-primary fw-bold mb-3"><i className="bi bi-check-circle-fill me-2"></i> Admin Access Token Ready!</h4> {/* Success icon */}
          <p className="fw-bold text-primary mb-3 selectable" style={{ backgroundColor: '#e7f1ff', padding: '10px', borderRadius: '5px' }}>{generatedToken}</p> {/* Highlighted token */}
          <Button variant="outline-primary" onClick={handleCopy} className="rounded-pill me-2 shadow-sm">
            <i className="bi bi-clipboard-check me-2"></i> Copy Token
          </Button>
          <Link to="/register" className="ms-2">
            <Button variant="primary" className="rounded-pill shadow-sm" style={{ backgroundColor: '#6610f2', borderColor: '#6610f2' }}>
              <i className="bi bi-person-plus-fill me-2"></i> Proceed to Admin Registration
            </Button>
          </Link>
          <p className="mt-3 text-muted small">
            <i className="bi bi-info-circle-fill me-2"></i>
            Securely copy your admin token and proceed to the registration page.
          </p>
        </div>
      )}
    </Container>
  );
};

export default PricingPage;
