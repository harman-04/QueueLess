import React from 'react';
import { FaStar, FaStarHalfAlt, FaRegStar } from 'react-icons/fa';
import './RatingDisplay.css';

const RatingDisplay = ({ rating, showNumber = true, size = 'md', reviewCount, showEmpty = false }) => {
  const renderStars = () => {
    if (!rating && !showEmpty) return null;
    
    const stars = [];
    const numericRating = rating || 0;
    const fullStars = Math.floor(numericRating);
    const hasHalfStar = numericRating % 1 >= 0.5;

    for (let i = 1; i <= 5; i++) {
      if (i <= fullStars) {
        stars.push(<FaStar key={i} className="star filled" />);
      } else if (i === fullStars + 1 && hasHalfStar) {
        stars.push(<FaStarHalfAlt key={i} className="star filled" />);
      } else {
        stars.push(<FaRegStar key={i} className="star" />);
      }
    }

    return stars;
  };

  const sizeClass = `stars-${size}`;

  return (
    <div className="rating-display">
      <div className={`stars ${sizeClass}`}>
        {renderStars()}
      </div>
      {showNumber && rating !== null && rating !== undefined && (
        <span className="rating-number">
          {rating.toFixed(1)}
          {reviewCount !== undefined && reviewCount !== null && (
            <span className="review-count">({reviewCount})</span>
          )}
        </span>
      )}
    </div>
  );
};

export default RatingDisplay;