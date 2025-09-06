import React, { useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { useNavigate } from 'react-router-dom';
import { fetchMyPlaces, deletePlace } from '../redux/placeSlice';
import { Card, Button, Spinner, Alert, Row, Col, Modal } from 'react-bootstrap';
import { FaPlus, FaEdit, FaTrash, FaBuilding, FaInfoCircle } from 'react-icons/fa';
import { toast } from 'react-toastify';

const AdminPlaces = () => {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const { items: places, loading, error } = useSelector((state) => state.places);
  const { token, role, id: userId } = useSelector((state) => state.auth);
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [placeToDelete, setPlaceToDelete] = useState(null);

  useEffect(() => {
    if (token && role === 'ADMIN') {
        dispatch(fetchMyPlaces()); // This should call /api/places/admin/my-places
    }
}, [dispatch, token, role]);
  const handleEditPlace = (placeId) => {
    navigate(`/places/edit/${placeId}`);
  };

  const handleDeletePlace = (place) => {
    setPlaceToDelete(place);
    setShowDeleteModal(true);
  };

  const confirmDeletePlace = async () => {
    try {
      await dispatch(deletePlace(placeToDelete.id)).unwrap();
      toast.success('Place deleted successfully!');
      setShowDeleteModal(false);
      setPlaceToDelete(null);
    } catch (error) {
      toast.error(`Failed to delete place: ${error.message}`);
    }
  };

  const handleManageServices = (placeId) => {
    navigate(`/admin/places/${placeId}/services`);
  };

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
        <h2>Manage Places</h2>
        <Button onClick={() => navigate('/places/new')}>
          <FaPlus className="me-2" /> Add New Place
        </Button>
      </div>

      {places.length === 0 ? (
        <Alert variant="info" className="text-center">
          <FaInfoCircle className="me-2" />
          You don't have any places yet. Create your first place to get started.
        </Alert>
      ) : (
        <Row>
          {places.map((place) => (
            <Col key={place.id} md={6} lg={4} className="mb-4">
              <Card className="h-100 shadow-sm">
                {place.imageUrls && place.imageUrls.length > 0 && (
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
                    <FaBuilding className="me-1" />
                    {place.address}
                  </Card.Text>
                  <Card.Text>{place.description}</Card.Text>
                  <div className="mt-auto d-grid gap-2">
                    <Button 
                      variant="outline-primary" 
                      onClick={() => handleManageServices(place.id)}
                    >
                      Manage Services
                    </Button>
                    <div className="d-flex gap-2">
                      <Button 
                        variant="outline-secondary" 
                        onClick={() => handleEditPlace(place.id)}
                        className="flex-fill"
                      >
                        <FaEdit className="me-1" /> Edit
                      </Button>
                      <Button 
                        variant="outline-danger" 
                        onClick={() => handleDeletePlace(place)}
                        className="flex-fill"
                      >
                        <FaTrash className="me-1" /> Delete
                      </Button>
                    </div>
                  </div>
                </Card.Body>
              </Card>
            </Col>
          ))}
        </Row>
      )}

      {/* Delete Confirmation Modal */}
      <Modal show={showDeleteModal} onHide={() => setShowDeleteModal(false)}>
        <Modal.Header closeButton>
          <Modal.Title>Confirm Delete</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          Are you sure you want to delete the place "{placeToDelete?.name}"? This action cannot be undone.
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={() => setShowDeleteModal(false)}>
            Cancel
          </Button>
          <Button variant="danger" onClick={confirmDeletePlace}>
            Delete
          </Button>
        </Modal.Footer>
      </Modal>
    </div>
  );
};

export default AdminPlaces;