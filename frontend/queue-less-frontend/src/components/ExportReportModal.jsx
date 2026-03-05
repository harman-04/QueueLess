// src/components/ExportReportModal.jsx
import React, { useState } from 'react';
import { Modal, Button, Row, Col, Card, Spinner, Form } from 'react-bootstrap';
import { FaFilePdf, FaFileExcel, FaChartBar, FaUserClock, FaFileExport } from 'react-icons/fa';
import './ExportReportModal.css';

const ExportReportModal = ({ show, onHide, onExport, isExporting }) => {
    const [includeUserDetails, setIncludeUserDetails] = useState(false);

    const handleExportClick = (format, reportType) => {
        onExport(format, reportType, includeUserDetails);
    };

    return (
        <Modal show={show} onHide={onHide} centered className="export-report-modal">
            <Modal.Header closeButton>
                <Modal.Title className="export-modal-title">
                    <FaFileExport className="me-2" /> Export Reports
                </Modal.Title>
            </Modal.Header>
            <Modal.Body className="export-modal-body">
                <p className="text-center mb-4">Choose the report type and format you wish to export.</p>

                {/* New Checkbox for User Details */}
                <Form.Group className="mb-4 text-center">
                    <Form.Check
                        type="checkbox"
                        id="includeUserDetails"
                        label="Include user details in report"
                        checked={includeUserDetails}
                        onChange={(e) => setIncludeUserDetails(e.target.checked)}
                    />
                    <Form.Text className="text-muted">
                        When checked, exported reports will contain user names and details (if visible).
                    </Form.Text>
                </Form.Group>

                <Row className="g-3">
                    {/* Tokens PDF Card */}
                    <Col xs={12} md={6}>
                        <Card
                            className={`export-card ${isExporting('pdf', 'tokens') ? 'exporting' : ''}`}
                            onClick={() => !isExporting('pdf', 'tokens') && handleExportClick('pdf', 'tokens')}
                        >
                            <Card.Body>
                                <div className="card-icon pdf-icon"><FaFilePdf /></div>
                                <Card.Title>Tokens PDF</Card.Title>
                                <Card.Text>Export all tokens in a PDF report.</Card.Text>
                                {isExporting('pdf', 'tokens') && <Spinner animation="border" size="sm" />}
                            </Card.Body>
                        </Card>
                    </Col>

                    {/* Statistics PDF Card */}
                    <Col xs={12} md={6}>
                        <Card
                            className={`export-card ${isExporting('pdf', 'statistics') ? 'exporting' : ''}`}
                            onClick={() => !isExporting('pdf', 'statistics') && handleExportClick('pdf', 'statistics')}
                        >
                            <Card.Body>
                                <div className="card-icon pdf-icon"><FaChartBar /></div>
                                <Card.Title>Stats PDF</Card.Title>
                                <Card.Text>Export queue statistics in a PDF report.</Card.Text>
                                {isExporting('pdf', 'statistics') && <Spinner animation="border" size="sm" />}
                            </Card.Body>
                        </Card>
                    </Col>

                    {/* Tokens Excel Card */}
                    <Col xs={12} md={6}>
                        <Card
                            className={`export-card ${isExporting('excel', 'tokens') ? 'exporting' : ''}`}
                            onClick={() => !isExporting('excel', 'tokens') && handleExportClick('excel', 'tokens')}
                        >
                            <Card.Body>
                                <div className="card-icon excel-icon"><FaFileExcel /></div>
                                <Card.Title>Tokens Excel</Card.Title>
                                <Card.Text>Export all tokens in an Excel file.</Card.Text>
                                {isExporting('excel', 'tokens') && <Spinner animation="border" size="sm" />}
                            </Card.Body>
                        </Card>
                    </Col>

                    {/* Statistics Excel Card */}
                    <Col xs={12} md={6}>
                        <Card
                            className={`export-card ${isExporting('excel', 'statistics') ? 'exporting' : ''}`}
                            onClick={() => !isExporting('excel', 'statistics') && handleExportClick('excel', 'statistics')}
                        >
                            <Card.Body>
                                <div className="card-icon excel-icon"><FaChartBar /></div>
                                <Card.Title>Stats Excel</Card.Title>
                                <Card.Text>Export queue statistics in an Excel file.</Card.Text>
                                {isExporting('excel', 'statistics') && <Spinner animation="border" size="sm" />}
                            </Card.Body>
                        </Card>
                    </Col>
                </Row>
            </Modal.Body>
            <Modal.Footer>
                <Button variant="secondary" onClick={onHide}>
                    Close
                </Button>
            </Modal.Footer>
        </Modal>
    );
};

export default ExportReportModal;