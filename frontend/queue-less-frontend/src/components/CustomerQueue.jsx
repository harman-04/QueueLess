//src/components/CustomerQueue.jsx
import React, { useState, useEffect } from "react";
import { useDispatch, useSelector } from "react-redux";
import { useParams, useNavigate } from "react-router-dom";
import axios from "axios";
import { toast } from "react-toastify";
import { 
  FaUserPlus, FaSpinner, FaHome, FaPauseCircle, FaUsers, 
  FaAmbulance, FaClock, FaUserCheck, FaCheckCircle, FaTimesCircle,
  FaArrowRight, FaExclamationCircle, FaSadCry, FaHandPointRight 
} from "react-icons/fa";
import { normalizeQueue } from "../utils/normalizeQueue";
import 'animate.css/animate.min.css';
import './CustomerQueue.css'; // New CSS file for custom styles

const API_BASE_URL = "http://localhost:8080/api/queues";

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
  const [groupMembers, setGroupMembers] = useState([{ name: "", details: "" }]);
  const [emergencyDetails, setEmergencyDetails] = useState("");
  const [userToken, setUserToken] = useState(null);
  const [refreshInterval, setRefreshInterval] = useState(null);
  const [isTokenExpired, setIsTokenExpired] = useState(false);

  // New state to manage the temporary display for expired tokens
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
          const userTokens = normalizedQueue.tokens.filter(
            token => token.userId === userId && 
            (token.status === 'WAITING' || token.status === 'IN_SERVICE')
          );
          
          if (userTokens.length > 0) {
            setUserToken(userTokens[0]);
            // If user token is now active, hide any expired messages
            setIsTokenExpired(false);
            setShowExpiredMessage(false);
          } else {
            // Check if user's token has just been completed/cancelled
            const previousTokenId = userToken?.tokenId;
            const completedToken = normalizedQueue.tokens.find(t => t.tokenId === previousTokenId && t.status === 'COMPLETED');
            if (completedToken) {
              setIsTokenExpired(true);
              // Show the temporary, beautiful message for a few seconds
              setShowExpiredMessage(true);
              setTimeout(() => {
                setShowExpiredMessage(false);
                setUserToken(null);
              }, 4000); // Display for 4 seconds
            } else {
              setUserToken(null);
            }
          }
        }
      } catch (error) {
        console.error("Failed to fetch queue:", error);
        toast.error("Failed to load queue details.");
      } finally {
        setLoading(false);
      }
    };

    fetchQueue();
    
    const interval = setInterval(fetchQueue, 10000);
    setRefreshInterval(interval);
    
    return () => {
      if (interval) clearInterval(interval);
    };
  }, [queueId, token, navigate, userId, userToken]);

  const handleAddToken = async (isGroup = false, isEmergency = false) => {
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
    const waitingTokens = queue.tokens.filter(t => t.status === 'WAITING' || t.status === 'IN_SERVICE');
    const userIndex = waitingTokens.findIndex(t => t.tokenId === userToken.tokenId);
    return userIndex >= 0 ? userIndex + 1 : null;
  };

  const position = calculatePosition();
  const currentServing = queue.tokens.find(t => t.status === 'IN_SERVICE');

  // New component for the temporary expired token message
  const ExpiredTokenMessage = () => (
    <div className="expired-message animate__animated animate__zoomIn">
      <div className="expired-content">
        <FaCheckCircle className="expired-icon" />
        <h2 className="expired-title">Your turn is completed! 🎉</h2>
        <p className="expired-text">Thank you for your visit. Hope we served you well.</p>
        <button 
          onClick={() => {
            setShowExpiredMessage(false);
            setUserToken(null);
          }}
          className="btn btn-primary mt-3"
        >
          <FaArrowRight /> Get a New Token
        </button>
      </div>
    </div>
  );

  return (
    <div className="customer-queue-container">
      {showExpiredMessage && <ExpiredTokenMessage />}

      <div className={`main-content ${showExpiredMessage ? 'blurred-content' : ''}`}>
        <div className="header-section animate__animated animate__fadeInDown">
          <h1 className="queue-title">
            {queue.serviceName} Queue
          </h1>
          <button onClick={() => navigate("/")} className="btn-back-home">
            <FaHome className="me-2" /> Back to Home
          </button>
        </div>

        {/* New Badges Container */}
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

        {/* User's Current Token Status Card */}
        {userToken && (
          <div className="status-card animate__animated animate__fadeIn">
            <div className="status-header">
              <FaHandPointRight className="status-icon" />
              <h4 className="status-title">Your Queue Status</h4>
            </div>
           
            <div className="status-body">
              <div className="d-flex justify-content-between align-items-center mb-3">
                <div>
                  <h5 className="mb-1">
                    Token: <span className="fw-bold text-primary">{userToken.tokenId}</span>
                  </h5>
                  <p className="mb-0 text-muted">
                    Status: 
                    <span className={`badge ms-2 ${
                      userToken.status === 'WAITING' ? 'bg-warning' :
                      userToken.status === 'IN_SERVICE' ? 'bg-success' : 'bg-secondary'
                    }`}>
                      {userToken.status === 'WAITING' ? 'Waiting' :
                       userToken.status === 'IN_SERVICE' ? 'In Service' : 'Completed'}
                    </span>
                  </p>
                </div>
               
                {userToken.status === 'WAITING' && (
                  <button 
                    onClick={handleCancelToken}
                    className="btn btn-outline-danger btn-sm"
                  >
                    <FaTimesCircle className="me-1" /> Cancel Token
                  </button>
                )}
              </div>
             
              {userToken.status === 'WAITING' && position && (
                <div className="alert alert-info">
                  <FaClock className="me-2" />
                  Your position in queue: <strong>{position}</strong> of {queue.tokens.filter(t => t.status === 'WAITING').length}
                  {queue.estimatedWaitTime > 0 && (
                    <span> • Estimated wait: ~{queue.estimatedWaitTime} minutes</span>
                  )}
                </div>
              )}
             
              {userToken.status === 'IN_SERVICE' && (
                <div className="alert alert-success animate__animated animate__pulse animate__infinite">
                  <FaUserCheck className="me-2" />
                  <strong>You're currently being served!</strong> Please proceed to the counter.
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
            </div>
          </div>
        )}

        {/* Current Serving Info Card */}
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

        {/* Join Queue Section - Only show if user doesn't have an active token */}
        {!userToken && !showExpiredMessage && (
          <div className="join-queue-card animate__animated animate__fadeIn">
            <div className="card-body text-center">
              <h4 className="fw-semibold text-secondary mb-3">Join this Queue</h4>
              <p className="text-muted">Current tokens in queue: {queue.tokens.filter(t => t.status === 'WAITING').length}</p>
              <p className="text-muted">Estimated wait time: {queue.estimatedWaitTime} minutes</p>
             
              {!queue.isActive && (
                <div className="alert alert-warning mb-3">
                  <FaPauseCircle className="me-2" /> This queue is currently paused by the provider.
                </div>
              )}
             
              <div className="d-flex flex-column gap-3 align-items-center">
                <button
                  onClick={() => handleAddToken(false, false)}
                  className="btn btn-primary btn-lg join-button"
                  disabled={addingToken || !queue.isActive}
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
               
                {queue.supportsGroupToken && (
                  <button
                    onClick={() => {
                      setShowGroupForm(!showGroupForm);
                      setShowEmergencyForm(false);
                    }}
                    className="btn btn-info btn-lg join-button"
                    disabled={addingToken || !queue.isActive}
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
                    disabled={addingToken || !queue.isActive}
                  >
                    <FaAmbulance className="me-2" />
                    {showEmergencyForm ? "Cancel Emergency" : "Emergency Token"}
                  </button>
                )}
              </div>
            </div>
          </div>
        )}

        {/* Group Token Form */}
        {showGroupForm && (
          <div className="form-card animate__animated animate__fadeIn">
            <div className="card-body">
              <h5 className="form-title">Group Token Details</h5>
              <p className="form-subtitle">Add all group members and their details.</p>
             
              {groupMembers.map((member, index) => (
                <div key={index} className="group-member-row mb-3">
                  <input
                    type="text"
                    className="form-control"
                    placeholder="Member Name"
                    value={member.name}
                    onChange={(e) => updateGroupMember(index, "name", e.target.value)}
                  />
                  <input
                    type="text"
                    className="form-control"
                    placeholder="Details (e.g., condition)"
                    value={member.details}
                    onChange={(e) => updateGroupMember(index, "details", e.target.value)}
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
                <button className="btn btn-outline-primary" onClick={addGroupMember}>
                  <FaUserPlus /> Add Member
                </button>
                <button
                  className="btn btn-success"
                  onClick={() => handleAddToken(true, false)}
                  disabled={addingToken || groupMembers.filter(m => m.name && m.details).length === 0}
                >
                  {addingToken ? <FaSpinner className="fa-spin me-2" /> : <FaCheckCircle className="me-2" />}
                  Submit Group Token
                </button>
              </div>
            </div>
          </div>
        )}

        {/* Emergency Token Form */}
        {showEmergencyForm && (
          <div className="form-card animate__animated animate__fadeIn">
            <div className="card-body">
              <h5 className="form-title">Emergency Details</h5>
              <p className="form-subtitle">Please describe the emergency situation.</p>
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
                  {addingToken ? <FaSpinner className="fa-spin me-2" /> : <FaCheckCircle className="me-2" />}
                  Submit Emergency Token
                </button>
              </div>
            </div>
          </div>
        )}

        {/* Queue Statistics */}
        <div className="stats-card animate__animated animate__fadeIn">
          <div className="card-body">
            <h5 className="fw-semibold text-secondary mb-3">Queue Information</h5>
            <div className="row text-center">
              <div className="col-md-4">
                <div className="stat-item">
                  <FaClock className="stat-icon text-primary" />
                  <div>
                    <h6 className="mb-0">Estimated Wait</h6>
                    <p className="mb-0 fw-bold">{queue.estimatedWaitTime} min</p>
                  </div>
                </div>
              </div>
              <div className="col-md-4">
                <div className="stat-item">
                  <FaUsers className="stat-icon text-info" />
                  <div>
                    <h6 className="mb-0">Waiting</h6>
                    <p className="mb-0 fw-bold">{queue.tokens.filter(t => t.status === 'WAITING').length} people</p>
                  </div>
                </div>
              </div>
              <div className="col-md-4">
                <div className="stat-item">
                  <FaUserCheck className="stat-icon text-success" />
                  <div>
                    <h6 className="mb-0">In Service</h6>
                    <p className="mb-0 fw-bold">{queue.tokens.filter(t => t.status === 'IN_SERVICE').length} person</p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default CustomerQueue;