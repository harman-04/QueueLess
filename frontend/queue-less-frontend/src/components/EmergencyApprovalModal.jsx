// src/components/EmergencyApprovalModal.jsx
import './EmergencyApprovalModal.css';
import React, { useState, useEffect } from 'react';
import { Modal, Button, ListGroup, Badge, Alert, Spinner, Form } from 'react-bootstrap';
import { FaAmbulance, FaCheck, FaTimes, FaClock } from 'react-icons/fa';
import axiosInstance from '../utils/axiosInstance';
import { getShortTokenId } from '../utils/tokenUtils';

const EmergencyApprovalModal = ({ show, onHide, queueId }) => {
    const [pendingTokens, setPendingTokens] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [approving, setApproving] = useState({});
    const [rejectionReason, setRejectionReason] = useState('');
    const [showReasonInput, setShowReasonInput] = useState(false);
    const [rejectingTokenId, setRejectingTokenId] = useState(null);

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

    const handleApprove = async (tokenId) => {
        setApproving(prev => ({ ...prev, [tokenId]: true }));
        try {
            await axiosInstance.post(`/queues/${queueId}/approve-emergency/${tokenId}?approve=true`);
            fetchPendingTokens();
        } catch (err) {
            setError(`Failed to approve token`);
            console.error('Approval error:', err);
        } finally {
            setApproving(prev => ({ ...prev, [tokenId]: false }));
        }
    };

    const handleRejectClick = (tokenId) => {
        setRejectingTokenId(tokenId);
        setShowReasonInput(true);
    };

    const confirmReject = async () => {
        setApproving(prev => ({ ...prev, [rejectingTokenId]: true }));
        try {
            await axiosInstance.post(
                `/queues/${queueId}/approve-emergency/${rejectingTokenId}?approve=false&reason=${encodeURIComponent(rejectionReason)}`
            );
            setShowReasonInput(false);
            setRejectionReason('');
            setRejectingTokenId(null);
            fetchPendingTokens();
        } catch (err) {
            setError(`Failed to reject token`);
            console.error('Rejection error:', err);
        } finally {
            setApproving(prev => ({ ...prev, [rejectingTokenId]: false }));
        }
    };

    const cancelReject = () => {
        setShowReasonInput(false);
        setRejectionReason('');
        setRejectingTokenId(null);
    };

    useEffect(() => {
        if (show) {
            fetchPendingTokens();
        }
    }, [show, queueId]);

    return (
    <Modal show={show} onHide={onHide} size="lg" className="emergency-modal">
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
                                        {getShortTokenId(token.tokenId)}
                                    </h6>
                                    <p className="mb-1 text-muted small">
                                        User: {token.userName}
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
                                        onClick={() => handleApprove(token.tokenId)}
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
                                        onClick={() => handleRejectClick(token.tokenId)}
                                    >
                                        {approving[token.tokenId] ? (
                                            <Spinner animation="border" size="sm" />
                                        ) : (
                                            <FaTimes className="me-1" />
                                        )}
                                        Reject
                                    </Button>
                                </div>
                                {showReasonInput && rejectingTokenId === token.tokenId && (
                                    <div className="mt-3 ms-3">
                                        <Form.Group>
                                            <Form.Label>Rejection Reason</Form.Label>
                                            <Form.Control
                                                as="textarea"
                                                rows={2}
                                                value={rejectionReason}
                                                onChange={(e) => setRejectionReason(e.target.value)}
                                                placeholder="Enter reason for rejection..."
                                            />
                                        </Form.Group>
                                        <div className="d-flex gap-2 mt-2">
                                            <Button
                                                variant="danger"
                                                size="sm"
                                                onClick={confirmReject}
                                                disabled={!rejectionReason.trim()}
                                            >
                                                Confirm Reject
                                            </Button>
                                            <Button
                                                variant="secondary"
                                                size="sm"
                                                onClick={cancelReject}
                                            >
                                                Cancel
                                            </Button>
                                        </div>
                                    </div>
                                )}
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