// src/pages/AdminDashboard.jsx
import React, { useState, useEffect } from 'react';
import {
  Container, Row, Col, Card, Table, Spinner, Alert, Tabs, Tab,
  Badge, Button, Form, InputGroup
} from 'react-bootstrap';
import {
  FaBuilding, FaList, FaUsers, FaCheckCircle, FaMoneyBill, FaChartLine,
  FaUserTie, FaCog, FaEye, FaPauseCircle, FaPlayCircle, FaSearch,
  FaFilter, FaPlus, FaEdit, FaTrash, FaSync, FaDownload, FaTimes, FaMapMarkerAlt, FaFilePdf, FaFileExcel, FaBell
} from 'react-icons/fa';
import axiosInstance from '../utils/axiosInstance';
import { toast } from 'react-toastify';
import { useNavigate } from 'react-router-dom';
import './AdminDashboard.css';
import './AnalyticsSkeleton.css';
import { useDispatch, useSelector } from 'react-redux';
import {
  fetchTokensOverTime,
  fetchBusiestHours,
} from '../redux/adminAnalyticsSlice';
import TokenVolumeChart from '../components/TokenVolumeChart';
import BusiestHoursChart from '../components/BusiestHoursChart';
import ProviderLeaderboard from '../components/ProviderLeaderboard';
import AlertConfigModal from '../components/AlertConfigModal';
import PlaceMap from '../components/PlaceMap';
import QueuesTableSkeleton from '../components/QueuesTableSkeleton'; // <-- import skeleton

