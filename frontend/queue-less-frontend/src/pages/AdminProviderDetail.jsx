import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useSelector } from 'react-redux';
import {
  Container, Row, Col, Card, Form, Button, Spinner, Alert,
  Badge, ListGroup, Modal
} from 'react-bootstrap';
import {
  FaUser, FaEnvelope, FaPhone, FaBuilding, FaCheckCircle,
  FaTimesCircle, FaEdit, FaSave, FaTrash, FaUndo, FaKey,
  FaChartLine, FaUsers, FaClock, FaStar, FaBan
} from 'react-icons/fa';
import axiosInstance from '../utils/axiosInstance';
import { toast } from 'react-toastify';
import './AdminProviderDetail.css';

const AdminProviderDetail = () => {
  const { providerId } = useParams();
  const navigate = useNavigate();
  const { token, role } = useSelector((state) => state.auth);

  const [provider, setProvider] = useState(null);
  const [allPlaces, setAllPlaces] = useState([]); // all places under this admin
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [editing, setEditing] = useState(false);
  const [formData, setFormData] = useState({});
  const [saving, setSaving] = useState(false);
  const [showResetModal, setShowResetModal] = useState(false);
  const [resetLoading, setResetLoading] = useState(false);

  useEffect(() => {
    if (!token || role !== 'ADMIN') {
      navigate('/');
      return;
    }
    fetchProvider();
    fetchAllPlaces();
  }, [providerId, token, role, navigate]);

  const fetchProvider = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await axiosInstance.get(`/admin/providers/${providerId}`);
      setProvider(response.data);
      setFormData({
        name: response.data.name,
        email: response.data.email,
        phoneNumber: response.data.phoneNumber || '',
        managedPlaceIds: response.data.managedPlaceIds || [],
        isActive: response.data.isActive
      });
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load provider');
    } finally {
      setLoading(false);
    }
  };

  const fetchAllPlaces = async () => {
    try {
      const response = await axiosInstance.get('/admin/places-with-queues');
      setAllPlaces(response.data);
    } catch (err) {
      console.error('Failed to fetch places:', err);
    }
  };

  const handleInputChange = (e) => {
    const { name, value, type, checked } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value
    }));
  };

  const handlePlaceToggle = (placeId) => {
    setFormData(prev => {
      const current = prev.managedPlaceIds || [];
      if (current.includes(placeId)) {
        return { ...prev, managedPlaceIds: current.filter(id => id !== placeId) };
      } else {
        return { ...prev, managedPlaceIds: [...current, placeId] };
      }
    });
  };

  const handleSave = async () => {
    setSaving(true);
    try {
      const response = await axiosInstance.put(`/admin/providers/${providerId}`, formData);
      setProvider(response.data);
      setEditing(false);
      toast.success('Provider updated successfully');
    } catch (err) {
      toast.error(err.response?.data?.message || 'Update failed');
    } finally {
      setSaving(false);
    }
  };

  const handleToggleStatus = async () => {
    const newStatus = !formData.isActive;
    try {
      await axiosInstance.patch(`/admin/providers/${providerId}/status?active=${newStatus}`);
      setFormData(prev => ({ ...prev, isActive: newStatus }));
      setProvider(prev => ({ ...prev, isActive: newStatus }));
      toast.success(`Provider ${newStatus ? 'activated' : 'deactivated'}`);
    } catch (err) {
      toast.error(err.response?.data?.message || 'Status update failed');
    }
  };

  const handleResetPassword = async () => {
    setResetLoading(true);
    try {
      await axiosInstance.post(`/admin/providers/${providerId}/reset-password`);
      toast.success('Password reset email sent');
      setShowResetModal(false);
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to send reset email');
    } finally {
      setResetLoading(false);
    }
  };

  if (loading) {
    return (
      <Container className="py-5 text-center">
        <Spinner animation="border" variant="primary" />
        <p className="mt-3">Loading provider details...</p>
      </Container>
    );
  }

  if (error) {
    return (
      <Container className="py-5">
        <Alert variant="danger">{error}</Alert>
        <Button variant="primary" onClick={() => navigate('/admin/dashboard')}>
          Back to Dashboard
        </Button>
      </Container>
    );
  }

  if (!provider) return null;

  return (
    <Container className="admin-provider-detail py-4">
      <Button variant="link" className="mb-3" onClick={() => navigate('/admin/dashboard?tab=providers')}>
        ← Back to Providers
      </Button>

      <Card className="shadow-lg">
        <Card.Header className="d-flex justify-content-between align-items-center">
          <h4 className="mb-0">
            <FaUser className="me-2" />
            Provider Details
          </h4>
          <div>
            {!editing ? (
              <Button variant="primary" onClick={() => setEditing(true)}>
                <FaEdit className="me-2" /> Edit
              </Button>
            ) : (
              <>
                <Button variant="success" onClick={handleSave} disabled={saving} className="me-2">
                  {saving ? <Spinner animation="border" size="sm" /> : <FaSave className="me-2" />}
                  Save
                </Button>
                <Button variant="secondary" onClick={() => { setEditing(false); setFormData(provider); }}>
                  Cancel
                </Button>
              </>
            )}
          </div>
        </Card.Header>
        <Card.Body>
          <Row>
            <Col md={6}>
              <h5 className="section-title">Basic Information</h5>
              <ListGroup variant="flush">
                <ListGroup.Item>
                  <FaUser className="me-2 text-primary" />
                  <strong>Name:</strong>{' '}
                  {editing ? (
                    <Form.Control
                      type="text"
                      name="name"
                      value={formData.name || ''}
                      onChange={handleInputChange}
                      className="d-inline-block w-75 ms-2"
                    />
                  ) : (
                    provider.name
                  )}
                </ListGroup.Item>
                <ListGroup.Item>
                  <FaEnvelope className="me-2 text-primary" />
                  <strong>Email:</strong>{' '}
                  {editing ? (
                    <Form.Control
                      type="email"
                      name="email"
                      value={formData.email || ''}
                      onChange={handleInputChange}
                      className="d-inline-block w-75 ms-2"
                    />
                  ) : (
                    provider.email
                  )}
                </ListGroup.Item>
                <ListGroup.Item>
                  <FaPhone className="me-2 text-primary" />
                  <strong>Phone:</strong>{' '}
                  {editing ? (
                    <Form.Control
                      type="text"
                      name="phoneNumber"
                      value={formData.phoneNumber || ''}
                      onChange={handleInputChange}
                      className="d-inline-block w-75 ms-2"
                    />
                  ) : (
                    provider.phoneNumber || '—'
                  )}
                </ListGroup.Item>
                <ListGroup.Item>
                  <FaCheckCircle className="me-2 text-success" />
                  <strong>Verified:</strong> {provider.isVerified ? 'Yes' : 'No'}
                </ListGroup.Item>
                <ListGroup.Item>
                  {provider.isActive ? (
                    <FaCheckCircle className="me-2 text-success" />
                  ) : (
                    <FaBan className="me-2 text-danger" />
                  )}
                  <strong>Status:</strong>{' '}
                  <Badge bg={provider.isActive ? 'success' : 'danger'}>
                    {provider.isActive ? 'Active' : 'Inactive'}
                  </Badge>
                  {editing && (
                    <Button
                      variant="outline-warning"
                      size="sm"
                      className="ms-3"
                      onClick={handleToggleStatus}
                    >
                      Toggle Status
                    </Button>
                  )}
                </ListGroup.Item>
              </ListGroup>

              <h5 className="section-title mt-4">Managed Places</h5>
              {editing ? (
                <>
                  <p className="text-muted small">Select places this provider can manage:</p>
                  {allPlaces.length > 0 ? (
                    <ListGroup>
                      {allPlaces.map(place => (
                        <ListGroup.Item key={place.id} className="d-flex justify-content-between align-items-center">
                          <div>
                            <FaBuilding className="me-2 text-secondary" />
                            {place.name}
                            <br />
                            <small className="text-muted">{place.address}</small>
                          </div>
                          <Form.Check
                            type="checkbox"
                            checked={formData.managedPlaceIds?.includes(place.id)}
                            onChange={() => handlePlaceToggle(place.id)}
                          />
                        </ListGroup.Item>
                      ))}
                    </ListGroup>
                  ) : (
                    <Alert variant="info">You have no places created yet.</Alert>
                  )}
                </>
              ) : (
                <>
                  {provider.managedPlaces && provider.managedPlaces.length > 0 ? (
                    <ListGroup>
                      {provider.managedPlaces.map(place => (
                        <ListGroup.Item key={place.id}>
                          <FaBuilding className="me-2 text-secondary" />
                          {place.name}
                          <br />
                          <small className="text-muted">{place.address}</small>
                        </ListGroup.Item>
                      ))}
                    </ListGroup>
                  ) : (
                    <p className="text-muted">No places assigned.</p>
                  )}
                </>
              )}
            </Col>

            <Col md={6}>
              <h5 className="section-title">Statistics</h5>
              <Card className="mb-3">
                <Card.Body>
                  <Row>
                    <Col xs={6}>
                      <div className="stat-item">
                        <FaChartLine className="stat-icon text-info" />
                        <div>
                          <div className="stat-label">Total Queues</div>
                          <div className="stat-value">{provider.totalQueues || 0}</div>
                        </div>
                      </div>
                    </Col>
                    <Col xs={6}>
                      <div className="stat-item">
                        <FaUsers className="stat-icon text-success" />
                        <div>
                          <div className="stat-label">Active Queues</div>
                          <div className="stat-value">{provider.activeQueues || 0}</div>
                        </div>
                      </div>
                    </Col>
                    <Col xs={6}>
                      <div className="stat-item">
                        <FaClock className="stat-icon text-warning" />
                        <div>
                          <div className="stat-label">Tokens Today</div>
                          <div className="stat-value">{provider.tokensServedToday || 0}</div>
                        </div>
                      </div>
                    </Col>
                    <Col xs={6}>
                      <div className="stat-item">
                        <FaStar className="stat-icon text-danger" />
                        <div>
                          <div className="stat-label">Avg Rating</div>
                          <div className="stat-value">
                            {provider.averageRating ? provider.averageRating.toFixed(1) : '—'}
                          </div>
                        </div>
                      </div>
                    </Col>
                  </Row>
                </Card.Body>
              </Card>

              <h5 className="section-title">Actions</h5>
              <Card>
                <Card.Body>
                  <Button
                    variant="danger"
                    className="w-100 mb-2"
                    onClick={() => setShowResetModal(true)}
                  >
                    <FaKey className="me-2" /> Reset Password
                  </Button>
                </Card.Body>
              </Card>
            </Col>
          </Row>
        </Card.Body>
      </Card>

      {/* Reset Password Confirmation Modal */}
      <Modal show={showResetModal} onHide={() => setShowResetModal(false)} centered>
        <Modal.Header closeButton>
          <Modal.Title>Reset Provider Password</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <p>
            Are you sure you want to reset the password for <strong>{provider.name}</strong>?
          </p>
          <p className="text-warning">
            A password reset email will be sent to {provider.email}.
          </p>
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={() => setShowResetModal(false)}>
            Cancel
          </Button>
          <Button
            variant="danger"
            onClick={handleResetPassword}
            disabled={resetLoading}
          >
            {resetLoading ? <Spinner animation="border" size="sm" /> : 'Send Reset Email'}
          </Button>
        </Modal.Footer>
      </Modal>
    </Container>
  );
};

export default AdminProviderDetail;