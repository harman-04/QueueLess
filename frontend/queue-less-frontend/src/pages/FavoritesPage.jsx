import React, { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { fetchFavoritePlacesWithDetails, removeFavoritePlace } from '../redux/userSlice';
import { Card, Row, Col, Button, Spinner, Alert } from 'react-bootstrap';
import { FaHeart, FaMapMarkerAlt, FaStar, FaTrash, FaCompass } from 'react-icons/fa';
import { useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import './FavoritesPage.css'; // The new, conflict-free CSS file

const FavoritesPage = () => {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const { favoritePlaces, loading, error } = useSelector((state) => state.user);
  const { token } = useSelector((state) => state.auth);

  useEffect(() => {
    if (token) {
      dispatch(fetchFavoritePlacesWithDetails());
    }
  }, [dispatch, token]);

  const handleRemoveFavorite = async (placeId) => {
    try {
      await dispatch(removeFavoritePlace(placeId)).unwrap();
      toast.success('Place removed from favorites!');
      // Refetch favorites to update the list
      dispatch(fetchFavoritePlacesWithDetails());
    } catch (error) {
      toast.error('Failed to remove favorite place.');
    }
  };

  if (!token) {
    return (
      <div className="ql-fav-container ql-fav-center-content">
        <Alert variant="warning" className="text-center ql-fav-alert">
          Please log in to view your favorite places.
        </Alert>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="ql-fav-container ql-fav-center-content">
        <Spinner animation="border" variant="primary" className="ql-fav-spinner" />
        <p className="mt-3 text-muted">Loading your favorite places...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="ql-fav-container ql-fav-center-content">
        <Alert variant="danger" className="ql-fav-alert">
          Error: {error}
        </Alert>
      </div>
    );
  }

  return (
    <div className="container py-5 ql-fav-main-container">
      <header className="d-flex flex-column flex-md-row justify-content-between align-items-center mb-4 ql-fav-header">
        <h1 className="ql-fav-heading">
          <FaHeart className="me-3 ql-fav-heading-icon" />
          My Favorite Places
        </h1>
        <Button
          variant="outline-secondary"
          className="ql-fav-explore-btn"
          onClick={() => navigate('/places')}
        >
          <FaCompass className="me-2" />
          Explore More Places
        </Button>
      </header>

      {favoritePlaces.length === 0 ? (
        <div className="ql-fav-empty-state text-center">
          <FaHeart className="ql-fav-empty-icon" />
          <h4 className="mt-3 fw-bold">No Favorites Yet</h4>
          <p className="text-muted">Start adding places you love by clicking the heart icon on their details page!</p>
          <Button onClick={() => navigate('/places')} className="mt-3 ql-fav-empty-btn">
            <FaCompass className="me-2" /> Start Exploring
          </Button>
        </div>
      ) : (
        <Row xs={1} md={2} lg={3} className="g-4">
          {favoritePlaces.map((place) => (
            <Col key={place.id}>
              <Card className="ql-fav-card h-100 shadow-sm">
                <div className="ql-fav-img-container">
                  <Card.Img
                    variant="top"
                    src={place.imageUrls?.[0] || 'https://via.placeholder.com/400x250?text=No+Image'}
                    alt={`Image of ${place.name}`}
                    className="ql-fav-card-img"
                    onError={(e) => {
                      e.target.src = 'https://via.placeholder.com/400x250?text=No+Image';
                    }}
                  />
                  <Button
                    variant="danger"
                    className="ql-fav-remove-btn"
                    onClick={() => handleRemoveFavorite(place.id)}
                  >
                    <FaTrash className="me-1" />
                    Remove
                  </Button>
                </div>
                <Card.Body className="d-flex flex-column ql-fav-card-body">
                  <Card.Title className="ql-fav-card-title">{place.name}</Card.Title>
                  <Card.Text className="text-muted small">
                    <FaMapMarkerAlt className="me-1" />
                    {place.address}
                  </Card.Text>
                  <p className="ql-fav-card-description">{place.description}</p>
                  <div className="mt-auto">
                    <div className="d-flex justify-content-between align-items-center mb-3">
                      {place.rating !== null && (
                        <div className="d-flex align-items-center">
                          <FaStar className="ql-fav-rating-icon me-1" />
                          <span className="fw-bold">{place.rating.toFixed(1)}</span>
                          <span className="text-muted ms-2 small">({place.totalRatings || 0})</span>
                        </div>
                      )}
                      <span className="badge ql-fav-type-badge">{place.type}</span>
                    </div>
                    <div className="d-grid">
                      <Button
                        variant="primary"
                        className="ql-fav-view-btn"
                        onClick={() => navigate(`/places/${place.id}`)}
                      >
                        View Details
                      </Button>
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

export default FavoritesPage;