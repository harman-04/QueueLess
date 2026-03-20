// HowToUse.jsx
import React from 'react';
import { Container, Row, Col, Card, Accordion, Badge, ListGroup, Alert } from 'react-bootstrap';
import {
  FaUser, FaUserTie, FaUserShield, FaSearch, FaMapMarkerAlt, FaStar,
  FaClock, FaUsers, FaAmbulance, FaBell, FaChartLine, FaFileExport,
  FaQrcode, FaHeart, FaComment, FaUserCheck, FaPlusCircle, FaTrashAlt,
  FaEdit, FaEye, FaKey, FaCog, FaPlayCircle, FaMoneyBill, FaLightbulb
} from 'react-icons/fa';
import './HowToUse.css';

const HowToUse = () => {
  return (
    <div className="how-to-use-page">
      <Container className="py-5">
        {/* Header */}
        <div className="text-center mb-5">
          <h1 className="display-4 fw-bold gradient-text">How to Use QueueLess</h1>
          <p className="lead text-muted">A complete guide for users, providers, and administrators</p>
        </div>

        {/* Role Selection Cards */}
        <Row className="mb-5 g-4">
          <Col md={4}>
            <Card className="role-card h-100 text-center">
              <Card.Body>
                <div className="role-icon user-icon mb-3">
                  <FaUser size={48} />
                </div>
                <Card.Title className="h3">For Users</Card.Title>
                <Card.Text>
                  Join queues, track your position, receive notifications, and provide feedback.
                </Card.Text>
              </Card.Body>
            </Card>
          </Col>
          <Col md={4}>
            <Card className="role-card h-100 text-center">
              <Card.Body>
                <div className="role-icon provider-icon mb-3">
                  <FaUserTie size={48} />
                </div>
                <Card.Title className="h3">For Providers</Card.Title>
                <Card.Text>
                  Manage queues, serve tokens, handle emergencies, and view analytics.
                </Card.Text>
              </Card.Body>
            </Card>
          </Col>
          <Col md={4}>
            <Card className="role-card h-100 text-center">
              <Card.Body>
                <div className="role-icon admin-icon mb-3">
                  <FaUserShield size={48} />
                </div>
                <Card.Title className="h3">For Administrators</Card.Title>
                <Card.Text>
                  Create places and services, manage providers, and oversee the entire system.
                </Card.Text>
              </Card.Body>
            </Card>
          </Col>
        </Row>

        {/* Accordion Guides */}
        <Accordion defaultActiveKey="0" className="custom-accordion">
          <Accordion.Item eventKey="0">
            <Accordion.Header>
              <FaUser className="me-2 text-primary" /> User Guide
            </Accordion.Header>
            <Accordion.Body>
              <Row>
                <Col md={6}>
                  <h5><FaSearch className="me-2" /> Finding a Place</h5>
                  <p>Use the search bar or advanced search to find places by name, type, or location. Filter by rating, wait time, or features like group/emergency support.</p>
                  <h5><FaMapMarkerAlt className="me-2" /> Viewing Place Details</h5>
                  <p>Click on a place to see its services, contact info, business hours, and user ratings. You can also add it to your favorites (<FaHeart />).</p>
                  <h5><FaUsers className="me-2" /> Joining a Queue</h5>
                  <p>Choose a service and click “Join Queue”. You can select:</p>
                  <ul>
                    <li><strong>Regular Token</strong> – standard waiting.</li>
                    <li><strong>Group Token</strong> – for multiple people (if supported).</li>
                    <li><strong>Emergency Token</strong> – for urgent needs (if supported, may require provider approval).</li>
                  </ul>
                  <p>You can also provide additional details (purpose, condition) when joining.</p>
                  <h5><FaClock className="me-2" /> Tracking Your Token</h5>
                  <p>After joining, you'll see your token ID, position in the queue, and estimated wait time. You'll receive email and push notifications when your turn is approaching.</p>
                  <h5><FaAmbulance className="me-2" /> Emergency Token</h5>
                  <p>If the queue supports emergency tokens, you can request priority. The provider will review your details and approve/reject.</p>
                </Col>
                <Col md={6}>
                  <h5><FaBell className="me-2" /> Notifications</h5>
                  <p>Manage your notification preferences per queue from your dashboard. You can choose to be notified:</p>
                  <ul>
                    <li>Minutes before your turn</li>
                    <li>On status changes (e.g., when token is served)</li>
                    <li>When the queue becomes short (best time to join)</li>
                  </ul>
                  <h5><FaStar className="me-2" /> Providing Feedback</h5>
                  <p>After your token is completed, you'll be prompted to rate your experience (overall, staff, service, wait time) and leave a comment.</p>
                  <h5><FaHeart className="me-2" /> Favorites</h5>
                  <p>Click the heart icon on any place to add it to your favorites. Quickly access them from your dashboard.</p>
                  <h5><FaUser className="me-2" /> User Dashboard</h5>
                  <p>Your dashboard shows active queues, token history, favorite places, and analytics charts. You can also edit your profile and upload a profile picture.</p>
                  <Alert variant="info" className="mt-3">
                    <FaQrcode className="me-2" /> <strong>QR Code Scanning:</strong> Scan a queue's QR code to join instantly.
                  </Alert>
                </Col>
              </Row>
            </Accordion.Body>
          </Accordion.Item>

          <Accordion.Item eventKey="1">
            <Accordion.Header>
              <FaUserTie className="me-2 text-success" /> Provider Guide
            </Accordion.Header>
            <Accordion.Body>
              <Row>
                <Col md={6}>
                  <h5><FaPlusCircle className="me-2" /> Creating a Queue</h5>
                  <p>From the “My Queues” page, select a place and service, set a name, max capacity, and optional features (group tokens, emergency support).</p>
                  <h5><FaPlayCircle className="me-2" /> Managing a Queue</h5>
                  <p>On your queue dashboard, you can:</p>
                  <ul>
                    <li><strong>Serve Next:</strong> Moves the next waiting token to “in service”.</li>
                    <li><strong>Complete:</strong> Marks the current token as completed (after service).</li>
                    <li><strong>Cancel:</strong> Remove a waiting token (optionally provide a reason).</li>
                    <li><strong>Reorder:</strong> Drag and drop waiting tokens to change priority.</li>
                    <li><strong>Pause/Resume:</strong> Temporarily stop accepting new tokens.</li>
                    <li><strong>Reset Queue:</strong> Clear all tokens (optionally export data first).</li>
                  </ul>
                  <h5><FaAmbulance className="me-2" /> Emergency Approvals</h5>
                  <p>When a user requests an emergency token, you'll receive a notification. You can approve (with priority) or reject (with reason).</p>
                  <h5><FaEye className="me-2" /> Viewing User Details</h5>
                  <p>For any token, click “View Details” to see the user's provided information (purpose, condition, notes) – respecting their privacy settings.</p>
                </Col>
                <Col md={6}>
                  <h5><FaChartLine className="me-2" /> Analytics</h5>
                  <p>Access token volume charts, busiest hours, and average wait time trends. These help you optimize your service.</p>
                  <h5><FaFileExport className="me-2" /> Export Reports</h5>
                  <p>Export queue data as PDF or Excel (tokens only, statistics, or full report). You can also export data before resetting a queue.</p>
                  <h5><FaQrcode className="me-2" /> QR Code Generation</h5>
                  <p>Generate a QR code for your queue. Display it in your physical location so users can scan and join instantly.</p>
                  <h5><FaUserCheck className="me-2" /> Provider Dashboard</h5>
                  <p>Your dashboard shows all your queues, live statistics, and recent activity. You can also manage your profile and upload a profile picture.</p>
                  <Alert variant="warning" className="mt-3">
                    <strong>Note:</strong> Your admin may assign you to specific places. You'll only see queues for those places.
                  </Alert>
                </Col>
              </Row>
            </Accordion.Body>
          </Accordion.Item>

          <Accordion.Item eventKey="2">
            <Accordion.Header>
              <FaUserShield className="me-2 text-danger" /> Admin Guide
            </Accordion.Header>
            <Accordion.Body>
              <Row>
                <Col md={6}>
                  <h5><FaPlusCircle className="me-2" /> Creating a Place</h5>
                  <p>Add places with name, type, address, location (latitude/longitude), contact info, business hours, and images.</p>
                  <h5><FaCog className="me-2" /> Managing Services</h5>
                  <p>Under each place, define services with average service time, group/emergency support flags.</p>
                  <h5><FaUserTie className="me-2" /> Provider Management</h5>
                  <p>Admins can purchase provider tokens (1 month, 1 year, lifetime) and assign them to new providers. You can also:</p>
                  <ul>
                    <li><strong>View provider details:</strong> See their profile, assigned places, queue statistics, ratings.</li>
                    <li><strong>Edit provider:</strong> Update name, email, phone, assigned places.</li>
                    <li><strong>Toggle status:</strong> Enable/disable a provider's account.</li>
                    <li><strong>Reset password:</strong> Send a password reset link to the provider's email.</li>
                  </ul>
                  <h5><FaChartLine className="me-2" /> Analytics</h5>
                  <p>View global token volume trends, busiest hours, and provider performance leaderboards.</p>
                  <h5><FaFileExport className="me-2" /> Reports</h5>
                  <p>Generate comprehensive PDF/Excel reports covering all places, queues, and aggregated statistics.</p>
                </Col>
                <Col md={6}>
                  <h5><FaBell className="me-2" /> Alerts</h5>
                  <p>Configure email alerts to be notified when any queue exceeds a wait time threshold.</p>
                  <h5><FaMapMarkerAlt className="me-2" /> Geographic Heat Map</h5>
                  <p>View all your places on a map, with markers sized by queue load – useful for spotting busy locations.</p>
                  <h5><FaMoneyBill className="me-2" /> Payment History</h5>
                  <p>Track all payments made for admin and provider tokens.</p>
                  <h5><FaKey className="me-2" /> Admin Tokens</h5>
                  <p>Admins must purchase an admin token (via Razorpay) to register. Tokens are valid for 1 month, 1 year, or lifetime.</p>
                  <Alert variant="info" className="mt-3">
                    <strong>Tip:</strong> Use the “Make Providers” button to bulk‑purchase provider tokens for multiple providers at once.
                  </Alert>
                </Col>
              </Row>
            </Accordion.Body>
          </Accordion.Item>
        </Accordion>

        {/* Pro Tips */}
        <Card className="mt-5 pro-tips-card">
          <Card.Body>
            <h5 className="mb-3"><FaLightbulb className="me-2 text-warning" /> Pro Tips</h5>
            <Row>
              <Col md={4}>
                <ul className="list-unstyled">
                  <li className="mb-2"><FaBell className="me-2 text-primary" /> Enable push notifications in your browser to never miss your turn.</li>
                  <li className="mb-2"><FaClock className="me-2 text-primary" /> Use the “Best Time to Join” feature to avoid peak hours.</li>
                </ul>
              </Col>
              <Col md={4}>
                <ul className="list-unstyled">
                  <li className="mb-2"><FaQrcode className="me-2 text-success" /> Providers: Display the QR code in your shop for quick user access.</li>
                  <li className="mb-2"><FaChartLine className="me-2 text-success" /> Admins: Regularly review provider performance to ensure quality service.</li>
                </ul>
              </Col>
              <Col md={4}>
                <ul className="list-unstyled">
                  <li className="mb-2"><FaUser className="me-2 text-danger" /> All users: Update your profile picture for a personalized experience.</li>
                  <li className="mb-2"><FaStar className="me-2 text-danger" /> Check your token history to see past visits and ratings.</li>
                </ul>
              </Col>
            </Row>
          </Card.Body>
        </Card>
      </Container>
    </div>
  );
};

export default HowToUse;