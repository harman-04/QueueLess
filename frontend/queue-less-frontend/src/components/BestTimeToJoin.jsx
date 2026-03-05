// src/components/BestTimeToJoin.jsx
import React, { useState, useEffect } from 'react';
import { Card, Spinner, Alert, ListGroup } from 'react-bootstrap';
import { FaClock, FaInfoCircle } from 'react-icons/fa';
import axiosInstance from '../utils/axiosInstance';
import './BestTimeToJoin.css';
const BestTimeToJoin = ({ queueId }) => {
    const [bestHours, setBestHours] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        if (!queueId) return;
        const fetchBestTime = async () => {
            try {
                const response = await axiosInstance.get(`/queues/${queueId}/best-time`);
                setBestHours(response.data.bestHours || []);
            } catch (err) {
                setError('Could not load best time data');
                console.error(err);
            } finally {
                setLoading(false);
            }
        };
        fetchBestTime();
    }, [queueId]);


    if (loading) return <Spinner animation="border" size="sm" />;
    if (error) return <Alert variant="warning"><FaInfoCircle /> {error}</Alert>;
    if (bestHours.length === 0) return null;


    return (
        <Card className="mt-3 best-time-card">
            <Card.Header>
                <FaClock className="me-2" />
                Best Time to Join
            </Card.Header>
            <Card.Body>
                <p className="text-muted">Based on historical data, the queue tends to be shortest during:</p>
                <ListGroup variant="flush">
                    {bestHours.map((hour, idx) => (
                        <ListGroup.Item key={idx} className="d-flex justify-content-between">
                            <span>{hour}</span>
                            <span className="text-success">✓ Recommended</span>
                        </ListGroup.Item>
                    ))}
                </ListGroup>
            </Card.Body>
        </Card>
    );
};

export default BestTimeToJoin;