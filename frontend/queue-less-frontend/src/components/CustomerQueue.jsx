// src/components/CustomerQueue.jsx
import React, { useState, useEffect } from "react";
import { useDispatch, useSelector } from "react-redux";
import { useParams, useNavigate } from "react-router-dom";
import axios from "axios";
import { toast } from "react-toastify";
import { FaUserPlus, FaSpinner, FaHome, FaPauseCircle } from "react-icons/fa";
import 'animate.css/animate.min.css';

const API_BASE_URL = "http://localhost:8080/api/queues";

const CustomerQueue = () => {
  const { queueId } = useParams();
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const { id: userId, token, role } = useSelector((state) => state.auth);

  const [queue, setQueue] = useState(null);
  const [loading, setLoading] = useState(true);
  const [addingToken, setAddingToken] = useState(false);

  useEffect(() => {
    if (!queueId) {
      navigate("/");
      return;
    }

    // In CustomerQueue.jsx - Update the fetchQueue function
const fetchQueue = async () => {
  try {
    const response = await axios.get(`${API_BASE_URL}/${queueId}`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    
    // Normalize the queue data
    const normalizedQueue = {
      ...response.data,
      isActive: response.data.active !== undefined ? response.data.active : response.data.isActive
    };
    
    setQueue(normalizedQueue);
  } catch (error) {
    console.error("Failed to fetch queue:", error);
    toast.error("Failed to load queue details.");
  } finally {
    setLoading(false);
  }
};

    fetchQueue();
  }, [queueId, token, navigate]);

  const handleAddToken = async () => {
    if (!userId || !token) {
      toast.error("You must be logged in to join a queue.");
      navigate("/login");
      return;
    }
   
    if (!queue.isActive) {
      toast.error("This queue is currently paused by the provider.");
      return;
    }

    setAddingToken(true);
    try {
      const response = await axios.post(
        `${API_BASE_URL}/${queueId}/add-token`,
        { userId: userId },
        { headers: { Authorization: `Bearer ${token}` } }
      );
     
      const newTokenId = response.data.tokenId;
      toast.success(`You have joined the queue! Your token is ${newTokenId}.`);
      setQueue(prevQueue => {
        if (!prevQueue) return null;
        return {
          ...prevQueue,
          tokens: [...prevQueue.tokens, response.data],
          tokenCounter: prevQueue.tokenCounter + 1,
        };
      });
      console.log("Token added:", response.data);
    } catch (error) {
      console.error("Error adding token:", error);
      if (error.response?.status === 409) {
        toast.error("This queue is currently paused by the provider.");
      } else {
        toast.error("Failed to add token to the queue.");
      }
    } finally {
      setAddingToken(false);
    }
  };

  if (loading) {
    return (
      <div className="d-flex justify-content-center align-items-center vh-100 text-primary animate__animated animate__fadeIn">
        <FaSpinner className="fa-spin me-2" /> Loading queue...
      </div>
    );
  }

  if (!queue) {
    return (
      <div className="container py-5 text-center animate__animated animate__fadeIn">
        <div className="alert alert-danger">Queue not found.</div>
        <button onClick={() => navigate("/")} className="btn btn-primary mt-3">
          <FaHome className="me-2" /> Go to Home
        </button>
      </div>
    );
  }

  return (
    <div className="container py-5 animate__animated animate__fadeIn">
      <div className="d-flex justify-content-between align-items-center mb-5 animate__animated animate__fadeInDown">
        <h1 className="fw-bold text-dark">
          {queue.serviceName} Queue
          {!queue.isActive && (
            <span className="badge bg-warning ms-2">
              <FaPauseCircle className="me-1" /> Paused
            </span>
          )}
        </h1>
        <button onClick={() => navigate("/")} className="btn btn-outline-secondary">
          <FaHome className="me-2" /> Back to Home
        </button>
      </div>

      <div className="card shadow-lg border-0 p-4 mb-4">
        <div className="card-body text-center">
          <h4 className="fw-semibold text-secondary mb-3">Join this Queue</h4>
          <p className="text-muted">Current tokens in queue: {queue.tokens.length}</p>
          {!queue.isActive && (
            <div className="alert alert-warning mb-3">
              <FaPauseCircle className="me-2" /> This queue is currently paused by the provider.
            </div>
          )}
          <button
            onClick={handleAddToken}
            className="btn btn-primary btn-lg d-flex align-items-center justify-content-center mx-auto"
            disabled={addingToken || !queue.isActive}
          >
            {addingToken ? (
              <FaSpinner className="fa-spin me-2" />
            ) : (
              <FaUserPlus className="me-2" />
            )}
            {addingToken ? "Adding..." : "Get a Token"}
          </button>
        </div>
      </div>
    </div>
  );
};

export default CustomerQueue;