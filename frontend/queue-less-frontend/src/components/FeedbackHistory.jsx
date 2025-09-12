import React, { useState, useEffect } from 'react';
import { Card, Spinner, Alert, Badge, Button, Row, Col, Form } from 'react-bootstrap';
import { FaUser, FaClock, FaComment, FaFilter, FaStar, FaCaretDown, FaCaretUp } from 'react-icons/fa';
import RatingDisplay from './RatingDisplay';
import axiosInstance from '../utils/axiosInstance';
import './FeedbackHistory.css';

const FeedbackHistory = ({ placeId, providerId }) => {
  const [feedbacks, setFeedbacks] = useState([]);
  const [filteredFeedbacks, setFilteredFeedbacks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [filters, setFilters] = useState({
    rating: null,
    hasComment: false,
    sortBy: 'newest'
  });
  const [showFilters, setShowFilters] = useState(false);
  const [visibleCount, setVisibleCount] = useState(5);

  useEffect(() => {
    fetchFeedbacks();
  }, [placeId, providerId]);

  useEffect(() => {
    applyFilters();
  }, [feedbacks, filters]);

  const fetchFeedbacks = async () => {
    try {
      setLoading(true);
      let url = '';
      if (placeId) {
        url = `/feedback/place/${placeId}`;
      } else if (providerId) {
        url = `/feedback/provider/${providerId}`;
      } else {
        return;
      }
      const response = await axiosInstance.get(url);
      setFeedbacks(response.data);
    } catch (error) {
      console.error('Error fetching feedbacks:', error);
      setError('Failed to load feedbacks');
    } finally {
      setLoading(false);
    }
  };

  const applyFilters = () => {
    let result = [...feedbacks];

    if (filters.rating) {
      result = result.filter(feedback => feedback.rating === parseInt(filters.rating));
    }

    if (filters.hasComment) {
      result = result.filter(feedback => feedback.comment && feedback.comment.trim().length > 0);
    }

    if (filters.sortBy === 'newest') {
      result.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
    } else if (filters.sortBy === 'oldest') {
      result.sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt));
    } else if (filters.sortBy === 'highest') {
      result.sort((a, b) => b.rating - a.rating);
    } else if (filters.sortBy === 'lowest') {
      result.sort((a, b) => a.rating - b.rating);
    }

    setFilteredFeedbacks(result);
    setVisibleCount(3);
  };

  const handleFilterChange = (filterName, value) => {
    setFilters(prev => ({
      ...prev,
      [filterName]: value
    }));
  };

  const clearFilters = () => {
    setFilters({
      rating: null,
      hasComment: false,
      sortBy: 'newest'
    });
  };

  const handleShowMore = () => {
    setVisibleCount(prevCount => prevCount + 5);
  };

  const formatDate = (dateString) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const getRatingDistribution = () => {
    const distribution = { 5: 0, 4: 0, 3: 0, 2: 0, 1: 0 };
    feedbacks.forEach(feedback => {
      if (feedback.rating >= 1 && feedback.rating <= 5) {
        distribution[feedback.rating]++;
      }
    });
    return distribution;
  };

  if (loading) {
    return (
      <div className="text-center py-5">
        <Spinner animation="border" variant="primary" />
        <p className="mt-2 text-muted">Loading feedback...</p>
      </div>
    );
  }

  if (error) {
    return <Alert variant="danger" className="text-center">{error}</Alert>;
  }

  if (feedbacks.length === 0) {
    return (
      <Alert variant="info" className="text-center">
        No feedback available yet.
      </Alert>
    );
  }

  const ratingDistribution = getRatingDistribution();
  const totalRatings = feedbacks.length;
  const feedbacksToDisplay = filteredFeedbacks.slice(0, visibleCount);
  const showMoreButton = filteredFeedbacks.length > visibleCount;

  return (
    <div className="feedback-history-container">
      <Card className="feedback-history-card">
        <Card.Body>
          <div className="d-flex justify-content-between align-items-center mb-4">
            <h4 className="card-title mb-0">Customer Feedback</h4>
            <Button
              variant="outline-secondary"
              size="sm"
              onClick={() => setShowFilters(!showFilters)}
              className="filter-toggle-btn"
            >
              <FaFilter className="me-1" />
              Filters {showFilters ? <FaCaretUp /> : <FaCaretDown />}
            </Button>
          </div>

          {showFilters && (
            <div className="filter-panel mb-4">
              <Row className="g-3 align-items-end">
                <Col md={4} sm={6}>
                  <Form.Group>
                    <Form.Label className="filter-label">Rating</Form.Label>
                    <Form.Select
                      value={filters.rating || ''}
                      onChange={(e) => handleFilterChange('rating', e.target.value || null)}
                      className="filter-select"
                    >
                      <option value="">All Ratings</option>
                      <option value="5">5 Stars</option>
                      <option value="4">4 Stars</option>
                      <option value="3">3 Stars</option>
                      <option value="2">2 Stars</option>
                      <option value="1">1 Star</option>
                    </Form.Select>
                  </Form.Group>
                </Col>
                <Col md={4} sm={6}>
                  <Form.Group>
                    <Form.Label className="filter-label">Sort By</Form.Label>
                    <Form.Select
                      value={filters.sortBy}
                      onChange={(e) => handleFilterChange('sortBy', e.target.value)}
                      className="filter-select"
                    >
                      <option value="newest">Newest First</option>
                      <option value="highest">Highest Rated</option>
                      <option value="lowest">Lowest Rated</option>
                      <option value="oldest">Oldest First</option>
                    </Form.Select>
                  </Form.Group>
                </Col>
                <Col md={4} sm={12}>
                  <div className="d-flex align-items-center justify-content-between">
                    <Form.Check
                      type="checkbox"
                      label="Only with comments"
                      checked={filters.hasComment}
                      onChange={(e) => handleFilterChange('hasComment', e.target.checked)}
                      className="filter-checkbox"
                    />
                    <Button variant="link" onClick={clearFilters} className="clear-filters-btn">
                      Clear
                    </Button>
                  </div>
                </Col>
              </Row>
            </div>
          )}

          <div className="rating-summary-section mb-4">
            <h6 className="section-title">Rating Distribution</h6>
            <Row className="g-2">
              {[5, 4, 3, 2, 1].map(rating => (
                <Col xs={12} key={rating}>
                  <div className="rating-bar-container">
                    <div className="rating-bar-label">
                      <span>{rating} <FaStar className="text-warning" /></span>
                      <span className="text-muted small">
                        {ratingDistribution[rating]} ({Math.round((ratingDistribution[rating] / totalRatings) * 100)}%)
                      </span>
                    </div>
                    <div className="progress">
                      <div
                        className="progress-bar bg-warning"
                        role="progressbar"
                        style={{ width: `${(ratingDistribution[rating] / totalRatings) * 100}%` }}
                      />
                    </div>
                  </div>
                </Col>
              ))}
            </Row>
          </div>

          <div className="feedback-list">
            {feedbacksToDisplay.length > 0 ? (
              feedbacksToDisplay.map((feedback) => (
                <Card key={feedback.id} className="mb-3 feedback-item">
                  <Card.Body>
                    <div className="feedback-header">
                      <div className="user-info">
                        <div className="user-avatar-placeholder">
                          <FaUser />
                        </div>
                        <div className="user-details">
                          <div className="user-name">Anonymous User</div>
                          <RatingDisplay rating={feedback.rating} showNumber={true} size="sm" />
                        </div>
                      </div>
                      <div className="feedback-date">
                        <FaClock className="me-1" />
                        {formatDate(feedback.createdAt)}
                      </div>
                    </div>

                    {feedback.comment && (
                      <div className="feedback-comment">
                        <FaComment className="comment-icon" />
                        <p className="mb-0">{feedback.comment}</p>
                      </div>
                    )}

                    <div className="detailed-ratings-summary mt-3">
                      <h6 className="detailed-ratings-title">Detailed Ratings</h6>
                      <div className="detailed-ratings-grid">
                        {feedback.staffRating > 0 && (
                          <div className="rating-item">
                            <span>Staff:</span>
                            <RatingDisplay rating={feedback.staffRating} showNumber={true} size="sm" />
                          </div>
                        )}
                        {feedback.serviceRating > 0 && (
                          <div className="rating-item">
                            <span>Service:</span>
                            <RatingDisplay rating={feedback.serviceRating} showNumber={true} size="sm" />
                          </div>
                        )}
                        {feedback.waitTimeRating > 0 && (
                          <div className="rating-item">
                            <span>Wait Time:</span>
                            <RatingDisplay rating={feedback.waitTimeRating} showNumber={true} size="sm" />
                          </div>
                        )}
                      </div>
                    </div>
                  </Card.Body>
                </Card>
              ))
            ) : (
              <Alert variant="info" className="text-center">
                No feedback matches your filters.
              </Alert>
            )}
          </div>

          {showMoreButton && (
            <div className="text-center mt-4">
              <Button variant="outline-primary" onClick={handleShowMore} className="show-more-btn">
                Show More Feedback
              </Button>
            </div>
          )}
        </Card.Body>
      </Card>
    </div>
  );
};

export default FeedbackHistory;