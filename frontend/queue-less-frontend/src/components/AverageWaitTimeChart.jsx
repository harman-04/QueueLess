// src/components/AverageWaitTimeChart.jsx
import React from 'react';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import { Card, Spinner, Alert } from 'react-bootstrap';
import './AverageWaitTimeChart.css';
const AverageWaitTimeChart = ({ data, loading, error }) => {
  if (loading) return <Spinner animation="border" />;
  if (error) return <Alert variant="danger">{error}</Alert>;
  if (!data || !data.dates || data.dates.length === 0) {
    return <Alert variant="info">No data available</Alert>;
  }

  const chartData = data.dates.map((date, index) => ({
    date,
    avgWaitTime: data.averages[index],
  }));

  return (
    <Card className="mb-4 average-wait-time-chart">
      <Card.Header>Average Wait Time Trend (Last 30 Days)</Card.Header>
      <Card.Body style={{ height: 300 }}>
        <ResponsiveContainer width="100%" height="100%">
          <LineChart data={chartData} margin={{ top: 5, right: 20, left: 0, bottom: 5 }}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="date" />
            <YAxis />
            <Tooltip />
            <Legend />
            <Line type="monotone" dataKey="avgWaitTime" stroke="var(--accent-color)" dot={false} />
          </LineChart>
        </ResponsiveContainer>
      </Card.Body>
    </Card>
  );
};

export default AverageWaitTimeChart;