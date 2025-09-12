// Enhanced AdminDashboard.jsx with beautiful UI
import React, { useState, useEffect } from 'react';
import {
  Container, Row, Col, Card, Table, Spinner, Alert, Tabs, Tab,
  Badge, Button, Modal, Form, InputGroup
} from 'react-bootstrap';
import {
  FaBuilding, FaList, FaUsers, FaCheckCircle, FaMoneyBill, FaChartLine,
  FaUserTie, FaCog, FaEye, FaPauseCircle, FaPlayCircle, FaSearch,
  FaFilter, FaPlus, FaEdit, FaTrash, FaSync, FaDownload, FaTimes
} from 'react-icons/fa';
import axiosInstance from '../utils/axiosInstance';
import { toast } from 'react-toastify';
import './AdminDashboard.css';
import { useNavigate } from 'react-router-dom';

const AdminDashboard = () => {
  const [stats, setStats] = useState(null);
  const [payments, setPayments] = useState([]);
  const [providers, setProviders] = useState([]);
  const [queues, setQueues] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [activeTab, setActiveTab] = useState('stats');
  const [searchTerm, setSearchTerm] = useState('');
  const [filterStatus, setFilterStatus] = useState('all');
  const [showProviderModal, setShowProviderModal] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    fetchDashboardData();
  }, []);

  const fetchDashboardData = async () => {
    try {
      setLoading(true);
      const [statsResponse, paymentsResponse, providersResponse, queuesResponse] = await Promise.all([
        axiosInstance.get('/admin/stats'),
        axiosInstance.get('/admin/payments/enhanced'),
        axiosInstance.get('/admin/providers'),
        axiosInstance.get('/admin/queues/enhanced')
      ]);

      setStats(statsResponse.data);
      setPayments(paymentsResponse.data);
      setProviders(providersResponse.data);
      setQueues(queuesResponse.data);
    } catch (err) {
      setError('Failed to load dashboard data');
      toast.error('Failed to load dashboard data');
    } finally {
      setLoading(false);
    }
  };

  const formatCurrency = (amount) => {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR'
    }).format(amount / 100);
  };

  const formatDate = (dateString) => {
    return new Date(dateString).toLocaleDateString('en-IN', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const handleQueueStatusToggle = async (queueId, currentStatus) => {
    try {
      const endpoint = currentStatus ?
        `/queues/${queueId}/deactivate` :
        `/queues/${queueId}/activate`;

      await axiosInstance.put(endpoint);
      toast.success(`Queue ${currentStatus ? 'paused' : 'resumed'} successfully!`);
      fetchDashboardData();
    } catch (error) {
      toast.error('Failed to update queue status');
    }
  };

  const filteredQueues = queues.filter(queue => {
    const matchesSearch = queue.serviceName.toLowerCase().includes(searchTerm.toLowerCase()) ||
      queue.placeName.toLowerCase().includes(searchTerm.toLowerCase()) ||
      queue.providerName.toLowerCase().includes(searchTerm.toLowerCase());

    const matchesStatus = filterStatus === 'all' ||
      (filterStatus === 'active' && queue.isActive) ||
      (filterStatus === 'inactive' && !queue.isActive);

    return matchesSearch && matchesStatus;
  });

  if (loading) {
    return (
      <Container className="admin-dashboard loading">
        <div className="loading-content">
          <Spinner animation="border" variant="primary" />
          <p className="mt-3 text-muted">Loading dashboard data...</p>
        </div>
      </Container>
    );
  }

  if (error) {
    return (
      <Container className="admin-dashboard">
        <Alert variant="danger" className="error-alert">
          {error}
          <Button variant="outline-primary" onClick={fetchDashboardData} className="ms-3">
            <FaSync /> Retry
          </Button>
        </Alert>
      </Container>
    );
  }

  return (
    <Container fluid className="admin-dashboard-container">
      <div className="admin-dashboard">
        <div className="dashboard-header">
          <h1 className="dashboard-title">Admin Dashboard ⚡</h1>
          <div className="header-actions">
            <Button variant="outline-light" onClick={fetchDashboardData}>
              <FaSync /> Refresh
            </Button>
          </div>
        </div>

        <Tabs activeKey={activeTab} onSelect={(k) => setActiveTab(k)} className="dashboard-tabs">
          <Tab eventKey="stats" title={
            <span><FaChartLine className="me-2" /> Overview</span>
          }>
            {/* Statistics Cards */}
            {stats && (
              <Row className="stats-grid">
                <Col xs={12} sm={6} lg={3} className="mb-4">
                  <Card className="stat-card">
                    <Card.Body className="d-flex align-items-center">
                      <div className="stat-icon me-3">
                        <FaBuilding className="text-primary" />
                      </div>
                      <div className="stat-content">
                        <h3>{stats.totalPlaces || 0}</h3>
                        <p className="text-muted mb-0">Total Places</p>
                      </div>
                    </Card.Body>
                  </Card>
                </Col>

                <Col xs={12} sm={6} lg={3} className="mb-4">
                  <Card className="stat-card">
                    <Card.Body className="d-flex align-items-center">
                      <div className="stat-icon me-3">
                        <FaList className="text-success" />
                      </div>
                      <div className="stat-content">
                        <h3>{stats.totalQueues || 0}</h3>
                        <p className="text-muted mb-0">Total Queues</p>
                      </div>
                    </Card.Body>
                  </Card>
                </Col>

                <Col xs={12} sm={6} lg={3} className="mb-4">
                  <Card className="stat-card">
                    <Card.Body className="d-flex align-items-center">
                      <div className="stat-icon me-3">
                        <FaUsers className="text-warning" />
                      </div>
                      <div className="stat-content">
                        <h3>{stats.providerCount || 0}</h3>
                        <p className="text-muted mb-0">Providers</p>
                      </div>
                    </Card.Body>
                  </Card>
                </Col>

                <Col xs={12} sm={6} lg={3} className="mb-4">
                  <Card className="stat-card">
                    <Card.Body className="d-flex align-items-center">
                      <div className="stat-icon me-3">
                        <FaCheckCircle className="text-info" />
                      </div>
                      <div className="stat-content">
                        <h3>{stats.tokensServedToday || 0}</h3>
                        <p className="text-muted mb-0">Today's Tokens</p>
                      </div>
                    </Card.Body>
                  </Card>
                </Col>
              </Row>
            )}

            {/* Quick Actions */}
            <Card className="mb-4 dashboard-card">
              <Card.Header className="card-header-styled">
                <h5 className="mb-0">Quick Actions</h5>
              </Card.Header>
              <Card.Body>
                <Row>
                  <Col xs={6} md={3} className="text-center mb-3">
                    <Button variant="light" className="action-btn" onClick={() => navigate('/places/new')}>
                      <FaPlus size={24} className="mb-2 text-primary" />
                      <div>Add Place</div>
                    </Button>
                  </Col>
                  <Col xs={6} md={3} className="text-center mb-3">
                    <Button variant="light" className="action-btn" onClick={() => setActiveTab('providers')}>
                      <FaUserTie size={24} className="mb-2 text-success" />
                      <div>Manage Providers</div>
                    </Button>
                  </Col>
                  <Col xs={6} md={3} className="text-center mb-3">
                    <Button variant="light" className="action-btn" onClick={() => navigate('/provider-pricing')}>
                      <FaMoneyBill size={24} className="mb-2 text-info" />
                      <div>Buy Tokens</div>
                    </Button>
                  </Col>
                  <Col xs={6} md={3} className="text-center mb-3">
                    <Button variant="light" className="action-btn">
                      <FaDownload size={24} className="mb-2 text-secondary" />
                      <div>Export Data</div>
                    </Button>
                  </Col>
                </Row>
              </Card.Body>
            </Card>
          </Tab>

          <Tab eventKey="queues" title={
            <span><FaList className="me-2" /> Queues</span>
          }>
            <Card className="dashboard-card">
              <Card.Header className="d-flex flex-column flex-md-row justify-content-between align-items-md-center card-header-styled">
                <h5 className="mb-2 mb-md-0">All Queues</h5>
                <div className="d-flex flex-column flex-sm-row">
                  <InputGroup className="me-sm-2 mb-2 mb-sm-0">
                    <InputGroup.Text><FaSearch /></InputGroup.Text>
                    <Form.Control
                      type="text"
                      placeholder="Search queues..."
                      value={searchTerm}
                      onChange={(e) => setSearchTerm(e.target.value)}
                    />
                  </InputGroup>
                  <Form.Select
                    value={filterStatus}
                    onChange={(e) => setFilterStatus(e.target.value)}
                  >
                    <option value="all">All Status</option>
                    <option value="active">Active</option>
                    <option value="inactive">Inactive</option>
                  </Form.Select>
                </div>
              </Card.Header>
              <Card.Body className="p-0">
                {filteredQueues.length === 0 ? (
                  <div className="text-center py-5">
                    <FaList size={48} className="text-muted mb-3" />
                    <p className="text-muted">No queues found matching your criteria.</p>
                  </div>
                ) : (
                  <div className="table-responsive">
                    <Table hover className="queues-table mb-0">
                      <thead>
                        <tr>
                          <th>Service</th>
                          <th>Place</th>
                          <th>Provider</th>
                          <th>Status</th>
                          <th>Waiting</th>
                          <th>In Service</th>
                          <th>Completed</th>
                          <th>Wait Time</th>
                          <th>Actions</th>
                        </tr>
                      </thead>
                      <tbody>
                        {filteredQueues.map((queue) => (
                          <tr key={queue.id}>
                            <td className="fw-semibold">{queue.serviceName}</td>
                            <td>{queue.placeName}</td>
                            <td>{queue.providerName}</td>
                            <td>
                              <Badge pill bg={queue.isActive ? 'success' : 'secondary'}>
                                {queue.isActive ? 'Active' : 'Inactive'}
                              </Badge>
                            </td>
                            <td>
                              <Badge pill bg="warning">{queue.waitingTokens}</Badge>
                            </td>
                            <td>
                              <Badge pill bg="info">{queue.inServiceTokens}</Badge>
                            </td>
                            <td>
                              <Badge pill bg="success">{queue.completedTokens}</Badge>
                            </td>
                            <td>{queue.estimatedWaitTime} min</td>
                            <td>
                              <Button
                                size="sm"
                                variant={queue.isActive ? 'warning' : 'success'}
                                className="me-2"
                                onClick={() => handleQueueStatusToggle(queue.id, queue.isActive)}
                              >
                                {queue.isActive ? <FaPauseCircle /> : <FaPlayCircle />}
                              </Button>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </Table>
                  </div>
                )}
              </Card.Body>
            </Card>
          </Tab>

          <Tab eventKey="payments" title={
            <span><FaMoneyBill className="me-2" /> Payments</span>
          }>
            <Card className="dashboard-card">
              <Card.Header className="d-flex justify-content-between align-items-center card-header-styled">
                <h5 className="mb-0">Payment History</h5>
                <Badge bg="primary" className="text-white">{payments.length} transactions</Badge>
              </Card.Header>
              <Card.Body className="p-0">
                {payments.length === 0 ? (
                  <div className="text-center py-5">
                    <FaMoneyBill size={48} className="text-muted mb-3" />
                    <p className="text-muted">No payment history found.</p>
                  </div>
                ) : (
                  <div className="table-responsive">
                    <Table hover className="payments-table mb-0">
                      <thead>
                        <tr>
                          <th>Date</th>
                          <th>Description</th>
                          <th>Amount</th>
                          <th>Type</th>
                          <th>Status</th>
                          <th>Reference</th>
                        </tr>
                      </thead>
                      <tbody>
                        {payments.map((payment) => (
                          <tr key={payment.id}>
                            <td>{formatDate(payment.createdAt)}</td>
                            <td>
                              {payment.role === 'PROVIDER'
                                ? `Provider token for ${payment.createdForEmail}`
                                : `Admin token for ${payment.createdForEmail}`
                              }
                            </td>
                            <td className="fw-semibold">{formatCurrency(payment.amount)}</td>
                            <td>
                              <Badge pill bg={payment.role === 'ADMIN' ? 'primary' : 'info'}>
                                {payment.role}
                              </Badge>
                            </td>
                            <td>
                              <Badge pill bg={payment.isPaid ? 'success' : 'warning'}>
                                {payment.isPaid ? 'Completed' : 'Pending'}
                              </Badge>
                            </td>
                            <td className="text-muted">{payment.razorpayOrderId}</td>
                          </tr>
                        ))}
                      </tbody>
                    </Table>
                  </div>
                )}
              </Card.Body>
            </Card>
          </Tab>

          <Tab eventKey="providers" title={
            <span><FaUserTie className="me-2" /> Providers</span>
          }>
            <Card className="dashboard-card">
              <Card.Header className="d-flex flex-column flex-md-row justify-content-between align-items-md-center card-header-styled">
                <h5 className="mb-2 mb-md-0">Provider Management</h5>
                <Button variant="primary" size="sm" onClick={() => setShowProviderModal(true)}>
                  <FaPlus className="me-2" /> Add Provider
                </Button>
              </Card.Header>
              <Card.Body className="p-4">
                {providers.length === 0 ? (
                  <div className="text-center py-5">
                    <FaUserTie size={48} className="text-muted mb-3" />
                    <p className="text-muted">No providers found.</p>
                  </div>
                ) : (
                  <Row>
                    {providers.map((providerData) => {
                      const provider = providerData.provider;
                      const stats = providerData.stats;

                      return (
                        <Col xs={12} md={6} lg={4} key={provider.id} className="mb-4">
                          <Card className="provider-card-styled">
                            <Card.Body>
                              <div className="d-flex align-items-center mb-3">
                                <div className="provider-avatar me-3">
                                  {provider.name.charAt(0).toUpperCase()}
                                </div>
                                <div className="flex-grow-1">
                                  <h6 className="mb-0 fw-bold">{provider.name}</h6>
                                  <small className="text-muted">{provider.email}</small>
                                </div>
                              </div>

                              <div className="provider-stats-grid mb-3">
                                <div className="stat-item">
                                  <span className="value text-primary">{stats.totalQueues}</span>
                                  <span className="label">Queues</span>
                                </div>
                                <div className="stat-item">
                                  <span className="value text-success">{stats.activeQueues}</span>
                                  <span className="label">Active</span>
                                </div>
                                <div className="stat-item">
                                  <span className="value text-info">{stats.tokensServedToday}</span>
                                  <span className="label">Today</span>
                                </div>
                              </div>

                              <div className="d-flex justify-content-end">
                                <Button variant="outline-primary" size="sm" className="me-2">
                                  <FaEye /> View
                                </Button>
                                <Button variant="outline-secondary" size="sm">
                                  <FaCog /> Manage
                                </Button>
                              </div>
                            </Card.Body>
                          </Card>
                        </Col>
                      );
                    })}
                  </Row>
                )}
              </Card.Body>
            </Card>
          </Tab>
        </Tabs>

        {/* Add Provider Modal */}
        <Modal show={showProviderModal} onHide={() => setShowProviderModal(false)} size="lg" centered>
          <Modal.Header className="border-0 modal-header-styled">
            <Modal.Title className="fw-bold">Add New Provider</Modal.Title>
            <Button variant="light" onClick={() => setShowProviderModal(false)}><FaTimes /></Button>
          </Modal.Header>
          <Modal.Body>
            <Form>
              <Row>
                <Col md={6}>
                  <Form.Group className="mb-3">
                    <Form.Label>Name</Form.Label>
                    <Form.Control type="text" placeholder="Enter provider name" />
                  </Form.Group>
                </Col>
                <Col md={6}>
                  <Form.Group className="mb-3">
                    <Form.Label>Email</Form.Label>
                    <Form.Control type="email" placeholder="Enter provider email" />
                  </Form.Group>
                </Col>
              </Row>
              <Form.Group className="mb-3">
                <Form.Label>Phone Number</Form.Label>
                <Form.Control type="tel" placeholder="Enter phone number" />
              </Form.Group>
              <Form.Group className="mb-3">
                <Form.Label>Assign Places</Form.Label>
                <Form.Select multiple>
                  <option>Place 1</option>
                  <option>Place 2</option>
                  <option>Place 3</option>
                </Form.Select>
              </Form.Group>
            </Form>
          </Modal.Body>
          <Modal.Footer className="border-0">
            <Button variant="secondary" onClick={() => setShowProviderModal(false)}>
              Cancel
            </Button>
            <Button variant="primary" onClick={() => setShowProviderModal(false)}>
              Add Provider
            </Button>
          </Modal.Footer>
        </Modal>
      </div>
    </Container>
  );
};

export default AdminDashboard;