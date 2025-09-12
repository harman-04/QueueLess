import React, { useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { fetchPlaces } from '../redux/placeSlice';
import { Card, Button, Spinner, Alert, Row, Col, Form } from 'react-bootstrap';
import { FaMapMarkerAlt, FaStar, FaStarHalfAlt, FaRegStar, FaClock, FaPlus, FaSearch } from 'react-icons/fa';
import { useNavigate } from 'react-router-dom';

// 🆕 New component to handle star display for fractional ratings
const StarRatingDisplay = ({ rating }) => {
  if (rating === null || rating === undefined || rating <= 0) {
    return (
      <span className="text-muted small">No ratings yet</span>
    );
  }

  // Round to the nearest half-star for visual representation
  const roundedRating = Math.round(rating * 2) / 2;
  const fullStars = Math.floor(roundedRating);
  const hasHalfStar = roundedRating % 1 !== 0;
  const emptyStars = 5 - fullStars - (hasHalfStar ? 1 : 0);

  return (
    <div className="d-flex align-items-center">
      {[...Array(fullStars)].map((_, i) => (
        <FaStar key={`full-${i}`} className="text-warning" />
      ))}
      {hasHalfStar && <FaStarHalfAlt className="text-warning" />}
      {[...Array(emptyStars)].map((_, i) => (
        <FaRegStar key={`empty-${i}`} className="text-warning" />
      ))}
      <span className="ms-2">{rating.toFixed(1)}</span>
    </div>
  );
};

const PlaceList = () => {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const { items: places, loading, error } = useSelector((state) => state.places);
  const { token, role, id: userId } = useSelector((state) => state.auth);

  const [searchTerm, setSearchTerm] = useState('');
  const [filterType, setFilterType] = useState('ALL');
  const [sortBy, setSortBy] = useState('name');

  useEffect(() => {
    if (token) {
      dispatch(fetchPlaces());
    }
  }, [dispatch, token]);

  const filteredAndSortedPlaces = places
    .filter(place => {
      const matchesSearch = place.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
                          place.description.toLowerCase().includes(searchTerm.toLowerCase());
      const matchesType = filterType === 'ALL' || place.type === filterType;
      return matchesSearch && matchesType;
    })
    .sort((a, b) => {
      switch (sortBy) {
        case 'name':
          return a.name.localeCompare(b.name);
        case 'rating':
          return (b.rating || 0) - (a.rating || 0);
        case 'type':
          return a.type.localeCompare(b.type);
        default:
          return 0;
      }
    });

  if (loading) {
    return (
      <div className="d-flex justify-content-center align-items-center" style={{ height: '200px' }}>
        <Spinner animation="border" variant="primary" />
      </div>
    );
  }

  if (error) {
    return <Alert variant="danger">Error loading places: {error.message}</Alert>;
  }

  return (
    <div className="container py-4">
      <div className="d-flex justify-content-between align-items-center mb-4">
        <h2>Places</h2>
        {role === 'ADMIN' && (
          <Button onClick={() => navigate('/places/new')}>
            <FaPlus className="me-2" /> Add New Place
          </Button>
        )}
      </div>

      <Card className="mb-4">
        <Card.Body>
          <Row>
            <Col md={6}>
              <div className="input-group">
                <span className="input-group-text">
                  <FaSearch />
                </span>
                <Form.Control
                  type="text"
                  placeholder="Search places..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                />
              </div>
            </Col>
            <Col md={3}>
              <Form.Select
                value={filterType}
                onChange={(e) => setFilterType(e.target.value)}
              >
                <option value="ALL">All Types</option>
                <option value="HOSPITAL">Hospital</option>
                <option value="SHOP">Shop</option>
                <option value="BANK">Bank</option>
                <option value="RESTAURANT">Restaurant</option>
                <option value="SALON">Salon</option>
                <option value="OTHER">Other</option>
              </Form.Select>
            </Col>
            <Col md={3}>
              <Form.Select
                value={sortBy}
                onChange={(e) => setSortBy(e.target.value)}
              >
                <option value="name">Sort by Name</option>
                <option value="rating">Sort by Rating</option>
                <option value="type">Sort by Type</option>
              </Form.Select>
            </Col>
          </Row>
        </Card.Body>
      </Card>

      {filteredAndSortedPlaces.length === 0 ? (
        <Alert variant="info">
          {searchTerm || filterType !== 'ALL'
            ? 'No places match your search criteria.'
            : 'No places found.'}
        </Alert>
      ) : (
        <Row>
          {filteredAndSortedPlaces.map((place) => (
            <div key={place.id} className="col-md-4 mb-4">
              <Card className="h-100 shadow-sm">
                {place.imageUrls?.length > 0 && (
                  <Card.Img
                    variant="top"
                    src={place.imageUrls[0]}
                    style={{ height: '200px', objectFit: 'cover' }}
                    onError={(e) => {
                      e.target.src = 'https://via.placeholder.com/300x200?text=No+Image';
                    }}
                  />
                )}
                <Card.Body className="d-flex flex-column">
                  <Card.Title>{place.name}</Card.Title>
                  <Card.Text className="text-muted">
                    <FaMapMarkerAlt className="me-1" />
                    {place.address}
                  </Card.Text>
                  <Card.Text>{place.description}</Card.Text>

                  <div className="d-flex justify-content-between align-items-center mb-2 mt-auto">
                    {/* 🆕 Use the new StarRatingDisplay component */}
                    <StarRatingDisplay rating={place.rating} />
                    <span className="text-muted ms-2">({place.totalRatings || 0})</span>
                    <span className="badge bg-primary">{place.type}</span>
                  </div>

                  <div className="d-flex gap-2 mt-2">
                    <Button
                      variant={place.isActive ? 'primary' : 'secondary'}
                      onClick={() => place.isActive && navigate(`/places/${place.id}`)}
                      className="flex-fill"
                      disabled={!place.isActive}
                    >
                      {place.isActive ? 'View Details' : 'Currently Closed'}
                    </Button>

                    {role === 'ADMIN' && place.adminId === userId ? (
                      <Button
                        variant="outline-secondary"
                        onClick={() => navigate(`/admin/places/${place.id}/services`)}
                        className="flex-fill"
                      >
                        Manage Services
                      </Button>
                    ) : (
                      <div className="flex-fill" style={{ visibility: 'hidden' }}>
                        <Button className="w-100">Placeholder</Button>
                      </div>
                    )}
                  </div>
                </Card.Body>
              </Card>
            </div>
          ))}
        </Row>
      )}
    </div>
  );
};

export default PlaceList;