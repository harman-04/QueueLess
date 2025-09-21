import React, { useState, useEffect } from 'react';
import { Modal, Button, ListGroup, Badge, Alert, Spinner } from 'react-bootstrap';
import { FaAmbulance, FaCheck, FaTimes, FaClock } from 'react-icons/fa';
import axiosInstance from '../utils/axiosInstance';

const EmergencyApprovalModal = ({ show, onHide, queueId }) => {
  const [pendingTokens, setPendingTokens] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [approving, setApproving] = useState({});

  const fetchPendingTokens = async () => {
    if (!queueId) return;
    
    setLoading(true);
    try {
      const response = await axiosInstance.get(`/queues/${queueId}/pending-emergency`);
      setPendingTokens(response.data);
    } catch (err) {
      setError('Failed to fetch pending emergency tokens');
      console.error('Fetch error:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleApprove = async (tokenId, approve) => {
    setApproving(prev => ({ ...prev, [tokenId]: true }));
    try {
      await axiosInstance.post(`/queues/${queueId}/approve-emergency/${tokenId}?approve=${approve}`);
      // Refresh the list
      fetchPendingTokens();
    } catch (err) {
      setError(`Failed to ${approve ? 'approve' : 'reject'} token`);
      console.error('Approval error:', err);
    } finally {
      setApproving(prev => ({ ...prev, [tokenId]: false }));
    }
  };

  useEffect(() => {
    if (show) {
      fetchPendingTokens();
    }
  }, [show, queueId]);

  return (
    <Modal show={show} onHide={onHide} size="lg">
      <Modal.Header closeButton>
        <Modal.Title>
          <FaAmbulance className="me-2 text-danger" />
          Pending Emergency Tokens
        </Modal.Title>
      </Modal.Header>
      <Modal.Body>
        {error && <Alert variant="danger">{error}</Alert>}
        
        {loading ? (
          <div className="text-center">
            <Spinner animation="border" />
            <p>Loading pending tokens...</p>
          </div>
        ) : pendingTokens.length === 0 ? (
          <Alert variant="info">
            No pending emergency tokens
          </Alert>
        ) : (
          <ListGroup variant="flush">
            {pendingTokens.map(token => (
              <ListGroup.Item key={token.tokenId} className="d-flex justify-content-between align-items-center">
                <div className="flex-grow-1">
                  <h6 className="mb-1">
                    <Badge bg="danger" className="me-2">Emergency</Badge>
                    {token.tokenId}
                  </h6>
                  <p className="mb-1 text-muted small">
                    User: {token.userId}
                  </p>
                  <p className="mb-0">
                    <strong>Details:</strong> {token.emergencyDetails}
                  </p>
                  <small className="text-muted">
                    <FaClock className="me-1" />
                    Requested: {new Date(token.issuedAt).toLocaleTimeString()}
                  </small>
                </div>
                <div className="d-flex gap-2">
                  <Button
                    variant="success"
                    size="sm"
                    disabled={approving[token.tokenId]}
                    onClick={() => handleApprove(token.tokenId, true)}
                  >
                    {approving[token.tokenId] ? (
                      <Spinner animation="border" size="sm" />
                    ) : (
                      <FaCheck className="me-1" />
                    )}
                    Approve
                  </Button>
                  <Button
                    variant="outline-danger"
                    size="sm"
                    disabled={approving[token.tokenId]}
                    onClick={() => handleApprove(token.tokenId, false)}
                  >
                    {approving[token.tokenId] ? (
                      <Spinner animation="border" size="sm" />
                    ) : (
                      <FaTimes className="me-1" />
                    )}
                    Reject
                  </Button>
                </div>
              </ListGroup.Item>
            ))}
          </ListGroup>
        )}
      </Modal.Body>
      <Modal.Footer>
        <Button variant="secondary" onClick={onHide}>
          Close
        </Button>
        <Button variant="primary" onClick={fetchPendingTokens}>
          Refresh
        </Button>
      </Modal.Footer>
    </Modal>
  );
};

export default EmergencyApprovalModal;