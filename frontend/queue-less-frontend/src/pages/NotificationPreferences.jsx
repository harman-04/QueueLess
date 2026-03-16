import React, { useState, useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { useNavigate } from 'react-router-dom';
import {
    Container, Card, Button, Spinner, Alert, Form,
    Row, Col, Badge, ListGroup, Modal
} from 'react-bootstrap';
import {
    FaBell, FaClock, FaExclamationTriangle, FaTrash,
    FaSave, FaTimes, FaEdit, FaPlus
} from 'react-icons/fa';
import { notificationPreferenceService } from '../services/notificationPreferenceService';
import axiosInstance from '../utils/axiosInstance';
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
    const [showAddModal, setShowAddModal] = useState(false);
    const [userQueues, setUserQueues] = useState([]);
    const [selectedQueueId, setSelectedQueueId] = useState('');
    const [loadingQueues, setLoadingQueues] = useState(false);
    const [saving, setSaving] = useState(false);
    const [showDeleteModal, setShowDeleteModal] = useState(false);
    const [deleteQueueId, setDeleteQueueId] = useState(null);
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

    const fetchUserQueues = async () => {
        setLoadingQueues(true);
        try {
            const response = await axiosInstance.get(`/queues/by-user/${userId}`);

            // Get all unique placeIds from the queues
            const placeIds = [...new Set(response.data.map(q => q.placeId))];

            // Fetch place details for all those placeIds
            const placePromises = placeIds.map(id => axiosInstance.get(`/places/${id}`));
            const placeResponses = await Promise.all(placePromises);
            const placeMap = {};
            placeResponses.forEach(res => {
                placeMap[res.data.id] = res.data.name;
            });

            // Enrich each queue with placeName
            const enrichedQueues = response.data.map(q => ({
                ...q,
                placeName: placeMap[q.placeId] || 'Unknown place'
            }));

            // Filter out queues that already have preferences
            const existingQueueIds = new Set(preferences.map(p => p.queueId));
            const available = enrichedQueues.filter(q => !existingQueueIds.has(q.id));
            setUserQueues(available);
        } catch (err) {
            toast.error('Failed to load your queues');
        } finally {
            setLoadingQueues(false);
        }
    };

    const handleEditClick = (pref) => {
        setEditingPref({
            queueId: pref.queueId,
            queueName: pref.queueName,
            notifyBeforeMinutes: pref.notifyBeforeMinutes || 5,
            notifyOnStatusChange: pref.notifyOnStatusChange ?? true,
            notifyOnEmergencyApproval: pref.notifyOnEmergencyApproval ?? true,
            enabled: pref.enabled ?? true,
            notifyOnBestTime: pref.notifyOnBestTime ?? false

        });
        setShowEditModal(true);
    };

    const handleAddClick = () => {
        setEditingPref({
            queueId: '',
            queueName: '',
            notifyBeforeMinutes: 5,
            notifyOnStatusChange: true,
            notifyOnEmergencyApproval: true,
            enabled: true,
            notifyOnBestTime: false
        });
        setSelectedQueueId('');
        fetchUserQueues();
        setShowAddModal(true);
    };

    const handleDeleteClick = (queueId) => {
        setDeleteQueueId(queueId);
        setShowDeleteModal(true);
    };

    const handleConfirmDelete = async () => {
        if (!deleteQueueId) return;
        try {
            await notificationPreferenceService.deletePreference(deleteQueueId);
            setPreferences(prefs => prefs.filter(p => p.queueId !== deleteQueueId));
            toast.success('Preference deleted');
        } catch (err) {
            toast.error('Failed to delete preference');
        } finally {
            setShowDeleteModal(false);
            setDeleteQueueId(null);
        }
    };

    const handleSaveEdit = async () => {
        setSaving(true);
        try {
            const response = await notificationPreferenceService.updatePreference(
                editingPref.queueId,
                editingPref
            );
            // Preserve the queue name from editing state
            const updatedPref = {
                ...response.data,
                queueName: editingPref.queueName
            };
            setPreferences(prefs =>
                prefs.map(p => p.queueId === editingPref.queueId ? updatedPref : p)
            );
            setShowEditModal(false);
            toast.success('Preference updated');
        } catch (err) {
            toast.error('Failed to update preference');
        } finally {
            setSaving(false);
        }
    };

    const handleSaveAdd = async () => {
        if (!selectedQueueId) {
            toast.error('Please select a queue');
            return;
        }
        setSaving(true);
        try {
            const response = await notificationPreferenceService.updatePreference(
                selectedQueueId,
                editingPref
            );
            // Get the queue details from userQueues to set the name
            const queue = userQueues.find(q => q.id === selectedQueueId);
            const newPref = {
                ...response.data,
                queueName: queue?.serviceName || ''
            };
            setPreferences(prev => [...prev, newPref]);
            setShowAddModal(false);
            toast.success('Preference added');
        } catch (err) {
            toast.error('Failed to add preference');
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
                    <FaPlus className="me-2" /> Add Preference
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
                        <Button variant="primary" onClick={handleAddClick}>
                            Add Your First Preference
                        </Button>
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

                                        <ListGroup.Item>
                                            <FaClock className="me-2 text-success" />
                                            Best time notification: <strong>{pref.notifyOnBestTime ? 'Yes' : 'No'}</strong>
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
                                        onClick={() => handleDeleteClick(pref.queueId)}
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

                        <Form.Group className="mb-3">
                            <Form.Check
                                type="checkbox"
                                label="Notify me when queue is shortest (once per day)"
                                name="notifyOnBestTime"
                                checked={editingPref?.notifyOnBestTime || false}
                                onChange={handleInputChange}
                            />
                            <Form.Text className="text-muted">
                                You'll receive a notification when the number of waiting people drops below 3.
                            </Form.Text>
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

            {/* Add Modal */}
            <Modal show={showAddModal} onHide={() => setShowAddModal(false)} centered size="lg">
                <Modal.Header closeButton>
                    <Modal.Title>Add Notification Preference</Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    {loadingQueues ? (
                        <div className="text-center py-4">
                            <Spinner animation="border" variant="primary" />
                            <p className="mt-2">Loading your queues...</p>
                        </div>
                    ) : userQueues.length === 0 ? (
                        <Alert variant="info">
                            You don't have any queues without preferences. All your active queues already have custom settings.
                        </Alert>
                    ) : (
                        <Form>
                            <Form.Group className="mb-3">
                                <Form.Label>Select Queue</Form.Label>
                                <Form.Select
                                    value={selectedQueueId}
                                    onChange={(e) => {
                                        const queueId = e.target.value;
                                        setSelectedQueueId(queueId);
                                        const queue = userQueues.find(q => q.id === queueId);
                                        setEditingPref(prev => ({
                                            ...prev,
                                            queueId,
                                            queueName: queue?.serviceName || ''
                                        }));
                                    }}
                                >
                                    <option value="">Choose a queue...</option>
                                    {userQueues.map(queue => (
                                        <option key={queue.id} value={queue.id}>
                                            {queue.serviceName} ({queue.placeName})
                                        </option>
                                    ))}
                                </Form.Select>
                            </Form.Group>

                            {selectedQueueId && (
                                <>
                                    <Form.Group className="mb-3">
                                        <Form.Label>Notify Before (minutes)</Form.Label>
                                        <Form.Control
                                            type="number"
                                            name="notifyBeforeMinutes"
                                            value={editingPref.notifyBeforeMinutes}
                                            onChange={handleInputChange}
                                            min="1"
                                            max="120"
                                        />
                                    </Form.Group>

                                    <Form.Group className="mb-3">
                                        <Form.Check
                                            type="checkbox"
                                            label="Notify on status change"
                                            name="notifyOnStatusChange"
                                            checked={editingPref.notifyOnStatusChange}
                                            onChange={handleInputChange}
                                        />
                                    </Form.Group>

                                    <Form.Group className="mb-3">
                                        <Form.Check
                                            type="checkbox"
                                            label="Notify on emergency approval"
                                            name="notifyOnEmergencyApproval"
                                            checked={editingPref.notifyOnEmergencyApproval}
                                            onChange={handleInputChange}
                                        />
                                    </Form.Group>

                                    <Form.Group className="mb-3">
                                        <Form.Check
                                            type="checkbox"
                                            label="Enable"
                                            name="enabled"
                                            checked={editingPref.enabled}
                                            onChange={handleInputChange}
                                        />
                                    </Form.Group>

                                    <Form.Group className="mb-3">
                                        <Form.Check
                                            type="checkbox"
                                            label="Notify me when queue is shortest (once per day)"
                                            name="notifyOnBestTime"
                                            checked={editingPref.notifyOnBestTime}
                                            onChange={handleInputChange}
                                        />
                                        <Form.Text className="text-muted">
                                            You'll receive a notification when the number of waiting people drops below 3.
                                        </Form.Text>
                                    </Form.Group>
                                </>
                            )}


                        </Form>
                    )}
                </Modal.Body>
                <Modal.Footer>
                    <Button variant="secondary" onClick={() => setShowAddModal(false)}>
                        Cancel
                    </Button>
                    <Button
                        variant="primary"
                        onClick={handleSaveAdd}
                        disabled={saving || !selectedQueueId || userQueues.length === 0}
                    >
                        {saving ? <Spinner animation="border" size="sm" /> : 'Add Preference'}
                    </Button>
                </Modal.Footer>
            </Modal>

            {/* Delete Confirmation Modal */}
            <Modal show={showDeleteModal} onHide={() => setShowDeleteModal(false)} centered className="delete-confirm-modal">
                <Modal.Header closeButton className="border-0">
                    <Modal.Title className="text-danger">
                        <FaExclamationTriangle className="me-2" /> Confirm Deletion
                    </Modal.Title>
                </Modal.Header>
                <Modal.Body className="text-center py-4">
                    <FaTrash size={48} className="text-danger mb-3" />
                    <h5>Are you sure?</h5>
                    <p className="text-muted">This action cannot be undone. You will revert to global notification settings for this queue.</p>
                </Modal.Body>
                <Modal.Footer className="border-0 justify-content-center">
                    <Button variant="outline-secondary" onClick={() => setShowDeleteModal(false)}>
                        Cancel
                    </Button>
                    <Button variant="danger" onClick={handleConfirmDelete}>
                        <FaTrash className="me-2" /> Delete Permanently
                    </Button>
                </Modal.Footer>
            </Modal>
        </Container>
    );
};

export default NotificationPreferences;