import React, { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { fetchUserTokenHistory, resetHistory } from '../redux/userTokenHistorySlice';
import { Card, Table, Badge, Spinner, Alert, Button } from 'react-bootstrap';
import { FaHistory, FaStar, FaClock, FaCheckCircle, FaTimesCircle } from 'react-icons/fa';
import './UserTokenHistoryList.css';
import { getShortTokenId } from '../utils/tokenUtils';
import TokenHistorySkeleton from './TokenHistorySkeleton'; // optional

const UserTokenHistoryList = ({ days = 30 }) => {
  const dispatch = useDispatch();
  const { tokens, loading, loadingMore, error, hasMore, page, size } = useSelector((state) => state.userTokenHistory);

  useEffect(() => {
    dispatch(resetHistory());
    dispatch(fetchUserTokenHistory({ days, page: 0, size }));
  }, [dispatch, days, size]);

  const loadMore = () => {
    if (!loadingMore && hasMore) {
      dispatch(fetchUserTokenHistory({ days, page, size }));
    }
  };

  const formatDateTime = (dateString) => {
    return new Date(dateString).toLocaleString();
  };

  const getStatusBadge = (status) => {
    switch (status) {
      case 'COMPLETED':
        return <Badge bg="success"><FaCheckCircle className="me-1" /> Completed</Badge>;
      case 'CANCELLED':
        return <Badge bg="danger"><FaTimesCircle className="me-1" /> Cancelled</Badge>;
      default:
        return <Badge bg="secondary">{status}</Badge>;
    }
  };

  const getRatingStars = (rating) => {
    if (!rating) return 'Not rated';
    return (
      <span>
        {[...Array(5)].map((_, i) => (
          <FaStar key={i} className={i < rating ? 'text-warning' : 'text-muted'} />
        ))}
      </span>
    );
  };

  if (loading) {
    return <TokenHistorySkeleton />; // or a simple spinner
  }

  if (error) {
    return (
      <Alert variant="danger">
        Error: {error}
        <Button variant="outline-danger" size="sm" className="ms-3" onClick={() => dispatch(resetHistory())}>
          Retry
        </Button>
      </Alert>
    );
  }

  if (tokens.length === 0) {
    return (
      <Card className="text-center py-5">
        <Card.Body>
          <FaHistory size={48} className="text-muted mb-3" />
          <h5>No token history found</h5>
          <p className="text-muted">You haven't completed or cancelled any tokens in the last {days} days.</p>
        </Card.Body>
      </Card>
    );
  }

  return (
    <Card className="user-token-history-card">
      <Card.Header>
        <h5 className="mb-0"><FaHistory className="me-2" /> Your Token History (Last {days} Days)</h5>
      </Card.Header>
      <Card.Body className="p-0">
        <div className="table-responsive">
          <Table hover className="token-history-table mb-0">
            <thead>
              <tr>
                <th>Token</th>
                <th>Service</th>
                <th>Place</th>
                <th>Status</th>
                <th>Issued At</th>
                <th>Wait Time</th>
                <th>Service Duration</th>
                <th>Your Rating</th>
              </tr>
            </thead>
            <tbody>
              {tokens.map((token) => (
                <tr key={token.tokenId}>
                  <td className="fw-semibold">{getShortTokenId(token.tokenId)}</td>
                  <td>{token.serviceName}</td>
                  <td>{token.placeName}</td>
                  <td>{getStatusBadge(token.status)}</td>
                  <td>{formatDateTime(token.issuedAt)}</td>
                  <td>
                    {token.waitTimeMinutes != null ? (
                      <span><FaClock className="me-1" /> {token.waitTimeMinutes} min</span>
                    ) : '—'}
                  </td>
                  <td>
                    {token.serviceDurationMinutes != null ? (
                      <span><FaClock className="me-1" /> {token.serviceDurationMinutes} min</span>
                    ) : '—'}
                  </td>
                  <td>{getRatingStars(token.rating)}</td>
                </tr>
              ))}
            </tbody>
          </Table>
        </div>
        {hasMore && (
          <div className="text-center p-3">
            <Button
              variant="outline-primary"
              onClick={loadMore}
              disabled={loadingMore}
            >
              {loadingMore ? <Spinner animation="border" size="sm" /> : 'Load More'}
            </Button>
          </div>
        )}
      </Card.Body>
    </Card>
  );
};

export default UserTokenHistoryList;