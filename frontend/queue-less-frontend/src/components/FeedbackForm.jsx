import React, { useState } from 'react';
import { Form, Button, Card, Row, Col, Modal } from 'react-bootstrap';
import { FaStar, FaComment, FaTimes, FaSmile, FaMeh, FaFrown } from 'react-icons/fa';
import { toast } from 'react-toastify';
import axiosInstance from '../utils/axiosInstance';
import './FeedbackForm.css';

const FeedbackForm = ({ show, onHide, tokenId, queueId, onFeedbackSubmitted }) => {
  const [formData, setFormData] = useState({
    rating: 0,
    comment: '',
    staffRating: 0,
    serviceRating: 0,
    waitTimeRating: 0
  });
  const [submitting, setSubmitting] = useState(false);
  const [hoveredRating, setHoveredRating] = useState(0);

  const ratingLabels = {
    1: 'Poor',
    2: 'Fair',
    3: 'Good',
    4: 'Very Good',
    5: 'Excellent'
  };

  const handleChange = (field, value) => {
    setFormData(prev => ({ ...prev, [field]: value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
   
    if (formData.rating === 0) {
      toast.error('Please provide an overall rating');
      return;
    }

    setSubmitting(true);
   
    try {
      await axiosInstance.post('/feedback', {
        tokenId,
        queueId,
        ...formData
      });
     
      toast.success('Thank you for your feedback!');
      onFeedbackSubmitted();
      onHide();
      resetForm();
    } catch (error) {
      console.error('Feedback submission error:', error);
      const errorMessage = error.response?.data?.message || 'Failed to submit feedback';
      
      if (error.response?.status === 409) {
        toast.info('You have already submitted feedback for this token');
      } else {
        toast.error(errorMessage);
      }
    } finally {
      setSubmitting(false);
    }
  };

  const resetForm = () => {
    setFormData({
      rating: 0,
      comment: '',
      staffRating: 0,
      serviceRating: 0,
      waitTimeRating: 0
    });
    setHoveredRating(0);
  };

  const StarRating = ({ rating, setRating, label, size = 'md' }) => (
    <div className="star-rating-group">
      <label>{label}</label>
      <div className={`stars stars-${size}`}>
        {[1, 2, 3, 4, 5].map((star) => (
          <div
            key={star}
            className="star-container"
            onMouseEnter={() => setHoveredRating(star)}
            onMouseLeave={() => setHoveredRating(0)}
          >
            <FaStar
              className={star <= (hoveredRating || rating) ? 'star filled' : 'star'}
              onClick={() => setRating(star)}
            />
          </div>
        ))}
      </div>
      <div className="rating-label">
        {rating > 0 && ratingLabels[rating]}
      </div>
    </div>
  );

  const RatingEmoji = ({ rating }) => {
    if (rating >= 4) return <FaSmile className="text-success me-2" />;
    if (rating >= 3) return <FaMeh className="text-warning me-2" />;
    return <FaFrown className="text-danger me-2" />;
  };

  return (
    <Modal show={show} onHide={onHide} size="lg" centered className="feedback-modal">
      <Modal.Header closeButton className="border-bottom-0 pb-0">
        <Modal.Title className="w-100 text-center">
          <div className="feedback-header-icon">
            <FaStar />
          </div>
          <h4 className="mt-2">Share Your Experience</h4>
          <p className="text-muted small">Your feedback helps us improve our service</p>
        </Modal.Title>
      </Modal.Header>
      <Modal.Body className="pt-0">
        <Card className="feedback-card">
          <Card.Body>
            <Form onSubmit={handleSubmit}>
              <div className="rating-section main-rating">
                <StarRating
                  rating={formData.rating}
                  setRating={(value) => handleChange('rating', value)}
                  label="Overall Rating*"
                  size="lg"
                />
              </div>

              <div className="detailed-ratings-section">
                <h6 className="section-title">Rate specific aspects (optional)</h6>
                <Row className="detailed-ratings">
                  <Col md={4}>
                    <StarRating
                      rating={formData.staffRating}
                      setRating={(value) => handleChange('staffRating', value)}
                      label="Staff Courtesy"
                    />
                  </Col>
                  <Col md={4}>
                    <StarRating
                      rating={formData.serviceRating}
                      setRating={(value) => handleChange('serviceRating', value)}
                      label="Service Quality"
                    />
                  </Col>
                  <Col md={4}>
                    <StarRating
                      rating={formData.waitTimeRating}
                      setRating={(value) => handleChange('waitTimeRating', value)}
                      label="Wait Time"
                    />
                  </Col>
                </Row>
              </div>

              <Form.Group className="mb-3">
                <Form.Label>
                  <FaComment className="me-2" />
                  Additional Comments
                </Form.Label>
                <Form.Control
                  as="textarea"
                  rows={3}
                  placeholder="Tell us more about your experience..."
                  value={formData.comment}
                  onChange={(e) => handleChange('comment', e.target.value)}
                  maxLength={500}
                />
                <div className="text-end">
                  <small className="text-muted">{formData.comment.length}/500 characters</small>
                </div>
              </Form.Group>

              <div className="d-flex gap-2">
                <Button
                  variant="primary"
                  type="submit"
                  disabled={submitting || formData.rating === 0}
                  className="flex-fill submit-button"
                >
                  {submitting ? (
                    <>
                      <span className="spinner-border spinner-border-sm me-2" />
                      Submitting...
                    </>
                  ) : (
                    'Submit Feedback'
                  )}
                </Button>
                <Button
                  variant="outline-secondary"
                  onClick={onHide}
                >
                  Cancel
                </Button>
              </div>
            </Form>
          </Card.Body>
        </Card>
      </Modal.Body>
    </Modal>
  );
};

export default FeedbackForm;