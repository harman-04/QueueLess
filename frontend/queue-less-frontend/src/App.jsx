import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { useEffect } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import Register from './pages/Register';
import Login from './pages/Login';
import Home from './pages/Home';
import Navbar from './components/Navbar';
import Footer from './components/Footer';
import ForgotPassword from './pages/ForgotPassword';
import VerifyOtp from './pages/VerifyOtp';
import ResetPassword from './pages/ResetPassword';
import PricingPage from './pages/PricingPage';
import ProviderPricingPage from './pages/ProviderPricingPage';
import AdminDashboard from './pages/AdminDashboard';
import ProviderDashboard from './pages/ProviderDashboard';
import ProviderQueueManagement from './components/ProviderQueueManagement';
import UserDashboard from './pages/UserDashboard';
import CustomerQueue from './components/CustomerQueue';
import PlaceList from './components/PlaceList';
import PlaceForm from './components/PlaceForm';
import PlaceDetail from './components/PlaceDetail';
import ServiceManagement from './components/ServiceManagement';
import ProtectedRoute from './components/ProtectedRoute';
import { Navigate } from 'react-router-dom';
import WebSocketService from './services/websocketService';
import AdminPlaces from './components/AdminPlaces';
import UserProfile from './pages/UserProfile';
import AdvancedSearch from './components/AdvancedSearch';
import FavoritesPage from './pages/FavoritesPage';
import { logout } from './redux/authSlice';
import ErrorBoundary from './components/ErrorBoundary';
import VerifyEmail from './pages/VerifyEmail';
import useFcmToken from './hooks/useFcmToken';
import NotFound from './pages/NotFound';
import AdminProviderDetail from './pages/AdminProviderDetail';
import NotificationPreferences from './pages/NotificationPreferences';
import ResetPasswordWithToken from './pages/ResetPasswordWithToken';
import AboutUs from './pages/AboutUs';
import Contact from './pages/Contact';
import Legal from "./pages/Legal"
import TermsOfService from './pages/TermsOfService';
import Documentation from './pages/Documentation';
import HowToUse from './pages/HowToUse';

