import React, { useState, useMemo } from 'react';
import { Table, Badge, Button, OverlayTrigger, Tooltip, Form, Spinner, Alert } from 'react-bootstrap';
import { FaSort, FaSortUp, FaSortDown, FaEye } from 'react-icons/fa';
import { useNavigate } from 'react-router-dom';
import './ProviderLeaderboard.css';

const ProviderLeaderboard = ({ providers, loading, error }) => {
  const navigate = useNavigate();
  const [sortField, setSortField] = useState('name');
  const [sortDirection, setSortDirection] = useState('asc');
  const [filter, setFilter] = useState('');

  const handleSort = (field) => {
    if (field === sortField) {
      setSortDirection(sortDirection === 'asc' ? 'desc' : 'asc');
    } else {
      setSortField(field);
      setSortDirection('asc');
    }
  };

  const sortedProviders = useMemo(() => {
    if (!providers) return [];
    let filtered = providers.filter(p =>
      p.name.toLowerCase().includes(filter.toLowerCase()) ||
      p.email.toLowerCase().includes(filter.toLowerCase())
    );
    return filtered.sort((a, b) => {
      let aVal = a[sortField];
      let bVal = b[sortField];
      if (typeof aVal === 'string') {
        aVal = aVal.toLowerCase();
        bVal = bVal.toLowerCase();
      }
      if (aVal < bVal) return sortDirection === 'asc' ? -1 : 1;
      if (aVal > bVal) return sortDirection === 'asc' ? 1 : -1;
      return 0;
    });
  }, [providers, sortField, sortDirection, filter]);

  const SortIcon = ({ field }) => {
    if (field !== sortField) return <FaSort className="ms-1" />;
    return sortDirection === 'asc' ? <FaSortUp className="ms-1" /> : <FaSortDown className="ms-1" />;
  };

  if (loading) return <div className="text-center py-4"><Spinner animation="border" /></div>;
  if (error) return <Alert variant="danger">{error}</Alert>;
  if (!providers || providers.length === 0) return <Alert variant="info">No providers found.</Alert>;

  return (
    <div className="provider-leaderboard-container">
      <Form.Control
        type="text"
        placeholder="Filter by name or email"
        value={filter}
        onChange={(e) => setFilter(e.target.value)}
        className="mb-3"
      />
      <Table striped bordered hover responsive className="provider-leaderboard-table">
        <thead>
          <tr>
            <th onClick={() => handleSort('name')} style={{ cursor: 'pointer' }}>
              Provider <SortIcon field="name" />
            </th>
            <th onClick={() => handleSort('email')} style={{ cursor: 'pointer' }}>
              Email <SortIcon field="email" />
            </th>
            <th onClick={() => handleSort('totalQueues')} style={{ cursor: 'pointer' }}>
              Total Queues <SortIcon field="totalQueues" />
            </th>
            <th onClick={() => handleSort('activeQueues')} style={{ cursor: 'pointer' }}>
              Active <SortIcon field="activeQueues" />
            </th>
            <th onClick={() => handleSort('tokensServedToday')} style={{ cursor: 'pointer' }}>
              Today's Tokens <SortIcon field="tokensServedToday" />
            </th>
            <th onClick={() => handleSort('averageRating')} style={{ cursor: 'pointer' }}>
              Avg Rating <SortIcon field="averageRating" />
            </th>
            <th onClick={() => handleSort('cancellationRate')} style={{ cursor: 'pointer' }}>
              Cancellation Rate <SortIcon field="cancellationRate" />
            </th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {sortedProviders.map((provider) => (
            <tr key={provider.id}>
              <td>{provider.name}</td>
              <td>{provider.email}</td>
              <td>{provider.totalQueues}</td>
              <td>
                <Badge bg={provider.activeQueues > 0 ? 'success' : 'secondary'}>
                  {provider.activeQueues}
                </Badge>
              </td>
              <td>{provider.tokensServedToday}</td>
              <td>
                {provider.averageRating > 0 ? provider.averageRating.toFixed(1) : 'N/A'}
                {provider.averageRating > 0 && (
                  <div className="progress mt-1" style={{ height: '5px', width: '80px' }}>
                    <div
                      className="progress-bar bg-warning"
                      style={{ width: `${(provider.averageRating / 5) * 100}%` }}
                    />
                  </div>
                )}
              </td>
              <td>
                {provider.cancellationRate > 0 ? provider.cancellationRate.toFixed(1) + '%' : '0%'}
                {provider.cancellationRate > 0 && (
                  <div className="progress mt-1" style={{ height: '5px', width: '80px' }}>
                    <div
                      className="progress-bar bg-danger"
                      style={{ width: `${provider.cancellationRate}%` }}
                    />
                  </div>
                )}
              </td>
              <td>
                <OverlayTrigger placement="top" overlay={<Tooltip>View Details</Tooltip>}>
                  <Button
                    variant="outline-primary"
                    size="sm"
                    onClick={() => navigate(`/admin/providers/${provider.id}`)}
                  >
                    <FaEye />
                  </Button>
                </OverlayTrigger>
              </td>
            </tr>
          ))}
        </tbody>
      </Table>
    </div>
  );
};

export default ProviderLeaderboard;