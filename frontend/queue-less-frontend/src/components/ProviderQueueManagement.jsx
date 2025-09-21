import React, { useState, useEffect } from "react";
import { useDispatch, useSelector } from "react-redux";
import { useNavigate } from "react-router-dom";
import axios from "axios";
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
import { normalizeQueues, normalizeQueue } from "../utils/normalizeQueue";
import "animate.css";

const QUEUE_API_BASE_URL = "https://localhost:8443/api/queues";
const PROVIDER_API_BASE_URL = "https://localhost:8443/api/providers"; // New base URL for provider endpoints

const ProviderQueueManagement = () => {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const { id: providerId, token, name } = useSelector((state) => state.auth);

  const [queues, setQueues] = useState([]);
  const [newQueueName, setNewQueueName] = useState("");
  const [selectedPlaceId, setSelectedPlaceId] = useState("");
  const [selectedServiceId, setSelectedServiceId] = useState("");
  const [supportsGroupToken, setSupportsGroupToken] = useState(false);
  const [emergencySupport, setEmergencySupport] = useState(false);
  const [emergencyPriorityWeight, setEmergencyPriorityWeight] = useState(10);
  const [maxCapacity, setMaxCapacity] = useState(50);
  const [loading, setLoading] = useState(true);
  const [updatingStatus, setUpdatingStatus] = useState({});
  const [managedPlaces, setManagedPlaces] = useState([]);
  const [managedServices, setManagedServices] = useState([]);

  useEffect(() => {
    if (providerId && token) {
      fetchQueues();
      fetchManagedPlaces();
      fetchManagedServices();
    }
  }, [providerId, token]);

  const fetchManagedPlaces = async () => {
    try {
      const response = await axios.get(`${PROVIDER_API_BASE_URL}/my-places`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      // Defensive check to ensure the response data is an array
      if (Array.isArray(response.data)) {
        setManagedPlaces(response.data);
      } else {
        console.error("API response for managed places is not an array:", response.data);
        setManagedPlaces([]);
        toast.error("Invalid data received for places.");
      }
    } catch (error) {
      console.error("Failed to fetch managed places:", error);
      setManagedPlaces([]);
      toast.error("Failed to fetch managed places.");
    }
  };

  const fetchManagedServices = async () => {
    try {
      const response = await axios.get(`${PROVIDER_API_BASE_URL}/my-services`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      // Defensive check to ensure the response data is an array
      if (Array.isArray(response.data)) {
        setManagedServices(response.data);
      } else {
        console.error("API response for managed services is not an array:", response.data);
        setManagedServices([]);
        toast.error("Invalid data received for services.");
      }
    } catch (error) {
      console.error("Failed to fetch managed services:", error);
      setManagedServices([]);
      toast.error("Failed to fetch managed services.");
    }
  };

  const fetchQueues = async () => {
    try {
      const response = await axios.get(`${QUEUE_API_BASE_URL}/by-provider`, {
        headers: { Authorization: `Bearer ${token}` },
      });

      if (response.status === 204) {
        setQueues([]);
        toast.info("You don't have any queues yet.");
        return;
      }

      const normalizedQueues = normalizeQueues(response.data);
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

    const isPlaceManaged = managedPlaces.some(place => place.id === selectedPlaceId);
    if (!isPlaceManaged) {
      toast.error("You don't have permission to create queues for this place.");
      return;
    }

    const isServiceManaged = managedServices.some(service => service.id === selectedServiceId);
    if (!isServiceManaged) {
      toast.error("You don't have permission to create queues for this service.");
      return;
    }

    try {
      const response = await axios.post(
        `${QUEUE_API_BASE_URL}/create`,
        {
          providerId,
          serviceName: newQueueName,
          placeId: selectedPlaceId,
          serviceId: selectedServiceId,
          supportsGroupToken,
          emergencySupport,
          emergencyPriorityWeight,
          maxCapacity: maxCapacity > 0 ? maxCapacity : null,
        },
        { headers: { Authorization: `Bearer ${token}` } }
      );

      toast.success(`${response.data.serviceName} queue created!`);
      setNewQueueName("");
      setSelectedPlaceId("");
      setSelectedServiceId("");
      setSupportsGroupToken(false);
      setEmergencySupport(false);
      setEmergencyPriorityWeight(10);
      setMaxCapacity(50);
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
        ? `${QUEUE_API_BASE_URL}/${queueId}/deactivate`
        : `${QUEUE_API_BASE_URL}/${queueId}/activate`;

      const response = await axios.put(
        endpoint,
        {},
        {
          headers: { Authorization: `Bearer ${token}` },
        }
      );

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

  const servicesForSelectedPlace = managedServices.filter(service => service.placeId === selectedPlaceId);

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
                {managedPlaces.map((place) => (
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
                {servicesForSelectedPlace.map((service) => (
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
                            {managedPlaces.find((p) => p.id === queue.placeId)?.name ||
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