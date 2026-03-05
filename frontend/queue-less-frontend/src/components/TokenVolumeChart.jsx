// src/components/TokenVolumeChart.jsx
import React from 'react';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import { Card, Spinner, Alert } from 'react-bootstrap';
import './TokenVolumeChart.css';

const TokenVolumeChart = ({ data, loading, error }) => {
  if (loading) return <Spinner animation="border" />;
  if (error) return <Alert variant="danger">{error}</Alert>;
  if (!data || !data.dates || data.dates.length === 0) {
    return <Alert variant="info">No data available</Alert>;
  }

  // Transform data for Recharts
  const chartData = data.dates.map((date, index) => ({
    date,
    tokens: data.counts[index],
  }));

  return (
    <Card className="mb-4 token-volume-chart-card">
      <Card.Header>Token Volume (Last 30 Days)</Card.Header>
      <Card.Body style={{ height: 300 }}>
        <ResponsiveContainer width="100%" height="100%">
          <LineChart data={chartData} margin={{ top: 5, right: 20, left: 0, bottom: 5 }}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="date" />
            <YAxis />
            <Tooltip />
            <Legend />
            <Line type="monotone" dataKey="tokens" stroke="#8884d8" activeDot={{ r: 8 }} />
          </LineChart>
        </ResponsiveContainer>
      </Card.Body>
    </Card>
  );
};

export default TokenVolumeChart;