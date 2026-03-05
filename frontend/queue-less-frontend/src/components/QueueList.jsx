import React, { useState } from 'react';
import { FaUserCheck, FaCheckCircle, FaTimesCircle, FaEye } from 'react-icons/fa';
import { useSelector } from 'react-redux';
import WebSocketService from '../services/websocketService';
import { toast } from 'react-toastify';
import { DragDropContext, Droppable, Draggable } from '@hello-pangea/dnd';
import { getShortTokenId } from '../utils/tokenUtils';
import './QueueList.css';
import { Card, Button, Badge } from 'react-bootstrap';
import CancelTokenModal from './CancelTokenModal';
import axiosInstance from '../utils/axiosInstance';

const QueueList = ({ queue, onServeNext, onViewUserDetails }) => {
    const [showCancelModal, setShowCancelModal] = useState(false);
    const [tokenToCancel, setTokenToCancel] = useState(null);
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
            const success = WebSocketService.sendMessage('/app/queue/serve-next', { queueId: queue.id });
            if (!success) {
                toast.error("Failed to send serve next request. Please try again.");
            }
        } else {
            toast.error("Cannot serve next token. A token is already in service or the queue is empty.");
        }
    };

    const handleCompleteToken = async (tokenId) => {
        try {
            await axiosInstance.post(
                `/queues/${queue.id}/complete-token`,
                { tokenId }
            );
            toast.success(`Token ${tokenId} completed successfully!`);
        } catch (error) {
            toast.error("Failed to complete token.");
        }
    };

    const handleCancelClick = (token) => {
        setTokenToCancel(token);
        setShowCancelModal(true);
    };

    const handleConfirmCancel = async (reason) => {
        try {
            await axiosInstance.delete(
                `/queues/${queue.id}/cancel-token/${tokenToCancel.tokenId}?reason=${encodeURIComponent(reason || '')}`
            );
            toast.info(`Token ${tokenToCancel.tokenId} has been cancelled.`);
            // Optionally refresh queue data
        } catch (error) {
            toast.error('Failed to cancel token.');
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
            await axiosInstance.put(
                `/queues/${queue.id}/reorder`,
                newCombinedList
            );
            toast.success("Queue order updated!");
        } catch (error) {
            toast.error("Failed to reorder queue.");
        }
    };

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
                            <h1 className="queuelist-serving-token">{getShortTokenId(inServiceToken.tokenId)}</h1>
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
                                                                <span className="queuelist-token-number">{getShortTokenId(token.tokenId)}</span>
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
                                                                        onClick={() => handleCancelClick(token)}
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

                <CancelTokenModal
                    show={showCancelModal}
                    onHide={() => setShowCancelModal(false)}
                    token={tokenToCancel}
                    onConfirm={handleConfirmCancel}
                />
            </div>
        </div>
    );
};

export default QueueList;