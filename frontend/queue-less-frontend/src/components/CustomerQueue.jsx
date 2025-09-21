import React, { useState, useEffect } from "react";
import { useDispatch, useSelector } from "react-redux";
import { useParams, useNavigate } from "react-router-dom";
import axios from "axios";
import { toast } from "react-toastify";
import {
  FaUserPlus,
  FaSpinner,
  FaHome,
  FaPauseCircle,
  FaUsers,
  FaAmbulance,
  FaClock,
  FaUserCheck,
  FaCheckCircle,
  FaTimesCircle,
  FaArrowRight,
  FaExclamationCircle,
  FaHandPointRight,
  FaInfoCircle,
  FaFileMedical
} from "react-icons/fa";
import { Modal, Button, Form } from "react-bootstrap";
import { normalizeQueue } from "../utils/normalizeQueue";
import "animate.css/animate.min.css";
import "./CustomerQueue.css";
import FeedbackPrompt from "./FeedbackPrompt";
import UserQueueRestriction from "./UserQueueRestriction";

const API_BASE_URL = "https://localhost:8443/api/queues";

const CustomerQueue = () => {
  const { queueId } = useParams();
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const { id: userId, token, role, name } = useSelector((state) => state.auth);

  const [queue, setQueue] = useState(null);
  const [loading, setLoading] = useState(true);
  const [addingToken, setAddingToken] = useState(false);
  const [showGroupForm, setShowGroupForm] = useState(false);
  const [showEmergencyForm, setShowEmergencyForm] = useState(false);
  const [showDetailsForm, setShowDetailsForm] = useState(false);
  const [groupMembers, setGroupMembers] = useState([{ name: "", details: "" }]);
  const [emergencyDetails, setEmergencyDetails] = useState("");
  const [userDetails, setUserDetails] = useState({
    purpose: "",
    condition: "",
    notes: "",
    isPrivate: false,
    visibleToProvider: true,
    visibleToAdmin: true
  });
  const [userToken, setUserToken] = useState(null);
  const [refreshInterval, setRefreshInterval] = useState(null);
  const [isTokenExpired, setIsTokenExpired] = useState(false);
  const [showFeedback, setShowFeedback] = useState(false);
  const [canJoinQueue, setCanJoinQueue] = useState(true);
  const [showExpiredMessage, setShowExpiredMessage] = useState(false);

  useEffect(() => {
    if (!queueId) {
      navigate("/");
      return;
    }

    const fetchQueue = async () => {
      try {
        const response = await axios.get(`${API_BASE_URL}/${queueId}`, {
          headers: { Authorization: `Bearer ${token}` },
        });

        const normalizedQueue = normalizeQueue(response.data);
        setQueue(normalizedQueue);

        if (userId) {
          const userActiveToken = normalizedQueue.tokens.find(
            (t) =>
              t.userId === userId &&
              (t.status === "WAITING" || t.status === "IN_SERVICE")
          );

          const completedToken = normalizedQueue.tokens.find(
            (t) =>
              t.userId === userId &&
              t.tokenId === userToken?.tokenId &&
              t.status === "COMPLETED"
          );

          if (userActiveToken) {
            setUserToken(userActiveToken);
            setIsTokenExpired(false);
            setShowExpiredMessage(false);
          } else if (completedToken) {
            setUserToken(completedToken);
            setIsTokenExpired(true);
            setShowExpiredMessage(false);
          } else {
            setUserToken(null);
            setIsTokenExpired(false);
            setShowExpiredMessage(false);
          }
        } else {
          setUserToken(null);
        }
      } catch (error) {
        console.error("Failed to fetch queue:", error);
        toast.error("Failed to load queue details.");
      } finally {
        setLoading(false);
      }
    };

    fetchQueue();

    const interval = setInterval(fetchQueue, 5000);
    setRefreshInterval(interval);

    return () => {
      if (interval) clearInterval(interval);
    };
  }, [queueId, token, navigate, userId, userToken?.tokenId]);

  useEffect(() => {
  const checkFeedbackEligibility = async () => {
    
    console.log("Checking feedback eligibility for:", userToken);
      // Read the dismissed tokens from local storage
    const dismissedTokens = JSON.parse(localStorage.getItem('dismissedFeedbackPrompts') || '[]');
    const isDismissed = dismissedTokens.includes(userToken?.tokenId);

    if (userToken?.status === "COMPLETED" && !isDismissed) {
      try {
        const response = await axios.get(
          `https://localhost:8443/api/feedback/token/${userToken.tokenId}`,
          { headers: { Authorization: `Bearer ${token}` } }
        );
        console.log("Feedback already provided:", response.data);
        setShowFeedback(false);
      } catch (error) {
        if (error.response?.status === 404) {
          console.log("Feedback not provided yet. Showing prompt.");
          setShowFeedback(true);
        } else {
          console.error("Error checking feedback eligibility:", error);
          setShowFeedback(true);
        }
      }
    } else {
      setShowFeedback(false);
    }
  };
  checkFeedbackEligibility();
}, [userToken, token]);

  const handleAddToken = async (isGroup = false, isEmergency = false, withDetails = false) => {
    if (!userId || !token) {
      toast.error("You must be logged in to join a queue.");
      navigate("/login");
      return;
    }

    if (!queue.isActive) {
      toast.error("This queue is currently paused by the provider.");
      return;
    }

    if (isEmergency && !queue.emergencySupport) {
      toast.error("This queue does not support emergency tokens.");
      return;
    }

    if (isGroup && !queue.supportsGroupToken) {
      toast.error("This queue does not support group tokens.");
      return;
    }

    if (withDetails) {
      setShowDetailsForm(true);
      return;
    }

    setAddingToken(true);
    try {
      let endpoint = `${API_BASE_URL}/${queueId}/add-token`;
      let requestData = null;

      if (isGroup) {
        endpoint = `${API_BASE_URL}/${queueId}/add-group-token`;
        requestData = { 
          groupMembers: groupMembers.filter(m => m.name && m.details) 
        };
      } else if (isEmergency) {
        endpoint = `${API_BASE_URL}/${queueId}/add-emergency-token`;
        requestData = { emergencyDetails };
      }

      const response = await axios.post(
        endpoint,
        requestData,
        { headers: { Authorization: `Bearer ${token}` } }
      );
      
      const newTokenId = response.data.tokenId;
      const tokenType = isGroup ? "group" : isEmergency ? "emergency" : "regular";
      
      toast.success(`You have joined the queue! Your ${tokenType} token is ${newTokenId}.`);
      
      setUserToken(response.data);
      
      setShowGroupForm(false);
      setShowEmergencyForm(false);
      setGroupMembers([{ name: "", details: "" }]);
      setEmergencyDetails("");
      
      const queueResponse = await axios.get(`${API_BASE_URL}/${queueId}`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      setQueue(normalizeQueue(queueResponse.data));
    } catch (error) {
      console.error("Error adding token:", error);
      if (error.response?.status === 409) {
        toast.error(error.response.data.message || "You already have an active token in this queue.");
      } else if (error.response?.status === 400) {
        toast.error(error.response.data.message || "Invalid request.");
      } else if (error.response?.status === 403) {
        toast.error("You don't have permission to perform this action.");
      } else {
        toast.error("Failed to add token to the queue.");
      }
    } finally {
      setAddingToken(false);
    }
  };

  const handleAddTokenWithDetails = async () => {
    setAddingToken(true);
    try {
      const response = await axios.post(
        `${API_BASE_URL}/${queueId}/add-token-with-details`,
        userDetails,
        { headers: { Authorization: `Bearer ${token}` } }
      );
      
      toast.success(`You have joined the queue! Your token is ${response.data.tokenId}.`);
      
      setUserToken(response.data);
      setShowDetailsForm(false);
      setUserDetails({
        purpose: "",
        condition: "",
        notes: "",
        isPrivate: false,
        visibleToProvider: true,
        visibleToAdmin: true
      });
      
      const queueResponse = await axios.get(`${API_BASE_URL}/${queueId}`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      setQueue(normalizeQueue(queueResponse.data));
    } catch (error) {
      console.error("Error adding token with details:", error);
      if (error.response?.status === 409) {
        toast.error(error.response.data.message || "You already have an active token in this queue.");
      } else if (error.response?.status === 400) {
        toast.error(error.response.data.message || "Invalid request.");
      } else if (error.response?.status === 403) {
        toast.error("You don't have permission to perform this action.");
      } else {
        toast.error("Failed to add token to the queue.");
      }
    } finally {
      setAddingToken(false);
    }
  };

  const addGroupMember = () => {
    setGroupMembers([...groupMembers, { name: "", details: "" }]);
  };

  const removeGroupMember = (index) => {
    if (groupMembers.length <= 1) return;
    const updatedMembers = [...groupMembers];
    updatedMembers.splice(index, 1);
    setGroupMembers(updatedMembers);
  };

  const updateGroupMember = (index, field, value) => {
    const updatedMembers = [...groupMembers];
    updatedMembers[index][field] = value;
    setGroupMembers(updatedMembers);
  };

  const handleCancelToken = async () => {
    if (!userToken) return;

    try {
      await axios.delete(
        `${API_BASE_URL}/${queueId}/cancel-token/${userToken.tokenId}`,
        { headers: { Authorization: `Bearer ${token}` } }
      );

      toast.info(`Token ${userToken.tokenId} has been canceled.`);
      setUserToken(null);
      
      const response = await axios.get(`${API_BASE_URL}/${queueId}`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      setQueue(normalizeQueue(response.data));
    } catch (error) {
      console.error("Error canceling token:", error);
      toast.error("Failed to cancel token.");
    }
  };

  const handleFeedbackDismissed = () => {
    setShowFeedback(false);
    setShowExpiredMessage(true);
  };

   // ADD THIS NEW HANDLER
  const handleFeedbackPromptClosed = () => {
      setShowFeedback(false); // This is the crucial line to remove the blur
  };

  const handleExpiredMessageDismissed = () => {
    setShowExpiredMessage(false);
    setUserToken(null);
    setIsTokenExpired(false);
  };

  if (loading) {
    return (
      <div className="loading-screen animate__animated animate__fadeIn">
        <FaSpinner className="fa-spin spinner-icon" />
        <h3 className="loading-text">Loading queue...</h3>
      </div>
    );
  }

  if (!queue) {
    return (
      <div className="error-screen animate__animated animate__fadeIn">
        <FaExclamationCircle className="error-icon" />
        <h2 className="error-title">Queue not found.</h2>
        <button onClick={() => navigate("/")} className="btn btn-primary mt-4">
          <FaHome className="me-2" /> Go to Home
        </button>
      </div>
    );
  }

  const calculatePosition = () => {
    if (!userToken) return null;
    const waitingTokens = queue.tokens.filter(
      (t) => t.status === "WAITING" || t.status === "IN_SERVICE"
    );
    const userIndex = waitingTokens.findIndex(
      (t) => t.tokenId === userToken.tokenId
    );
    return userIndex >= 0 ? userIndex + 1 : null;
  };

  const position = calculatePosition();
  const currentServing = queue.tokens.find((t) => t.status === "IN_SERVICE");

  const ExpiredTokenMessage = () => (
    <div className="expired-message animate__animated animate__zoomIn">
      <div className="expired-content">
        <FaCheckCircle className="expired-icon" />
        <h2 className="expired-title">Your turn is completed! 🎉</h2>
        <p className="expired-text">Thank you for your visit. Hope we served you well.</p>
        <button onClick={handleExpiredMessageDismissed} className="btn btn-primary mt-3">
          <FaArrowRight /> Get a New Token
        </button>
      </div>
    </div>
  );

  return (
    <div className="customer-queue-container">
      {showExpiredMessage && !showFeedback && <ExpiredTokenMessage />}

      <div
        className={`main-content ${
          showExpiredMessage || showFeedback ? "blurred-content" : ""
        }`}
      >
        <div className="header-section animate__animated animate__fadeInDown">
          <h1 className="queue-title">{queue.serviceName} Queue</h1>
          <button onClick={() => navigate("/")} className="btn-back-home">
            <FaHome className="me-2" /> Back to Home
          </button>
        </div>

        <div className="badges-container mb-4 animate__animated animate__fadeIn">
          {!queue.isActive && (
            <span className="badge badge-paused">
              <FaPauseCircle className="me-1" /> Paused
            </span>
          )}
          {queue.supportsGroupToken && (
            <span className="badge badge-info">
              <FaUsers className="me-1" /> Group Tokens
            </span>
          )}
          {queue.emergencySupport && (
            <span className="badge badge-danger">
              <FaAmbulance className="me-1" /> Emergency Support
            </span>
          )}
        </div>

        {userToken && userToken.status !== 'COMPLETED' && (
          <div className="status-card animate__animated animate__fadeIn">
            <div className="status-header">
              <FaHandPointRight className="status-icon" />
              <h4 className="status-title">Your Queue Status</h4>
            </div>

            <div className="status-body">
              <div className="d-flex justify-content-between align-items-center mb-3">
                <div>
                  <h5 className="mb-1">
                    Token:{" "}
                    <span className="fw-bold text-primary">
                      {userToken.tokenId}
                    </span>
                  </h5>
                  <p className="mb-0 text-muted">
                    Status:
                    <span
                      className={`badge ms-2 ${
                        userToken.status === "WAITING"
                          ? "bg-warning"
                          : userToken.status === "IN_SERVICE"
                          ? "bg-success"
                          : "bg-secondary"
                      }`}
                    >
                      {userToken.status === "WAITING"
                        ? "Waiting"
                        : userToken.status === "IN_SERVICE"
                        ? "In Service"
                        : "Completed"}
                    </span>
                  </p>
                </div>

                {userToken.status === "WAITING" && (
                  <button
                    onClick={handleCancelToken}
                    className="btn btn-outline-danger btn-sm"
                  >
                    <FaTimesCircle className="me-1" /> Cancel Token
                  </button>
                )}
              </div>

              {userToken.status === "WAITING" && position && (
                <div className="alert alert-info">
                  <FaClock className="me-2" />
                  Your position in queue: <strong>{position}</strong> of{" "}
                  {
                    queue.tokens.filter((t) => t.status === "WAITING")
                      .length
                  }
                  {queue.estimatedWaitTime > 0 && (
                    <span>
                      {" "}
                      • Estimated wait: ~{queue.estimatedWaitTime} minutes
                    </span>
                  )}
                </div>
              )}

              {userToken.status === "IN_SERVICE" && (
                <div className="alert alert-success animate__animated animate__pulse animate__infinite">
                  <FaUserCheck className="me-2" />
                  <strong>You're currently being served!</strong> Please proceed
                  to the counter.
                </div>
              )}

              {userToken.isGroup && userToken.groupMembers && (
                <div className="mt-3">
                  <h6>Group Members:</h6>
                  <ul className="list-group">
                    {userToken.groupMembers.map((member, index) => (
                      <li key={index} className="list-group-item">
                        <strong>{member.name}</strong>: {member.details}
                      </li>
                    ))}
                  </ul>
                </div>
              )}

              {userToken.isEmergency && userToken.emergencyDetails && (
                <div className="mt-3">
                  <h6>Emergency Details:</h6>
                  <p className="text-muted">{userToken.emergencyDetails}</p>
                </div>
              )}

              {userToken.userDetails && (
                <div className="mt-3">
                  <h6>Your Details:</h6>
                  <div className="card">
                    <div className="card-body">
                      {userToken.userDetails.purpose && (
                        <p><strong>Purpose:</strong> {userToken.userDetails.purpose}</p>
                      )}
                      {userToken.userDetails.condition && (
                        <p><strong>Condition:</strong> {userToken.userDetails.condition}</p>
                      )}
                      {userToken.userDetails.notes && (
                        <p><strong>Notes:</strong> {userToken.userDetails.notes}</p>
                      )}
                      <small className="text-muted">
                        Privacy: {userToken.userDetails.isPrivate ? "Private" : "Visible to provider"}
                      </small>
                    </div>
                  </div>
                </div>
              )}
            </div>
          </div>
        )}

        {currentServing && (
          <div className="serving-card animate__animated animate__fadeIn">
            <div className="card-body text-center">
              <h4 className="fw-semibold text-secondary mb-3">Now Serving</h4>
              <div className="display-4 fw-bold text-primary mb-2 serving-token-id">
                {currentServing.tokenId}
              </div>
              <p className="text-muted">Please wait for your turn</p>
            </div>
          </div>
        )}

        {!userToken && (
          <div className="join-queue-card animate__animated animate__fadeIn">
            <div className="card-body text-center">
              <h4 className="fw-semibold text-secondary mb-3">
                Join this Queue
              </h4>

              <UserQueueRestriction
                onRestrictionCheck={(restriction) =>
                  setCanJoinQueue(restriction.canJoinQueue)
                }
              />

              <p className="text-muted">
                Current tokens in queue:{" "}
                {queue.tokens.filter((t) => t.status === "WAITING").length}
              </p>
              <p className="text-muted">
                Estimated wait time: {queue.estimatedWaitTime} minutes
              </p>

              {!queue.isActive && (
                <div className="alert alert-warning mb-3">
                  <FaPauseCircle className="me-2" /> This queue is currently
                  paused by the provider.
                </div>
              )}

              <div className="d-flex flex-column gap-3 align-items-center">
                <button
                  onClick={() => handleAddToken(false, false)}
                  className="btn btn-primary btn-lg join-button"
                  disabled={addingToken || !queue.isActive || !canJoinQueue}
                >
                  {addingToken ? (
                    <>
                      <FaSpinner className="fa-spin me-2" /> Adding...
                    </>
                  ) : (
                    <>
                      <FaUserPlus className="me-2" /> Get a Regular Token
                    </>
                  )}
                </button>

                <button
                  onClick={() => handleAddToken(false, false, true)}
                  className="btn btn-secondary btn-lg join-button"
                  disabled={addingToken || !queue.isActive || !canJoinQueue}
                >
                  {addingToken ? (
                    <>
                      <FaSpinner className="fa-spin me-2" /> Adding...
                    </>
                  ) : (
                    <>
                      <FaFileMedical className="me-2" /> Get Token with Details
                    </>
                  )}
                </button>

                {queue.supportsGroupToken && (
                  <button
                    onClick={() => {
                      setShowGroupForm(!showGroupForm);
                      setShowEmergencyForm(false);
                    }}
                    className="btn btn-info btn-lg join-button"
                    disabled={addingToken || !queue.isActive || !canJoinQueue}
                  >
                    <FaUsers className="me-2" />
                    {showGroupForm ? "Cancel Group" : "Get a Group Token"}
                  </button>
                )}

                {queue.emergencySupport && (
                  <button
                    onClick={() => {
                      setShowEmergencyForm(!showEmergencyForm);
                      setShowGroupForm(false);
                    }}
                    className="btn btn-danger btn-lg join-button"
                    disabled={addingToken || !queue.isActive || !canJoinQueue}
                  >
                    <FaAmbulance className="me-2" />
                    {showEmergencyForm ? "Cancel Emergency" : "Emergency Token"}
                  </button>
                )}
              </div>
            </div>
          </div>
        )}

        {showGroupForm && (
          <div className="form-card animate__animated animate__fadeIn">
            <div className="card-body">
              <h5 className="form-title">Group Token Details</h5>
              <p className="form-subtitle">
                Add all group members and their details.
              </p>

              {groupMembers.map((member, index) => (
                <div key={index} className="group-member-row mb-3">
                  <input
                    type="text"
                    className="form-control"
                    placeholder="Member Name"
                    value={member.name}
                    onChange={(e) =>
                      updateGroupMember(index, "name", e.target.value)
                    }
                  />
                  <input
                    type="text"
                    className="form-control"
                    placeholder="Details (e.g., condition)"
                    value={member.details}
                    onChange={(e) =>
                      updateGroupMember(index, "details", e.target.value)
                    }
                  />
                  <button
                    className="btn btn-outline-danger"
                    onClick={() => removeGroupMember(index)}
                    disabled={groupMembers.length <= 1}
                  >
                    <FaTimesCircle />
                  </button>
                </div>
              ))}

              <div className="d-flex justify-content-between mt-3">
                <button
                  className="btn btn-outline-primary"
                  onClick={addGroupMember}
                >
                  <FaUserPlus /> Add Member
                </button>
                <button
                  className="btn btn-success"
                  onClick={() => handleAddToken(true, false)}
                  disabled={
                    addingToken ||
                    groupMembers.filter((m) => m.name && m.details).length === 0
                  }
                >
                  {addingToken ? (
                    <FaSpinner className="fa-spin me-2" />
                  ) : (
                    <FaCheckCircle className="me-2" />
                  )}
                  Submit Group Token
                </button>
              </div>
            </div>
          </div>
        )}

        {showEmergencyForm && (
          <div className="form-card animate__animated animate__fadeIn">
            <div className="card-body">
              <h5 className="form-title">Emergency Details</h5>
              <p className="form-subtitle">
                Please describe the emergency situation.
              </p>
              <div className="mb-3">
                <textarea
                  className="form-control"
                  rows="3"
                  placeholder="Describe the emergency..."
                  value={emergencyDetails}
                  onChange={(e) => setEmergencyDetails(e.target.value)}
                />
              </div>
              <div className="d-flex justify-content-end">
                <button
                  className="btn btn-success"
                  onClick={() => handleAddToken(false, true)}
                  disabled={addingToken || !emergencyDetails.trim()}
                >
                  {addingToken ? (
                    <FaSpinner className="fa-spin me-2" />
                  ) : (
                    <FaCheckCircle className="me-2" />
                  )}
                  Submit Emergency Token
                </button>
              </div>
            </div>
          </div>
        )}

        <div className="stats-card animate__animated animate__fadeIn">
          <div className="card-body">
            <h5 className="fw-semibold text-secondary mb-3">
              Queue Information
            </h5>
            <div className="row text-center">
              <div className="col-md-4">
                <div className="stat-item">
                  <FaClock className="stat-icon text-primary" />
                  <div>
                    <h6 className="mb-0">Estimated Wait</h6>
                    <p className="mb-0 fw-bold">
                      {queue.estimatedWaitTime} min
                    </p>
                  </div>
                </div>
              </div>
              <div className="col-md-4">
                <div className="stat-item">
                  <FaUsers className="stat-icon text-info" />
                  <div>
                    <h6 className="mb-0">Waiting</h6>
                    <p className="mb-0 fw-bold">
                      {
                        queue.tokens.filter((t) => t.status === "WAITING")
                          .length
                      }{" "}
                      people
                    </p>
                  </div>
                </div>
              </div>
              <div className="col-md-4">
                <div className="stat-item">
                  <FaUserCheck className="stat-icon text-success" />
                  <div>
                    <h6 className="mb-0">In Service</h6>
                    <p className="mb-0 fw-bold">
                      {
                        queue.tokens.filter((t) => t.status === "IN_SERVICE")
                          .length
                      }{" "}
                      person
                    </p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* User Details Modal */}
      <Modal show={showDetailsForm} onHide={() => setShowDetailsForm(false)} size="lg">
        <Modal.Header closeButton>
          <Modal.Title>
            <FaFileMedical className="me-2" />
            Provide Your Details
          </Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <Form>
            <Form.Group className="mb-3">
              <Form.Label>Purpose *</Form.Label>
              <Form.Control
                type="text"
                placeholder="Why are you joining this queue?"
                value={userDetails.purpose}
                onChange={(e) => setUserDetails({...userDetails, purpose: e.target.value})}
                required
              />
            </Form.Group>

            <Form.Group className="mb-3">
              <Form.Label>Condition (if applicable)</Form.Label>
              <Form.Control
                type="text"
                placeholder="Medical condition or special circumstances"
                value={userDetails.condition}
                onChange={(e) => setUserDetails({...userDetails, condition: e.target.value})}
              />
            </Form.Group>

            <Form.Group className="mb-3">
              <Form.Label>Additional Notes</Form.Label>
              <Form.Control
                as="textarea"
                rows={3}
                placeholder="Any additional information the provider should know"
                value={userDetails.notes}
                onChange={(e) => setUserDetails({...userDetails, notes: e.target.value})}
              />
            </Form.Group>

            <Form.Group className="mb-3">
              <Form.Check
                type="checkbox"
                label="Keep my details private"
                checked={userDetails.isPrivate}
                onChange={(e) => setUserDetails({...userDetails, isPrivate: e.target.checked})}
              />
              <Form.Text className="text-muted">
                When checked, your details will only be visible to administrators
              </Form.Text>
            </Form.Group>

            {!userDetails.isPrivate && (
              <>
                <Form.Group className="mb-3">
                  <Form.Check
                    type="checkbox"
                    label="Visible to provider"
                    checked={userDetails.visibleToProvider}
                    onChange={(e) => setUserDetails({...userDetails, visibleToProvider: e.target.checked})}
                  />
                </Form.Group>
                <Form.Group className="mb-3">
                  <Form.Check
                    type="checkbox"
                    label="Visible to administrators"
                    checked={userDetails.visibleToAdmin}
                    onChange={(e) => setUserDetails({...userDetails, visibleToAdmin: e.target.checked})}
                  />
                </Form.Group>
              </>
            )}
          </Form>
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={() => setShowDetailsForm(false)}>
            Cancel
          </Button>
          <Button 
            variant="primary" 
            onClick={handleAddTokenWithDetails}
            disabled={addingToken || !userDetails.purpose.trim()}
          >
            {addingToken ? (
              <>
                <FaSpinner className="fa-spin me-2" /> Adding...
              </>
            ) : (
              <>
                <FaUserPlus className="me-2" /> Join Queue
              </>
            )}
          </Button>
        </Modal.Footer>
      </Modal>

      {showFeedback && userToken?.status === "COMPLETED" && (
        <FeedbackPrompt
          queueId={queueId}
          tokenId={userToken.tokenId}
          onFeedbackSubmitted={handleFeedbackDismissed}
           onClose={handleFeedbackPromptClosed}
        />
      )}
    </div>
  );
};

export default CustomerQueue;