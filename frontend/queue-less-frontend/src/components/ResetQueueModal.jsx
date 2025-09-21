import React, { useState } from 'react';
import { Modal, Button, Form, Alert } from 'react-bootstrap';
import { FaExclamationTriangle, FaFileExport } from 'react-icons/fa';

const ResetQueueModal = ({ show, onHide, onReset, queueName }) => {
  const [options, setOptions] = useState({
    preserveData: true,
    reportType: 'full',
    includeUserDetails: true
  });

  const handleReset = () => {
    onReset(options);
    onHide();
  };

  return (
    <Modal show={show} onHide={onHide}>
      <Modal.Header closeButton>
        <Modal.Title>
          <FaExclamationTriangle className="me-2 text-warning" />
          Reset Queue
        </Modal.Title>
      </Modal.Header>
      <Modal.Body>
        <Alert variant="warning">
          <strong>Warning:</strong> This will remove all tokens from the queue "{queueName}". 
          This action cannot be undone.
        </Alert>
        
        <Form>
          <Form.Check
            type="checkbox"
            label="Preserve data by exporting first"
            checked={options.preserveData}
            onChange={(e) => setOptions({...options, preserveData: e.target.checked})}
            className="mb-3"
          />
          
          {options.preserveData && (
            <>
              <Form.Group className="mb-3">
                <Form.Label>Report Type</Form.Label>
                <Form.Select
                  value={options.reportType}
                  onChange={(e) => setOptions({...options, reportType: e.target.value})}
                >
                  <option value="tokens">Tokens Only</option>
                  <option value="statistics">Statistics</option>
                  <option value="full">Full Report</option>
                </Form.Select>
              </Form.Group>
              
              <Form.Check
                type="checkbox"
                label="Include user details in export"
                checked={options.includeUserDetails}
                onChange={(e) => setOptions({...options, includeUserDetails: e.target.checked})}
                className="mb-3"
              />
            </>
          )}
        </Form>
      </Modal.Body>
      <Modal.Footer>
        <Button variant="secondary" onClick={onHide}>
          Cancel
        </Button>
        <Button variant="danger" onClick={handleReset}>
          <FaFileExport className="me-2" />
          Reset Queue
        </Button>
      </Modal.Footer>
    </Modal>
  );
};

export default ResetQueueModal;