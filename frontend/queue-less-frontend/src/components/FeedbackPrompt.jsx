import React, { useState, useEffect } from "react";
import { Card, Button, OverlayTrigger, Tooltip } from "react-bootstrap";
import { FaStar, FaTimes } from "react-icons/fa";
import FeedbackForm from "./FeedbackForm";
import './FeedbackPrompt.css';

const FeedbackPrompt = ({ tokenId, queueId, onFeedbackSubmitted }) => {
  const [showFeedbackForm, setShowFeedbackForm] = useState(false);
  const [dismissed, setDismissed] = useState(false);

  useEffect(() => {
    // Check if user has dismissed the prompt for this token
    const dismissedTokens = JSON.parse(localStorage.getItem('dismissedFeedbackPrompts') || '[]');
    setDismissed(dismissedTokens.includes(tokenId));
  }, [tokenId]);

  const handleDismiss = () => {
    // Store dismissal in localStorage
    const dismissedTokens = JSON.parse(localStorage.getItem('dismissedFeedbackPrompts') || '[]');
    if (!dismissedTokens.includes(tokenId)) {
      dismissedTokens.push(tokenId);
      localStorage.setItem('dismissedFeedbackPrompts', JSON.stringify(dismissedTokens));
    }
    setDismissed(true);
  };

  const handleFeedbackFormSubmitted = () => {
    setShowFeedbackForm(false);
    onFeedbackSubmitted();
  };

  if (!tokenId || dismissed) {
    return null;
  }

  return (
    <div className="feedback-prompt-container">
      <Card className="feedback-prompt">
        <Card.Body>
          <div className="prompt-content">
            <div className="prompt-icon">
              <FaStar />
            </div>
            <div className="prompt-text">
              <h5>How was your experience?</h5>
              <p>Your feedback helps us improve our service</p>
            </div>
            <div className="prompt-actions">
              <Button
                variant="primary"
                size="sm"
                onClick={() => setShowFeedbackForm(true)}
                className="me-2"
              >
                Provide Feedback
              </Button>
              <OverlayTrigger
                placement="top"
                overlay={<Tooltip>Don't ask again for this visit</Tooltip>}
              >
                <Button
                  variant="outline-secondary"
                  size="sm"
                  onClick={handleDismiss}
                  className="dismiss-btn"
                >
                  <FaTimes />
                </Button>
              </OverlayTrigger>
            </div>
          </div>
        </Card.Body>
      </Card>
     
      <FeedbackForm
        show={showFeedbackForm}
        onHide={() => setShowFeedbackForm(false)}
        tokenId={tokenId}
        queueId={queueId}
        onFeedbackSubmitted={handleFeedbackFormSubmitted}
      />
    </div>
  );
};

export default FeedbackPrompt;