// src/components/SearchResultsSkeleton.jsx
import React from 'react';
import { Row, Col, Card, Placeholder } from 'react-bootstrap';
import './SearchResultsSkeleton.css';

const SearchResultsSkeleton = () => {
  return (
    <div className="container py-5">
      {[...Array(3)].map((_, sectionIdx) => (
        <div key={sectionIdx} className="mt-5">
          <Placeholder animation="glow" className="mb-4">
            <Placeholder xs={2} size="lg" />
          </Placeholder>
          <Row className="g-4">
            {[...Array(3)].map((_, idx) => (
              <Col key={idx} md={4}>
                <Card className="h-100 ql-search-skeleton-card">
                  <div className="ql-search-skeleton-image" />
                  <Card.Body>
                    <Placeholder animation="glow">
                      <Placeholder xs={8} />
                      <Placeholder xs={4} /> <Placeholder xs={6} />
                    </Placeholder>
                  </Card.Body>
                </Card>
              </Col>
            ))}
          </Row>
        </div>
      ))}
    </div>
  );
};

export default SearchResultsSkeleton;