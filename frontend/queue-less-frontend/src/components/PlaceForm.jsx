// src/components/PlaceForm.jsx
import React, { useState, useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { useNavigate, useParams } from 'react-router-dom';
import { createPlace, updatePlace, fetchPlaceById } from '../redux/placeSlice';
import { Form, Button, Card, Spinner, Alert, Row, Col } from 'react-bootstrap';
import { toast } from 'react-toastify';

const DAYS_OF_WEEK = [
  'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 
  'FRIDAY', 'SATURDAY', 'SUNDAY'
];

const PlaceForm = () => {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const { id } = useParams();
  const { currentPlace, loading, error } = useSelector((state) => state.places);
  const { token, role, id: userId } = useSelector((state) => state.auth);
  
  const isEdit = Boolean(id);
  const [formData, setFormData] = useState({
    name: '',
    type: 'SHOP',
    address: '',
    location: [0, 0],
    description: '',
    contactInfo: { phone: '', email: '', website: '' },
    businessHours: DAYS_OF_WEEK.map(day => ({
      day,
      openTime: '09:00',
      closeTime: '17:00',
      isOpen: true
    })),
    imageUrls: [],
    isActive: true,
    adminId: userId // Set the adminId to current user
  });

  const [newImageUrl, setNewImageUrl] = useState('');

  useEffect(() => {
    if (isEdit && token) {
      dispatch(fetchPlaceById(id));
    }
  }, [dispatch, id, isEdit, token]);

  useEffect(() => {
    if (isEdit && currentPlace) {
      // Ensure the current user owns this place before allowing edit
      if (currentPlace.adminId !== userId) {
        toast.error("You don't have permission to edit this place");
        navigate('/admin/places');
        return;
      }
      
      setFormData({
        name: currentPlace.name || '',
        type: currentPlace.type || 'SHOP',
        address: currentPlace.address || '',
        location: currentPlace.location || [0, 0],
        description: currentPlace.description || '',
        contactInfo: currentPlace.contactInfo || { phone: '', email: '', website: '' },
        businessHours: currentPlace.businessHours || DAYS_OF_WEEK.map(day => ({
          day,
          openTime: '09:00',
          closeTime: '17:00',
          isOpen: true
        })),
        imageUrls: currentPlace.imageUrls || [],
        isActive: currentPlace.isActive !== undefined ? currentPlace.isActive : true,
        adminId: userId // Keep the adminId as current user
      });
    }
  }, [currentPlace, isEdit, userId, navigate]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
  };
  const handleContactChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      contactInfo: { ...prev.contactInfo, [name]: value }
    }));
  };

  const handleBusinessHoursChange = (index, field, value) => {
    const updatedBusinessHours = formData.businessHours.map((hours, i) => {
      if (i === index) {
        return { ...hours, [field]: value };
      }
      return hours;
    });
    setFormData(prev => ({ ...prev, businessHours: updatedBusinessHours }));
  };

  const handleLocationChange = (index, value) => {
    const updatedLocation = [...formData.location];
    updatedLocation[index] = parseFloat(value) || 0;
    setFormData(prev => ({ ...prev, location: updatedLocation }));
  };

  const addImageUrl = () => {
    if (newImageUrl.trim()) {
      setFormData(prev => ({
        ...prev,
        imageUrls: [...prev.imageUrls, newImageUrl.trim()]
      }));
      setNewImageUrl('');
    }
  };

  const removeImageUrl = (index) => {
    setFormData(prev => ({
      ...prev,
      imageUrls: prev.imageUrls.filter((_, i) => i !== index)
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    try {
      if (isEdit) {
        await dispatch(updatePlace({ id, placeData: formData })).unwrap();
        toast.success('Place updated successfully!');
      } else {
        // Add adminId for new places
        const placeDataWithAdmin = { ...formData, adminId: userId };
        await dispatch(createPlace(placeDataWithAdmin)).unwrap();
        toast.success('Place created successfully!');
      }
      navigate('/admin/places');
    } catch (error) {
      toast.error(`Failed to ${isEdit ? 'update' : 'create'} place: ${error.message}`);
    }
  };

  if (loading) {
    return (
      <div className="d-flex justify-content-center align-items-center" style={{ height: '200px' }}>
        <Spinner animation="border" variant="primary" />
      </div>
    );
  }

  if (role !== 'ADMIN') {
    return (
      <Alert variant="danger" className="mt-4">
        You don't have permission to access this page.
      </Alert>
    );
  }

  return (
    <div className="container py-4">
      <Card className="shadow-sm">
        <Card.Header>
          <h3>{isEdit ? 'Edit Place' : 'Create New Place'}</h3>
        </Card.Header>
        <Card.Body>
          {error && <Alert variant="danger">{error.message}</Alert>}
                  
          <Form onSubmit={handleSubmit}>
            <Row>
              <Col md={8}>
                <Form.Group className="mb-3">
                  <Form.Label>Name *</Form.Label>
                  <Form.Control
                    type="text"
                    name="name"
                    value={formData.name}
                    onChange={handleChange}
                    required
                  />
                </Form.Group>
              </Col>
              <Col md={4}>
                <Form.Group className="mb-3">
                  <Form.Label>Type *</Form.Label>
                  <Form.Select
                    name="type"
                    value={formData.type}
                    onChange={handleChange}
                    required
                  >
                    <option value="SHOP">Shop</option>
                    <option value="HOSPITAL">Hospital</option>
                    <option value="BANK">Bank</option>
                    <option value="RESTAURANT">Restaurant</option>
                    <option value="SALON">Salon</option>
                    <option value="OTHER">Other</option>
                  </Form.Select>
                </Form.Group>
              </Col>
            </Row>

            <Form.Group className="mb-3">
              <Form.Label>Address *</Form.Label>
              <Form.Control
                type="text"
                name="address"
                value={formData.address}
                onChange={handleChange}
                required
              />
            </Form.Group>

            <Row className="mb-3">
              <Col md={6}>
                <Form.Label>Longitude</Form.Label>
                <Form.Control
                  type="number"
                  step="any"
                  value={formData.location[0]}
                  onChange={(e) => handleLocationChange(0, e.target.value)}
                />
              </Col>
              <Col md={6}>
                <Form.Label>Latitude</Form.Label>
                <Form.Control
                  type="number"
                  step="any"
                  value={formData.location[1]}
                  onChange={(e) => handleLocationChange(1, e.target.value)}
                />
              </Col>
            </Row>

            <Form.Group className="mb-3">
              <Form.Label>Description</Form.Label>
              <Form.Control
                as="textarea"
                rows={3}
                name="description"
                value={formData.description}
                onChange={handleChange}
              />
            </Form.Group>

            <h5>Contact Information</h5>
            <Row className="mb-3">
              <Col md={4}>
                <Form.Label>Phone</Form.Label>
                <Form.Control
                  type="tel"
                  name="phone"
                  value={formData.contactInfo.phone}
                  onChange={handleContactChange}
                />
              </Col>
              <Col md={4}>
                <Form.Label>Email</Form.Label>
                <Form.Control
                  type="email"
                  name="email"
                  value={formData.contactInfo.email}
                  onChange={handleContactChange}
                />
              </Col>
              <Col md={4}>
                <Form.Label>Website</Form.Label>
                <Form.Control
                  type="url"
                  name="website"
                  value={formData.contactInfo.website}
                  onChange={handleContactChange}
                />
              </Col>
            </Row>

            <h5>Business Hours</h5>
            {formData.businessHours.map((hours, index) => (
              <Row key={hours.day} className="mb-2 align-items-center">
                <Col md={3}>
                  <Form.Label>{hours.day}</Form.Label>
                </Col>
                <Col md={2}>
                  <Form.Check
                    type="checkbox"
                    label="Open"
                    checked={hours.isOpen}
                    onChange={(e) => handleBusinessHoursChange(index, 'isOpen', e.target.checked)}
                  />
                </Col>
                <Col md={3}>
                  <Form.Control
                    type="time"
                    value={hours.openTime}
                    onChange={(e) => handleBusinessHoursChange(index, 'openTime', e.target.value)}
                    disabled={!hours.isOpen}
                  />
                </Col>
                <Col md={3}>
                  <Form.Control
                    type="time"
                    value={hours.closeTime}
                    onChange={(e) => handleBusinessHoursChange(index, 'closeTime', e.target.value)}
                    disabled={!hours.isOpen}
                  />
                </Col>
              </Row>
            ))}

            <h5 className="mt-4">Images</h5>
            <Row className="mb-3">
              <Col md={8}>
                <Form.Control
                  type="url"
                  placeholder="Enter image URL"
                  value={newImageUrl}
                  onChange={(e) => setNewImageUrl(e.target.value)}
                />
              </Col>
              <Col md={4}>
                <Button variant="outline-primary" onClick={addImageUrl}>
                  Add Image
                </Button>
              </Col>
            </Row>

            {formData.imageUrls.length > 0 && (
              <div className="mb-3">
                {formData.imageUrls.map((url, index) => (
                  <div key={index} className="d-flex align-items-center mb-2">
                    <img 
                      src={url} 
                      alt="Place" 
                      className="me-2" 
                      style={{ width: '50px', height: '50px', objectFit: 'cover' }}
                      onError={(e) => {
                        e.target.src = 'https://via.placeholder.com/50x50?text=Error';
                      }}
                    />
                    <div className="flex-grow-1 text-truncate">
                      {url}
                    </div>
                    <Button 
                      variant="outline-danger" 
                      size="sm"
                      onClick={() => removeImageUrl(index)}
                    >
                      Remove
                    </Button>
                  </div>
                ))}
              </div>
            )}

            <Form.Check
              type="checkbox"
              label="Active"
              checked={formData.isActive}
              onChange={(e) => setFormData(prev => ({ ...prev, isActive: e.target.checked }))}
              className="mb-3"
            />

            <div className="d-flex gap-2">
              <Button type="submit" variant="primary" disabled={loading}>
                {loading ? <Spinner animation="border" size="sm" /> : (isEdit ? 'Update' : 'Create')}
              </Button>
              <Button type="button" variant="secondary" onClick={() => navigate('/places')}>
                Cancel
              </Button>
            </div>
          </Form>
        </Card.Body>
      </Card>
    </div>
  );
};

export default PlaceForm;