function App() {
  const { token, role } = useSelector((state) => state.auth);
  const { darkMode } = useSelector((state) => state.auth.preferences);
  const dispatch = useDispatch();

  const { token: authToken, id: userId, preferences } = useSelector((state) => state.auth);
  const isLoggedIn = !!authToken;
  const pushEnabled = preferences?.pushNotifications ?? true; // default true if undefined
  useFcmToken(userId, isLoggedIn, pushEnabled);


  useEffect(() => {
    if (darkMode) {
      document.body.classList.add('dark-mode');
    } else {
      document.body.classList.remove('dark-mode');
    }
  }, [darkMode]);

  useEffect(() => {
    // 1. Check Token Expiry
    const checkTokenExpiry = () => {
      const storedToken = localStorage.getItem('token');
      if (storedToken) {
        try {
          const payload = JSON.parse(atob(storedToken.split('.')[1]));
          const expiryTime = payload.exp * 1000;
          if (Date.now() > expiryTime) {
            dispatch(logout());
            return false;
          }
          return true;
        } catch (error) {
          console.error('Error checking token expiry:', error);
          dispatch(logout());
          return false;
        }
      }
      return false;
    };

    const isTokenValid = checkTokenExpiry();

    // 2. Manage WebSocket Lifecycle
    if (token && isTokenValid) {
      // Set Handlers FIRST before connecting
      WebSocketService.setEmergencyApprovalHandler((data) => {
        window.dispatchEvent(new CustomEvent('emergency-approval', { detail: data }));
      });

      WebSocketService.setTokenCancelledHandler((data) => {
        window.dispatchEvent(new CustomEvent('token-cancelled', { detail: data }));
      });

      WebSocketService.connect();

      // Subscribe to role-specific updates
      if (role === 'PROVIDER' || role === 'ADMIN') {
        WebSocketService.subscribeToUserUpdates();
      }
    } else {
      WebSocketService.disconnect();
    }

    return () => {
      // Cleanup: Unset handlers and disconnect
      WebSocketService.setEmergencyApprovalHandler(null);
      WebSocketService.setTokenCancelledHandler(null);
      WebSocketService.disconnect();
    };
  }, [token, role, dispatch]);

  return (
    <BrowserRouter>
      <Navbar />
      <ErrorBoundary>
        <main className="app-main">
          <Routes>
            <Route path='/' element={<Home />} />
            <Route path="/register" element={<Register />} />
            <Route path="/login" element={<Login />} />
            <Route path="/forgot-password" element={<ForgotPassword />} />
            <Route path="/verify-otp" element={<VerifyOtp />} />
            <Route path="/reset-password" element={<ResetPassword />} />
            <Route path="/pricing" element={<PricingPage />} />
            <Route path="/customer/queue/:queueId" element={<CustomerQueue />} />
            <Route path="/about" element={<AboutUs />} />
            <Route path="/contact" element={<Contact />} />
            <Route path="/documentation" element={<Documentation />} />
            <Route path="/legal" element={<Legal />} />
            <Route path="/terms-of-service" element={<TermsOfService />} />
            <Route path="/how-to-use" element={<HowToUse />} />
            <Route path="/search" element={<AdvancedSearch />} />
            <Route path="/favorites" element={<ProtectedRoute><FavoritesPage /></ProtectedRoute>} />
            <Route path="/verify-email" element={<VerifyEmail />} />
            <Route path="/reset-password-token" element={<ResetPasswordWithToken />} />
            <Route path='/profile' element={
              <ProtectedRoute allowedRoles={['USER', 'ADMIN', 'PROVIDER']}>
                <UserProfile />
              </ProtectedRoute>
            } />

            <Route path='/provider-pricing' element={
              <ProtectedRoute allowedRoles={['ADMIN']}>
                <ProviderPricingPage />
              </ProtectedRoute>
            } />

            <Route path="/user/dashboard" element={
              <ProtectedRoute allowedRoles={['USER']}>
                <UserDashboard />
              </ProtectedRoute>
            } />

            <Route path="/user/notifications" element={
              <ProtectedRoute allowedRoles={['USER']}>
                <NotificationPreferences />
              </ProtectedRoute>
            } />

            <Route path="/provider/queues" element={
              <ProtectedRoute allowedRoles={['PROVIDER', 'ADMIN']}>
                <ProviderQueueManagement />
              </ProtectedRoute>
            } />

            <Route path="/provider/dashboard/:queueId" element={
              <ProtectedRoute allowedRoles={['PROVIDER', 'ADMIN']}>
                <ProviderDashboard />
              </ProtectedRoute>
            } />

            <Route path="/provider/dashboard" element={<Navigate to="/provider/queues" />} />

            <Route path="/admin/dashboard" element={
              <ProtectedRoute allowedRoles={['ADMIN']}>
                <AdminDashboard />
              </ProtectedRoute>
            } />

            <Route path="/admin/places" element={
              <ProtectedRoute allowedRoles={['ADMIN']}>
                <AdminPlaces />
              </ProtectedRoute>
            } />

            {/* Place routes */}
            <Route path="/places" element={<PlaceList />} />
            <Route path="/places/new" element={
              <ProtectedRoute allowedRoles={['ADMIN']}>
                <PlaceForm />
              </ProtectedRoute>
            } />
            <Route path="/places/edit/:id" element={
              <ProtectedRoute allowedRoles={['ADMIN']}>
                <PlaceForm />
              </ProtectedRoute>
            } />
            <Route path="/places/:id" element={<PlaceDetail />} />

            <Route path="/admin/places/:placeId/services" element={
              <ProtectedRoute allowedRoles={['ADMIN']}>
                <ServiceManagement />
              </ProtectedRoute>
            } />

            <Route path="/admin/providers/:providerId" element={
              <ProtectedRoute allowedRoles={['ADMIN']}>
                <AdminProviderDetail />
              </ProtectedRoute>
            } />

            {/* Catch all route */}
            {/* <Route path="*" element={<Navigate to="/" replace />} /> */}
            <Route path="*" element={<NotFound />} />
          </Routes>
        </main>

      </ErrorBoundary>

      <Footer />
    </BrowserRouter>
  );
}

export default App;