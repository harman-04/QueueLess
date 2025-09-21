import React, { useEffect, useState } from 'react';
import { Card, Row, Col } from 'react-bootstrap';
import { FaStar, FaStarHalfAlt, FaRegStar, FaInfoCircle } from 'react-icons/fa';
import axios from 'axios';
import './PlaceRatingSummary.css';

const PlaceRatingSummary = ({ placeId }) => {
  const [ratings, setRatings] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchRatings = async () => {
      try {
        setLoading(true);
        const response = await axios.get(`https://localhost:8443/api/feedback/place/${placeId}/detailed-ratings`);
        
        // Check if the ratings data is empty or all ratings are 0
        const hasRatings = response.data && 
                          (response.data.overall > 0 || 
                           response.data.staff > 0 || 
                           response.data.service > 0 || 
                           response.data.waitTime > 0);

        if (hasRatings) {
          setRatings(response.data);
          setError(null);
        } else {
          setRatings(null);
          setError("No ratings available yet.");
        }

      } catch (err) {
        console.error("Error fetching ratings:", err);
        setRatings(null);
        setError("Failed to load ratings.");
      } finally {
        setLoading(false);
      }
    };
    if (placeId) {
      fetchRatings();
    }
  }, [placeId]);

  const StarDisplay = ({ rating }) => {
    // If rating is 0 or less, display empty stars
    if (rating <= 0) {
      return (
        <div className="prs-star-display">
          {[...Array(5)].map((_, i) => (
            <FaRegStar key={`empty-${i}`} className="prs-star prs-star-empty" />
          ))}
        </div>
      );
    }

    const roundedRating = Math.round(rating * 2) / 2;
    const fullStars = Math.floor(roundedRating);
    const hasHalfStar = roundedRating % 1 !== 0;
    const emptyStars = 5 - fullStars - (hasHalfStar ? 1 : 0);
    
    return (
      <div className="prs-star-display">
        {[...Array(fullStars)].map((_, i) => (
          <FaStar key={`full-${i}`} className="prs-star prs-star-full" />
        ))}
        {hasHalfStar && <FaStarHalfAlt key="half-star" className="prs-star prs-star-half" />}
        {[...Array(emptyStars)].map((_, i) => (
          <FaRegStar key={`empty-${i}`} className="prs-star prs-star-empty" />
        ))}
      </div>
    );
  };
  
  if (loading) {
    return <div className="prs-loading">Loading ratings...</div>;
  }
  
  // This is the new conditional render for no ratings
  if (!ratings) {
    return (
      <Card className="prs-no-rating-card">
        <Card.Body className="text-center">
          <FaInfoCircle className="prs-info-icon" />
          <h5 className="mt-3 prs-no-rating-text">This place has no ratings yet.</h5>
          <p className="text-muted">Be the first to leave a review!</p>
        </Card.Body>
      </Card>
    );
  }

  return (
    <Card className="prs-rating-summary-card">
      <Card.Header className="prs-card-header">
        <h3 className="prs-title">Overall Ratings</h3>
      </Card.Header>
      <Card.Body className="prs-card-body">
        <Row className="prs-rating-row">
          <Col md={3} className="prs-main-rating-col">
            <div className="prs-overall-rating">
              <div className="prs-average-number">{ratings.overall.toFixed(1)}</div>
              <StarDisplay rating={ratings.overall} />
              <div className="prs-rating-label">Overall</div>
            </div>
          </Col>
          
          <Col md={9} className="prs-detail-ratings-col">
            <Row className="prs-detail-row">
              <Col xs={6} md={4} className="prs-rating-category">
                <div className="prs-category-rating">
                  <StarDisplay rating={ratings.staff} />
                  <div className="prs-category-value">{ratings.staff.toFixed(1)}</div>
                  <div className="prs-category-label">Staff</div>
                </div>
              </Col>
              
              <Col xs={6} md={4} className="prs-rating-category">
                <div className="prs-category-rating">
                  <StarDisplay rating={ratings.service} />
                  <div className="prs-category-value">{ratings.service.toFixed(1)}</div>
                  <div className="prs-category-label">Service</div>
                </div>
              </Col>
              
              <Col xs={6} md={4} className="prs-rating-category">
                <div className="prs-category-rating">
                  <StarDisplay rating={ratings.waitTime} />
                  <div className="prs-category-value">{ratings.waitTime.toFixed(1)}</div>
                  <div className="prs-category-label">Wait Time</div>
                </div>
              </Col>
            </Row>
          </Col>
        </Row>
      </Card.Body>
    </Card>
  );
};

export default PlaceRatingSummary;