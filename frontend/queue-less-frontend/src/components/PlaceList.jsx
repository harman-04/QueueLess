import React, { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { fetchPlaces } from '../redux/placeSlice';
import { Card, Button, Spinner, Alert, Row, Col } from 'react-bootstrap';
import { FaMapMarkerAlt, FaStar, FaPlus, FaSearch } from 'react-icons/fa';
import { useNavigate } from 'react-router-dom';
import './PlaceList.css'; // New CSS file for styling

const PlaceList = () => {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const { items: places, loading, error } = useSelector((state) => state.places);
  const searchState = useSelector((state) => state.search);
  const { token, role, id: userId } = useSelector((state) => state.auth);

  useEffect(() => {
    if (token) {
      dispatch(fetchPlaces());
    }
  }, [dispatch, token]);

  const searchResults = searchState?.results?.places || [];
  const isLoading = loading || searchState?.loading;

  const displayedPlaces = searchState?.filters?.query ? searchResults : places;

  if (isLoading) {
    return (
      <div className="ql-places-loading-container">
        <Spinner animation="border" variant="primary" className="ql-places-spinner" />
        <p className="mt-3 text-muted">Loading places...</p>
      </div>
    );
  }

  if (error || searchState.error) {
    return (
      <div className="ql-places-error-container">
        <Alert variant="danger" className="ql-places-alert">
          Error loading places: {error?.message || searchState.error?.message}
        </Alert>
      </div>
    );
  }

  return (
    <div className="container py-5 ql-places-main-container">
      <div className="d-flex justify-content-between align-items-center mb-4 ql-places-header">
        <h1 className="ql-places-heading">Explore Places</h1>
        {role === 'ADMIN' && (
          <Button onClick={() => navigate('/places/new')} className="ql-places-add-btn">
            <FaPlus className="me-2" /> Add New Place
          </Button>
        )}
      </div>

      {displayedPlaces.length === 0 ? (
        <div className="ql-places-empty-state text-center">
          <FaSearch className="ql-places-empty-icon" />
          <h4 className="mt-3 fw-bold">No places found</h4>
          <p className="text-muted">Try adjusting your search or filters.</p>
        </div>
      ) : (
        <Row xs={1} md={2} lg={3} className="g-4">
          {displayedPlaces.map((place) => (
            <Col key={place.id}>
              <Card className="ql-places-card h-100 shadow-sm">
                <div className="ql-places-img-container">
                  <Card.Img
                    variant="top"
                    src={place.imageUrls?.[0] || 'https://via.placeholder.com/400x250?text=No+Image'}
                    alt={`Image of ${place.name}`}
                    className="ql-places-card-img"
                    onError={(e) => {
                      e.target.src = 'https://via.placeholder.com/400x250?text=No+Image';
                    }}
                  />
                </div>
                <Card.Body className="d-flex flex-column ql-places-card-body">
                  <Card.Title className="ql-places-card-title">{place.name}</Card.Title>
                  <Card.Text className="text-muted small">
                    <FaMapMarkerAlt className="me-1" />
                    {place.address}
                  </Card.Text>
                  <p className="ql-places-card-description">{place.description}</p>
                  <div className="mt-auto">
                    <div className="d-flex justify-content-between align-items-center mb-3">
                      <div className="d-flex align-items-center">
                        {place.rating !== null && place.rating !== undefined ? (
                          <>
                            <FaStar className="text-warning me-1" />
                            <span className="fw-bold">{place.rating.toFixed(1)}</span>
                            <span className="text-muted ms-2 small">({place.totalRatings || 0})</span>
                          </>
                        ) : (
                          <span className="text-muted small">No ratings yet</span>
                        )}
                      </div>
                      <span className="badge ql-places-type-badge">{place.type}</span>
                    </div>

                    <div className="d-grid gap-2">
                      <Button
                        variant={place.isActive ? 'primary' : 'secondary'}
                        onClick={() => place.isActive && navigate(`/places/${place.id}`)}
                        disabled={!place.isActive}
                        className="ql-places-view-btn"
                      >
                        {place.isActive ? 'View Details' : 'Currently Closed'}
                      </Button>
                      {role === 'ADMIN' && place.adminId === userId && (
                        <Button
                          variant="outline-secondary"
                          onClick={() => navigate(`/admin/places/${place.id}/services`)}
                          className="ql-places-manage-btn"
                        >
                          Manage Services
                        </Button>
                      )}
                    </div>
                  </div>
                </Card.Body>
              </Card>
            </Col>
          ))}
        </Row>
      )}
    </div>
  );
};

export default PlaceList;