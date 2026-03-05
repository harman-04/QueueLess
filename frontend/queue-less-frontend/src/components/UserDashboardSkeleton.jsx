// src/components/UserDashboardSkeleton.jsx
import React from 'react';
import { Card, Row, Col, Placeholder } from 'react-bootstrap';
import './UserDashboardSkeleton.css';

const UserDashboardSkeleton = () => {
  return (
     <div className="user-dashboard-skeleton">
    <div className="container py-5">
      <Placeholder animation="glow" className="mb-4">
        <Placeholder xs={4} size="lg" />
      </Placeholder>

      {/* Queues section */}
      <Card className="shadow-lg border-0 mb-5">
        <Card.Body>
          <Placeholder animation="glow">
            <Placeholder xs={3} className="mb-3" />
          </Placeholder>
          <Row>
            {[...Array(3)].map((_, idx) => (
              <Col md={4} key={idx} className="mb-4">
                <Card className="h-100">
                  <Card.Body>
                    <Placeholder animation="glow">
                      <Placeholder xs={8} />
                      <Placeholder xs={6} /> <Placeholder xs={4} />
                    </Placeholder>
                  </Card.Body>
                </Card>
              </Col>
            ))}
          </Row>
        </Card.Body>
      </Card>

      {/* Favorite places */}
      <Card className="shadow-lg border-0 mb-5">
        <Card.Body>
          <Placeholder animation="glow">
            <Placeholder xs={3} className="mb-3" />
          </Placeholder>
          <Row>
            {[...Array(3)].map((_, idx) => (
              <Col md={4} key={idx} className="mb-3">
                <Card className="h-100">
                  <div className="skeleton-image" />
                </Card>
              </Col>
            ))}
          </Row>
        </Card.Body>
      </Card>

      {/* Places and Services sections omitted for brevity – similar pattern */}
    </div>
    </div>
  );
};

export default UserDashboardSkeleton;