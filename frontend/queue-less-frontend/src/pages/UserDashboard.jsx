//src/pages/UserDashboard.jsx
import React, { useState, useEffect } from "react";
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
  FaSearch,
  FaBuilding,
  FaClock,
  FaUserCheck,
  FaCheckCircle,
  FaUsers,
  FaAmbulance,
  FaTrophy,
  FaRegSmileBeam
} from "react-icons/fa";
import { logout } from "../redux/authSlice";
import { fetchPlaces } from "../redux/placeSlice";
import { fetchServices } from "../redux/serviceSlice";
import { Spinner } from "react-bootstrap";
import "animate.css/animate.min.css";
import axiosInstance from "../utils/axiosInstance";

const UserDashboard = () => {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const { token, name, id: userId } = useSelector((state) => state.auth);
  const { items: places, loading: placesLoading } = useSelector(
    (state) => state.places
  );
  const { items: services, loading: servicesLoading } = useSelector(
    (state) => state.services
  );

  const [userQueues, setUserQueues] = useState([]);
  const [queuesLoading, setQueuesLoading] = useState(false);
  const [searchTerm, setSearchTerm] = useState("");
  const [filteredPlaces, setFilteredPlaces] = useState([]);
  const [filteredServices, setFilteredServices] = useState([]);
  const [refreshCount, setRefreshCount] = useState(0);

  useEffect(() => {
    if (token) {
      dispatch(fetchPlaces());
      dispatch(fetchServices());
    }
  }, [dispatch, token]);

  useEffect(() => {
    if (searchTerm) {
      const filteredP = places.filter(
        (place) =>
          place.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
          place.description.toLowerCase().includes(searchTerm.toLowerCase())
      );
      setFilteredPlaces(filteredP);

      const filteredS = services.filter(
        (service) =>
          service.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
          service.description.toLowerCase().includes(searchTerm.toLowerCase())
      );
      setFilteredServices(filteredS);
    } else {
      setFilteredPlaces(places);
      setFilteredServices(services);
    }
  }, [searchTerm, places, services]);

  useEffect(() => {
    const fetchUserQueues = async () => {
  setQueuesLoading(true);
  try {
    const response = await axiosInstance.get(`/queues/by-user/${userId}`);
    
    // Check if the response data is an array before mapping
    if (Array.isArray(response.data)) {
      const queuesWithUserTokens = response.data.map(queue => {
        const userToken = queue.tokens.find(token => token.userId === userId);
        return {
          ...queue,
          userToken: userToken || null,
          position: userToken && userToken.status === 'WAITING' 
            ? queue.tokens.filter(t => t.status === 'WAITING').findIndex(t => t.tokenId === userToken.tokenId) + 1
            : null
        };
      });
      setUserQueues(queuesWithUserTokens);
    } else {
      // Handle the case where data is not an array (e.g., an empty state)
      setUserQueues([]);
    }
  } catch (error) {
    console.error("Failed to fetch user queues:", error);
    if (error.status !== 404) {
      toast.error("Failed to load your queues.");
    }
    setUserQueues([]); // Ensure a clean state on error
  } finally {
    setQueuesLoading(false);
  }
};
    if (userId && token) {
      fetchUserQueues();
      
      // Set up auto-refresh every 15 seconds
      const interval = setInterval(() => {
        setRefreshCount(prev => prev + 1);
        fetchUserQueues();
      }, 15000);
      
      return () => clearInterval(interval);
    }
  }, [userId, token, refreshCount]);

  const handleLogout = () => {
    dispatch(logout());
    navigate("/login");
    toast.info("You have been logged out.");
  };

  const handleRefresh = () => {
    dispatch(fetchPlaces());
    dispatch(fetchServices());
    setRefreshCount(prev => prev + 1);
    toast.info("Refreshing data...");
  };

  const getStatusBadge = (status) => {
    switch (status) {
      case 'WAITING':
        return <span className="badge bg-warning">Waiting</span>;
      case 'IN_SERVICE':
        return <span className="badge bg-success">In Service</span>;
      case 'COMPLETED':
        return <span className="badge bg-info">Completed</span>;
      default:
        return <span className="badge bg-secondary">Unknown</span>;
    }
  };

  const getTokenTypeBadge = (token) => {
    if (token.isGroup) {
      return <span className="badge bg-info me-1"><FaUsers className="me-1" /> Group</span>;
    }
    if (token.isEmergency) {
      return <span className="badge bg-danger me-1"><FaAmbulance className="me-1" /> Emergency</span>;
    }
    return <span className="badge bg-primary me-1">Regular</span>;
  };

  return (
    <div className="container py-5 animate__animated animate__fadeIn">
      {/* Header Section */}
      <div className="d-flex justify-content-between align-items-center mb-5 animate__animated animate__fadeInDown">
        <h1 className="fw-bold text-dark">
          <FaBuilding className="me-2 text-primary" />
          {name ? `${name}'s Dashboard` : "Explore Places & Services"}
        </h1>
        <div className="d-flex gap-2">
          <button
            onClick={handleRefresh}
            className="btn btn-outline-primary d-flex align-items-center"
            title="Refresh"
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

      {/* Your Active Queues Section */}
      <div className="card shadow-lg border-0 mb-5 animate__animated animate__fadeInUp">
        <div className="card-body p-4">
          <div className="d-flex justify-content-between align-items-center mb-4">
            <h4 className="fw-semibold text-secondary mb-0">
              <FaList className="me-2 text-info" /> Your Queues
            </h4>
            <small className="text-muted">Auto-updates every 15 seconds</small>
          </div>

          {queuesLoading ? (
            <div className="d-flex justify-content-center py-4">
              <Spinner animation="border" variant="primary" />
            </div>
          ) : userQueues.length === 0 ? (
            <div className="alert alert-info text-center py-4">
              <FaRegSmileBeam className="display-4 text-info mb-3" />
              <h5>No Active Queues</h5>
              <p className="mb-0">You haven't joined any queues yet. Explore places and services to get started!</p>
            </div>
          ) : (
            <div className="row">
              {userQueues.map((queue) => (
                <div key={queue.id} className="col-md-6 col-lg-4 mb-4">
                  <div className="card h-100 shadow-sm border-0">
                    <div className="card-body">
                      <div className="d-flex justify-content-between align-items-start mb-3">
                        <h5 className="card-title text-truncate">{queue.serviceName}</h5>
                        {queue.userToken && getStatusBadge(queue.userToken.status)}
                      </div>
                      
                      <div className="mb-3">
                        <p className="text-muted mb-1">
                          <FaBuilding className="me-1" />
                          <strong>Place:</strong> {queue.placeName || 'Unknown'}
                        </p>
                        {queue.userToken && (
                          <>
                            <p className="mb-1">
                              <strong>Token:</strong> {queue.userToken.tokenId}
                              {getTokenTypeBadge(queue.userToken)}
                            </p>
                            
                            {queue.userToken.status === 'WAITING' && queue.position && (
                              <div className="alert alert-warning py-2 mb-2">
                                <FaClock className="me-2" />
                                <strong>Position:</strong> {queue.position} of {queue.tokens.filter(t => t.status === 'WAITING').length}
                                {queue.estimatedWaitTime > 0 && (
                                  <span> • ~{queue.estimatedWaitTime} min wait</span>
                                )}
                              </div>
                            )}
                            
                            {queue.userToken.status === 'IN_SERVICE' && (
                              <div className="alert alert-success py-2 mb-2">
                                <FaUserCheck className="me-2" />
                                <strong>You're being served!</strong>
                              </div>
                            )}
                            
                            {queue.userToken.status === 'COMPLETED' && (
                              <div className="alert alert-info py-2 mb-2">
                                <FaCheckCircle className="me-2" />
                                <strong>Service Completed!</strong>
                                {queue.userToken.completedAt && (
                                  <div className="small mt-1">
                                    Completed at: {new Date(queue.userToken.completedAt).toLocaleTimeString()}
                                  </div>
                                )}
                              </div>
                            )}
                            
                            {queue.userToken.isGroup && queue.userToken.groupMembers && (
                              <div className="mt-2">
                                <h6 className="small fw-bold">Group Members:</h6>
                                <ul className="list-group list-group-flush small">
                                  {queue.userToken.groupMembers.map((member, index) => (
                                    <li key={index} className="list-group-item px-0 py-1">
                                      <strong>{member.name}</strong>: {member.details}
                                    </li>
                                  ))}
                                </ul>
                              </div>
                            )}
                            
                            {queue.userToken.isEmergency && queue.userToken.emergencyDetails && (
                              <div className="mt-2">
                                <h6 className="small fw-bold">Emergency Details:</h6>
                                <p className="small text-muted">{queue.userToken.emergencyDetails}</p>
                              </div>
                            )}
                          </>
                        )}
                      </div>
                      
                      <button
                        onClick={() => navigate(`/customer/queue/${queue.id}`)}
                        className="btn btn-outline-primary w-100 mt-auto"
                      >
                        View Queue <FaArrowRight className="ms-2" />
                      </button>
                    </div>
                    
                    {/* Queue Statistics Footer */}
                    <div className="card-footer bg-transparent">
                      <div className="d-flex justify-content-between small text-muted">
                        <span>
                          <FaUsers className="me-1" />
                          {queue.tokens.filter(t => t.status === 'WAITING').length} waiting
                        </span>
                        <span>
                          <FaUserCheck className="me-1" />
                          {queue.tokens.filter(t => t.status === 'IN_SERVICE').length} in service
                        </span>
                        <span>
                          <FaCheckCircle className="me-1" />
                          {queue.tokens.filter(t => t.status === 'COMPLETED').length} completed
                        </span>
                      </div>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Search Section */}
      <div className="card shadow-lg border-0 mb-5 animate__animated animate__fadeInUp">
        <div className="card-body p-4">
          <h4 className="fw-semibold text-secondary mb-4">
            <FaSearch className="me-2 text-info" />
            Search Places & Services
          </h4>
          <div className="input-group">
            <input
              type="text"
              className="form-control form-control-lg"
              placeholder="Search for places or services..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
          </div>
        </div>
      </div>

      {/* Places Section */}
      <div className="card shadow-lg border-0 mb-5 animate__animated animate__fadeInUp">
        <div className="card-body p-4">
          <h4 className="fw-semibold text-secondary mb-4">
            <FaBuilding className="me-2 text-info" /> Places
          </h4>
          {placesLoading ? (
            <div className="d-flex justify-content-center">
              <Spinner animation="border" variant="primary" />
            </div>
          ) : filteredPlaces.length === 0 ? (
            <div className="alert alert-info">No places found.</div>
          ) : (
            <div className="row">
              {filteredPlaces.map((place) => (
                <div key={place.id} className="col-md-4 mb-4">
                  <div className="card h-100 shadow-sm">
                    {place.imageUrls && place.imageUrls.length > 0 && (
                      <img
                        src={place.imageUrls[0]}
                        className="card-img-top"
                        alt={place.name}
                        style={{ height: "200px", objectFit: "cover" }}
                      />
                    )}
                    <div className="card-body d-flex flex-column">
                      <h5 className="card-title">{place.name}</h5>
                      <p className="card-text text-muted">
                        <FaBuilding className="me-1" />
                        {place.address}
                      </p>
                      <p className="card-text">{place.description}</p>
                      <div className="mt-auto">
                        <button
                          className="btn btn-primary w-100"
                          onClick={() => navigate(`/places/${place.id}`)}
                        >
                          View Details
                        </button>
                      </div>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Services Section */}
      <div className="card shadow-lg border-0 animate__animated animate__fadeInUp">
        <div className="card-body p-4">
          <h4 className="fw-semibold text-secondary mb-4">
            <FaList className="me-2 text-info" /> Services
          </h4>
          {servicesLoading ? (
            <div className="d-flex justify-content-center">
              <Spinner animation="border" variant="primary" />
            </div>
          ) : filteredServices.length === 0 ? (
            <div className="alert alert-info">No services found.</div>
          ) : (
            <div className="row">
              {filteredServices.map((service) => (
                <div key={service.id} className="col-md-6 mb-3">
                  <div className="card h-100">
                    <div className="card-body">
                      <h5 className="card-title">{service.name}</h5>
                      <p className="card-text">{service.description}</p>
                      <div className="d-flex justify-content-between align-items-center">
                        <span className="badge bg-info">
                          {service.averageServiceTime} mins
                        </span>
                        <button
                          className="btn btn-sm btn-primary"
                          onClick={() => navigate(`/places/${service.placeId}`)}
                        >
                          View Place
                        </button>
                      </div>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default UserDashboard;