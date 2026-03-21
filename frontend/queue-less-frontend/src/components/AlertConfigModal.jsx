// src/components/AlertConfigModal.jsx
import React, { useState, useEffect } from 'react';
import { Modal, Button, Form, Spinner, Alert } from 'react-bootstrap';
import { FaBell, FaSave, FaTrash } from 'react-icons/fa';
import axiosInstance from '../utils/axiosInstance';
import { toast } from 'react-toastify';
import './AlertConfigModal.css';

const AlertConfigModal = ({ show, onHide, onConfigChanged }) => {
  const [threshold, setThreshold] = useState(15);
  const [email, setEmail] = useState('');
  const [enabled, setEnabled] = useState(true);
  const [loading, setLoading] = useState(false);
  const [fetching, setFetching] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (show) {
      fetchConfig();
    }
  }, [show]);

  const fetchConfig = async () => {
  setFetching(true);
  setError(null);
  try {
    const response = await axiosInstance.get('/admin/alert-config');
    setThreshold(response.data.thresholdWaitTime);
    setEmail(response.data.notificationEmail || '');
    setEnabled(response.data.enabled);
  } catch (err) {
    // Check for 404 – the normalized error from axiosInstance has a status property
    const is404 = err.status === 404 ||
                  (err.response?.status === 404) ||
                  (err.message?.includes('404'));

    if (is404) {
      // No configuration yet – use defaults and do not show error
      setThreshold(15);
      setEmail('');
      setEnabled(true);
      // Optional: log a debug message instead of error
      console.debug('No alert config found, using defaults');
    } else {
      console.error('Failed to load alert config:', err);
      setError('Failed to load alert configuration');
    }
  } finally {
    setFetching(false);
  }
};

  const handleSave = async () => {
    setLoading(true);
    setError(null);
    try {
      await axiosInstance.post('/admin/alert-config', null, {
        params: {
          thresholdWaitTime: threshold,
          notificationEmail: email || undefined,
        }
      });
      toast.success('Alert configuration saved');
      onConfigChanged();
      onHide();
    } catch (err) {
      setError('Failed to save configuration');
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async () => {
    if (!window.confirm('Are you sure you want to delete alert configuration?')) return;
    setLoading(true);
    try {
      await axiosInstance.delete('/admin/alert-config');
      toast.success('Alert configuration deleted');
      onConfigChanged();
      onHide();
    } catch (err) {
      setError('Failed to delete configuration');
    } finally {
      setLoading(false);
    }
  };

  const handleToggle = async () => {
    try {
      await axiosInstance.put('/admin/alert-config/toggle', null, {
        params: { enabled: !enabled }
      });
      setEnabled(!enabled);
      toast.success(`Alerts ${!enabled ? 'enabled' : 'disabled'}`);
    } catch (err) {
      toast.error('Failed to toggle alerts');
    }
  };

  return (
    <Modal show={show} onHide={onHide} size="lg" className="alert-config-modal">
      <Modal.Header closeButton>
        <Modal.Title>
          <FaBell className="me-2 text-warning" />
          Alert Configuration
        </Modal.Title>
      </Modal.Header>
      <Modal.Body>
        {fetching ? (
          <div className="text-center">
            <Spinner animation="border" />
            <p>Loading configuration...</p>
          </div>
        ) : (
          <>
            {error && <Alert variant="danger">{error}</Alert>}
            <Form>
              <Form.Group className="mb-3">
                <Form.Label>Wait Time Threshold (minutes)</Form.Label>
                <Form.Control
                  type="number"
                  min="1"
                  value={threshold}
                  onChange={(e) => setThreshold(parseInt(e.target.value))}
                />
                <Form.Text className="text-muted">
                  You will be alerted when any queue exceeds this wait time.
                </Form.Text>
              </Form.Group>
              <Form.Group className="mb-3">
                <Form.Label>Notification Email</Form.Label>
                <Form.Control
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="Leave blank to use your account email"
                />
              </Form.Group>
              <Form.Check
                type="switch"
                label="Alerts Enabled"
                checked={enabled}
                onChange={handleToggle}
                className="mb-3"
              />
            </Form>
          </>
        )}
      </Modal.Body>
      <Modal.Footer>
        <Button variant="danger" onClick={handleDelete} disabled={loading || fetching}>
          <FaTrash className="me-2" /> Delete
        </Button>
        <Button variant="secondary" onClick={onHide}>
          Cancel
        </Button>
        <Button variant="primary" onClick={handleSave} disabled={loading || fetching}>
          {loading ? <Spinner animation="border" size="sm" /> : <FaSave className="me-2" />}
          Save
        </Button>
      </Modal.Footer>
    </Modal>
  );
};

export default AlertConfigModal;