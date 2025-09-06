//src/pages/Home
import { useNavigate } from 'react-router-dom';
import { Button } from 'react-bootstrap';
import { FaStar, FaMobileAlt, FaClock, FaCheckCircle } from 'react-icons/fa';
import { useSelector } from 'react-redux'; // Import useSelector
import './Home.css';


const Home = () => {
  const navigate = useNavigate();
  const { token, role } = useSelector((state) => state.auth);

  const handleGetStarted = () => {
    if (token) {
      // User is logged in, navigate to their respective dashboard
      switch (role) {
        case 'ADMIN':
          navigate('/admin/dashboard');
          break;
        case 'PROVIDER':
          navigate('/provider/queues');
          break;
        case 'USER':
          navigate('/user/dashboard');
          break;
        default:
          navigate('/');
          break;
      }
    } else {
      // User is not logged in, navigate to register page
      navigate('/register');
    }
  };

  return (
    <div className="home-wrapper">
      {/* Hero Section */}
      <section className="hero-section text-white text-center d-flex align-items-center">
        <div className="container">
          <h1 className="display-4 fw-bold animate__animated animate__fadeInDown">
            Your Time is Valuable. Don't Waste It in Queues.
          </h1>
          <p className="lead mt-3 animate__animated animate__fadeInUp animate__delay-1s">
            QueueLess provides a seamless way to book tokens and manage your appointments.
          </p>
          <Button
            size="lg"
            variant="light"
            className="mt-4 animate__animated animate__zoomIn animate__delay-2s"
            onClick={handleGetStarted}
          >
            {token ? 'Go to Dashboard' : 'Get Started'}
          </Button>
        </div>
      </section>

      {/* Features Section */}
      <section className="features-section bg-white text-center py-5">
        <div className="container">
          <h2 className="fw-bold mb-5 text-dark">Why QueueLess?</h2>
          <div className="row g-4">
            <div className="col-md-4">
              <div className="p-4 rounded h-100 feature-card">
                <FaMobileAlt size={50} color="#4f8df7" className="mb-3" />
                <h5>Book on the Go</h5>
                <p className="text-muted">Easily reserve your spot from anywhere using your smartphone.</p>
              </div>
            </div>
            <div className="col-md-4">
              <div className="p-4 rounded h-100 feature-card">
                <FaClock size={50} color="#4f8df7" className="mb-3" />
                <h5>Real-Time Updates</h5>
                <p className="text-muted">Receive live notifications on queue status and estimated wait times.</p>
              </div>
            </div>
            <div className="col-md-4">
              <div className="p-4 rounded h-100 feature-card">
                <FaCheckCircle size={50} color="#4f8df7" className="mb-3" />
                <h5>Streamlined Experience</h5>
                <p className="text-muted">Arrive just in time for your turn, avoiding unnecessary waiting.</p>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Ratings */}
      <section className="rating-section py-5 text-center bg-light">
        <div className="container">
          <h3 className="fw-bold mb-3 text-dark">Trusted by Thousands</h3>
          <div className="d-flex justify-content-center align-items-center">
            {[...Array(5)].map((_, i) => (
              <FaStar key={i} size={28} color="#f7c948" className="mx-1" />
            ))}
          </div>
          <p className="mt-3 text-muted">Rated 5.0 based on user feedback</p>
        </div>
      </section>
    </div>
  );
};

export default Home;