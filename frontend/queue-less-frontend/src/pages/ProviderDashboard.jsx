// src/components/ProviderDashboard.jsx
import React, { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { useParams, useNavigate } from "react-router-dom";
import QueueList from "../components/QueueList";
import { toast } from "react-toastify";
import "animate.css/animate.min.css";
import './ProviderDashboard.css';
import {
  FaTasks,
  FaSpinner,
  FaListAlt,
  FaPlayCircle,
  FaPauseCircle,
  FaSync,
  FaUsers,
  FaChartLine,
  FaClock,
  FaUserCheck,
  FaArrowLeft,
  FaWifi,
  FaExclamationTriangle,
  FaCog,
  FaHistory
} from "react-icons/fa";
import axios from "axios";
import WebSocketService from "../services/websocketService";

const API_BASE_URL = "http://localhost:8080/api/queues";

const ProviderDashboard = () => {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const { queueId } = useParams();

  const { data: queueData, connected, error } = useSelector((state) => state.queue);
  const { token, role, name } = useSelector((state) => state.auth);
  const [updatingStatus, setUpdatingStatus] = useState(false);
  const [localQueueData, setLocalQueueData] = useState(null);
  const [connectionStatus, setConnectionStatus] = useState('disconnected');
  const [stats, setStats] = useState({
    waiting: 0,
    inService: 0,
    completed: 0,
    avgWaitTime: 0
  });

  // Update local state when Redux state changes
  useEffect(() => {
    if (queueData) {
      const normalizedQueue = {
        ...queueData,
        isActive: queueData.active !== undefined ? queueData.active : queueData.isActive
      };
      setLocalQueueData(normalizedQueue);
      
      // Calculate stats
      const waiting = normalizedQueue.tokens?.filter(t => t.status === 'WAITING').length || 0;
      const inService = normalizedQueue.tokens?.filter(t => t.status === 'IN_SERVICE').length || 0;
      const completed = normalizedQueue.tokens?.filter(t => t.status === 'COMPLETED').length || 0;
      
      setStats({
        waiting,
        inService,
        completed,
        avgWaitTime: normalizedQueue.estimatedWaitTime || 0
      });
    }
  }, [queueData]);

  useEffect(() => {
    if (token && role === "PROVIDER" && queueId) {
      // Initialize WebSocket connection
      WebSocketService.connect();
      WebSocketService.subscribeToQueue(queueId);
      WebSocketService.subscribeToUserUpdates();
      
      setConnectionStatus('connecting');

      // Fetch initial queue data
      const fetchInitialQueueData = async () => {
        try {
          const response = await axios.get(`${API_BASE_URL}/${queueId}`, {
            headers: { Authorization: `Bearer ${token}` },
          });
          const normalizedQueue = {
            ...response.data,
            isActive: response.data.active !== undefined ? response.data.active : response.data.isActive
          };
          setLocalQueueData(normalizedQueue);
          setConnectionStatus('connected');
        } catch (err) {
          console.error("Failed to fetch initial queue data:", err);
          setConnectionStatus('error');
        }
      };
      
      fetchInitialQueueData();
    }

    return () => {
      WebSocketService.unsubscribeFromQueue(queueId);
    };
  }, [dispatch, token, role, queueId]);

  const handleToggleQueueStatus = async () => {
    if (!localQueueData) return;
    
    setUpdatingStatus(true);
    try {
      // Use direct HTTP API call instead of WebSocket for reliability
      const endpoint = localQueueData.isActive 
        ? `${API_BASE_URL}/${queueId}/deactivate`
        : `${API_BASE_URL}/${queueId}/activate`;
      
      const response = await axios.put(endpoint, {}, { 
        headers: { Authorization: `Bearer ${token}` } 
      });
      
      const updatedQueue = {
        ...response.data,
        isActive: response.data.active !== undefined ? response.data.active : response.data.isActive
      };
      
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
  };

  const handleServeNext = () => {
    // Send WebSocket message to serve next token
    const success = WebSocketService.sendMessage("/app/queue/serve-next", { queueId });
    
    if (!success) {
      toast.error("Failed to send serve next request. Please try again.");
    }
  };

  const handleRefresh = () => {
    // Manually refresh queue data
    const fetchQueueData = async () => {
      try {
        const response = await axios.get(`${API_BASE_URL}/${queueId}`, {
          headers: { Authorization: `Bearer ${token}` },
        });
        const normalizedQueue = {
          ...response.data,
          isActive: response.data.active !== undefined ? response.data.active : response.data.isActive
        };
        setLocalQueueData(normalizedQueue);
        toast.success("Queue data refreshed");
      } catch (error) {
        console.error("Failed to refresh queue data:", error);
        toast.error("Failed to refresh queue data");
      }
    };
    
    fetchQueueData();
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
    <div className="provider-dashboard-container">
      {/* Header */}
      <div className="dashboard-header animate__animated animate__fadeInDown">
        <div className="header-content">
          <button 
            onClick={() => navigate("/provider/queues")}
            className="back-button"
          >
            <FaArrowLeft className="me-2" /> All Queues
          </button>
          
          <div className="queue-title-section">
            <div className="queue-icon">
              <FaTasks />
            </div>
            <div className="queue-info">
              <h1 className="queue-name">
                {localQueueData ? localQueueData.serviceName : "Loading Queue..."}
              </h1>
              <div className="queue-meta">
                <span className="queue-id">ID: {queueId}</span>
                <div className="queue-status">
                  <span className={`status-badge ${localQueueData?.isActive ? 'active' : 'paused'}`}>
                    {localQueueData?.isActive ? 'Active' : 'Paused'}
                  </span>
                  <div className="connection-status">
                    <div className={`connection-indicator ${connectionStatus}`}>
                      {connectionStatus === 'connected' ? <FaWifi /> : <FaExclamationTriangle />}
                    </div>
                    <span className="status-text">
                      {connectionStatus === 'connected' ? 'Live Connected' : 
                      connectionStatus === 'connecting' ? 'Connecting...' : 
                      connectionStatus === 'error' ? 'Connection Error' : 'Disconnected'}
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </div>
          
          <div className="header-actions">
            <button
              onClick={handleRefresh}
              className="action-btn refresh-btn"
              title="Refresh queue data"
            >
              <FaSync />
            </button>
            <button
              onClick={() => navigate("/provider/queues")}
              className="action-btn queues-btn"
              title="View all queues"
            >
              <FaListAlt />
            </button>
            {localQueueData && (
              <button
                onClick={handleToggleQueueStatus}
                className={`action-btn status-toggle-btn ${localQueueData.isActive ? 'pause' : 'resume'}`}
                disabled={updatingStatus}
              >
                {updatingStatus ? (
                  <FaSpinner className="spinning" />
                ) : localQueueData.isActive ? (
                  <FaPauseCircle />
                ) : (
                  <FaPlayCircle />
                )}
                <span>{localQueueData.isActive ? 'Pause Queue' : 'Resume Queue'}</span>
              </button>
            )}
          </div>
        </div>
      </div>

      {/* Stats Cards */}
      <div className="stats-grid animate__animated animate__fadeIn">
        <div className="stat-card waiting">
          <div className="stat-icon">
            <FaUsers />
          </div>
          <div className="stat-content">
            <h3 className="stat-value">{stats.waiting}</h3>
            <p className="stat-label">Waiting</p>
          </div>
          <div className="stat-trend">
            <FaChartLine className="trend-up" />
          </div>
        </div>
        
        <div className="stat-card in-service">
          <div className="stat-icon">
            <FaUserCheck />
          </div>
          <div className="stat-content">
            <h3 className="stat-value">{stats.inService}</h3>
            <p className="stat-label">In Service</p>
          </div>
        </div>
        
        <div className="stat-card completed">
          <div className="stat-icon">
            <FaHistory />
          </div>
          <div className="stat-content">
            <h3 className="stat-value">{stats.completed}</h3>
            <p className="stat-label">Completed</p>
          </div>
        </div>
        
        <div className="stat-card wait-time">
          <div className="stat-icon">
            <FaClock />
          </div>
          <div className="stat-content">
            <h3 className="stat-value">{stats.avgWaitTime}m</h3>
            <p className="stat-label">Avg. Wait Time</p>
          </div>
        </div>
      </div>

      {/* Status Messages */}
      {connectionStatus === 'error' && (
        <div className="status-alert error animate__animated animate__fadeIn">
          <FaExclamationTriangle className="me-2" />
          Connection error. Please check your internet connection and try refreshing.
        </div>
      )}

      {!connected && connectionStatus === 'connecting' && (
        <div className="status-alert warning animate__animated animate__fadeIn">
          <FaSpinner className="spinning me-2" />
          Connecting to live queue...
        </div>
      )}

      {/* Queue List */}
      <div className="queue-list-container animate__animated animate__fadeIn">
        {localQueueData ? (
          <QueueList queue={localQueueData} onServeNext={handleServeNext} />
        ) : (
          <div className="no-queue-message">
            <div className="loading-spinner">
              <FaSpinner className="spinning" size="2rem" />
            </div>
            <h3>Loading Queue Data</h3>
            <p>Please wait while we load your queue information</p>
          </div>
        )}
      </div>
      
    </div>
  );
};

export default ProviderDashboard;