import { useNavigate } from 'react-router-dom';
import { Button, Spinner, Carousel } from 'react-bootstrap';
import { FaStar, FaUsers , FaMobileAlt, FaClock, FaCheckCircle, FaQrcode, FaChartLine, FaMapMarkerAlt, FaHeart } from 'react-icons/fa';
import { useSelector } from 'react-redux';
import { useState, useEffect } from 'react';
import 'animate.css';
import './Home.css';
import QRScannerModal from '../components/QRScannerModal';
import { getRecentFeedback, getTopRatedPlaces, getPublicStats , getLiveStats } from '../services/homeService';
import { useInView } from '../hooks/useInView';
import { useCountUp } from '../hooks/useCountUp';

const Home = () => {
  const navigate = useNavigate();
  const { token, role } = useSelector((state) => state.auth);
  const [showScanner, setShowScanner] = useState(false);

  // Data states
  const [stats, setStats] = useState({ totalUsers: 0, totalPlaces: 0, totalQueuesServed: 0 });
  const [topPlaces, setTopPlaces] = useState([]);
  const [recentFeedback, setRecentFeedback] = useState([]);
  const [loadingStats, setLoadingStats] = useState(true);
  const [loadingPlaces, setLoadingPlaces] = useState(true);
  const [loadingFeedback, setLoadingFeedback] = useState(true);

  const [liveStats, setLiveStats] = useState({ activeQueues: 0, averageWaitTime: 0 });
const [loadingLiveStats, setLoadingLiveStats] = useState(true);

  // Format large numbers (e.g., 1542 -> 1.5K)
  const formatNumber = (num) => {
    if (num >= 1000000) return (num / 1000000).toFixed(1) + 'M';
    if (num >= 1000) return (num / 1000).toFixed(1) + 'K';
    return num.toString();
  };
  

  useEffect(() => {
    const fetchHomeData = async () => {
  try {
    const [statsRes, placesRes, feedbackRes, liveStatsRes] = await Promise.all([
      getPublicStats(),
      getTopRatedPlaces(3),
      getRecentFeedback(5),
      getLiveStats()              // new function
    ]);
    setStats(statsRes.data);
    setTopPlaces(placesRes.data);
    setRecentFeedback(feedbackRes.data);
    setLiveStats(liveStatsRes.data);
  } catch (error) {
    console.error('Failed to fetch homepage data:', error);
    // fallbacks remain
  } finally {
    setLoadingStats(false);
    setLoadingPlaces(false);
    setLoadingFeedback(false);
    setLoadingLiveStats(false);
  }
};
    fetchHomeData();
  }, []);

  const handleGetStarted = () => {
    if (token) {
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
      }
    } else {
      navigate('/register');
    }
  };

  // Animated section wrapper
  const AnimatedSection = ({ children, className }) => {
    const [ref, inView] = useInView({ threshold: 0.2 });
    return (
      <div
        ref={ref}
        className={`${className} ${inView ? 'animate-in' : 'animate-out'}`}
      >
        {children}
      </div>
    );
  };

  // Animated stat item
  const StatItem = ({ value, label, loading }) => {
    const [ref, inView] = useInView({ threshold: 0.1 }); 
  const count = useCountUp(inView ? value : 0, 2000, 100);
      return (
      <div ref={ref} className="stat-item">
        <h3 className="fw-bold">
          {loading ? <Spinner animation="border" size="sm" variant="light" /> : formatNumber(count) + '+'}
        </h3>
        <p>{label}</p>
      </div>
    );
  };

  return (
    <div className="home-wrapper">
      {/* Hero Section */}
      <section className="hero-section text-white text-center d-flex align-items-center">
        <div className="container">
          <h1 className="display-3 fw-bold animate__animated animate__fadeInDown">
            Your Time is Valuable. <span className="text-highlight">Don't Waste It in Queues.</span>
          </h1>
          <p className="lead mt-3 animate__animated animate__fadeInUp animate__delay-1s">
            QueueLess provides a seamless way to book tokens, track queues, and get notified when it's your turn.
          </p>
          <div className="hero-buttons mt-4 animate__animated animate__zoomIn animate__delay-2s">
            <Button size="lg" variant="light" className="me-3 shadow-sm" onClick={handleGetStarted}>
              {token ? 'Go to Dashboard' : 'Get Started'}
            </Button>
            {token && (
              <Button size="lg" variant="outline-light" className="shadow-sm" onClick={() => setShowScanner(true)}>
                <FaQrcode className="me-2" /> Scan QR
              </Button>
            )}
          </div>
          <div className="hero-stats mt-5 d-flex justify-content-center gap-5">
            <StatItem value={stats.totalUsers} label="Active Users" loading={loadingStats} />
            <StatItem value={stats.totalPlaces} label="Places" loading={loadingStats} />
            <StatItem value={stats.totalQueuesServed} label="Queues Served" loading={loadingStats} />
          </div>
        </div>
      </section>

      {/* Features Section */}
      <AnimatedSection className="features-section py-5">
        <div className="container">
          <h2 className="text-center fw-bold mb-5 section-title">Why Choose QueueLess?</h2>
          <div className="row g-4">
            <div className="col-md-4">
              <div className="feature-card h-100 text-center p-4">
                <div className="feature-icon mb-3">
                  <FaMobileAlt size={50} />
                </div>
                <h5>Book on the Go</h5>
                <p className="text-secondary">Easily reserve your spot from anywhere using your smartphone.</p>
              </div>
            </div>
            <div className="col-md-4">
              <div className="feature-card h-100 text-center p-4">
                <div className="feature-icon mb-3">
                  <FaClock size={50} />
                </div>
                <h5>Real-Time Updates</h5>
                <p className="text-secondary">Receive live notifications on queue status and estimated wait times.</p>
              </div>
            </div>
            <div className="col-md-4">
              <div className="feature-card h-100 text-center p-4">
                <div className="feature-icon mb-3">
                  <FaCheckCircle size={50} />
                </div>
                <h5>Streamlined Experience</h5>
                <p className="text-secondary">Arrive just in time for your turn, avoiding unnecessary waiting.</p>
              </div>
            </div>
          </div>
        </div>
      </AnimatedSection>

      {/* Popular Places Section */}
      <AnimatedSection className="popular-places-section py-5">
        <div className="container">
          <h2 className="text-center fw-bold mb-5 section-title">Popular Places</h2>
          {loadingPlaces ? (
            <div className="text-center py-4">
              <Spinner animation="border" variant="primary" />
            </div>
          ) : (
            <div className="row g-4">
              {topPlaces.map((place) => (
                <div key={place.id} className="col-md-4">
                  <div className="popular-place-card h-100">
                    <div className="popular-place-img-container">
                      <img
                        src={place.imageUrls?.[0] || 'https://via.placeholder.com/400x250?text=No+Image'}
                        alt={place.name}
                        className="popular-place-img"
                        onError={(e) => (e.target.src = 'https://via.placeholder.com/400x250?text=No+Image')}
                      />
                    </div>
                    <div className="popular-place-body">
                      <h5 className="popular-place-title">{place.name}</h5>
                      <p className="popular-place-address">
                        <FaMapMarkerAlt className="me-1" /> {place.address}
                      </p>
                      <div className="popular-place-rating mb-2">
                        <FaStar className="text-warning me-1" />
                        <span>{place.rating?.toFixed(1) || 'N/A'}</span>
                        <span className="text-muted ms-2 small">({place.totalRatings || 0} reviews)</span>
                      </div>
                      <Button
                        variant="outline-primary"
                        className="w-100"
                        onClick={() => navigate(`/places/${place.id}`)}
                      >
                        View Details
                      </Button>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </AnimatedSection>

{/* Live Queue Stats */}
<AnimatedSection className="live-stats-section py-5">
  <div className="container">
    <h2 className="text-center fw-bold mb-5 section-title">Live Queue Status</h2>
    {loadingLiveStats ? (
      <div className="text-center py-4">
        <Spinner animation="border" variant="primary" />
      </div>
    ) : (
      <div className="row justify-content-center">
        <div className="col-md-5 col-lg-4 mb-3">
          <div className="live-stats-card text-center p-4">
            <FaUsers className="live-stats-icon mb-3" />
            <h3 className="live-stats-number">{liveStats.activeQueues}</h3>
            <p className="live-stats-label">Active Queues</p>
          </div>
        </div>
        <div className="col-md-5 col-lg-4 mb-3">
          <div className="live-stats-card text-center p-4">
            <FaClock className="live-stats-icon mb-3" />
            <h3 className="live-stats-number">{liveStats.averageWaitTime} <small>min</small></h3>
            <p className="live-stats-label">Average Wait Time</p>
          </div>
        </div>
      </div>
    )}
  </div>
</AnimatedSection>
{/* Pricing Teaser */}
<AnimatedSection className="pricing-teaser-section py-5">
  <div className="container">
    <h2 className="text-center fw-bold mb-5 section-title">Simple, Transparent Pricing</h2>
    <div className="row g-4 justify-content-center">
      {/* Basic Plan */}
      <div className="col-md-4">
        <div className="pricing-teaser-card text-center p-4">
          <h3 className="pricing-plan-name">Basic</h3>
          <div className="pricing-price">
            <span className="pricing-amount">₹100</span>
            <span className="pricing-duration">/month</span>
          </div>
          <p className="pricing-description">Perfect for new admins starting out.</p>
          <Button
            variant="primary"
            className="mt-3 w-100"
            onClick={() => navigate('/pricing')}
          >
            Get Started
          </Button>
        </div>
      </div>

      {/* Standard Plan */}
      <div className="col-md-4">
        <div className="pricing-teaser-card text-center p-4 featured">
          <h3 className="pricing-plan-name">Standard</h3>
          <div className="pricing-price">
            <span className="pricing-amount">₹500</span>
            <span className="pricing-duration">/year</span>
          </div>
          <p className="pricing-description">Best value for committed admins.</p>
          <Button
            variant="primary"
            className="mt-3 w-100"
            onClick={() => navigate('/pricing')}
          >
            Get Started
          </Button>
        </div>
      </div>

      {/* Enterprise Plan */}
      <div className="col-md-4">
        <div className="pricing-teaser-card text-center p-4">
          <h3 className="pricing-plan-name">Enterprise</h3>
          <div className="pricing-price">
            <span className="pricing-amount">₹1000</span>
            <span className="pricing-duration">lifetime</span>
          </div>
          <p className="pricing-description">Full access for platform leaders.</p>
          <Button
            variant="primary"
            className="mt-3 w-100"
            onClick={() => navigate('/pricing')}
          >
            Get Started
          </Button>
        </div>
      </div>
    </div>
    
    {/* Provider link – only visible to admins */}
    {role === 'ADMIN' && (
      <div className="text-center mt-5">
        <p className="text-muted">
          <FaHeart className="me-2 text-danger" />
          Provider plans also available – 
          <Button variant="link" className="p-0 ms-1" onClick={() => navigate('/provider-pricing')}>
            learn more
          </Button>
        </p>
      </div>
    )}
  </div>
</AnimatedSection>

      {/* How It Works Section */}
      <AnimatedSection className="how-it-works-section py-5">
        <div className="container">
          <h2 className="text-center fw-bold mb-5 section-title">How It Works</h2>
          <div className="row align-items-center">
            <div className="col-md-6">
              <img src="/queueless-how-it-work.png" alt="How it works" className="img-fluid rounded-4 shadow" />
            </div>
            <div className="col-md-6">
              <div className="step-item d-flex mb-4">
                <div className="step-number me-3">1</div>
                <div>
                  <h5>Find a Place</h5>
                  <p className="text-secondary">Search for nearby places or scan a QR code to join instantly.</p>
                </div>
              </div>
              <div className="step-item d-flex mb-4">
                <div className="step-number me-3">2</div>
                <div>
                  <h5>Join the Queue</h5>
                  <p className="text-secondary">Choose your token type and get a position in line.</p>
                </div>
              </div>
              <div className="step-item d-flex mb-4">
                <div className="step-number me-3">3</div>
                <div>
                  <h5>Get Notified</h5>
                  <p className="text-secondary">Receive updates when it's almost your turn.</p>
                </div>
              </div>
              <div className="step-item d-flex">
                <div className="step-number me-3">4</div>
                <div>
                  <h5>Provide Feedback</h5>
                  <p className="text-secondary">Rate your experience and help others.</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </AnimatedSection>

      {/* Testimonials Section */}
      <AnimatedSection className="testimonials-section py-5">
        <div className="container">
          <h2 className="text-center fw-bold mb-5 section-title">What Our Users Say</h2>
          {loadingFeedback ? (
            <div className="text-center py-4">
              <Spinner animation="border" variant="primary" />
            </div>
          ) : recentFeedback.length === 0 ? (
            <p className="text-center text-muted">No feedback yet. Be the first to share your experience!</p>
          ) : (
            <Carousel indicators={false} controls={true} interval={5000} className="testimonial-carousel">
              {recentFeedback.map((fb, idx) => (
                <Carousel.Item key={fb.tokenId}>
                  <div className="testimonial-card text-center p-5">
                    <div className="testimonial-stars mb-3">
                      {[1, 2, 3, 4, 5].map((star) => (
                        <FaStar
                          key={star}
                          className={`star-icon ${star <= fb.rating ? 'filled' : ''}`}
                          size={24}
                        />
                      ))}
                    </div>
                    <p className="testimonial-comment lead">“{fb.comment || 'No comment provided.'}”</p>
                    <div className="testimonial-meta mt-4">
                      <span className="fw-bold">– Anonymous User</span>
                    </div>
                  </div>
                </Carousel.Item>
              ))}
            </Carousel>
          )}
        </div>
      </AnimatedSection>

      {/* CTA Section */}
      <AnimatedSection className="cta-section py-5">
        <div className="container">
          <div className="row align-items-center">
            <div className="col-md-8">
              <h2 className="fw-bold mb-3">Are you a business owner?</h2>
              <p className="lead mb-4">Join QueueLess as a provider or admin and streamline your customer flow.</p>
              {!token ? (
                <>
                  <Button variant="primary" size="lg" onClick={() => navigate('/pricing')} className="me-3 mb-3">
                    Become an Admin
                  </Button>
                  <Button variant="outline-primary" size="lg" onClick={() => navigate('/provider-pricing')} className="mb-3">
                    Become a Provider
                  </Button>
                </>
              ) : role === 'USER' ? (
                <>
                  <Button variant="primary" size="lg" onClick={() => navigate('/user/dashboard')} className="me-3 mb-3">
                    User Dashboard
                  </Button>
                  <Button variant="outline-primary" size="lg" onClick={() => navigate('/pricing')} className="mb-3">
                    Upgrade to Admin
                  </Button>
                </>
              ) : role === 'PROVIDER' ? (
                <>
                  <Button variant="primary" size="lg" onClick={() => navigate('/provider/queues')} className="me-3 mb-3">
                    Provider Dashboard
                  </Button>
                  <Button variant="outline-primary" size="lg" onClick={() => navigate('/pricing')} className="mb-3">
                    Upgrade to Admin
                  </Button>
                </>
              ) : role === 'ADMIN' ? (
                <>
                  <Button variant="primary" size="lg" onClick={() => navigate('/admin/dashboard')} className="me-3 mb-3">
                    Admin Dashboard
                  </Button>
                  <Button variant="outline-primary" size="lg" onClick={() => navigate('/provider-pricing')} className="mb-3">
                    Buy Provider Tokens
                  </Button>
                </>
              ) : null}
            </div>
            <div className="col-md-4 text-center">
              <FaChartLine size={120} className="cta-icon" />
            </div>
          </div>
        </div>
      </AnimatedSection>

      {/* Rating Section */}
      <AnimatedSection className="rating-section py-5 text-center">
        <div className="container">
          <h3 className="fw-bold mb-3">Trusted by Thousands</h3>
          <div className="d-flex justify-content-center align-items-center">
            {[...Array(5)].map((_, i) => (
              <FaStar key={i} size={32} className="star-icon mx-1" />
            ))}
          </div>
          <p className="mt-3 text-secondary">Rated 4.9 based on user feedback</p>
        </div>
      </AnimatedSection>

      <QRScannerModal show={showScanner} onHide={() => setShowScanner(false)} />
    </div>
  );
};

export default Home;