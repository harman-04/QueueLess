//src/components/PlaceDetail.jsx
import React, { useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { useParams, useNavigate } from 'react-router-dom';
import { fetchPlaceById } from '../redux/placeSlice';
import { fetchServicesByPlace } from '../redux/serviceSlice';
import { Card, Button, Spinner, Alert, Row, Col, Badge } from 'react-bootstrap';
import { FaMapMarkerAlt, FaStar, FaClock, FaPhone, FaEnvelope, FaGlobe, FaBusinessTime, FaUsers } from 'react-icons/fa';
import axios from 'axios';
import { toast } from 'react-toastify';

const PlaceDetail = () => {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const { id } = useParams();
  const { currentPlace, loading: placeLoading, error: placeError } = useSelector((state) => state.places);
  const { servicesByPlace, loading: servicesLoading, error: servicesError } = useSelector((state) => state.services);
  const { token, role } = useSelector((state) => state.auth);
  const [joiningQueue, setJoiningQueue] = useState(false);
  const [queues, setQueues] = useState([]);

  const services = servicesByPlace[id] || [];

  useEffect(() => {
    if (id) {
      dispatch(fetchPlaceById(id));
      dispatch(fetchServicesByPlace(id));
    }
  }, [dispatch, id]);

 useEffect(() => {
  const fetchQueues = async () => {
    try {
      const response = await axios.get(`http://localhost:8080/api/queues/by-place/${id}`);
      setQueues(response.data);
    } catch (error) {
      if (error.response?.status === 404) {
        // No queues found, set empty array
        setQueues([]);
      } else {
        console.error("Error fetching queues:", error);
      }
    }
  };

  if (id) {
    fetchQueues();
  }
}, [id]);

  // In PlaceDetail.jsx - Update the handleJoinQueue function
const handleJoinQueue = (serviceId) => {
  if (!token) {
    toast.error("You need to login first to join a queue");
    navigate('/login');
    return;
  }

  const queue = queues.find(q => q.serviceId === serviceId);
  if (!queue) {
    toast.error("No active queue found for this service");
    return;
  }

  // ✅ Only navigate, don't post
  navigate(`/customer/queue/${queue.id}`);
};


  if (placeLoading) {
    return (
      <div className="d-flex justify-content-center align-items-center" style={{ height: '200px' }}>
        <Spinner animation="border" variant="primary" />
      </div>
    );
  }

  if (placeError) {
    return <Alert variant="danger">Error loading place: {placeError.message}</Alert>;
  }

  if (!currentPlace) {
    return <Alert variant="info">Place not found</Alert>;
  }

  const getDayName = (day) => {
    const days = {
      MONDAY: 'Monday',
      TUESDAY: 'Tuesday',
      WEDNESDAY: 'Wednesday',
      THURSDAY: 'Thursday',
      FRIDAY: 'Friday',
      SATURDAY: 'Saturday',
      SUNDAY: 'Sunday'
    };
    return days[day] || day;
  };

  return (
    <div className="container py-4">
      <Row>
        <Col md={8}>
          <Card className="mb-4">
            {currentPlace.imageUrls && currentPlace.imageUrls.length > 0 && (
              <div id="placeCarousel" className="carousel slide">
                <div className="carousel-inner">
                  {currentPlace.imageUrls.map((url, index) => (
                    <div key={index} className={`carousel-item ${index === 0 ? 'active' : ''}`}>
                      <Card.Img 
                        variant="top" 
                        src={url} 
                        style={{ height: '300px', objectFit: 'cover' }}
                        onError={(e) => {
                          e.target.src = 'https://via.placeholder.com/800x300?text=Image+Not+Found';
                        }}
                      />
                    </div>
                  ))}
                </div>
                {currentPlace.imageUrls.length > 1 && (
                  <>
                    <button className="carousel-control-prev" type="button" data-bs-target="#placeCarousel" data-bs-slide="prev">
                      <span className="carousel-control-prev-icon" aria-hidden="true"></span>
                      <span className="visually-hidden">Previous</span>
                    </button>
                    <button className="carousel-control-next" type="button" data-bs-target="#placeCarousel" data-bs-slide="next">
                      <span className="carousel-control-next-icon" aria-hidden="true"></span>
                      <span className="visually-hidden">Next</span>
                    </button>
                  </>
                )}
              </div>
            )}
            <Card.Body>
              <div className="d-flex justify-content-between align-items-start">
                <Card.Title>{currentPlace.name}</Card.Title>
                <Badge bg="primary">{currentPlace.type}</Badge>
              </div>
              <Card.Text>
                <FaMapMarkerAlt className="me-2" />
                {currentPlace.address}
              </Card.Text>
              <Card.Text>{currentPlace.description}</Card.Text>
              
              {currentPlace.rating !== null && (
                <div className="mb-3">
                  <FaStar className="text-warning me-2" />
                  <strong>{currentPlace.rating.toFixed(1)}</strong>
                  <span className="text-muted ms-2">({currentPlace.totalRatings || 0} ratings)</span>
                </div>
              )}
              
              <div className="mb-3">
                <h6>Contact Information</h6>
                {currentPlace.contactInfo?.phone && (
                  <div>
                    <FaPhone className="me-2" />
                    {currentPlace.contactInfo.phone}
                  </div>
                )}
                {currentPlace.contactInfo?.email && (
                  <div>
                    <FaEnvelope className="me-2" />
                    {currentPlace.contactInfo.email}
                  </div>
                )}
                {currentPlace.contactInfo?.website && (
                  <div>
                    <FaGlobe className="me-2" />
                    <a href={currentPlace.contactInfo.website} target="_blank" rel="noopener noreferrer">
                      {currentPlace.contactInfo.website}
                    </a>
                  </div>
                )}
              </div>

              {currentPlace.businessHours && currentPlace.businessHours.length > 0 && (
                <div>
                  <h6><FaBusinessTime className="me-2" />Business Hours</h6>
                  {currentPlace.businessHours.map((hours, index) => (
                    <div key={index} className="d-flex justify-content-between">
                      <span>{getDayName(hours.day)}:</span>
                      <span>{hours.isOpen ? `${hours.openTime} - ${hours.closeTime}` : 'Closed'}</span>
                    </div>
                  ))}
                </div>
              )}
            </Card.Body>
          </Card>
        </Col>

        <Col md={4}>
          <Card>
            <Card.Header>
              <h5>Services</h5>
            </Card.Header>
            <Card.Body>
              {servicesLoading && <Spinner animation="border" size="sm" />}
              {servicesError && <Alert variant="danger">{servicesError.message}</Alert>}
              
              {services.length === 0 && !servicesLoading && (
                <Alert variant="info">No services available</Alert>
              )}

              {services.map(service => {
                const queue = queues.find(q => q.serviceId === service.id);
                const isQueueActive = queue && queue.isActive;
                
                return (
                  <div key={service.id} className="mb-3 p-2 border rounded">
                    <h6>{service.name}</h6>
                    <p className="text-muted small">{service.description}</p>
                    <div className="d-flex justify-content-between align-items-center">
                      <div>
                        <Badge bg="info" className="me-2">
                          <FaClock className="me-1" /> {service.averageServiceTime} mins
                        </Badge>
                        {service.supportsGroupToken && (
                          <Badge bg="success" className="me-2">
                            <FaUsers className="me-1" /> Group
                          </Badge>
                        )}
                        {service.emergencySupport && (
                          <Badge bg="danger">
                            <FaUsers className="me-1" /> Emergency
                          </Badge>
                        )}
                      </div>
                      {role === 'USER' && <Button 
                        size="sm" 
                        onClick={() => handleJoinQueue(service.id)}
                        disabled={!service.isActive || joiningQueue || !isQueueActive}
                      >
                        {joiningQueue ? <Spinner animation="border" size="sm" /> : 
                         !service.isActive ? 'Unavailable' : 
                         !isQueueActive ? 'Queue Closed' : 'Join Queue'}
                      </Button>}
                    </div>
                    {queue && (
                      <div className="mt-2 small text-muted">
                        {queue.tokens && `${queue.tokens.length} people in queue`}
                        {queue.estimatedWaitTime && ` • ~${queue.estimatedWaitTime} min wait`}
                      </div>
                    )}
                  </div>
                );
              })}

              {role === 'ADMIN' && currentPlace && currentPlace.adminId && (
  currentPlace.adminId.toString() === localStorage.getItem("userId") && (
    <Button 
      variant="outline-primary" 
      className="w-100 mt-3"
      onClick={() => navigate(`/admin/places/${currentPlace.id}/services`)}
    >
      Manage Services
    </Button>
  )
)}

            </Card.Body>
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default PlaceDetail;