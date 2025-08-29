// src/redux/websocketActions.js


export const sendWebSocketMessage = (destination, body) => ({

  type: "WS_SEND",

  payload: { destination, body },

});


export const connectWebSocket = (queueId) => {

  return { type: 'WS_CONNECT', payload: { queueId } };

};


export const disconnectWebSocket = () => {

  return { type: 'WS_DISCONNECT' };

};


// export const sendWebSocketMessage = (destination, body) => {

//   return { type: 'WS_SEND_MESSAGE', payload: { destination, body } };

// };