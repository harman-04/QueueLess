// src/components/QRCodeModal.jsx
import React, { useState, useEffect } from 'react';
import { Modal, Button, Spinner, Alert } from 'react-bootstrap';
import { FaQrcode, FaDownload } from 'react-icons/fa';
import axiosInstance from '../utils/axiosInstance';
import './QRCodeModal.css';

const QRCodeModal = ({ show, onHide, queueId, queueName }) => {
  const [qrImage, setQrImage] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (show && queueId) {
      fetchQRCode();
    }
  }, [show, queueId]);

  const fetchQRCode = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await axiosInstance.get(`/queues/${queueId}/qr`, {
        responseType: 'blob',
      });
      const url = URL.createObjectURL(response.data);
      setQrImage(url);
    } catch (err) {
      setError('Failed to generate QR code');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleDownload = () => {
    if (!qrImage) return;
    const link = document.createElement('a');
    link.href = qrImage;
    link.download = `queue-${queueId}-qr.png`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  return (
    <Modal show={show} onHide={onHide} centered className="qr-modal">
      <Modal.Header closeButton>
        <Modal.Title>
          <FaQrcode className="me-2" />
          QR Code for {queueName}
        </Modal.Title>
      </Modal.Header>
      <Modal.Body className="text-center">
        {loading && <Spinner animation="border" />}
        {error && <Alert variant="danger">{error}</Alert>}
        {qrImage && (
          <img src={qrImage} alt="Queue QR Code" style={{ maxWidth: '100%', height: 'auto' }} />
        )}
      </Modal.Body>
      <Modal.Footer>
        <Button variant="secondary" onClick={onHide}>
          Close
        </Button>
        {qrImage && (
          <Button variant="primary" onClick={handleDownload}>
            <FaDownload className="me-2" /> Download
          </Button>
        )}
      </Modal.Footer>
    </Modal>
  );
};

export default QRCodeModal;