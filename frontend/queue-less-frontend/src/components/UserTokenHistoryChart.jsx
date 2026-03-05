// src/components/UserTokenHistoryChart.jsx
import React from 'react';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import { Card, Spinner, Alert } from 'react-bootstrap';
import './UserTokenHistoryChart.css';

const UserTokenHistoryChart = ({ data, loading, error }) => {
  if (loading) return <Spinner animation="border" />;
  if (error) return <Alert variant="danger">{error}</Alert>;
  if (!data || !data.dates || data.dates.length === 0) {
    return <Alert variant="info">No data available</Alert>;
  }

  const chartData = data.dates.map((date, index) => ({
    date,
    tokens: data.counts[index],
  }));

  return (
   <Card className="mb-4 user-token-history-card">
      <Card.Header>Your Token Usage (Last 30 Days)</Card.Header>
      <Card.Body style={{ height: 300 }}>
        <ResponsiveContainer width="100%" height="100%">
          <BarChart data={chartData} margin={{ top: 5, right: 20, left: 0, bottom: 5 }}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="date" />
            <YAxis />
            <Tooltip />
            <Legend />
            <Bar dataKey="tokens" fill="#8884d8" />
          </BarChart>
        </ResponsiveContainer>
      </Card.Body>
    </Card>
  );
};

export default UserTokenHistoryChart;