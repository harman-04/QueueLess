// src/utils/normalizeQueue.js
export const normalizeQueue = (queueData) => {
  if (!queueData) return null;
  
  return {
    ...queueData,
    isActive: queueData.active !== undefined ? queueData.active : queueData.isActive
  };
};

export const normalizeQueues = (queuesData) => {
  if (!Array.isArray(queuesData)) return [];
  
  return queuesData.map(queue => ({
    ...queue,
    isActive: queue.active !== undefined ? queue.active : queue.isActive
  }));
};