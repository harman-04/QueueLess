import React, { useState, useEffect } from "react";
import { useDispatch, useSelector } from "react-redux";
import { useNavigate } from "react-router-dom";
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
    FaRegSmileBeam,
    FaHeart
} from "react-icons/fa";
import { getShortTokenId } from '../utils/tokenUtils';
import { logout } from "../redux/authSlice";
import { fetchPlaces } from "../redux/placeSlice";
import { fetchServices } from "../redux/serviceSlice";
import { fetchFavoritePlacesWithDetails } from "../redux/userSlice";
import { fetchUserTokenHistory } from "../redux/userAnalyticsSlice";
import { Spinner, Card, Row, Col, Button, Alert } from "react-bootstrap";
import "animate.css/animate.min.css";
import axiosInstance from "../utils/axiosInstance";
import UserDashboardSkeleton from "../components/UserDashboardSkeleton";
import UserTokenHistoryChart from "../components/UserTokenHistoryChart";
import './UserDashboard.css';
const UserDashboard = () => {
    const dispatch = useDispatch();
    const navigate = useNavigate();
    const { token, name, id: userId } = useSelector((state) => state.auth);
    const { items: places, loading: placesLoading } = useSelector((state) => state.places);
    const { items: services, loading: servicesLoading } = useSelector((state) => state.services);
    const { favoritePlaces, loading: favoritesLoading } = useSelector((state) => state.user);
    const { tokenHistory, loading: tokenHistoryLoading, error: tokenHistoryError } = useSelector((state) => state.userAnalytics);

    const [userQueues, setUserQueues] = useState([]);
    const [queuesLoading, setQueuesLoading] = useState(false);
    const [searchTerm, setSearchTerm] = useState("");
    const [filteredPlaces, setFilteredPlaces] = useState([]);
    const [filteredServices, setFilteredServices] = useState([]);
    const [refreshCount, setRefreshCount] = useState(0);
    const [visiblePlaces, setVisiblePlaces] = useState(6);
    const [visibleServices, setVisibleServices] = useState(6);
    const [cancelling, setCancelling] = useState(null);
    const isLoading = placesLoading || servicesLoading || favoritesLoading;

    useEffect(() => {
        if (token) {
            dispatch(fetchPlaces());
            dispatch(fetchServices());
            dispatch(fetchFavoritePlacesWithDetails());
            dispatch(fetchUserTokenHistory(30));
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
        setVisiblePlaces(6);
        setVisibleServices(6);
    }, [searchTerm, places, services]);

    useEffect(() => {
        const fetchUserQueues = async () => {
            setQueuesLoading(true);
            try {
                const response = await axiosInstance.get(`/queues/by-user/${userId}`);

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
                    setUserQueues([]);
                }
            } catch (error) {
                console.error("Failed to fetch user queues:", error);
                if (error.status !== 404) {
                    toast.error("Failed to load your queues.");
                }
                setUserQueues([]);
            } finally {
                setQueuesLoading(false);
            }
        };

        if (userId && token) {
            fetchUserQueues();

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
        dispatch(fetchFavoritePlacesWithDetails());
        dispatch(fetchUserTokenHistory(30));
        setRefreshCount(prev => prev + 1);
        toast.info("Refreshing data...");
    };

    const handleCancelQueue = async (queueId, tokenId) => {
        if (!window.confirm('Are you sure you want to leave this queue?')) return;
        setCancelling(queueId);
        try {
            await axiosInstance.delete(`/queues/${queueId}/cancel-token/${tokenId}`);
            toast.success('You have left the queue.');
            // Refresh user queues
            const response = await axiosInstance.get(`/queues/by-user/${userId}`);
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
            }
        } catch (error) {
            toast.error('Failed to leave queue.');
        } finally {
            setCancelling(null);
        }
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

    if (isLoading) {
        return <UserDashboardSkeleton />;
    }

    return (
        <div className="user-dashboard-container animate__animated animate__fadeIn">
            {/* Header Section */}
            <div className="user-dashboard-header animate__animated animate__fadeInDown">
                <h1 className="user-dashboard-title">
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
            <div className="user-dashboard-card animate__animated animate__fadeInUp">
                <div className="card-body p-4">
                    <div className="d-flex justify-content-between align-items-center mb-4">
                        <h4 className="user-dashboard-section-title">
                            <FaList className="me-2 text-info" /> Your Queues
                        </h4>
                        <small className="text-muted">Auto-updates every 15 seconds</small>
                    </div>

                    {queuesLoading ? (
                        <div className="d-flex justify-content-center py-4">
                            <Spinner animation="border" variant="primary" />
                        </div>
                    ) : userQueues.length === 0 ? (
                        <div className="user-dashboard-empty-alert text-center py-4">
                            <FaRegSmileBeam className="display-4 mb-3" />
                            <h5>No Active Queues</h5>
                            <p className="mb-0">You haven't joined any queues yet. Explore places and services to get started!</p>
                        </div>
                    ) : (
                        <div className="row">
                            {userQueues.map((queue) => (
                                <div key={queue.id} className="col-md-6 col-lg-4 mb-4">
                                    <div className="user-queue-card h-100">
                                        <div className="card-body">
                                            <div className="d-flex justify-content-between align-items-start mb-3">
                                                <h5 className="card-title text-truncate">{queue.serviceName}</h5>
                                                {queue.userToken && getStatusBadge(queue.userToken.status)}
                                            </div>

                                            <div className="mb-3">
                                                <p className="text-muted mb-1">
                                                    <FaBuilding className="me-1" />
                                                    <strong>Place:</strong> {
                                                        // Find the place in the Redux 'places' array that matches this queue's placeId
                                                        places.find(p => p.id === queue.placeId)?.name || 'Unknown'
                                                    }
                                                </p>
                                                {queue.userToken && (
                                                    <>
                                                        <p className="mb-2">
                                                            <strong>Token:</strong> {getShortTokenId(queue.userToken.tokenId)}
                                                            <span className="ms-2 ">
                                                                {getTokenTypeBadge(queue.userToken)} </span>

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

                                            {queue.userToken && queue.userToken.status === 'WAITING' && (
                                                <Button
                                                    variant="outline-danger"
                                                    size="sm"
                                                    className="mt-2"
                                                    onClick={() => handleCancelQueue(queue.id, queue.userToken.tokenId)}
                                                    disabled={cancelling === queue.id}
                                                >
                                                    {cancelling === queue.id ? <Spinner animation="border" size="sm" /> : 'Leave Queue'}
                                                </Button>
                                            )}
                                        </div>

                                        {/* Queue Statistics Footer */}
                                        <div className="card-footer">
                                            <div className="d-flex justify-content-between small">
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

            {/* Favorite Places Section */}
            <div className="user-dashboard-card animate__animated animate__fadeInUp">
                <div className="card-body p-4">
                    <div className="d-flex justify-content-between align-items-center mb-4">
                        <h4 className="user-dashboard-section-title">
                            <FaHeart className="me-2 text-danger" /> Favorite Places
                        </h4>
                        <Button variant="outline-primary" size="sm" onClick={() => navigate('/favorites')}>
                            View All
                        </Button>
                    </div>

                    {favoritesLoading ? (
                        <div className="d-flex justify-content-center py-3">
                            <Spinner animation="border" variant="primary" />
                        </div>
                    ) : favoritePlaces.slice(0, 3).length === 0 ? (
                        <Alert variant="info" className="text-center py-3">
                            <FaHeart className="me-2" />
                            You don't have any favorite places yet.
                        </Alert>
                    ) : (
                        <Row>
                            {favoritePlaces.slice(0, 3).map((place) => (
                                <Col md={4} key={place.id} className="mb-3">
                                    <div className="user-favorite-card h-100">
                                        {place.imageUrls && place.imageUrls.length > 0 && (
                                            <Card.Img
                                                variant="top"
                                                src={place.imageUrls[0]}
                                                style={{ height: '120px', objectFit: 'cover' }}
                                                onError={(e) => { e.target.src = 'https://via.placeholder.com/300x200?text=No+Image'; }}
                                            />
                                        )}
                                        <div className="card-body">
                                            <Card.Title className="h6">{place.name}</Card.Title>
                                            <Button
                                                variant="outline-primary"
                                                size="sm"
                                                onClick={() => navigate(`/places/${place.id}`)}
                                            >
                                                View Details
                                            </Button>
                                        </div>
                                    </div>
                                </Col>
                            ))}
                        </Row>
                    )}
                </div>
            </div>

            {/* Token Usage Chart */}
            <UserTokenHistoryChart data={tokenHistory} loading={tokenHistoryLoading} error={tokenHistoryError} />

            {/* Search Section */}
            <div className="user-dashboard-search animate__animated animate__fadeInUp">
                <div className="p-4">
                    <h4 className="user-dashboard-section-title mb-4">
                        <FaSearch className="me-2 text-info" />
                        Search Places & Services
                    </h4>
                    <div className="input-group">
                        <input
                            type="text"
                            className="form-control"
                            placeholder="Search for places or services..."
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
                        />
                    </div>
                </div>
            </div>

            {/* Places Section */}
            <div className="user-dashboard-card animate__animated animate__fadeInUp">
                <div className="card-body p-4">
                    <h4 className="user-dashboard-section-title mb-4">
                        <FaBuilding className="me-2 text-info" /> Places
                    </h4>
                    {placesLoading ? (
                        <div className="d-flex justify-content-center">
                            <Spinner animation="border" variant="primary" />
                        </div>
                    ) : filteredPlaces.length === 0 ? (
                        <div className="user-dashboard-empty-alert text-center py-4">
                            <p className="mb-0">No places found.</p>
                        </div>
                    ) : (
                        <>
                            <div className="row">
                                {filteredPlaces.slice(0, visiblePlaces).map((place) => (
                                    <div key={place.id} className="col-md-4 mb-4">
                                        <div className="user-place-card h-100">
                                            {place.imageUrls && place.imageUrls.length > 0 && (
                                                <img
                                                    src={place.imageUrls[0]}
                                                    className="card-img-top"
                                                    alt={place.name}
                                                    style={{ height: "200px", objectFit: "cover" }}
                                                    onError={(e) => { e.target.src = 'https://via.placeholder.com/300x200?text=No+Image'; }}
                                                />
                                            )}
                                            <div className="card-body d-flex flex-column">
                                                <h5 className="card-title">{place.name}</h5>
                                                <p className="card-text">
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
                            {visiblePlaces < filteredPlaces.length && (
                                <div className="user-dashboard-load-more mt-4">
                                    <Button
                                        variant="outline-primary"
                                        onClick={() => setVisiblePlaces(prev => prev + 6)}
                                    >
                                        Load More Places
                                    </Button>
                                </div>
                            )}
                        </>
                    )}
                </div>
            </div>

            {/* Services Section */}
            <div className="user-dashboard-card animate__animated animate__fadeInUp">
                <div className="card-body p-4">
                    <h4 className="user-dashboard-section-title mb-4">
                        <FaList className="me-2 text-info" /> Services
                    </h4>
                    {servicesLoading ? (
                        <div className="d-flex justify-content-center">
                            <Spinner animation="border" variant="primary" />
                        </div>
                    ) : filteredServices.length === 0 ? (
                        <div className="user-dashboard-empty-alert text-center py-4">
                            <p className="mb-0">No services found.</p>
                        </div>
                    ) : (
                        <>
                            <div className="row">
                                {filteredServices.slice(0, visibleServices).map((service) => (
                                    <div key={service.id} className="col-md-6 mb-3">
                                        <div className="user-service-card h-100">
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
                            {visibleServices < filteredServices.length && (
                                <div className="user-dashboard-load-more mt-4">
                                    <Button
                                        variant="outline-primary"
                                        onClick={() => setVisibleServices(prev => prev + 6)}
                                    >
                                        Load More Services
                                    </Button>
                                </div>
                            )}
                        </>
                    )}
                </div>
            </div>
        </div>
    );
};

export default UserDashboard;
