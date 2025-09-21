import React from 'react';
import { Card, Accordion, Badge } from 'react-bootstrap';
import { FaUserCheck, FaClock, FaCalendarAlt, FaHistory } from 'react-icons/fa';
import './CompletedTokensList.css'; // NEW CSS FILE

const CompletedTokensList = ({ completedTokens }) => {
    if (!completedTokens || completedTokens.length === 0) {
        return (
            <div className="completed-tokens-empty">
                <FaHistory size="3rem" className="mb-3 text-muted" />
                <h5>No completed tokens yet.</h5>
                <p className="text-muted">Tokens that have been served or cancelled will appear here.</p>
            </div>
        );
    }

    // Function to calculate duration
    const getServiceDuration = (servedAt, completedAt) => {
        if (!servedAt || !completedAt) {
            return 'N/A';
        }
        const start = new Date(servedAt);
        const end = new Date(completedAt);
        const durationMs = end - start;
        const durationMinutes = Math.round(durationMs / 60000); // Convert milliseconds to minutes
        return durationMinutes > 0 ? `${durationMinutes} mins` : 'less than a minute';
    };

    return (
        <div className="completed-tokens-container">
            <Accordion alwaysOpen>
                <Accordion.Item eventKey="0">
                    <Accordion.Header className="completed-tokens-accordion-header">
                        <div className="completed-tokens-title-section">
                            <FaUserCheck className="me-2" />
                            <h5>Completed Tokens <Badge bg="secondary">{completedTokens.length}</Badge></h5>
                        </div>
                    </Accordion.Header>
                    <Accordion.Body>
                        <div className="completed-tokens-grid">
                            {completedTokens.map((token) => (
                                <Card key={token.tokenId} className="completed-token-card">
                                    <Card.Body>
                                        <div className="completed-card-header-info">
                                            <h4 className="completed-token-number">#{token.tokenId}</h4>
                                            <Badge bg="success" className="completed-status-badge">
                                                <FaUserCheck className="me-1" /> Completed
                                            </Badge>
                                        </div>
                                        <hr className="my-2" />
                                        <div className="completed-card-body-details">
                                            {/* CHANGE 1: Use token.issuedAt instead of token.createdAt */}
                                            <div className="completed-detail-item">
                                                <FaCalendarAlt />
                                                <span>{new Date(token.issuedAt).toLocaleString()}</span>
                                            </div>
                                            {/* CHANGE 2: Calculate duration using the new function */}
                                            <div className="completed-detail-item">
                                                <FaClock />
                                                <span>Duration: {getServiceDuration(token.servedAt, token.completedAt)}</span>
                                            </div>
                                        </div>
                                        {token.userName && (
                                            <div className="completed-user-details-summary mt-3">
                                                <strong>User:</strong> {token.userName}
                                            </div>
                                        )}
                                    </Card.Body>
                                </Card>
                            ))}
                        </div>
                    </Accordion.Body>
                </Accordion.Item>
            </Accordion>
        </div>
    );
};

export default CompletedTokensList;