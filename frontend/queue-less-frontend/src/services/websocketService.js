// src/services/WebSocketService.js
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { updateQueue } from '../redux/queue/queueSlice';
import store from '../store/store';

class WebSocketService {
  constructor() {
    this.client = null;
    this.subscriptions = new Map();
    this.reconnectAttempts = 0;
    this.maxReconnectAttempts = 5;
  }

  // In WebSocketService.js - Update the connect method
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
        webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
        connectHeaders: {
            Authorization: `Bearer ${token}`,
        },
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
        
        onConnect: () => {
            console.log('✅ WebSocket connected');
            this.reconnectAttempts = 0;
            this.resubscribeToAll();
        },

        onStompError: (frame) => {
            console.error('WebSocket STOMP error:', frame.headers['message']);
            if (frame.headers['message'].includes('JWT') || frame.headers['message'].includes('token')) {
                console.error('Token-related WebSocket error');
                this.handleDisconnection();
            }
        },

        onDisconnect: () => {
            console.log('❌ WebSocket disconnected');
            this.handleDisconnection();
        }
    });

    this.client.activate();
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
    if (!this.client || !this.client.connected) {
      return;
    }

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
    }
  }

  queueSubscription(type, id) {
    // Store subscription requests to resubscribe after reconnection
    setTimeout(() => {
      if (type === 'queue') {
        this.subscribeToQueue(id);
      }
    }, 1000);
  }

  resubscribeToAll() {
    // Unsubscribe old subscriptions before resubscribing
    this.subscriptions.forEach((subscription) => {
      subscription.unsubscribe();
    });
    this.subscriptions.clear();

    const state = store.getState();

    // Subscribe to user updates
    this.subscribeToUserUpdates();

    // If already viewing a queue, resubscribe
    if (state.queue.data) {
      this.subscribeToQueue(state.queue.data.id);
    }
  }

  handleError(errorMessage) {
    console.error('WebSocket error:', errorMessage);

    if (errorMessage.includes('Invalid token') || errorMessage.includes('JWT')) {
      store.dispatch({ type: 'auth/logout' });
      this.disconnect();
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