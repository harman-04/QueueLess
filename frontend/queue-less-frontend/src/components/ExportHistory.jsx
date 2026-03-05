// src/components/ExportHistory.jsx
import React, { useState, useEffect } from 'react';
import { Modal, Button, ListGroup, Spinner, Alert } from 'react-bootstrap';
import { FaFilePdf, FaFileExcel, FaDownload, FaClock } from 'react-icons/fa';
import axiosInstance from '../utils/axiosInstance';
import { toast } from 'react-toastify';
import './ExportHistory.css';

const ExportHistory = ({ show, onHide }) => {
    const [exports, setExports] = useState([]);
    const [loading, setLoading] = useState(false);
    const [downloading, setDownloading] = useState(null);

    useEffect(() => {
        if (show) {
            fetchExports();
        }
    }, [show]);

    const fetchExports = async () => {
        setLoading(true);
        try {
            const response = await axiosInstance.get('/export/exports');
            setExports(response.data);
        } catch (error) {
            toast.error('Failed to load export history');
        } finally {
            setLoading(false);
        }
    };

    const handleDownload = async (exportId) => {
        setDownloading(exportId);
        try {
            const response = await axiosInstance.get(`/export/exports/${exportId}`, {
                responseType: 'blob'
            });
            const url = window.URL.createObjectURL(new Blob([response.data]));
            const link = document.createElement('a');
            link.href = url;
            // Extract filename from Content-Disposition header
            const contentDisposition = response.headers['content-disposition'];
            let filename = 'export.pdf';
            if (contentDisposition) {
                const match = contentDisposition.match(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/);
                if (match && match[1]) {
                    filename = match[1].replace(/['"]/g, '');
                }
            }
            link.setAttribute('download', filename);
            document.body.appendChild(link);
            link.click();
            link.remove();
            window.URL.revokeObjectURL(url);
        } catch (error) {
            toast.error('Failed to download export');
        } finally {
            setDownloading(null);
        }
    };

    const formatDate = (dateString) => {
        return new Date(dateString).toLocaleString();
    };

    return (
        <Modal show={show} onHide={onHide} size="lg" className="export-history-modal">
            <Modal.Header closeButton>
                <Modal.Title>Export History</Modal.Title>
            </Modal.Header>
            <Modal.Body>
                {loading ? (
                    <div className="text-center">
                        <Spinner animation="border" variant="primary" />
                        <p>Loading exports...</p>
                    </div>
                ) : exports.length === 0 ? (
                    <Alert variant="info">No exports found. Reset a queue with "Preserve data" to generate one.</Alert>
                ) : (
                    <ListGroup variant="flush">
                        {exports.map((exp) => (
                            <ListGroup.Item key={exp.exportId} className="d-flex justify-content-between align-items-center">
                                <div>
                                    <div>
                                        {exp.format === 'pdf' ? <FaFilePdf className="text-danger me-2" /> : <FaFileExcel className="text-success me-2" />}
                                        <strong>{exp.filename}</strong>
                                    </div>
                                    <small className="text-muted">
                                        <FaClock className="me-1" />
                                        {formatDate(exp.createdAt)} • Queue: {exp.queueId} • Type: {exp.reportType}
                                    </small>
                                </div>
                                <Button
                                    variant="outline-primary"
                                    size="sm"
                                    onClick={() => handleDownload(exp.exportId)}
                                    disabled={downloading === exp.exportId}
                                >
                                    {downloading === exp.exportId ? <Spinner animation="border" size="sm" /> : <FaDownload />}
                                </Button>
                            </ListGroup.Item>
                        ))}
                    </ListGroup>
                )}
            </Modal.Body>
            <Modal.Footer>
                <Button variant="secondary" onClick={onHide}>
                    Close
                </Button>
                <Button variant="primary" onClick={fetchExports} disabled={loading}>
                    Refresh
                </Button>
            </Modal.Footer>
        </Modal>
    );
};

export default ExportHistory;