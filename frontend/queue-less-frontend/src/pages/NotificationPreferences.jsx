import React, { useState, useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { useNavigate } from 'react-router-dom';
import {
  Container, Card, Button, Spinner, Alert, Form,
  Row, Col, Badge, ListGroup, Modal
} from 'react-bootstrap';
import {
  FaBell, FaClock, FaExclamationTriangle, FaTrash,
  FaSave, FaTimes, FaEdit
} from 'react-icons/fa';
import { notificationPreferenceService } from '../services/notificationPreferenceService';
import { toast } from 'react-toastify';
import './NotificationPreferences.css';

const NotificationPreferences = () => {
  const navigate = useNavigate();
  const dispatch = useDispatch();
  const { token, id: userId } = useSelector((state) => state.auth);

  const [preferences, setPreferences] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [editingPref, setEditingPref] = useState(null);
  const [showEditModal, setShowEditModal] = useState(false);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!token) {
      navigate('/login');
      return;
    }
    fetchPreferences();
  }, [token, navigate]);

  const fetchPreferences = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await notificationPreferenceService.getMyPreferences();
      setPreferences(response.data);
    } catch (err) {
      setError('Failed to load notification preferences');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleEditClick = (pref) => {
    setEditingPref({
      queueId: pref.queueId,
      queueName: pref.queueName,
      notifyBeforeMinutes: pref.notifyBeforeMinutes || 5,
      notifyOnStatusChange: pref.notifyOnStatusChange ?? true,
      notifyOnEmergencyApproval: pref.notifyOnEmergencyApproval ?? true,
      enabled: pref.enabled ?? true
    });
    setShowEditModal(true);
  };

  const handleAddClick = () => {
    // For adding new preference, we need to select a queue from user's active queues.
    // This could be a separate modal with a dropdown of queues the user has tokens in.
    // We'll implement this later if needed.
    toast.info('Add feature coming soon');
  };

  const handleDelete = async (queueId) => {
    if (!window.confirm('Are you sure you want to delete this preference? You will revert to global settings.')) return;
    try {
      await notificationPreferenceService.deletePreference(queueId);
      setPreferences(prefs => prefs.filter(p => p.queueId !== queueId));
      toast.success('Preference deleted');
    } catch (err) {
      toast.error('Failed to delete preference');
    }
  };

  const handleSaveEdit = async () => {
    setSaving(true);
    try {
      const response = await notificationPreferenceService.updatePreference(
        editingPref.queueId,
        editingPref
      );
      setPreferences(prefs =>
        prefs.map(p => p.queueId === editingPref.queueId ? response.data : p)
      );
      setShowEditModal(false);
      toast.success('Preference updated');
    } catch (err) {
      toast.error('Failed to update preference');
    } finally {
      setSaving(false);
    }
  };

  const handleInputChange = (e) => {
    const { name, value, type, checked } = e.target;
    setEditingPref(prev => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : (type === 'number' ? parseInt(value) : value)
    }));
  };

  if (loading) {
    return (
      <Container className="text-center py-5">
        <Spinner animation="border" variant="primary" />
        <p className="mt-3">Loading preferences...</p>
      </Container>
    );
  }

  return (
    <Container className="notification-preferences py-4">
      <div className="d-flex justify-content-between align-items-center mb-4">
        <h1>
          <FaBell className="me-2" />
          Notification Preferences
        </h1>
        <Button variant="primary" onClick={handleAddClick}>
          Add Preference
        </Button>
      </div>

      {error && <Alert variant="danger">{error}</Alert>}

      {preferences.length === 0 ? (
        <Card className="text-center p-5">
          <Card.Body>
            <FaBell size={48} className="text-muted mb-3" />
            <h4>No custom preferences set</h4>
            <p className="text-muted">
              You are using global notification settings for all queues.
              Add a preference to customize when you receive notifications for specific queues.
            </p>
          </Card.Body>
        </Card>
      ) : (
        <Row>
          {preferences.map(pref => (
            <Col md={6} lg={4} key={pref.queueId} className="mb-4">
              <Card className="preference-card h-100">
                <Card.Body>
                  <div className="d-flex justify-content-between align-items-start">
                    <Card.Title>{pref.queueName}</Card.Title>
                    <Badge bg={pref.enabled ? 'success' : 'secondary'}>
                      {pref.enabled ? 'Active' : 'Disabled'}
                    </Badge>
                  </div>
                  <ListGroup variant="flush" className="mt-3">
                    <ListGroup.Item>
                      <FaClock className="me-2 text-primary" />
                      Notify before: <strong>{pref.notifyBeforeMinutes} min</strong>
                    </ListGroup.Item>
                    <ListGroup.Item>
                      <FaExclamationTriangle className="me-2 text-warning" />
                      Status change: <strong>{pref.notifyOnStatusChange ? 'Yes' : 'No'}</strong>
                    </ListGroup.Item>
                    <ListGroup.Item>
                      <FaBell className="me-2 text-info" />
                      Emergency approval: <strong>{pref.notifyOnEmergencyApproval ? 'Yes' : 'No'}</strong>
                    </ListGroup.Item>
                  </ListGroup>
                </Card.Body>
                <Card.Footer className="d-flex justify-content-end gap-2">
                  <Button
                    variant="outline-primary"
                    size="sm"
                    onClick={() => handleEditClick(pref)}
                  >
                    <FaEdit className="me-1" /> Edit
                  </Button>
                  <Button
                    variant="outline-danger"
                    size="sm"
                    onClick={() => handleDelete(pref.queueId)}
                  >
                    <FaTrash className="me-1" /> Delete
                  </Button>
                </Card.Footer>
              </Card>
            </Col>
          ))}
        </Row>
      )}

      {/* Edit Modal */}
      <Modal show={showEditModal} onHide={() => setShowEditModal(false)} centered>
        <Modal.Header closeButton>
          <Modal.Title>Edit Preference for {editingPref?.queueName}</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <Form>
            <Form.Group className="mb-3">
              <Form.Label>Notify Before (minutes)</Form.Label>
              <Form.Control
                type="number"
                name="notifyBeforeMinutes"
                value={editingPref?.notifyBeforeMinutes || 5}
                onChange={handleInputChange}
                min="1"
                max="120"
              />
              <Form.Text className="text-muted">
                You will be notified this many minutes before your turn.
              </Form.Text>
            </Form.Group>

            <Form.Group className="mb-3">
              <Form.Check
                type="checkbox"
                label="Notify on status change (e.g., when token is served)"
                name="notifyOnStatusChange"
                checked={editingPref?.notifyOnStatusChange || false}
                onChange={handleInputChange}
              />
            </Form.Group>

            <Form.Group className="mb-3">
              <Form.Check
                type="checkbox"
                label="Notify on emergency approval/rejection"
                name="notifyOnEmergencyApproval"
                checked={editingPref?.notifyOnEmergencyApproval || false}
                onChange={handleInputChange}
              />
            </Form.Group>

            <Form.Group className="mb-3">
              <Form.Check
                type="checkbox"
                label="Enable these preferences"
                name="enabled"
                checked={editingPref?.enabled ?? true}
                onChange={handleInputChange}
              />
            </Form.Group>
          </Form>
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={() => setShowEditModal(false)}>
            Cancel
          </Button>
          <Button
            variant="primary"
            onClick={handleSaveEdit}
            disabled={saving}
          >
            {saving ? <Spinner animation="border" size="sm" /> : <FaSave className="me-2" />}
            Save Changes
          </Button>
        </Modal.Footer>
      </Modal>
    </Container>
  );
};

export default NotificationPreferences;