import React from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { Row, Col, Card, Spinner, Pagination, Badge, Alert, Button } from 'react-bootstrap';
import { FaMapMarkerAlt, FaStar, FaClock, FaUsers, FaAmbulance, FaCaretUp, FaInfoCircle, FaHourglassHalf, FaTicketAlt, FaSearch } from 'react-icons/fa';
import { setPage, performSearch } from '../redux/searchSlice';
import { useNavigate } from 'react-router-dom';
import 'animate.css';
import './SearchResults.css'; // The new, conflict-free CSS file

const SearchResults = () => {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const { results, loading, filters, currentPage, totalPages, sortBy, sortDirection } = useSelector((state) => state.search);

  const handlePageChange = (page) => {
    dispatch(setPage(page));
    dispatch(performSearch({
      filters,
      page,
      sortBy,
      sortDirection
    }));
  };

  const handleViewDetails = (placeId) => {
    navigate(`/places/${placeId}`);
  };

  if (loading) {
    return (
      <div className="ql-search-loading-container animate__animated animate__fadeIn">
        <Spinner animation="border" variant="primary" />
        <p className="mt-2 text-muted">Searching for the best places...</p>
      </div>
    );
  }

  const hasResults = results?.places?.length > 0 || results?.services?.length > 0 || results?.queues?.length > 0;

  if (!hasResults) {
    return (
      <div className="ql-search-no-results-state animate__animated animate__fadeIn">
        <FaSearch className="ql-search-empty-icon" />
        <h4 className="mt-3 fw-bold">No results found</h4>
        <p className="text-muted">Try adjusting your search criteria or clear your filters.</p>
      </div>
    );
  }

  return (
    <div className="container py-5 ql-search-results-page animate__animated animate__fadeInUp mt-4">
      {/* Search Summary Section */}
      <div className="ql-search-results-summary animate__animated animate__fadeIn">
        <h6 className="text-muted">
          Showing results for "<span className="fw-bold text-dark">{filters?.query}</span>"
        </h6>
        <div className="ql-search-results-count mt-3">
          {results?.totalPlaces > 0 && <span><FaMapMarkerAlt /> {results.totalPlaces} Places</span>}
          {results?.totalServices > 0 && <span><FaUsers /> {results.totalServices} Services</span>}
          {results?.totalQueues > 0 && <span><FaClock /> {results.totalQueues} Queues</span>}
        </div>
      </div>

      {/* Places Section */}
      {results.places?.length > 0 && (
        <div className="ql-search-results-section mt-5">
          <h5 className="ql-search-section-title">Places</h5>
          <Row className="g-4">
            {results.places.map((place) => (
              <Col key={place.id} sm={12} md={6} lg={4}>
                <Card className="ql-search-result-card ql-search-place-card animate__animated animate__fadeInUp">
                  {place.imageUrls && place.imageUrls.length > 0 ? (
                    <div className="ql-search-card-image-container">
                      <Card.Img
                        variant="top"
                        src={place.imageUrls[0]}
                        className="ql-search-card-image-fixed"
                        onError={(e) => { e.target.src = 'https://via.placeholder.com/400x220?text=No+Image'; }}
                      />
                    </div>
                  ) : (
                    <div className="ql-search-card-no-image">
                      <FaMapMarkerAlt style={{ fontSize: '4rem' }} />
                    </div>
                  )}
                  <Card.Body className="ql-search-card-body-content">
                    <div className="ql-search-card-text-content mb-auto">
                      <Card.Title className="ql-search-result-title">{place.name}</Card.Title>
                      <Card.Text className="ql-search-result-text-big">{place.description}</Card.Text>
                      <Card.Text className="ql-search-result-text-small">{place.address}</Card.Text>
                    </div>
                    <div className="ql-search-card-footer-details">
                      <div className="d-flex align-items-center">
                        <FaStar className="text-warning me-1" />
                        <span className="ql-search-rating-value">{place.rating?.toFixed(1) || 'N/A'}</span>
                      </div>
                      <Badge bg="primary" className="ql-search-result-badge">{place.type}</Badge>
                    </div>
                    <Button
                      variant="primary"
                      className="ql-search-view-details-btn mt-3"
                      onClick={() => handleViewDetails(place.id)}
                    >
                      <FaInfoCircle className="me-2" /> View Details
                    </Button>
                  </Card.Body>
                </Card>
              </Col>
            ))}
          </Row>
        </div>
      )}

      {/* Services Section */}
      {results.services?.length > 0 && (
        <div className="ql-search-results-section mt-5">
          <h5 className="ql-search-section-title">Services</h5>
          <Row className="g-4">
            {results.services.map((service) => (
              <Col key={service.id} sm={12} md={6}>
                <Card className="ql-search-result-card ql-search-service-card animate__animated animate__fadeInUp">
                  <Card.Body className="ql-search-card-body-content">
                    <div className="ql-search-card-text-content mb-auto">
                      <Card.Title className="ql-search-result-title">{service.name}</Card.Title>
                      <Card.Text className="ql-search-result-text-big">{service.description}</Card.Text>
                    </div>
                    <div className="ql-search-card-footer-details">
                      <div className="d-flex flex-wrap gap-2">
                        {service.supportsGroupToken && (
                          <Badge bg="success" className="ql-search-feature-badge">
                            <FaUsers className="me-1" /> Group
                          </Badge>
                        )}
                        {service.emergencySupport && (
                          <Badge bg="danger" className="ql-search-feature-badge">
                            <FaAmbulance className="me-1" /> Emergency
                          </Badge>
                        )}
                        <Badge bg="info" className="ql-search-feature-badge">
                          <FaClock className="me-1" /> {service.averageServiceTime} min
                        </Badge>
                      </div>
                      <Button
                        variant="outline-primary"
                        className="ql-search-view-details-btn mt-3"
                        onClick={() => handleViewDetails(service.placeId)}
                      >
                        <FaInfoCircle className="me-2" /> View Place Details
                      </Button>
                    </div>
                  </Card.Body>
                </Card>
              </Col>
            ))}
          </Row>
        </div>
      )}

      {/* Queues Section */}
      {results.queues?.length > 0 && (
        <div className="ql-search-results-section mt-5">
          <h5 className="ql-search-section-title">Queues</h5>
          <Row className="g-4">
            {results.queues.map((queue) => (
              <Col key={queue.id} sm={12} md={6}>
                <Card className="ql-search-result-card ql-search-queue-card animate__animated animate__fadeInUp">
                  <Card.Body className="ql-search-card-body-content">
                    <div className="ql-search-card-text-content mb-auto">
                      <Card.Title className="ql-search-result-title">{queue.serviceName}</Card.Title>
                      <Card.Text className="ql-search-result-text-big">
                        {queue.placeName}
                      </Card.Text>
                      <Card.Text className="ql-search-result-text-small">
                        <FaMapMarkerAlt className="me-1" />{queue.placeAddress}
                      </Card.Text>
                    </div>
                    <div className="ql-search-card-footer-details">
                      <div className="d-flex flex-column gap-2">
                        <div className="ql-search-stat-item text-danger">
                          <FaHourglassHalf />
                          <span className="fw-bold">{queue.currentWaitTime || queue.estimatedWaitTime} min wait</span>
                        </div>
                        <div className="ql-search-stat-item text-success">
                          <FaTicketAlt />
                          <span className="fw-bold">{queue.waitingTokens} waiting</span>
                        </div>
                      </div>
                      <div className="d-flex flex-wrap gap-2">
                        {queue.supportsGroupToken && (
                          <Badge bg="success" className="ql-search-feature-badge">Group</Badge>
                        )}
                        {queue.emergencySupport && (
                          <Badge bg="danger" className="ql-search-feature-badge">Emergency</Badge>
                        )}
                      </div>
                    </div>
                    <Button
                      variant="outline-primary"
                      className="ql-search-view-details-btn mt-3"
                      onClick={() => handleViewDetails(queue.placeId)}
                    >
                      <FaInfoCircle className="me-2" /> View Place Details
                    </Button>
                  </Card.Body>
                </Card>
              </Col>
            ))}
          </Row>
        </div>
      )}

      {/* Pagination */}
      {results?.totalPages > 1 && (
        <div className="ql-search-pagination-container animate__animated animate__fadeIn">
          <Pagination>
            <Pagination.First
              onClick={() => handlePageChange(0)}
              disabled={currentPage === 0}
            />
            <Pagination.Prev
              onClick={() => handlePageChange(currentPage - 1)}
              disabled={currentPage === 0}
            />
            {[...Array(results.totalPages)].map((_, index) => (
              <Pagination.Item
                key={index}
                active={index === currentPage}
                onClick={() => handlePageChange(index)}
              >
                {index + 1}
              </Pagination.Item>
            ))}
            <Pagination.Next
              onClick={() => handlePageChange(currentPage + 1)}
              disabled={currentPage === results.totalPages - 1}
            />
            <Pagination.Last
              onClick={() => handlePageChange(results.totalPages - 1)}
              disabled={currentPage === results.totalPages - 1}
            />
          </Pagination>
        </div>
      )}
    </div>
  );
};

export default SearchResults;