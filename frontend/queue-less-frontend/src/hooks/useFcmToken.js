// src/hooks/useFcmToken.js
import { useEffect, useState } from 'react';
import { messaging, getToken, onMessage } from '../firebase';
import axiosInstance from '../utils/axiosInstance';
import { toast } from 'react-toastify';

const useFcmToken = (userId, isLoggedIn, pushEnabled) => {
  const [token, setToken] = useState(null);

  useEffect(() => {
    if (!isLoggedIn || !messaging || !pushEnabled) return;

    const registerToken = async (currentToken) => {
      setToken(currentToken);
      await axiosInstance.post(`/user/fcm-token?token=${encodeURIComponent(currentToken)}`);
    };

    const requestPermission = async () => {
      try {
        const permission = await Notification.requestPermission();
        if (permission === 'granted') {
          const currentToken = await getToken(messaging, {
            vapidKey: import.meta.env.VITE_FIREBASE_VAPID_KEY,
          });
          if (currentToken) {
            registerToken(currentToken);
          } else {
            console.log('No registration token available.');
          }
        } else {
          console.log('Notification permission denied.');
        }
      } catch (error) {
        console.error('Error getting FCM token:', error);
      }
    };

    requestPermission();

    const messageUnsubscribe = onMessage(messaging, (payload) => {
      console.log('Foreground message received:', payload);
      toast.info(`${payload.notification.title}: ${payload.notification.body}`);
    });

    return () => {
      messageUnsubscribe();
    };
  }, [isLoggedIn, userId, pushEnabled]);

  return token;
};

export default useFcmToken;