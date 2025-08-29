import { BrowserRouter, Routes, Route } from 'react-router-dom';
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
import { Navigate } from 'react-router-dom';
function App() {
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
        <Route path='/provider-pricing' element={<ProviderPricingPage/>}/>
                <Route path="/customer/queue/:queueId" element={<CustomerQueue />} />

                <Route path="/user/dashboard" element={<UserDashboard />} /> {/* ✅ New user dashboard route */}

        {/* New route for managing all queues */}
              <Route path="/provider/queues" element={<ProviderQueueManagement />} />
              {/* Updated route for the specific queue dashboard */}
              <Route path="/provider/dashboard/:queueId" element={<ProviderDashboard />} />
              {/* Redirect to the management page if dashboard route is incomplete */}
              <Route path="/provider/dashboard" element={<Navigate to="/provider/queues" />} />
      </Routes>
      <Footer/>
    </BrowserRouter>
  );
}

export default App;