const AdminDashboard = () => {
  const [stats, setStats] = useState(null);
  const [payments, setPayments] = useState([]);
  const [providers, setProviders] = useState([]);
  const [queues, setQueues] = useState([]);
  const [loading, setLoading] = useState(true);
  const [queuesLoading, setQueuesLoading] = useState(false); // separate for queues tab
  const [error, setError] = useState(null);
  const [activeTab, setActiveTab] = useState('stats');
  const [searchTerm, setSearchTerm] = useState('');
  const [filterStatus, setFilterStatus] = useState('all');
  const [showAlertModal, setShowAlertModal] = useState(false);
  const navigate = useNavigate();
  const dispatch = useDispatch();
  const { tokensOverTime, busiestHours, loading: analyticsLoading, error: analyticsError } = useSelector((state) => state.adminAnalytics);

  useEffect(() => {
    if (activeTab === 'analytics') {
      dispatch(fetchTokensOverTime(30));
      dispatch(fetchBusiestHours());
    }
  }, [activeTab, dispatch]);

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
      setQueuesLoading(false);
    }
  };

  const fetchQueuesData = async () => {
    setQueuesLoading(true);
    try {
      const response = await axiosInstance.get('/admin/queues/enhanced');
      setQueues(response.data);
    } catch (err) {
      toast.error('Failed to refresh queues');
    } finally {
      setQueuesLoading(false);
    }
  };

  const handleTabChange = (tab) => {
    setActiveTab(tab);
    if (tab === 'queues' && queues.length === 0) {
      // Optionally refresh queues when switching to the tab
      // fetchQueuesData();
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
      fetchQueuesData(); // refresh only queues
    } catch (error) {
      toast.error('Failed to update queue status');
    }
  };

  const handleExportReport = async (format) => {
    try {
      const response = await axiosInstance.get(`/admin/report/${format}`, {
        responseType: 'blob'
      });
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      const filename = `admin-report-${new Date().toISOString().slice(0, 10)}.${format === 'pdf' ? 'pdf' : 'xlsx'}`;
      link.setAttribute('download', filename);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
      toast.success('Report downloaded successfully!');
    } catch (error) {
      toast.error('Failed to download report');
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
    <Container fluid className="admin-dashboard-container  ">
      <div className="admin-dashboard">
        <div className="dashboard-header">
          <h1 className="dashboard-title">Admin Dashboard ⚡</h1>
          <div className="header-actions">
            <Button variant="outline-light" onClick={fetchDashboardData}>
              <FaSync /> Refresh
            </Button>
          </div>
        </div>

        <Tabs activeKey={activeTab} onSelect={handleTabChange} className="dashboard-tabs ">
          <Tab className='mt-4' eventKey="stats" title={<span><FaChartLine className="me-2" /> Overview</span>}>


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
                  <Col xs={6} md={2} className="text-center mb-3">
                    <Button variant="light" className="action-btn" onClick={() => navigate('/places/new')}>
                      <FaPlus size={24} className="mb-2 text-primary" />
                      <div>Add Place</div>
                    </Button>
                  </Col>
                  <Col xs={6} md={2} className="text-center mb-3">
                    <Button variant="light" className="action-btn" onClick={() => setActiveTab('providers')}>
                      <FaUserTie size={24} className="mb-2 text-success" />
                      <div>Manage Providers</div>
                    </Button>
                  </Col>
                  <Col xs={6} md={2} className="text-center mb-3">
                    <Button variant="light" className="action-btn" onClick={() => navigate('/provider-pricing')}>
                      <FaMoneyBill size={24} className="mb-2 text-info" />
                      <div>Buy Tokens</div>
                    </Button>
                  </Col>
                  <Col xs={6} md={2} className="text-center mb-3">
                    <Button variant="light" className="action-btn" onClick={() => handleExportReport('pdf')}>
                      <FaFilePdf size={24} className="mb-2 text-danger" />
                      <div>Export PDF</div>
                    </Button>
                  </Col>
                  <Col xs={6} md={2} className="text-center mb-3">
                    <Button variant="light" className="action-btn" onClick={() => handleExportReport('excel')}>
                      <FaFileExcel size={24} className="mb-2 text-success" />
                      <div>Export Excel</div>
                    </Button>
                  </Col>

                  <Col xs={6} md={2} className="text-center mb-3">
                    <Button variant="light" className="action-btn" onClick={() => setShowAlertModal(true)}>
                      <FaBell size={24} className="mb-2 text-warning" />
                      <div>Alert Config</div>
                    </Button>
                  </Col>


                </Row>
              </Card.Body>
            </Card>
          </Tab>

          <Tab eventKey="queues" title={<span><FaList className="me-2" /> Queues</span>}>
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
                {queuesLoading ? (
                  <QueuesTableSkeleton />
                ) : filteredQueues.length === 0 ? (
                  <div className="text-center py-5">
                    <FaList size={48} className="text-muted mb-3" />
                    <p className="text-muted">No queues found matching your criteria.</p>
                  </div>
                ) : (
                  <div className="table-responsive">
                    <Table hover className="queues-table mb-0 mt-4">
                      {/* table headers and rows as before */}
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
                    <Table hover className="payments-table mb-0 mt-4">
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
                            <td>{payment.description}</td>
                            <td className="fw-semibold">{formatCurrency(payment.amount)}</td>
                            <td>
                              <Badge pill bg={payment.role === 'ADMIN' ? 'primary' : 'info'}>
                                {payment.role}
                              </Badge>
                            </td>
                            <td>
                              <Badge pill bg={payment.paid ? 'success' : 'warning'}>
                                {payment.paid ? 'Completed' : 'Pending'}
                              </Badge>
                            </td>
                            <td className="text-muted">{payment.reference}</td>
                          </tr>
                        ))}
                      </tbody>
                    </Table>
                  </div>
                )}
              </Card.Body>
            </Card>
          </Tab>
          <Tab eventKey="providers" title={<span><FaUserTie className="me-2" /> Providers</span>}>
            <Card className="dashboard-card">
              <Card.Header className="d-flex flex-column flex-md-row justify-content-between align-items-md-center card-header-styled">
                <h5 className="mb-2 mb-md-0">Provider Management</h5>
                <Button variant="primary" size="sm" onClick={() => navigate('/provider-pricing')}>
                  <FaPlus className="me-2" /> Buy Provider Tokens
                </Button>
              </Card.Header>
              <Card.Body className="p-4">
                <ProviderLeaderboard providers={providers} loading={loading} error={error} />
              </Card.Body>
            </Card>
          </Tab>

          <Tab eventKey="analytics" title={<span><FaChartLine className="me-2" /> Analytics</span>}>
            <Row>
              <Col md={12}>
                {analyticsLoading ? (
                  <div className="skeleton-box analytics-skeleton-card animate__animated animate__fadeIn">
                    <div className="d-flex h-100 align-items-center justify-content-center">
                      <Spinner animation="grow" variant="primary" size="lg" />
                      <span className="ms-3 fw-bold text-primary">Analyzing Token Volume...</span>
                    </div>
                  </div>
                ) : (
                  <TokenVolumeChart data={tokensOverTime} error={analyticsError} />
                )}
              </Col>
            </Row>

            <Row>
              <Col md={12}>
                {analyticsLoading ? (
                  <div className="skeleton-box analytics-skeleton-card animate__animated animate__fadeIn">
                    <div className="d-flex h-100 align-items-center justify-content-center">
                      <Spinner animation="grow" variant="info" size="lg" />
                      <span className="ms-3 fw-bold text-info">Mapping Busiest Hours...</span>
                    </div>
                  </div>
                ) : (
                  <BusiestHoursChart data={busiestHours} error={analyticsError} />
                )}
              </Col>
            </Row>
          </Tab>

          <Tab eventKey="map" title={<span><FaMapMarkerAlt className="me-2" /> Heat Map</span>}>
            {activeTab === 'map' && <PlaceMap />}
          </Tab>
        </Tabs>

        {/* REMOVED: The Add Provider Modal */}
        <AlertConfigModal
          show={showAlertModal}
          onHide={() => setShowAlertModal(false)}
          onConfigChanged={() => { }} // optional callback to refresh something
        />
      </div>
    </Container>

  );

};



export default AdminDashboard;