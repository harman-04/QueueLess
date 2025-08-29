// src/components/ProviderQueueManagement.jsx
import React, { useState, useEffect } from "react";
import { useDispatch, useSelector } from "react-redux";
import { useNavigate } from "react-router-dom";
import axios from "axios";
import { logout } from "../redux/authSlice";
import { toast } from "react-toastify";
import {
  FaPlusCircle,
  FaListAlt,
  FaSignOutAlt,
  FaSpinner,
  FaTasks,
  FaPlayCircle,
  FaPauseCircle
} from "react-icons/fa";
import "animate.css";

const API_BASE_URL = "http://localhost:8080/api/queues";

// Add this utility function at the top of the file
const normalizeQueueData = (queueData) => {
  if (!queueData) return null;
  
  return {
    ...queueData,
    isActive: queueData.active !== undefined ? queueData.active : queueData.isActive
  };
};

const ProviderQueueManagement = () => {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const { id: providerId, token, name } = useSelector((state) => state.auth);
  const [queues, setQueues] = useState([]);
  const [newQueueName, setNewQueueName] = useState("");
  const [loading, setLoading] = useState(true);
  const [updatingStatus, setUpdatingStatus] = useState({});

  const fetchQueues = async () => {
    try {
      const response = await axios.get(
        `${API_BASE_URL}/by-provider/${providerId}`,
        { headers: { Authorization: `Bearer ${token}` } }
      );
      
      // Normalize the queue data to handle both 'active' and 'isActive' fields
      const normalizedQueues = response.data.map(queue => normalizeQueueData(queue));
      setQueues(normalizedQueues);
    } catch (error) {
      console.error("Failed to fetch queues:", error);
      toast.error("Failed to fetch your queues.");
      setQueues([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (providerId) {
      fetchQueues();
    }
  }, [providerId]);

  const handleCreateQueue = async (e) => {
    e.preventDefault();
    if (!newQueueName.trim()) {
      toast.warn("Please enter a service name.");
      return;
    }
    try {
      const response = await axios.post(
        `${API_BASE_URL}/create`,
        { providerId, serviceName: newQueueName },
        { headers: { Authorization: `Bearer ${token}` } }
      );
      
      // Normalize the new queue data
      const normalizedQueue = normalizeQueueData(response.data);
      
      toast.success(`${normalizedQueue.serviceName} queue created!`);
      setNewQueueName("");
      
      // Add the new queue to the list
      setQueues(prevQueues => [...prevQueues, normalizedQueue]);
    } catch (error) {
      toast.error("Failed to create queue.");
      console.error("Error creating queue:", error);
    }
  };

  const handleToggleQueueStatus = async (queueId, currentStatus) => {
    setUpdatingStatus(prev => ({ ...prev, [queueId]: true }));
    try {
      const endpoint = currentStatus 
        ? `${API_BASE_URL}/${queueId}/deactivate`
        : `${API_BASE_URL}/${queueId}/activate`;
      
      const response = await axios.put(endpoint, {}, { 
        headers: { Authorization: `Bearer ${token}` } 
      });
      
      // Normalize the updated queue data
      const updatedQueue = normalizeQueueData(response.data);
      
      // Update the specific queue in the state
      setQueues(prevQueues => 
        prevQueues.map(queue => 
          queue.id === queueId ? updatedQueue : queue
        )
      );
      
      toast.success(`Queue ${currentStatus ? 'paused' : 'resumed'} successfully!`);
    } catch (error) {
      console.error("Error updating queue status:", error);
      if (error.response?.status === 500) {
        toast.error("Server error. Please try again later.");
      } else {
        toast.error("Failed to update queue status.");
      }
    } finally {
      setUpdatingStatus(prev => ({ ...prev, [queueId]: false }));
    }
  };

  const handleLogout = () => {
    dispatch(logout());
    navigate("/login");
    toast.info("You have been logged out.");
  };

  const handleManageQueue = (queueId) => {
    navigate(`/provider/dashboard/${queueId}`);
  };

  if (loading) {
    return (
      <div className="d-flex justify-content-center align-items-center vh-100 text-primary animate__animated animate__fadeIn">
        <FaSpinner className="fa-spin me-2" /> Loading your queues...
      </div>
    );
  }

  return (
    <div className="container py-5 animate__animated animate__fadeIn">
      <div className="d-flex justify-content-between align-items-center mb-5 animate__animated animate__fadeInDown">
        <h1 className="fw-bold text-dark">
          <FaListAlt className="me-2 text-primary" />
          {name ? `${name}'s Queues` : "My Queues"}
        </h1>
        <button
          onClick={handleLogout}
          className="btn btn-outline-danger d-flex align-items-center"
        >
          <FaSignOutAlt className="me-2" /> Logout
        </button>
      </div>

      <div className="card shadow-lg border-0 mb-5 animate__animated animate__fadeInUp animate__faster">
        <div className="card-body p-4">
          <h4 className="fw-semibold text-secondary mb-4">
            <FaPlusCircle className="me-2 text-success" />
            Create a New Queue
          </h4>
          <form
            onSubmit={handleCreateQueue}
            className="row g-3 align-items-center"
          >
            <div className="col-md-9">
              <input
                type="text"
                className="form-control form-control-lg"
                value={newQueueName}
                onChange={(e) => setNewQueueName(e.target.value)}
                placeholder="Enter service name (e.g., 'Haircut', 'Car Wash')"
                required
              />
            </div>
            <div className="col-md-3 d-grid">
              <button type="submit" className="btn btn-primary btn-lg shadow-sm">
                Create Queue
              </button>
            </div>
          </form>
        </div>
      </div>

      <div className="card shadow-lg border-0 animate__animated animate__fadeInUp">
        <div className="card-body p-4">
          <h4 className="fw-semibold text-secondary mb-4">
            <FaTasks className="me-2 text-info" /> Your Queues
          </h4>

          {queues.length > 0 ? (
            <div className="row g-4">
              {queues.map((queue, index) => (
                <div
                  className="col-md-6 animate__animated animate__fadeInUp"
                  style={{ animationDelay: `${index * 0.2}s` }}
                  key={queue.id}
                >
                  <div className="card border-0 shadow-sm h-100 queue-card transition-all hover-shadow">
                    <div className="card-body d-flex flex-column justify-content-between">
                      <div>
                        <div className="d-flex justify-content-between align-items-start mb-2">
                          <h5 className="card-title text-dark fw-bold">
                            {queue.serviceName}
                          </h5>
                          <span className={`badge ${queue.isActive ? 'bg-success' : 'bg-warning'}`}>
                            {queue.isActive ? 'Active' : 'Paused'}
                          </span>
                        </div>
                        <p className="card-text text-muted mb-1">
                          <strong>Queue ID:</strong> {queue.id}
                        </p>
                        <p className="card-text text-muted">
                          <strong>Current Tokens:</strong>{" "}
                          {queue.tokens.length}
                        </p>
                      </div>
                      <div className="mt-3 d-grid gap-2">
                        <button
                          onClick={() => handleManageQueue(queue.id)}
                          className="btn btn-outline-primary btn-sm transition-all"
                        >
                          Manage Queue
                        </button>
                        <button
                          onClick={() => handleToggleQueueStatus(queue.id, queue.isActive)}
                          className={`btn btn-sm ${queue.isActive ? 'btn-warning' : 'btn-success'}`}
                          disabled={updatingStatus[queue.id]}
                        >
                          {updatingStatus[queue.id] ? (
                            <FaSpinner className="fa-spin me-1" />
                          ) : queue.isActive ? (
                            <FaPauseCircle className="me-1" />
                          ) : (
                            <FaPlayCircle className="me-1" />
                          )}
                          {queue.isActive ? 'Pause Queue' : 'Resume Queue'}
                        </button>
                      </div>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="alert alert-info text-center mt-3 animate__animated animate__fadeIn">
              You don't have any queues yet. Create one to get started!
            </div>
          )}
        </div>
      </div>

      <style>{`
        .queue-card {
          transition: transform 0.3s ease, box-shadow 0.3s ease;
        }
        .queue-card:hover {
          transform: translateY(-5px);
          box-shadow: 0px 6px 18px rgba(0, 0, 0, 0.15);
        }
        .transition-all {
          transition: all 0.3s ease;
        }
        .hover-shadow:hover {
          box-shadow: 0px 8px 20px rgba(0,0,0,0.2) !important;
        }
      `}</style>
    </div>
  );
};

export default ProviderQueueManagement;