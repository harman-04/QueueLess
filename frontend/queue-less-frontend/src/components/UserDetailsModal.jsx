import React, { useState, useEffect } from 'react';
import { Modal, Button, Spinner, Alert } from 'react-bootstrap';
import { FaUser, FaInfoCircle, FaEye, FaEyeSlash } from 'react-icons/fa';
import axiosInstance from '../utils/axiosInstance';

const UserDetailsModal = ({ show, onHide, queueId, tokenId }) => {
  const [userDetails, setUserDetails] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (show && tokenId) {
      fetchUserDetails();
    }
  }, [show, tokenId]);

  const fetchUserDetails = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await axiosInstance.get(
        `/queues/${queueId}/token/${tokenId}/user-details`
      );
      setUserDetails(response.data);
    } catch (err) {
      setError('Failed to fetch user details');
      console.error('Error fetching user details:', err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal show={show} onHide={onHide} size="lg">
      <Modal.Header closeButton>
        <Modal.Title>
          <FaUser className="me-2" />
          User Details for Token {tokenId}
        </Modal.Title>
      </Modal.Header>
      <Modal.Body>
        {loading ? (
          <div className="text-center">
            <Spinner animation="border" variant="primary" />
            <p>Loading user details...</p>
          </div>
        ) : error ? (
          <Alert variant="danger">
            <FaInfoCircle className="me-2" />
            {error}
          </Alert>
        ) : userDetails ? (
          <div>
            <div className="d-flex justify-content-between align-items-center mb-3">
              <h5>{userDetails.userName}</h5>
              <span className={`badge ${userDetails.detailsVisible ? 'bg-success' : 'bg-secondary'}`}>
                {userDetails.detailsVisible ? <FaEye /> : <FaEyeSlash />}
                {userDetails.detailsVisible ? ' Details Visible' : ' Details Private'}
              </span>
            </div>
            
            {userDetails.detailsVisible ? (
              <div>
                {userDetails.purpose && (
                  <div className="mb-3">
                    <h6>Purpose</h6>
                    <p>{userDetails.purpose}</p>
                  </div>
                )}
                
                {userDetails.condition && (
                  <div className="mb-3">
                    <h6>Condition</h6>
                    <p>{userDetails.condition}</p>
                  </div>
                )}
                
                {userDetails.notes && (
                  <div className="mb-3">
                    <h6>Notes</h6>
                    <p>{userDetails.notes}</p>
                  </div>
                )}
                
                {userDetails.customFields && Object.keys(userDetails.customFields).length > 0 && (
                  <div className="mb-3">
                    <h6>Additional Information</h6>
                    {Object.entries(userDetails.customFields).map(([key, value]) => (
                      <div key={key} className="d-flex justify-content-between border-bottom py-2">
                        <span className="fw-semibold">{key}:</span>
                        <span>{value}</span>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            ) : (
              <Alert variant="info">
                <FaInfoCircle className="me-2" />
                User has chosen to keep their details private.
              </Alert>
            )}
          </div>
        ) : null}
      </Modal.Body>
      <Modal.Footer>
        <Button variant="secondary" onClick={onHide}>
          Close
        </Button>
      </Modal.Footer>
    </Modal>
  );
};

export default UserDetailsModal;