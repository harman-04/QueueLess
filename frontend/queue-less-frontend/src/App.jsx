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

function App() {
  const { token, role } = useSelector((state) => state.auth);
  const dispatch = useDispatch();

  useEffect(() => {
    // Check if token exists and might be expired
    const checkTokenExpiry = () => {
      const token = localStorage.getItem('token');
      if (token) {
        try {
          // Simple check for JWT expiry (this is a basic check, you might want to use a library like jwt-decode)
          const payload = JSON.parse(atob(token.split('.')[1]));
          const expiryTime = payload.exp * 1000; // Convert to milliseconds
          if (Date.now() > expiryTime) {
            // Token has expired, log out user
            dispatch(logout());
            return;
          }
        } catch (error) {
          console.error('Error checking token expiry:', error);
          // If we can't parse the token, log out for safety
          dispatch(logout());
        }
      }
    };

    checkTokenExpiry();

    // Initialize WebSocket connection if user is authenticated
    if (token) {
      WebSocketService.connect();
      
      // Subscribe to user updates based on role
      if (role === 'PROVIDER' || role === 'ADMIN') {
        WebSocketService.subscribeToUserUpdates();
      }
    } else {
      // Disconnect WebSocket if user logs out
      WebSocketService.disconnect();
    }

    return () => {
      // Cleanup on component unmount
      WebSocketService.disconnect();
    };
  }, [token, role, dispatch]);

  return (
    <BrowserRouter>
      <Navbar/>
      <Routes>
        <Route path='/' element={<Home/>}/>
        <Route path="/register" element={<Register />} />
        <Route path="/login" element={<Login />} />
        <Route path="/forgot-password" element={<ForgotPassword />} />
        <Route path="/verify-otp" element={<VerifyOtp />} />
        <Route path="/reset-password" element={<ResetPassword />} />
        <Route path="/pricing" element={<PricingPage />} />
        <Route path="/customer/queue/:queueId" element={<CustomerQueue />} />
        <Route path="/search" element={<AdvancedSearch />} />
        <Route path="/favorites" element={<ProtectedRoute><FavoritesPage /></ProtectedRoute>} />
        
        <Route path='/profile' element={
          <ProtectedRoute allowedRoles={['USER', 'ADMIN', 'PROVIDER']}>
            <UserProfile/>
          </ProtectedRoute>
        }/>
        
        <Route path='/provider-pricing' element={
          <ProtectedRoute allowedRoles={['ADMIN']}>
            <ProviderPricingPage/>
          </ProtectedRoute>
        }/>

        <Route path="/user/dashboard" element={
          <ProtectedRoute allowedRoles={['USER']}>
            <UserDashboard />
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

        {/* Catch all route */}
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
      <Footer/>
    </BrowserRouter>
  );
}

export default App;