// src/components/QueueList.jsx
import React, { useState, useEffect } from 'react';
import { FaUserCheck, FaCheckCircle, FaTimesCircle, FaToggleOn, FaToggleOff } from 'react-icons/fa';
import { useDispatch, useSelector } from 'react-redux';
import { sendWebSocketMessage } from '../redux/websocketActions';
import axios from 'axios';
import { toast } from 'react-toastify';
import { DragDropContext, Droppable, Draggable } from '@hello-pangea/dnd';

const API_BASE_URL = "http://localhost:8080/api/queues";

const QueueList = ({ queue, onServeNext }) => {
  const dispatch = useDispatch();
  const { token: authToken } = useSelector((state) => state.auth);

  // CRITICAL FIX: Add a defensive check for the `queue` prop.
  if (!queue) {
    return (
      <div className="card shadow-lg border-0 h-100">
        <div className="card-body p-4 text-center">
          <p className="text-muted">Loading queue data...</p>
        </div>
      </div>
    );
  }

  
  // Sync local state with Redux state when Redux state changes
  useEffect(() => {
 
  }, [queue.isActive]);

  // src/components/QueueList.jsx - Update the token filtering logic
// src/components/QueueList.jsx - Update the token filtering logic
const inServiceToken = queue.tokens?.find(t => t.status === 'IN_SERVICE');
const waitingTokens = queue.tokens?.filter(t => t.status === 'WAITING') || [];
const completedTokens = queue.tokens?.filter(t => t.status === 'COMPLETED') || [];
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
    <div className="card shadow-lg border-0 h-100">
    
      <div className="card-body p-4">
        
        {/* In Service Token Section */}
        {inServiceToken && (
          <div className="alert alert-success d-flex justify-content-between align-items-center mb-4 animate__animated animate__fadeIn">
            <div>
              <h5 className="mb-1">
                <FaUserCheck className="me-2" /> Now Serving:
                <span className="fw-bold ms-2">{inServiceToken.tokenId}</span>
              </h5>
              <small className="text-muted">User ID: {inServiceToken.userId}</small>
            </div>
            <button
              onClick={() => handleCompleteToken(inServiceToken.tokenId)}
              className="btn btn-success btn-sm ms-3"
            >
              <FaCheckCircle className="me-1" /> Complete
            </button>
          </div>
        )}

        {/* Next in Queue Section */}
        <div className="mb-4 animate__animated animate__fadeIn">
          <h5 className="text-secondary fw-semibold">Next in Queue:</h5>
          {waitingTokens.length > 0 ? (
            <div className="d-flex justify-content-center">
              <span className="display-4 fw-bold text-primary">
                {waitingTokens[0].tokenId}
              </span>
            </div>
          ) : (
            <p className="text-muted text-center">No tokens waiting.</p>
          )}
        </div>

        {/* Control Button Section */}
        <div className="d-grid gap-2 mb-4">
          <button
            onClick={handleServeNext}
            className="btn btn-primary btn-lg"
            disabled={isServeNextDisabled}
          >
            Serve Next
          </button>
        </div>

        {/* Draggable Waiting Tokens List */}
        <div className="mb-4 animate__animated animate__fadeIn">
          <h5 className="text-secondary fw-semibold">Waiting Tokens ({waitingTokens.length}):</h5>
          <DragDropContext onDragEnd={onDragEnd}>
            <Droppable droppableId="waitingTokensList">
              {(provided) => (
                <ul
                  className="list-group list-group-flush"
                  {...provided.droppableProps}
                  ref={provided.innerRef}
                >
                  {waitingTokens.length > 0 ? (
                    waitingTokens.map((token, index) => (
                      <Draggable key={token.tokenId} draggableId={token.tokenId} index={index}>
                        {(provided) => (
                          <li
                            className="list-group-item d-flex justify-content-between align-items-center"
                            ref={provided.innerRef}
                            {...provided.draggableProps}
                            {...provided.dragHandleProps}
                            style={{ ...provided.draggableProps.style, cursor: 'grab' }}
                          >
                            <span>{token.tokenId}</span>
                            <button
                              onClick={() => handleCancelToken(token.tokenId)}
                              className="btn btn-danger btn-sm ms-3"
                            >
                              <FaTimesCircle className="me-1" /> Cancel
                            </button>
                          </li>
                        )}
                      </Draggable>
                    ))
                  ) : (
                    <li className="list-group-item text-muted text-center">No tokens waiting.</li>
                  )}
                  {provided.placeholder}
                </ul>
              )}
            </Droppable>
          </DragDropContext>
        </div>
        
        {/* Completed Tokens Section */}
        <div className="mt-4 animate__animated animate__fadeIn">
          <h5 className="text-success fw-semibold">Completed Tokens ({completedTokens.length}):</h5>
          <ul className="list-group list-group-flush">
            {completedTokens.length > 0 ? (
              completedTokens.map(token => (
                <li key={token.tokenId} className="list-group-item text-success d-flex align-items-center">
                  <FaCheckCircle className="me-2" /> {token.tokenId}
                </li>
              ))
            ) : (
              <li className="list-group-item text-muted text-center">No completed tokens yet.</li>
            )}
          </ul>
        </div>
      </div>
    </div>
  );
};

export default QueueList;