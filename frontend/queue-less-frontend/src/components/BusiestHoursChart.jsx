// src/components/BusiestHoursChart.jsx
import React from 'react';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import { Card, Spinner, Alert } from 'react-bootstrap';
import './BusiestHoursChart.css';
const BusiestHoursChart = ({ data, loading, error }) => {
  if (loading) return <Spinner animation="border" />;
  if (error) return <Alert variant="danger">{error}</Alert>;
  if (!data || Object.keys(data).length === 0) {
    return <Alert variant="info">No data available</Alert>;
  }

  // Transform hour object into array
  const chartData = Object.entries(data).map(([hour, avg]) => ({
    hour: `${hour}:00`,
    averageWaiting: avg,
  }));

  return (
    <Card className="mb-4 busiest-hours-chart-card">

      <Card.Header>Busiest Hours (Avg. Waiting Tokens)</Card.Header>
      <Card.Body style={{ height: 300 }}>
        <ResponsiveContainer width="100%" height="100%">
          <BarChart data={chartData} margin={{ top: 5, right: 20, left: 0, bottom: 5 }}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="hour" />
            <YAxis />
            <Tooltip />
            <Legend />
<Bar dataKey="averageWaiting" />           </BarChart>
        </ResponsiveContainer>
      </Card.Body>
    </Card>
  );
};

export default BusiestHoursChart;