// src/pages/Contact.jsx
import React, { useState } from 'react';
import { Container, Row, Col, Card, Form, Button, Alert, ListGroup } from 'react-bootstrap';
import { FaMapMarkerAlt, FaPhoneAlt, FaEnvelope, FaClock, FaFacebook, FaTwitter, FaInstagram, FaLinkedin } from 'react-icons/fa';
import { toast } from 'react-toastify';
// import './Contact.css'; // optional, you can add your own styles

const Contact = () => {
  const [formData, setFormData] = useState({
    name: '',
    email: '',
    subject: '',
    message: ''
  });
  const [submitting, setSubmitting] = useState(false);
  const [success, setSuccess] = useState(false);

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    // Replace with your actual backend endpoint
    try {
      // const response = await axiosInstance.post('/contact', formData);
      // For now, simulate success
      await new Promise(resolve => setTimeout(resolve, 1000));
      toast.success('Message sent successfully!');
      setSuccess(true);
      setFormData({ name: '', email: '', subject: '', message: '' });
      setTimeout(() => setSuccess(false), 5000);
    } catch (error) {
      toast.error('Failed to send message. Please try again.');
    } finally {
      setSubmitting(false);
    }
  };

  // Business hours data – replace with actual hours
  const businessHours = [
    { day: 'Monday - Friday', hours: '9:00 AM – 6:00 PM' },
    { day: 'Saturday', hours: '10:00 AM – 4:00 PM' },
    { day: 'Sunday', hours: 'Closed' }
  ];

  // Social links – update with your actual URLs
  const socialLinks = [
    { icon: FaFacebook, url: 'https://facebook.com/queueless', label: 'Facebook' },
    { icon: FaTwitter, url: 'https://twitter.com/queueless', label: 'Twitter' },
    { icon: FaInstagram, url: 'https://instagram.com/queueless', label: 'Instagram' },
    { icon: FaLinkedin, url: 'https://linkedin.com/company/queueless', label: 'LinkedIn' }
  ];

  return (
    <Container className="py-5">
      <h1 className="text-center mb-5">Contact Us</h1>
      <Row>
        {/* Left Column – Contact Info & Business Hours */}
        <Col lg={5} className="mb-4 mb-lg-0">
          <Card className="shadow-sm mb-4">
            <Card.Body>
              <h4>Get in Touch</h4>
              <ListGroup variant="flush">
                <ListGroup.Item>
                  <FaMapMarkerAlt className="me-2 text-primary" />
                  <strong>Address:</strong><br />
                  123 QueueLess Street<br />
                  Tech City, TC 12345
                </ListGroup.Item>
                <ListGroup.Item>
                  <FaPhoneAlt className="me-2 text-primary" />
                  <strong>Phone:</strong><br />
                  +91 123 456 7890
                </ListGroup.Item>
                <ListGroup.Item>
                  <FaEnvelope className="me-2 text-primary" />
                  <strong>Email:</strong><br />
                  support@queueless.com
                </ListGroup.Item>
              </ListGroup>
            </Card.Body>
          </Card>

          <Card className="shadow-sm">
            <Card.Body>
              <h4>Business Hours</h4>
              <ListGroup variant="flush">
                {businessHours.map((item, idx) => (
                  <ListGroup.Item key={idx} className="d-flex justify-content-between">
                    <span>{item.day}</span>
                    <span>{item.hours}</span>
                  </ListGroup.Item>
                ))}
              </ListGroup>
            </Card.Body>
          </Card>

          {/* Social Links */}
          <div className="mt-4 text-center">
            <h5>Follow Us</h5>
            <div className="d-flex justify-content-center gap-3 mt-2">
              {socialLinks.map((social, idx) => (
                <a
                  key={idx}
                  href={social.url}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="text-decoration-none"
                  style={{ fontSize: '1.8rem', color: '#6c757d' }}
                >
                  <social.icon />
                </a>
              ))}
            </div>
          </div>
        </Col>

        {/* Right Column – Contact Form */}
        <Col lg={7}>
          <Card className="shadow-sm">
            <Card.Body>
              <h4>Send Us a Message</h4>
              {success && (
                <Alert variant="success" dismissible onClose={() => setSuccess(false)}>
                  Your message has been sent. We'll get back to you soon!
                </Alert>
              )}
              <Form onSubmit={handleSubmit}>
                <Form.Group className="mb-3" controlId="contactName">
                  <Form.Label>Name *</Form.Label>
                  <Form.Control
                    type="text"
                    name="name"
                    value={formData.name}
                    onChange={handleChange}
                    required
                    placeholder="Your full name"
                  />
                </Form.Group>
                <Form.Group className="mb-3" controlId="contactEmail">
                  <Form.Label>Email *</Form.Label>
                  <Form.Control
                    type="email"
                    name="email"
                    value={formData.email}
                    onChange={handleChange}
                    required
                    placeholder="your@email.com"
                  />
                </Form.Group>
                <Form.Group className="mb-3" controlId="contactSubject">
                  <Form.Label>Subject</Form.Label>
                  <Form.Control
                    type="text"
                    name="subject"
                    value={formData.subject}
                    onChange={handleChange}
                    placeholder="What is this regarding?"
                  />
                </Form.Group>
                <Form.Group className="mb-3" controlId="contactMessage">
                  <Form.Label>Message *</Form.Label>
                  <Form.Control
                    as="textarea"
                    rows={5}
                    name="message"
                    value={formData.message}
                    onChange={handleChange}
                    required
                    placeholder="Your message..."
                  />
                </Form.Group>
                <Button
                  variant="primary"
                  type="submit"
                  disabled={submitting}
                  className="w-100"
                >
                  {submitting ? 'Sending...' : 'Send Message'}
                </Button>
              </Form>
            </Card.Body>
          </Card>
        </Col>
      </Row>

      {/* Map Section */}
      <Row className="mt-5">
        <Col>
          <Card className="shadow-sm">
            <Card.Body>
              <h4>Our Location</h4>
              <div className="ratio ratio-16x9">
                {/* Replace src with your actual Google Maps embed URL */}
                <iframe
                  title="QueueLess Location"
                  src="https://www.google.com/maps/embed?pb=!1m18!1m12!1m3!1d3500.0!2d77.0!3d28.0!2m3!1f0!2f0!3f0!3m2!1i1024!2i768!4f13.1!3m3!1m2!1s0x0%3A0x0!2zMjjCsDAwJzAwLjAiTiA3N8KwMDAnMDAuMCJF!5e0!3m2!1sen!2sin!4v1234567890"
                  style={{ border: 0 }}
                  allowFullScreen
                  loading="lazy"
                  referrerPolicy="no-referrer-when-downgrade"
                />
              </div>
            </Card.Body>
          </Card>
        </Col>
      </Row>

      {/* Frequently Asked Questions (Optional) */}
      <Row className="mt-5">
        <Col>
          <Card className="shadow-sm">
            <Card.Body>
              <h4>Frequently Asked Questions</h4>
              <p><strong>Q: How do I join a queue?</strong><br />A: Search for a place, select a service, and click "Join Queue". You'll receive a token and real‑time updates.</p>
              <p><strong>Q: Can I cancel my token?</strong><br />A: Yes, from the queue page or your dashboard.</p>
              <p><strong>Q: How do I become a provider?</strong><br />A: Contact us or purchase a provider token from the pricing page.</p>
              <p className="mb-0"><strong>Q: I'm an admin, how do I add a provider?</strong><br />A: Go to Admin Dashboard → Providers → Buy Provider Tokens, then share the token with the provider.</p>
            </Card.Body>
          </Card>
        </Col>
      </Row>
    </Container>
  );
};

export default Contact;