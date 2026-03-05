// src/components/NotificationModal.jsx
import React from 'react';
import { Modal, Button } from 'react-bootstrap';
import { FaExclamationTriangle, FaTimesCircle, FaCheckCircle } from 'react-icons/fa';
import './NotificationModal.css';

const NotificationModal = ({ show, onHide, title, message, variant = 'danger' }) => {
    const icon = variant === 'success' ? <FaCheckCircle /> : <FaTimesCircle />;
    return (
        <Modal show={show} onHide={onHide} centered className="notification-modal">
            <Modal.Header closeButton className={`border-0 pb-0 bg-${variant} text-white`}>
                <Modal.Title className="d-flex align-items-center">
                    {icon}
                    <span className="ms-2">{title}</span>
                </Modal.Title>
            </Modal.Header>
            <Modal.Body className="pt-4">
                <p className="lead">{message}</p>
            </Modal.Body>
            <Modal.Footer className="border-0">
                <Button variant={variant} onClick={onHide}>
                    Got it
                </Button>
            </Modal.Footer>
        </Modal>
    );
};

export default NotificationModal;