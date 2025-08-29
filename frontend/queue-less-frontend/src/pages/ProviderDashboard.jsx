// src/components/ProviderDashboard.jsx
import React, { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { useParams, useNavigate } from "react-router-dom";
import QueueList from "../components/QueueList";
import { logout } from "../redux/authSlice";
import { toast } from "react-toastify";
import "animate.css/animate.min.css";
import {
  FaTasks,
  FaSpinner,
  FaSignOutAlt,
  FaListAlt,
  FaPlayCircle,
  FaPauseCircle
} from "react-icons/fa";
import {
  connectWebSocket,
  disconnectWebSocket,
  sendWebSocketMessage
} from "../redux/websocketActions";
import axios from "axios";
import { debounce } from "../utils/debounce";

const API_BASE_URL = "http://localhost:8080/api/queues";

const ProviderDashboard = () => {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const { queueId } = useParams();

  const { data: queueData, connected, error } = useSelector(
    (state) => state.queue
  );
  const { token, role, name } = useSelector((state) => state.auth);
  const [updatingStatus, setUpdatingStatus] = useState(false);
  const [localQueueData, setLocalQueueData] = useState(null);

  // Update local state when Redux state changes
  useEffect(() => {
    if (queueData) {
      // Normalize the queue data to handle both 'active' and 'isActive' fields
      const normalizedQueue = {
        ...queueData,
        isActive: queueData.active !== undefined ? queueData.active : queueData.isActive
      };
      setLocalQueueData(normalizedQueue);
    }
  }, [queueData]);

  useEffect(() => {
    if (token && role === "PROVIDER" && queueId) {
      dispatch(connectWebSocket(queueId));

      // Fetch initial queue data
      const fetchInitialQueueData = async () => {
        try {
          const response = await axios.get(`${API_BASE_URL}/${queueId}`, {
            headers: { Authorization: `Bearer ${token}` },
          });
          // Normalize the queue data
          const normalizedQueue = {
            ...response.data,
            isActive: response.data.active !== undefined ? response.data.active : response.data.isActive
          };
          setLocalQueueData(normalizedQueue);
        } catch (err) {
          console.error("Failed to fetch initial queue data:", err);
        }
      };
      
      fetchInitialQueueData();
    }

    return () => {
      if (token && role === "PROVIDER") {
        dispatch(disconnectWebSocket());
      }
    };
  }, [dispatch, token, role, queueId]);

  // Debounced toggle function
  const debouncedToggleStatus = debounce(async () => {
    if (!localQueueData) return;
    
    setUpdatingStatus(true);
    try {
      const endpoint = localQueueData.isActive 
        ? `${API_BASE_URL}/${queueId}/deactivate`
        : `${API_BASE_URL}/${queueId}/activate`;
      
      const response = await axios.put(endpoint, {}, { 
        headers: { Authorization: `Bearer ${token}` } 
      });
      
      // Normalize the response data
      const updatedQueue = {
        ...response.data,
        isActive: response.data.active !== undefined ? response.data.active : response.data.isActive
      };
      
      // Update local state immediately for better UX
      setLocalQueueData(updatedQueue);
      
      toast.success(`Queue ${localQueueData.isActive ? 'paused' : 'resumed'} successfully!`);
    } catch (error) {
      console.error("Error updating queue status:", error);
      if (error.response?.status === 500) {
        toast.error("Server error. Please try again later.");
      } else {
        toast.error("Failed to update queue status.");
      }
    } finally {
      setUpdatingStatus(false);
    }
  }, 500);

  const handleToggleQueueStatus = () => {
    debouncedToggleStatus();
  };

  const handleServeNext = () => {
    dispatch(sendWebSocketMessage('/app/queue/serve-next', { queueId: queueId }));
  };

  const handleLogout = () => {
    dispatch(logout());
    navigate("/login");
    toast.info("You have been logged out.");
  };

  if (!token) {
    return (
      <div className="d-flex justify-content-center align-items-center vh-100 text-primary">
        Please log in to view the dashboard.
      </div>
    );
  }

  if (role !== "PROVIDER") {
    return (
      <div className="d-flex justify-content-center align-items-center vh-100 text-danger">
        Access Denied. You must be logged in as a provider.
      </div>
    );
  }

  return (
    <div className="container py-5">
      {/* Header */}
      <div className="d-flex justify-content-between align-items-center mb-5 animate__animated animate__fadeInDown">
        <h1 className="fw-bold text-dark">
          <FaTasks className="me-2 text-info" />
          {localQueueData ? `${localQueueData.serviceName} Queue` : "Loading Queue..."}
          {localQueueData && (
            <span className={`badge ms-2 ${localQueueData.isActive ? 'bg-success' : 'bg-warning'}`}>
              {localQueueData.isActive ? 'Active' : 'Paused'}
            </span>
          )}
        </h1>
        <div className="d-flex gap-3">
          {localQueueData && (
            <button
              onClick={handleToggleQueueStatus}
              className={`btn ${localQueueData.isActive ? 'btn-warning' : 'btn-success'}`}
              disabled={updatingStatus}
            >
              {updatingStatus ? (
                <FaSpinner className="fa-spin me-2" />
              ) : localQueueData.isActive ? (
                <FaPauseCircle className="me-2" />
              ) : (
                <FaPlayCircle className="me-2" />
              )}
              {localQueueData.isActive ? 'Pause Queue' : 'Resume Queue'}
            </button>
          )}
          <button
            onClick={() => navigate("/provider/queues")}
            className="btn btn-outline-primary"
          >
            <FaListAlt className="me-2" /> My Queues
          </button>
          <button onClick={handleLogout} className="btn btn-outline-danger">
            <FaSignOutAlt className="me-2" /> Logout
          </button>
        </div>
      </div>

      {/* Status Messages */}
      {!connected && (
        <div className="alert alert-warning animate__animated animate__fadeIn">
          Connecting to live queue...
        </div>
      )}
      {error && (
        <div className="alert alert-danger animate__animated animate__fadeIn">
          {error}
        </div>
      )}

      {/* Queue List */}
      {localQueueData ? (
        <div className="card shadow-lg border-0 animate__animated animate__fadeIn">
          <div className="card-body">
            <h4 className="fw-semibold text-secondary mb-4">Live Queue</h4>
            <QueueList 
              queue={localQueueData} 
              onServeNext={handleServeNext}
            />
          </div>
        </div>
      ) : (
        <div className="alert alert-info text-center animate__animated animate__fadeIn">
          No active queue.
        </div>
      )}
    </div>
  );
};

export default ProviderDashboard;