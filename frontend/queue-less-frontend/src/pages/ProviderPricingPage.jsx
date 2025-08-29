import React, { useState } from 'react';
import { Card, Button, Container, Row, Col, Form } from 'react-bootstrap';
import axios from 'axios';
import Swal from 'sweetalert2';
import { useFormik } from 'formik';
import * as Yup from 'yup';
import { Link } from 'react-router-dom';
import 'bootstrap-icons/font/bootstrap-icons.css';

const plans = [
  { name: "Basic", tokenType: "1_MONTH", price: 100, description: "Perfect for new providers." },
  { name: "Standard", tokenType: "1_YEAR", price: 500, description: "Great value for committed providers." },
  { name: "Premium", tokenType: "LIFETIME", price: 1000, description: "Full access for established providers." },
];

const ProviderPricingPage = () => {
  const [planLoading, setPlanLoading] = useState({});
  const [generatedToken, setGeneratedToken] = useState('');

  const formik = useFormik({
    initialValues: { email: '' },
    validationSchema: Yup.object({
      email: Yup.string().email('Invalid email address').required('Required'),
    }),
    onSubmit: () => {}, // Keep the onSubmit empty as we handle submission on button click
  });

  const handleBuy = async (tokenType) => {
    // Trigger form validation manually
    formik.handleSubmit();

    if (formik.errors.email) {
      Swal.fire('Error', 'Please enter a valid email address', 'error');
      return;
    }

    setPlanLoading(prevState => ({ ...prevState, [tokenType]: true }));

    try {
      const res = await axios.post('http://localhost:8080/api/payment/create-order', null, {
        params: {
          email: formik.values.email,
          role: 'PROVIDER',
          tokenType,
        }
      });

      const { amount, orderId, currency } = res.data;

      const options = {
        key: 'rzp_test_qye1jaTP5mlc26',
        amount: amount.toString(),
        currency,
        name: "QueueLess Provider Token",
        description: "Purchase Provider Token",
        order_id: orderId,
        handler: async (response) => {
          const confirmRes = await axios.post('http://localhost:8080/api/payment/confirm-provider', null, {
            params: {
              orderId: orderId,
              paymentId: response.razorpay_payment_id,
              email: formik.values.email,
              tokenType,
            }
          });

          setGeneratedToken(confirmRes.data.tokenValue);
          Swal.fire('Success', 'Payment Successful! Token Generated.', 'success');
        },
        prefill: {
          email: formik.values.email,
        },
        theme: {
          color: "#0d6efd",
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
    Swal.fire('Copied!', 'Token copied to clipboard.', 'success');
  };

  return (
    <Container className="py-5" style={{ backgroundColor: '#f0f8ff' }}> {/* Light blue background */}
      <h2 className="text-center mb-5 text-success fw-bold">Become a Valued Provider</h2> {/* Engaging title */}
      <p className="text-center lead mb-4 text-muted">Select a plan to unlock provider features.</p> {/* Clear subtitle */}

      <Form className="mb-5" onSubmit={formik.handleSubmit}>
        <Row className="justify-content-center">
          <Col md={6}>
            <Form.Group className="mb-3" controlId="formBasicEmail">
              <Form.Label className="fw-bold">Enter your professional email:</Form.Label>
              <Form.Control
                type="email"
                name="email"
                placeholder="professional@email.com"
                value={formik.values.email}
                onChange={formik.handleChange}
                isInvalid={formik.touched.email && formik.errors.email}
              />
              <Form.Control.Feedback type="invalid">{formik.errors.email}</Form.Control.Feedback>
              <Form.Text className="text-muted">We'll use this to send your token.</Form.Text>
            </Form.Group>
          </Col>
        </Row>
      </Form>

      <Row className="justify-content-center">
        {plans.map((plan) => (
          <Col md={4} key={plan.tokenType} className="mb-4">
            <Card className="shadow-sm border-primary rounded-lg h-100 d-flex flex-column"> {/* Subtle shadow, primary border */}
              <Card.Body className="text-center py-4 flex-grow-1 d-flex flex-column justify-content-between">
                <div>
                  <Card.Title className="h5 fw-bold mb-3 text-primary">{plan.name} Plan</Card.Title>
                  <Card.Subtitle className="h6 mb-3 text-muted">{plan.price} ₹</Card.Subtitle>
                  <Card.Text className="mb-3 text-secondary">{plan.description}</Card.Text>
                </div>
                <Button
                  variant="primary"
                  disabled={planLoading[plan.tokenType]}
                  onClick={() => handleBuy(plan.tokenType)}
                  className="mt-3 rounded-pill"
                >
                  {planLoading[plan.tokenType] ? <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span> : <i className="bi bi-briefcase-fill me-2"></i>}
                  {planLoading[plan.tokenType] ? 'Processing...' : 'Buy Now'}
                </Button>
              </Card.Body>
            </Card>
          </Col>
        ))}
      </Row>

      {generatedToken && (
        <div className="text-center mt-5 p-4 bg-light rounded-lg shadow-sm border border-success"> {/* Success border */}
          <h4 className="text-success fw-bold mb-3"><i className="bi bi-key-fill me-2"></i> Your Provider Token is Ready!</h4>
          <p className="fw-bold text-info mb-3 selectable">{generatedToken}</p>
          <Button variant="outline-success" onClick={handleCopy} className="rounded-pill me-2">
            <i className="bi bi-clipboard-check me-2"></i> Copy Token
          </Button>
          <Link to="/register" className="ms-2">
            <Button variant="success" className="rounded-pill">
              <i className="bi bi-person-plus-fill me-2"></i> Proceed to Registration
            </Button>
          </Link>
          <p className="mt-3 text-info small">
            <i className="bi bi-info-circle-fill me-2"></i>
            Copy your token and click <strong className="fw-bold">Proceed to Registration</strong>.
          </p>
        </div>
      )}
    </Container>
  );
};

export default ProviderPricingPage;