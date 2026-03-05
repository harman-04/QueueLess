// src/components/PlaceListSkeleton.jsx
import React from 'react';
import { Row, Col, Card, Placeholder } from 'react-bootstrap';
import './PlaceListSkeleton.css';

const PlaceListSkeleton = () => {
  return (
    <Row xs={1} md={2} lg={3} className="g-4">
      {[...Array(6)].map((_, idx) => (
        <Col key={idx}>
          <Card className="ql-places-card h-100 shadow-sm ql-places-skeleton-card">
            <div className="ql-places-skeleton-img" />
                        <Card.Body>
              <Placeholder as={Card.Title} animation="glow">
                <Placeholder xs={8} />
              </Placeholder>
              <Placeholder as={Card.Text} animation="glow">
                <Placeholder xs={6} /> <Placeholder xs={4} /> <Placeholder xs={8} />
              </Placeholder>
              <Placeholder.Button variant="primary" xs={6} />
            </Card.Body>
          </Card>
        </Col>
      ))}
    </Row>
  );
};

export default PlaceListSkeleton;