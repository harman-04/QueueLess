// src/components/ServiceManagement.jsx
import React, { useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { useParams, useNavigate } from 'react-router-dom';
import { fetchServicesByPlace, createService, updateService, deleteService } from '../redux/serviceSlice';
import { fetchPlaceById } from '../redux/placeSlice';
import { Form, Button, Card, Spinner, Alert, Table, Modal, Row, Col, Badge } from 'react-bootstrap';
import { FaPlus, FaEdit, FaTrash, FaClock, FaUsers, FaAmbulance } from 'react-icons/fa';
import { toast } from 'react-toastify';

const ServiceManagement = () => {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const { placeId } = useParams();
  const { currentPlace } = useSelector((state) => state.places);
  const { servicesByPlace, loading, error } = useSelector((state) => state.services);
  const { id: userId } = useSelector((state) => state.auth);
  const [showModal, setShowModal] = useState(false);
  const [editingService, setEditingService] = useState(null);
  const [formData, setFormData] = useState({
    name: '',
    description: '',
    averageServiceTime: 15,
    supportsGroupToken: false,
    emergencySupport: false,
    isActive: true
  });
  
  const services = servicesByPlace[placeId] || [];

  useEffect(() => {
    if (placeId) {
      dispatch(fetchPlaceById(placeId));
      dispatch(fetchServicesByPlace(placeId));
    }
  }, [dispatch, placeId]);

  // Check if the current user owns this place
  useEffect(() => {
    if (currentPlace && currentPlace.adminId !== userId) {
      toast.error("You don't have permission to manage services for this place");
      navigate('/admin/places');
    }
  }, [currentPlace, userId, navigate]);

  const handleShowModal = (service = null) => {
    if (service) {
      setEditingService(service);
      setFormData({
        name: service.name,
        description: service.description,
        averageServiceTime: service.averageServiceTime,
        supportsGroupToken: service.supportsGroupToken,
        emergencySupport: service.emergencySupport,
        isActive: service.isActive
      });
    } else {
      setEditingService(null);
      setFormData({
        name: '',
        description: '',
        averageServiceTime: 15,
        supportsGroupToken: false,
        emergencySupport: false,
        isActive: true
      });
    }
    setShowModal(true);
  };

  const handleCloseModal = () => {
    setShowModal(false);
    setEditingService(null);
  };

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      const serviceData = {
        ...formData,
        placeId: placeId,
        averageServiceTime: parseInt(formData.averageServiceTime)
      };

      if (editingService) {
        await dispatch(updateService({ id: editingService.id, serviceData })).unwrap();
        toast.success('Service updated successfully!');
      } else {
        await dispatch(createService(serviceData)).unwrap();
        toast.success('Service created successfully!');
      }
      handleCloseModal();
      // RE-FETCH SERVICES TO UPDATE THE LIST
      dispatch(fetchServicesByPlace(placeId));
    } catch (error) {
      toast.error(`Failed to save service: ${error.message}`);
    }
  };

  const handleDelete = async (serviceId) => {
    if (window.confirm('Are you sure you want to delete this service?')) {
      try {
        await dispatch(deleteService(serviceId)).unwrap();
        toast.success('Service deleted successfully!');
        // RE-FETCH SERVICES TO UPDATE THE LIST
        dispatch(fetchServicesByPlace(placeId));
      } catch (error) {
        toast.error(`Failed to delete service: ${error.message}`);
      }
    }
  };

  if (!currentPlace) {
    return (
      <div className="d-flex justify-content-center align-items-center" style={{ height: '200px' }}>
        <Spinner animation="border" variant="primary" />
      </div>
    );
  }

  return (
    <div className="container py-4">
      <div className="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h2>Manage Services</h2>
          <p className="text-muted">{currentPlace.name}</p>
        </div>
        <Button onClick={() => handleShowModal()}>
          <FaPlus className="me-2" /> Add Service
        </Button>
      </div>

      {error && <Alert variant="danger">{error.message}</Alert>}

      {loading ? (
        <div className="d-flex justify-content-center">
          <Spinner animation="border" variant="primary" />
        </div>
      ) : (
        <Card>
          <Card.Body>
            {services.length === 0 ? (
              <Alert variant="info">No services found. Create your first service.</Alert>
            ) : (
              <Table responsive>
                <thead>
                  <tr>
                    <th>Name</th>
                    <th>Description</th>
                    <th>Avg. Time</th>
                    <th>Features</th>
                    <th>Status</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {services.map(service => (
                    <tr key={service.id}>
                      <td>{service.name}</td>
                      <td>{service.description}</td>
                      <td>
                        <FaClock className="me-1" />
                        {service.averageServiceTime} mins
                      </td>
                      <td>
                        {service.supportsGroupToken && (
                          <Badge bg="success" className="me-1">
                            <FaUsers className="me-1" /> Group
                          </Badge>
                        )}
                        {service.emergencySupport && (
                          <Badge bg="danger">
                            <FaAmbulance className="me-1" /> Emergency
                          </Badge>
                        )}
                      </td>
                      <td>
                        <span className={`badge ${service.isActive ? 'bg-success' : 'bg-secondary'}`}>
                          {service.isActive ? 'Active' : 'Inactive'}
                        </span>
                      </td>
                      <td>
                        <Button
                          variant="outline-primary"
                          size="sm"
                          className="me-2"
                          onClick={() => handleShowModal(service)}
                        >
                          <FaEdit />
                        </Button>
                        <Button
                          variant="outline-danger"
                          size="sm"
                          onClick={() => handleDelete(service.id)}
                        >
                          <FaTrash />
                        </Button>
                      </td>
                  </tr>
                 ))}
               </tbody>
              </Table>
            )}
          </Card.Body>
        </Card>
      )}

      <Modal show={showModal} onHide={handleCloseModal} size="lg">
        <Modal.Header closeButton>
          <Modal.Title>{editingService ? 'Edit Service' : 'Create Service'}</Modal.Title>
        </Modal.Header>
        <Form onSubmit={handleSubmit}>
          <Modal.Body>
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
                  <Form.Label>Average Service Time (minutes) *</Form.Label>
                  <Form.Control
                    type="number"
                    name="averageServiceTime"
                    value={formData.averageServiceTime}
                    onChange={handleChange}
                    min="1"
                    required
                  />
                </Form.Group>
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

            <Row className="mb-3">
              <Col md={6}>
                <Form.Check
                  type="checkbox"
                  name="supportsGroupToken"
                  label="Supports Group Token"
                  checked={formData.supportsGroupToken}
                  onChange={handleChange}
                />
              </Col>
              <Col md={6}>
                <Form.Check
                  type="checkbox"
                  name="emergencySupport"
                  label="Emergency Support"
                  checked={formData.emergencySupport}
                  onChange={handleChange}
                />
              </Col>
            </Row>

            <Form.Check
              type="checkbox"
              name="isActive"
              label="Active"
              checked={formData.isActive}
              onChange={handleChange}
            />
          </Modal.Body>
          <Modal.Footer>
            <Button variant="secondary" onClick={handleCloseModal}>
              Cancel
            </Button>
            <Button variant="primary" type="submit">
              {editingService ? 'Update' : 'Create'}
            </Button>
          </Modal.Footer>
        </Form>
      </Modal>
    </div>
  );
};

export default ServiceManagement;