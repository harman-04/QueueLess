// src/components/CancelTokenModal.jsx
import React, { useState } from 'react';
import { Modal, Button, Form } from 'react-bootstrap';
import { FaTrashAlt, FaExclamationTriangle } from 'react-icons/fa';
import './CancelTokenModal.css'; // we'll create this for styling
import { getShortTokenId } from '../utils/tokenUtils';

const CancelTokenModal = ({ show, onHide, token, onConfirm }) => {
    const [reason, setReason] = useState('');
    const [isSubmitting, setIsSubmitting] = useState(false);

    const handleConfirm = async () => {
        setIsSubmitting(true);
        try {
            await onConfirm(reason);
            onHide();
        } catch (error) {
            console.error('Cancel failed:', error);
        } finally {
            setIsSubmitting(false);
        }
    };

    return (
        <Modal show={show} onHide={onHide} centered className="cancel-token-modal">
            <Modal.Header closeButton className="border-0 pb-0">
                <Modal.Title className="text-danger">
                    <FaExclamationTriangle className="me-2" />
                    Cancel Token
                </Modal.Title>
            </Modal.Header>
            <Modal.Body className="pt-2">
                <p>
                    Are you sure you want to cancel token <strong>{token ? getShortTokenId(token.tokenId) : ''}</strong>?
                </p>
                <Form.Group>
                    <Form.Label>Reason (optional)</Form.Label>
                    <Form.Control
                        as="textarea"
                        rows={3}
                        value={reason}
                        onChange={(e) => setReason(e.target.value)}
                        placeholder="Provide a reason for cancellation (optional)..."
                    />
                </Form.Group>
            </Modal.Body>
            <Modal.Footer className="border-0">
                <Button variant="outline-secondary" onClick={onHide} disabled={isSubmitting}>
                    No, Keep Token
                </Button>
                <Button
                    variant="danger"
                    onClick={handleConfirm}
                    disabled={isSubmitting}
                >
                    {isSubmitting ? 'Cancelling...' : 'Yes, Cancel Token'}
                </Button>
            </Modal.Footer>
        </Modal>
    );
};

export default CancelTokenModal;