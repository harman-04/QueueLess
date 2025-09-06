import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { useEffect } from 'react';
import { useSelector } from 'react-redux';
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

function App() {
  const { token, role } = useSelector((state) => state.auth);

  useEffect(() => {
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
  }, [token, role]);

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
        
        {/* Protected Routes */}

        <Route path='/provider-pricing' element={
          <ProtectedRoute allowedRoles={['ADMIN']}>
            <ProviderPricingPage/>
         </ProtectedRoute>
      }/>

        <Route path="/user/dashboard" element={
          <ProtectedRoute allowedRoles={['USER', 'ADMIN', 'PROVIDER']}>
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