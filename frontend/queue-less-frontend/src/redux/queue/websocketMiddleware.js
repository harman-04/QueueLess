// src/redux/queue/websocketMiddleware.js
import { Client } from "@stomp/stompjs";

let stompClient = null;
let currentSubscription = null;
let isConnecting = false;

const websocketMiddleware = (store) => (next) => (action) => {
  switch (action.type) {
    case "WS_CONNECT":
      const { queueId } = action.payload;
      
      // Prevent duplicate connections to the same queue
      if (stompClient && stompClient.connected && currentSubscription === queueId) {
        console.log("⚠️ STOMP client is already connected to this queue.");
        return;
      }
      
      if (isConnecting) {
        console.log("⚠️ STOMP client is already connecting.");
        return;
      }

      isConnecting = true;

      // Disconnect existing connection if any
      if (stompClient) {
        stompClient.deactivate();
        stompClient = null;
        currentSubscription = null;
      }

      stompClient = new Client({
        brokerURL: "ws://localhost:8080/ws",
        connectHeaders: {
          Authorization: "Bearer " + store.getState().auth.token,
        },
        debug: (str) => {
          console.log("[STOMP]", str);
        },
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
        onConnect: () => {
          console.log("✅ WebSocket connected!");
          isConnecting = false;
          currentSubscription = queueId;
          store.dispatch({ type: "WS_CONNECTED" });

          // Subscribe to the queue topic
          stompClient.subscribe(`/topic/queues/${queueId}`, (message) => {
            try {
              const body = JSON.parse(message.body);
              console.log("📥 Queue update:", body);
              
              // Normalize the queue data to handle both 'active' and 'isActive' fields
              const normalizedBody = {
                ...body,
                isActive: body.active !== undefined ? body.active : body.isActive
              };
              
              store.dispatch({ type: "WS_MESSAGE_RECEIVED", payload: normalizedBody });
            } catch (error) {
              console.error("Error parsing WebSocket message:", error);
            }
          });

          // Request initial queue state
          stompClient.publish({
            destination: "/app/queue/connect",
            body: JSON.stringify({ queueId: queueId }),
          });
        },
        onDisconnect: () => {
          isConnecting = false;
          currentSubscription = null;
          store.dispatch({ type: "WS_DISCONNECTED" });
        },
        onStompError: (frame) => {
          isConnecting = false;
          console.error("STOMP error:", frame);
          store.dispatch({ type: "WS_CONNECTION_FAILURE" });
        }
      });

      stompClient.activate();
      break;

    case "WS_DISCONNECT":
      if (stompClient) {
        stompClient.deactivate();
        stompClient = null;
        currentSubscription = null;
        isConnecting = false;
        console.log("❌ WebSocket deactivated.");
      }
      break;

    case "WS_SEND":
      if (stompClient && stompClient.connected) {
        console.log("📤 Sending to:", action.payload.destination, "with body:", action.payload.body);
        stompClient.publish({
          destination: action.payload.destination,
          body: JSON.stringify(action.payload.body),
        });
      } else {
        console.warn("⚠️ Cannot send, STOMP not connected");
        // Optionally try to reconnect
        if (currentSubscription) {
          store.dispatch({ type: "WS_CONNECT", payload: { queueId: currentSubscription } });
        }
      }
      break;

    default:
      return next(action);
  }
};

export default websocketMiddleware;