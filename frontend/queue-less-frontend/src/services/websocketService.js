import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import store from '../store/store';
import { updateQueue, connectionSuccess, connectionFailure } from '../redux/queue/queueSlice';

class WebSocketService {
  constructor() {
    this.client = null;
    this.subscriptions = new Map();
    this.reconnectAttempts = 0;
    this.maxReconnectAttempts = 5;
    this.emergencyApprovalHandler = null;
    this.tokenCancelledHandler = null;
  }

  setEmergencyApprovalHandler(handler) {
    this.emergencyApprovalHandler = handler;
  }

  setTokenCancelledHandler(handler) {
    this.tokenCancelledHandler = handler;
  }

  connect() {
    if (this.client && this.client.connected) {
      return;
    }

    const token = localStorage.getItem('token');
    if (!token) {
      console.error('No token available for WebSocket connection');
      return;
    }

    this.client = new Client({
      webSocketFactory: () => new SockJS('https://localhost:8443/ws'),
      connectHeaders: {
        Authorization: `Bearer ${token}`,
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,

      onConnect: () => {
        console.log('✅ WebSocket connected');
        store.dispatch(connectionSuccess());
        this.reconnectAttempts = 0;

        this.subscribeToUserNotifications();
        this.resubscribeToAll();
      },

      onStompError: (frame) => {
        console.error('WebSocket STOMP error:', frame.headers['message']);
        store.dispatch(connectionFailure());
        if (frame.headers['message']?.includes('JWT') || frame.headers['message']?.includes('token')) {
          console.error('Token-related WebSocket error');
          this.handleDisconnection();
        }
      },

      onDisconnect: () => {
        console.log('❌ WebSocket disconnected');
        store.dispatch(connectionFailure());
        this.handleDisconnection();
      }
    });

    this.client.activate();
  }

  subscribeToUserNotifications() {
    if (!this.client || !this.client.connected) return;

    if (this.emergencyApprovalHandler) {
      const sub = this.client.subscribe('/user/queue/emergency-approved', (message) => {
        try {
          const data = JSON.parse(message.body);
          this.emergencyApprovalHandler(data);
        } catch (error) {
          console.error('Error parsing emergency approval:', error);
        }
      });
      this.subscriptions.set('emergency-approvals', sub);
    }

    if (this.tokenCancelledHandler) {
      const sub = this.client.subscribe('/user/queue/token-cancelled', (message) => {
        try {
          const data = JSON.parse(message.body);
          this.tokenCancelledHandler(data);
        } catch (error) {
          console.error('Error parsing cancellation message:', error);
        }
      });
      this.subscriptions.set('token-cancellations', sub);
    }
  }

  subscribeToQueue(queueId) {
    if (!this.client || !this.client.connected) {
      console.warn('WebSocket not connected, queueing subscription');
      this.queueSubscription('queue', queueId);
      return;
    }

    const subscription = this.client.subscribe(`/topic/queues/${queueId}`, (message) => {
      try {
        const queueData = JSON.parse(message.body);
        store.dispatch(updateQueue(queueData));
      } catch (error) {
        console.error('Error parsing queue message:', error);
      }
    });

    this.subscriptions.set(`queue-${queueId}`, subscription);
    console.log(`✅ Subscribed to queue: ${queueId}`);
  }

  subscribeToUserUpdates() {
    if (!this.client || !this.client.connected) return;

    const user = store.getState().auth;
    if (!user || !user.id) {
      console.error('No user information available for subscription');
      return;
    }

    const subscription = this.client.subscribe(`/user/queue/provider-updates`, (message) => {
      try {
        const queueData = JSON.parse(message.body);
        store.dispatch(updateQueue(queueData));
      } catch (error) {
        console.error('Error parsing user update message:', error);
      }
    });

    this.subscriptions.set('user-updates', subscription);
  }

  sendMessage(destination, body) {
    if (!this.client || !this.client.connected) {
      console.error('WebSocket not connected, cannot send message');
      return false;
    }

    try {
      this.client.publish({
        destination,
        body: JSON.stringify(body),
      });
      return true;
    } catch (error) {
      console.error('Error sending WebSocket message:', error);
      return false;
    }
  }

  unsubscribeFromQueue(queueId) {
    const key = `queue-${queueId}`;
    const subscription = this.subscriptions.get(key);
    if (subscription) {
      subscription.unsubscribe();
      this.subscriptions.delete(key);
      console.log(`❌ Unsubscribed from queue: ${queueId}`);
    }
  }

  disconnect() {
    if (this.client) {
      this.subscriptions.forEach((subscription) => {
        subscription.unsubscribe();
      });
      this.subscriptions.clear();
      this.client.deactivate();
      console.log('WebSocket disconnected');
      store.dispatch(connectionFailure());
    }
  }

  queueSubscription(type, id) {
    setTimeout(() => {
      if (type === 'queue') {
        this.subscribeToQueue(id);
      }
    }, 1000);
  }

  resubscribeToAll() {
    this.subscriptions.forEach((subscription) => {
      subscription.unsubscribe();
    });
    this.subscriptions.clear();

    const state = store.getState();

    this.subscribeToUserUpdates();
    this.subscribeToUserNotifications();

    if (state.queue.data && state.queue.data.id) {
      this.subscribeToQueue(state.queue.data.id);
    }
  }

  handleDisconnection() {
    this.reconnectAttempts++;
    if (this.reconnectAttempts > this.maxReconnectAttempts) {
      console.error('Max reconnection attempts reached');
      this.disconnect();
      return;
    }
    console.log(`🔄 Attempting to reconnect (${this.reconnectAttempts}/${this.maxReconnectAttempts})`);
    setTimeout(() => {
      this.connect();
    }, 5000);
  }
}

export default new WebSocketService();