// src/utils/normalizeQueue.js
export const normalizeQueue = (queueData) => {
  if (!queueData) return null;
  
  // Handle both 'active' and 'isActive' fields for backward compatibility
  const isActive = queueData.active !== undefined ? queueData.active : queueData.isActive;
  
  return {
    ...queueData,
    isActive: isActive !== undefined ? isActive : true
  };
};

export const normalizeQueues = (queuesData) => {
  if (!Array.isArray(queuesData)) return [];
  
  return queuesData.map(queue => normalizeQueue(queue));
};