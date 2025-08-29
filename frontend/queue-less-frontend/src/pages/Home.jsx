import { useNavigate } from 'react-router-dom';
import { Button } from 'react-bootstrap';
import { FaStar } from 'react-icons/fa';
import './Home.css';


const Home = () => {
  const navigate = useNavigate();
//   const { token } = useSelector((state) => state.auth);

//   const handleLogin = () => navigate('/login');
  const handleSignup = () => navigate('/register');

  return (
    <div className="home-wrapper">
      {/* Navbar */}
     

      {/* Hero Section */}
      <section className="hero-section text-white text-center d-flex align-items-center">
        <div className="container">
          <h1 className="display-4 fw-bold">Say Goodbye to Long Queues!</h1>
          <p className="lead mt-3">QueueLess helps you book tokens, avoid waiting, and manage your time better.</p>
          <Button size="lg" variant="light" className="mt-4" onClick={handleSignup}>
            Get Started
          </Button>
        </div>
      </section>

      {/* Info Section */}
      <section className="info-section bg-light text-center py-5">
        <div className="container">
          <h2 className="fw-bold mb-4">Why QueueLess?</h2>
          <div className="row g-4">
            <div className="col-md-4">
              <div className="p-4 border rounded bg-white h-100">
                <h5>Smart Scheduling</h5>
                <p>Book time slots that fit your schedule and get real-time updates on wait times.</p>
              </div>
            </div>
            <div className="col-md-4">
              <div className="p-4 border rounded bg-white h-100">
                <h5>Group Tokening</h5>
                <p>Visit centers as a family/group and get a single token for all members.</p>
              </div>
            </div>
            <div className="col-md-4">
              <div className="p-4 border rounded bg-white h-100">
                <h5>AI Recommendations</h5>
                <p>Get suggestions for least-crowded times using smart prediction.</p>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Ratings */}
      <section className="rating-section py-5 text-center bg-white">
        <div className="container">
          <h3 className="fw-bold mb-3">Trusted by Thousands</h3>
          <div className="d-flex justify-content-center align-items-center">
            {[...Array(5)].map((_, i) => (
              <FaStar key={i} size={28} color="#f7c948" className="mx-1" />
            ))}
          </div>
          <p className="mt-3 text-muted">Rated 5.0 based on user feedback</p>
        </div>
      </section>

      {/* Footer */}
    
    </div>
  );
};

export default Home;
