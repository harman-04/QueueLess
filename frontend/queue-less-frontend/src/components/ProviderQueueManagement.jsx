//src/components/ProviderQueueManagement.jsx
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
  FaPauseCircle,
  FaBuilding,
  FaUsers,
  FaAmbulance,
} from "react-icons/fa";
import { fetchPlaces } from "../redux/placeSlice";
import { fetchServicesByPlace } from "../redux/serviceSlice";
import { normalizeQueues, normalizeQueue } from "../utils/normalizeQueue";
import "animate.css";

const API_BASE_URL = "http://localhost:8080/api/queues";

const ProviderQueueManagement = () => {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const { id: providerId, token, name } = useSelector((state) => state.auth);
  const { items: places } = useSelector((state) => state.places);
  const { servicesByPlace } = useSelector((state) => state.services);

  const [queues, setQueues] = useState([]);
  const [newQueueName, setNewQueueName] = useState("");
  const [selectedPlaceId, setSelectedPlaceId] = useState("");
  const [selectedServiceId, setSelectedServiceId] = useState("");
  const [supportsGroupToken, setSupportsGroupToken] = useState(false);
  const [emergencySupport, setEmergencySupport] = useState(false);
  const [emergencyPriorityWeight, setEmergencyPriorityWeight] = useState(10);
  const [maxCapacity, setMaxCapacity] = useState(50); // Keep this default
  const [loading, setLoading] = useState(true);
  const [updatingStatus, setUpdatingStatus] = useState({});

  useEffect(() => {
    if (providerId && token) {
      fetchQueues();
      dispatch(fetchPlaces());
    }
  }, [providerId, dispatch, token]);

  useEffect(() => {
    if (selectedPlaceId) {
      dispatch(fetchServicesByPlace(selectedPlaceId));
    }
  }, [selectedPlaceId, dispatch]);

  const fetchQueues = async () => {
    try {
      const response = await axios.get(`${API_BASE_URL}/by-provider`, {
        headers: { Authorization: `Bearer ${token}` },
      });

      // Handle 204 No Content
      if (response.status === 204) {
        setQueues([]);
        toast.info("You don't have any queues yet.");
        return;
      }

      const normalizedQueues = normalizeQueues(response.data);

      // Additional filter to ensure only own queues are shown
      const filteredQueues = normalizedQueues.filter(
        (queue) => queue.providerId === providerId
      );

      setQueues(filteredQueues);
    } catch (error) {
      if (error.response?.status === 404 || error.response?.status === 204) {
        setQueues([]);
        toast.info("You don't have any queues yet.");
      } else if (error.response?.status === 403) {
        toast.error("You don't have permission to view these queues.");
      } else {
        toast.error("Failed to fetch your queues.");
      }
      setQueues([]);
    } finally {
      setLoading(false);
    }
  };

  const handleCreateQueue = async (e) => {
    e.preventDefault();
    if (!newQueueName.trim() || !selectedPlaceId || !selectedServiceId) {
      toast.warn("Please fill all required fields.");
      return;
    }

    try {
      const response = await axios.post(
        `${API_BASE_URL}/create`,
        {
          providerId,
          serviceName: newQueueName,
          placeId: selectedPlaceId,
          serviceId: selectedServiceId,
          supportsGroupToken,
          emergencySupport,
          emergencyPriorityWeight,
          // Correctly handle maxCapacity: send null if it's 0 or empty, otherwise send the number
          maxCapacity: maxCapacity > 0 ? maxCapacity : null,
        },
        { headers: { Authorization: `Bearer ${token}` } }
      );

      toast.success(`${response.data.serviceName} queue created!`);
      // Reset form fields
      setNewQueueName("");
      setSelectedPlaceId("");
      setSelectedServiceId("");
      setSupportsGroupToken(false);
      setEmergencySupport(false);
      setEmergencyPriorityWeight(10);
      setMaxCapacity(50); // Reset to default
      setQueues((prevQueues) => [...prevQueues, normalizeQueue(response.data)]);
    } catch (error) {
      toast.error("Failed to create queue.");
      console.error("Error creating queue:", error);
    }
  };

  const handleToggleQueueStatus = async (queueId, currentStatus) => {
    setUpdatingStatus((prev) => ({ ...prev, [queueId]: true }));
    try {
      const endpoint = currentStatus
        ? `${API_BASE_URL}/${queueId}/deactivate`
        : `${API_BASE_URL}/${queueId}/activate`;

      const response = await axios.put(
        endpoint,
        {},
        {
          headers: { Authorization: `Bearer ${token}` },
        }
      );

      // Update the specific queue in the state
      setQueues((prevQueues) =>
        prevQueues.map((queue) =>
          queue.id === queueId ? normalizeQueue(response.data) : queue
        )
      );

      toast.success(`Queue ${currentStatus ? "paused" : "resumed"} successfully!`);
    } catch (error) {
      console.error("Error updating queue status:", error);
      if (error.response?.status === 500) {
        toast.error("Server error. Please try again later.");
      } else {
        toast.error("Failed to update queue status.");
      }
    } finally {
      setUpdatingStatus((prev) => ({ ...prev, [queueId]: false }));
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

  const services = selectedPlaceId ? servicesByPlace[selectedPlaceId] || [] : [];

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

      <div className="card shadow-lg border-0 mb-5 animate__animated animate__fadeInUp">
        <div className="card-body p-4">
          <h4 className="fw-semibold text-secondary mb-4">
            <FaPlusCircle className="me-2 text-success" />
            Create a New Queue
          </h4>
          <form
            onSubmit={handleCreateQueue}
            className="row g-3 align-items-center"
          >
            <div className="col-md-3">
              <select
                className="form-control form-control-lg"
                value={selectedPlaceId}
                onChange={(e) => setSelectedPlaceId(e.target.value)}
                required
              >
                <option value="">Select Place</option>
                {places.map((place) => (
                  <option key={place.id} value={place.id}>
                    {place.name}
                  </option>
                ))}
              </select>
            </div>

            <div className="col-md-3">
              <select
                className="form-control form-control-lg"
                value={selectedServiceId}
                onChange={(e) => setSelectedServiceId(e.target.value)}
                disabled={!selectedPlaceId}
                required
              >
                <option value="">Select Service</option>
                {services.map((service) => (
                  <option key={service.id} value={service.id}>
                    {service.name}
                  </option>
                ))}
              </select>
            </div>

            <div className="col-md-2">
              <input
                type="text"
                className="form-control form-control-lg"
                value={newQueueName}
                onChange={(e) => setNewQueueName(e.target.value)}
                placeholder="Service Name"
                required
              />
            </div>

            <div className="col-md-2">
              <input
                type="number"
                className="form-control form-control-lg"
                value={maxCapacity}
                onChange={(e) => setMaxCapacity(parseInt(e.target.value))}
                placeholder="Max Capacity"
                min="1"
              />
            </div>

            <div className="col-md-2 d-grid">
              <button type="submit" className="btn btn-primary btn-lg shadow-sm">
                Create Queue
              </button>
            </div>

            <div className="col-md-3">
              <div className="form-check">
                <input
                  type="checkbox"
                  className="form-check-input"
                  id="supportsGroupToken"
                  checked={supportsGroupToken}
                  onChange={(e) => setSupportsGroupToken(e.target.checked)}
                />
                <label className="form-check-label" htmlFor="supportsGroupToken">
                  <FaUsers className="me-1" /> Supports Group Tokens
                </label>
              </div>
            </div>

            <div className="col-md-3">
              <div className="form-check">
                <input
                  type="checkbox"
                  className="form-check-input"
                  id="emergencySupport"
                  checked={emergencySupport}
                  onChange={(e) => setEmergencySupport(e.target.checked)}
                />
                <label className="form-check-label" htmlFor="emergencySupport">
                  <FaAmbulance className="me-1" /> Emergency Support
                </label>
              </div>
            </div>

            {emergencySupport && (
              <div className="col-md-3">
                <input
                  type="number"
                  className="form-control"
                  value={emergencyPriorityWeight}
                  onChange={(e) =>
                    setEmergencyPriorityWeight(parseInt(e.target.value))
                  }
                  placeholder="Emergency Priority"
                  min="1"
                  max="100"
                />
                <small className="text-muted">Higher number = higher priority</small>
              </div>
            )}
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
                          <span
                            className={`badge ${
                              queue.isActive ? "bg-success" : "bg-warning"
                            }`}
                          >
                            {queue.isActive ? "Active" : "Paused"}
                          </span>
                        </div>
                        <p className="card-text text-muted mb-1">
                          <strong>Queue ID:</strong> {queue.id}
                        </p>
                        <p className="card-text text-muted">
                          <strong>Current Tokens:</strong>{" "}
                          {queue.tokens.length} /{" "}
                          {queue.maxCapacity || "∞"}
                        </p>
                        {queue.placeId && (
                          <p className="card-text text-muted">
                            <FaBuilding className="me-1" />
                            <strong>Place:</strong>{" "}
                            {places.find((p) => p.id === queue.placeId)?.name ||
                              queue.placeId}
                          </p>
                        )}
                        <div className="mt-2">
                          {queue.supportsGroupToken && (
                            <span className="badge bg-info me-1">
                              <FaUsers className="me-1" /> Group Tokens
                            </span>
                          )}
                          {queue.emergencySupport && (
                            <span className="badge bg-danger">
                              <FaAmbulance className="me-1" /> Emergency
                            </span>
                          )}
                        </div>
                      </div>
                      <div className="mt-3 d-grid gap-2">
                        <button
                          onClick={() => handleManageQueue(queue.id)}
                          className="btn btn-outline-primary btn-sm transition-all"
                        >
                          Manage Queue
                        </button>
                        <button
                          onClick={() =>
                            handleToggleQueueStatus(queue.id, queue.isActive)
                          }
                          className={`btn btn-sm ${
                            queue.isActive ? "btn-warning" : "btn-success"
                          }`}
                          disabled={updatingStatus[queue.id]}
                        >
                          {updatingStatus[queue.id] ? (
                            <FaSpinner className="fa-spin me-1" />
                          ) : queue.isActive ? (
                            <FaPauseCircle className="me-1" />
                          ) : (
                            <FaPlayCircle className="me-1" />
                          )}
                          {queue.isActive ? "Pause Queue" : "Resume Queue"}
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