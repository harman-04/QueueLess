// src/components/QRScannerModal.jsx
import React, { useState, useRef, useEffect } from 'react';
import { Modal, Button, Alert, Spinner, Form } from 'react-bootstrap';
import { Html5Qrcode } from 'html5-qrcode';
import { useNavigate, useLocation } from 'react-router-dom';
import { toast } from 'react-toastify';
import jsQR from 'jsqr';
import { useSelector } from 'react-redux';
import axiosInstance from '../utils/axiosInstance';
import './QRScannerModal.css';

const QRScannerModal = ({ show, onHide }) => {
  const [scanning, setScanning] = useState(false);
  const [error, setError] = useState(null);
  const [file, setFile] = useState(null);
  const [processingFile, setProcessingFile] = useState(false);
  const scannerRef = useRef(null);
  const scannerInitialized = useRef(false);
  const navigate = useNavigate();
  const location = useLocation();
  const { token: isLoggedIn } = useSelector((state) => state.auth);

  // Cleanup scanner
  const stopScanner = async () => {
    if (scannerRef.current && scannerInitialized.current) {
      try {
        await scannerRef.current.stop();
        await scannerRef.current.clear();
      } catch (err) {
        console.debug('Scanner stop error:', err);
      } finally {
        scannerRef.current = null;
        scannerInitialized.current = false;
        setScanning(false);
      }
    }
  };

  useEffect(() => {
    if (!show) {
      stopScanner();
      setError(null);
      setFile(null);
      setProcessingFile(false);
      return;
    }

    const startScanner = async () => {
      if (scannerInitialized.current) return;
      const scanner = new Html5Qrcode('qr-reader');
      scannerRef.current = scanner;
      scannerInitialized.current = true;
      try {
        setScanning(true);
        await scanner.start(
          { facingMode: 'environment' },
          { fps: 10, qrbox: { width: 250, height: 250 } },
          (decodedText) => handleScanSuccess(decodedText),
          (errorMessage) => console.debug(errorMessage)
        );
      } catch (err) {
        setError('Camera access failed. Please upload an image instead.');
        setScanning(false);
      }
    };

    startScanner();

    return () => { stopScanner(); };
  }, [show]);

  const processQrData = async (data) => {
    try {
      let qrData;
      // Try to parse JSON; if fails, assume it's just a queueId (backward compatibility)
      try {
        qrData = JSON.parse(data);
      } catch {
        qrData = { queueId: data, tokenType: 'REGULAR' };
      }

      const { queueId, tokenType = 'REGULAR' } = qrData;

      if (!queueId) {
        setError('Invalid QR code: missing queue ID');
        return;
      }

      if (isLoggedIn) {
        // Direct join
        const response = await axiosInstance.post('/queues/join-by-qr', { queueId, tokenType });
        toast.success('You have joined the queue!');
        onHide();
        navigate(`/customer/queue/${queueId}`);
      } else {
        // Save qr data and redirect to login
        onHide();
        navigate('/login', { state: { from: location.pathname, qrData: { queueId, tokenType } } });
      }
    } catch (err) {
      setError('Failed to process QR code');
      console.error(err);
    }
  };

  const handleScanSuccess = (decodedText) => {
    stopScanner().then(() => processQrData(decodedText));
  };

  const handleFileUpload = async () => {
    if (!file) return;
    setProcessingFile(true);
    setError(null);

    const reader = new FileReader();
    reader.onload = (e) => {
      const img = new Image();
      img.onload = () => {
        const canvas = document.createElement('canvas');
        canvas.width = img.width;
        canvas.height = img.height;
        const ctx = canvas.getContext('2d');
        ctx.drawImage(img, 0, 0);
        const imageData = ctx.getImageData(0, 0, img.width, img.height);
        const code = jsQR(imageData.data, imageData.width, imageData.height);
        if (code) {
          processQrData(code.data).finally(() => setProcessingFile(false));
        } else {
          setError('Could not read QR code from image. Try a clearer picture.');
          setProcessingFile(false);
        }
      };
      img.src = e.target.result;
    };
    reader.readAsDataURL(file);
  };

  return (
    <Modal show={show} onHide={onHide} size="lg" className="qr-scanner-modal">
      <Modal.Header closeButton>
        <Modal.Title>Scan Queue QR Code</Modal.Title>
      </Modal.Header>
      <Modal.Body>
        {error && <Alert variant="danger">{error}</Alert>}
        <div id="qr-reader" style={{ width: '100%', minHeight: '300px' }} />
        {scanning && <p className="text-center text-muted">Position the QR code in the frame</p>}

        <hr />
        <h6>Or upload an image</h6>
        <Form.Group controlId="qrFile">
          <Form.Control
            type="file"
            accept="image/*"
            onChange={(e) => setFile(e.target.files[0])}
          />
        </Form.Group>
        <Button
          variant="primary"
          className="mt-2"
          onClick={handleFileUpload}
          disabled={!file || processingFile}
        >
          {processingFile ? <Spinner animation="border" size="sm" /> : 'Scan from Image'}
        </Button>
      </Modal.Body>
    </Modal>
  );
};

export default QRScannerModal;