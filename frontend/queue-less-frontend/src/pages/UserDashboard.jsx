// src/components/UserDashboard.jsx
import React, { useState, useEffect, useRef } from "react";
import { useDispatch, useSelector } from "react-redux";
import { useNavigate } from "react-router-dom";
import axios from "axios";
import { toast } from "react-toastify";
import {
  FaList,
  FaSignOutAlt,
  FaSpinner,
  FaArrowRight,
  FaSync,
} from "react-icons/fa";
import { logout } from "../redux/authSlice";
import 'animate.css/animate.min.css';
import { Client } from '@stomp/stompjs';

const API_BASE_URL = "http://localhost:8080/api/queues";

const UserDashboard = () => {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const { token, name } = useSelector((state) => state.auth);
  const [queues, setQueues] = useState([]);
  const [loading, setLoading] = useState(true);
  const isFetching = useRef(false);
  const stompClient = useRef(null);
  const subscriptions = useRef({});
  const allQueuesSubscription = useRef(null);

  // Normalize queue data function
  const normalizeQueue = (queue) => {
    return {
      ...queue,
      isActive: queue.active !== undefined ? queue.active : queue.isActive
    };
  };

  // Fetch all available queues
  const fetchAllQueues = async () => {
    if (isFetching.current) return;
    
    isFetching.current = true;
    try {
      const response = await axios.get(`${API_BASE_URL}/all`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      
      const normalizedQueues = response.data.map(normalizeQueue);
      setQueues(normalizedQueues);
      
      // After fetching, set up WebSocket subscriptions for each queue
      setupWebSocketSubscriptions(normalizedQueues);
    } catch (error) {
      console.error("Failed to fetch all queues:", error);
      toast.error("Failed to fetch available queues.");
      setQueues([]);
    } finally {
      setLoading(false);
      isFetching.current = false;
    }
  };

  // Setup WebSocket connection and subscriptions
  const setupWebSocketSubscriptions = (queuesList) => {
    // Disconnect existing connection if any
    if (stompClient.current) {
      stompClient.current.deactivate();
      stompClient.current = null;
    }

    // Clear existing subscriptions
    Object.values(subscriptions.current).forEach(sub => sub.unsubscribe());
    subscriptions.current = {};

    // Create new STOMP client
    stompClient.current = new Client({
      brokerURL: "ws://localhost:8080/ws",
      connectHeaders: {
        Authorization: "Bearer " + token,
      },
      debug: (str) => {
        console.log("[STOMP UserDashboard]", str);
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      onConnect: () => {
        console.log("✅ UserDashboard WebSocket connected!");
        
        // Subscribe to a general queue updates topic if available
        // If not, we'll subscribe to each individual queue
        if (stompClient.current) {
          // Try to subscribe to a general topic for all queue updates
          allQueuesSubscription.current = stompClient.current.subscribe(
            '/topic/queues', 
            (message) => {
              try {
                const updatedQueue = normalizeQueue(JSON.parse(message.body));
                console.log("📥 General queue update:", updatedQueue);
                
                // Update the specific queue in the state
                setQueues(prevQueues => {
                  const exists = prevQueues.some(q => q.id === updatedQueue.id);
                  
                  if (exists) {
                    // Update existing queue
                    return prevQueues.map(q => 
                      q.id === updatedQueue.id ? updatedQueue : q
                    );
                  } else {
                    // Add new queue if it doesn't exist
                    return [...prevQueues, updatedQueue];
                  }
                });
              } catch (error) {
                console.error("Error parsing general queue update:", error);
              }
            }
          );
          
          // Also subscribe to each individual queue for redundancy
          queuesList.forEach(queue => {
            const subscription = stompClient.current.subscribe(
              `/topic/queues/${queue.id}`, 
              (message) => {
                try {
                  const updatedQueue = normalizeQueue(JSON.parse(message.body));
                  console.log("📥 Individual queue update:", updatedQueue);
                  
                  // Update the specific queue in the state
                  setQueues(prevQueues => 
                    prevQueues.map(q => q.id === updatedQueue.id ? updatedQueue : q)
                  );
                } catch (error) {
                  console.error("Error parsing individual queue message:", error);
                }
              }
            );
            
            subscriptions.current[queue.id] = subscription;
          });
        }
      },
      onStompError: (frame) => {
        console.error("STOMP error in UserDashboard:", frame);
      }
    });

    stompClient.current.activate();
  };

  // Manual refresh function
  const handleRefresh = () => {
    setLoading(true);
    fetchAllQueues();
    toast.info("Refreshing queues...");
  };

  useEffect(() => {
    fetchAllQueues();

    return () => {
      // Cleanup WebSocket connection on unmount
      if (stompClient.current) {
        stompClient.current.deactivate();
      }
    };
  }, [token]);

  const handleLogout = () => {
    dispatch(logout());
    navigate("/login");
    toast.info("You have been logged out.");
  };

  const handleSelectQueue = (queueId) => {
    navigate(`/customer/queue/${queueId}`);
  };

  if (loading) {
    return (
      <div className="d-flex justify-content-center align-items-center vh-100 text-primary animate__animated animate__fadeIn">
        <FaSpinner className="fa-spin me-2" /> Loading available queues...
      </div>
    );
  }

  return (
    <div className="container py-5 animate__animated animate__fadeIn">
      {/* Header Section */}
      <div className="d-flex justify-content-between align-items-center mb-5 animate__animated animate__fadeInDown">
        <h1 className="fw-bold text-dark">
          <FaList className="me-2 text-primary" />
          {name ? `${name}'s Dashboard` : "Available Queues"}
        </h1>
        <div className="d-flex gap-2">
          <button
            onClick={handleRefresh}
            className="btn btn-outline-primary d-flex align-items-center"
            title="Refresh queues"
          >
            <FaSync className="me-2" /> Refresh
          </button>
          <button
            onClick={handleLogout}
            className="btn btn-outline-danger d-flex align-items-center"
          >
            <FaSignOutAlt className="me-2" /> Logout
          </button>
        </div>
      </div>

      {/* Available Queues List */}
      <div className="card shadow-lg border-0 animate__animated animate__fadeInUp">
        <div className="card-body p-4">
          <div className="d-flex justify-content-between align-items-center mb-4">
            <h4 className="fw-semibold text-secondary mb-0">
              <FaList className="me-2 text-info" />
              Active Services
            </h4>
            <small className="text-muted">
              {queues.filter(q => q.isActive).length} active, {queues.filter(q => !q.isActive).length} paused
            </small>
          </div>

          {queues.length > 0 ? (
            <div className="list-group">
              {queues.map((queue) => (
                <button
                  key={queue.id}
                  onClick={() => handleSelectQueue(queue.id)}
                  className="list-group-item list-group-item-action d-flex justify-content-between align-items-center"
                  disabled={!queue.isActive}
                >
                  <div>
                    <h5 className="mb-1 fw-bold text-dark">{queue.serviceName}</h5>
                    <p className="mb-0 text-muted">
                      Status: {queue.isActive ? 
                        <span className="text-success">Active</span> : 
                        <span className="text-warning">Paused</span>
                      }
                    </p>
                    <p className="mb-0 text-muted">
                      Tokens in queue: {queue.tokens.length}
                    </p>
                  </div>
                  <span className={`badge rounded-pill d-flex align-items-center ${queue.isActive ? 'bg-primary' : 'bg-secondary'}`}>
                    {queue.isActive ? (
                      <>
                        Join Queue <FaArrowRight className="ms-2" />
                      </>
                    ) : (
                      "Unavailable"
                    )}
                  </span>
                </button>
              ))}
            </div>
          ) : (
            <div className="alert alert-info text-center mt-3 animate__animated animate__fadeIn">
              No active services available right now.
            </div>
          )}
        </div>
      </div>

      {/* Real-time status indicator */}
      <div className="mt-3 text-center">
        <small className="text-muted">
          <FaSync className="me-1 fa-spin" /> Live updates active
        </small>
      </div>
    </div>
  );
};

export default UserDashboard;