import React, { useState, useEffect } from 'react';
import { FaUserCheck, FaCheckCircle, FaTimesCircle, FaEye } from 'react-icons/fa';
import { useDispatch, useSelector } from 'react-redux';
import { sendWebSocketMessage } from '../redux/websocketActions';
import axios from 'axios';
import { toast } from 'react-toastify';
import { DragDropContext, Droppable, Draggable } from '@hello-pangea/dnd';
import './QueueList.css'; // NEW CSS FILE
import { Card, Button, Badge } from 'react-bootstrap';

const API_BASE_URL = "https://localhost:8443/api/queues";

const QueueList = ({ queue, onServeNext, onViewUserDetails }) => {
    const dispatch = useDispatch();
    const { token: authToken } = useSelector((state) => state.auth);

    if (!queue) {
        return (
            <div className="queuelist-card-container queuelist-empty">
                <p className="queuelist-muted">Loading queue data...</p>
            </div>
        );
    }

    const inServiceToken = queue.tokens?.find(t => t.status === 'IN_SERVICE');
    const waitingTokens = queue.tokens?.filter(t => t.status === 'WAITING') || [];
    
    const handleServeNext = () => {
        if (onServeNext) {
            onServeNext();
        } else if (!inServiceToken && waitingTokens.length > 0) {
            dispatch(sendWebSocketMessage('/app/queue/serve-next', { queueId: queue.id }));
        } else {
            toast.error("Cannot serve next token. A token is already in service or the queue is empty.");
        }
    };
    
    const handleCompleteToken = async (tokenId) => {
        try {
            await axios.post(
                `${API_BASE_URL}/${queue.id}/complete-token`,
                { tokenId: tokenId },
                { headers: { 'Authorization': `Bearer ${authToken}`, 'Content-Type': 'application/json' } }
            );
            toast.success(`Token ${tokenId} completed successfully!`);
        } catch (error) {
            toast.error("Failed to complete token.");
        }
    };

    const handleCancelToken = async (tokenId) => {
        try {
            await axios.delete(
                `${API_BASE_URL}/${queue.id}/cancel-token/${tokenId}`,
                { headers: { 'Authorization': `Bearer ${authToken}` } }
            );
            toast.info(`Token ${tokenId} has been canceled.`);
        } catch (error) {
            toast.error("Failed to cancel token.");
        }
    };

    const onDragEnd = async (result) => {
        if (!result.destination) return;

        const sourceIndex = result.source.index;
        const destinationIndex = result.destination.index;
        
        const reorderedTokens = Array.from(waitingTokens);
        const [removed] = reorderedTokens.splice(sourceIndex, 1);
        reorderedTokens.splice(destinationIndex, 0, removed);

        const otherTokens = queue.tokens.filter(t => t.status?.toLowerCase() !== 'waiting');
        const newCombinedList = [...otherTokens, ...reorderedTokens];
        
        try {
            await axios.put(
                `${API_BASE_URL}/${queue.id}/reorder`,
                newCombinedList,
                { headers: { 'Authorization': `Bearer ${authToken}` } }
            );
            toast.success("Queue order updated!");
        } catch (error) {
            toast.error("Failed to reorder queue.");
        }
    };

    const isServeNextDisabled = !!inServiceToken || waitingTokens.length === 0;

    return (
        <div className="queuelist-card-container">
            <div className="queuelist-body">
                
                {/* In Service Token Section */}
                {inServiceToken && (
                    <Card className="queuelist-serving-card queuelist-animate-in">
                        <Card.Body>
                            <div className="queuelist-card-header">
                                <FaUserCheck className="queuelist-icon-large queuelist-text-success" />
                                <h4 className="queuelist-serving-title">Now Serving</h4>
                            </div>
                            <h1 className="queuelist-serving-token">{inServiceToken.tokenId}</h1>
                            <div className="queuelist-serving-actions">
                                <Button 
                                    onClick={() => onViewUserDetails(inServiceToken.tokenId)}
                                    variant="outline-secondary"
                                    className="queuelist-btn-icon"
                                >
                                    <FaEye /> View Details
                                </Button>
                                <Button 
                                    onClick={() => handleCompleteToken(inServiceToken.tokenId)}
                                    variant="success"
                                >
                                    <FaCheckCircle /> Complete
                                </Button>
                            </div>
                        </Card.Body>
                    </Card>
                )}

                {/* Waiting Tokens Section */}
                <div className="queuelist-section queuelist-animate-in">
                    <h5 className="queuelist-section-title">Waiting Tokens <Badge bg="secondary">{waitingTokens.length}</Badge></h5>
                    {waitingTokens.length > 0 ? (
                        <DragDropContext onDragEnd={onDragEnd}>
                            <Droppable droppableId="waitingTokensList">
                                {(provided) => (
                                    <div
                                        className="queuelist-list-group"
                                        {...provided.droppableProps}
                                        ref={provided.innerRef}
                                    >
                                        {waitingTokens.map((token, index) => (
                                            <Draggable key={token.tokenId} draggableId={token.tokenId} index={index}>
                                                {(provided) => (
                                                    <Card
                                                        className="queuelist-token-item"
                                                        ref={provided.innerRef}
                                                        {...provided.draggableProps}
                                                        {...provided.dragHandleProps}
                                                        style={{ ...provided.draggableProps.style }}
                                                    >
                                                        <Card.Body>
                                                            <div className="queuelist-item-content">
                                                                <span className="queuelist-token-number">{token.tokenId}</span>
                                                                <div className="queuelist-item-actions">
                                                                    <Button
                                                                        onClick={() => onViewUserDetails(token.tokenId)}
                                                                        variant="info"
                                                                        size="sm"
                                                                        className="me-2"
                                                                    >
                                                                        <FaEye />
                                                                    </Button>
                                                                    <Button
                                                                        onClick={() => handleCancelToken(token.tokenId)}
                                                                        variant="danger"
                                                                        size="sm"
                                                                    >
                                                                        <FaTimesCircle />
                                                                    </Button>
                                                                </div>
                                                            </div>
                                                        </Card.Body>
                                                    </Card>
                                                )}
                                            </Draggable>
                                        ))}
                                        {provided.placeholder}
                                    </div>
                                )}
                            </Droppable>
                        </DragDropContext>
                    ) : (
                        <div className="queuelist-empty-message">
                            <p>No tokens are currently waiting.</p>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
};

export default QueueList;