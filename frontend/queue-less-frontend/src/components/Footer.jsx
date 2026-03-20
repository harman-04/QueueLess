import React, { useState, useEffect } from "react";
import { Link } from "react-router-dom";
import "./Footer.css";

const Footer = () => {
  const [showBackToTop, setShowBackToTop] = useState(false);

  useEffect(() => {
    const handleScroll = () => {
      setShowBackToTop(window.scrollY > 400);
    };
    window.addEventListener('scroll', handleScroll);
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  const scrollToTop = () => {
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  return (
    <footer className="footer-section">
      <div className="footer-container">
        {/* Brand Section */}
        <div className="footer-brand">
          <h2 className="brand-name">Queue<span>Less</span></h2>
          <p className="brand-tagline">
            Making your waiting experience simple, smart, and seamless.
          </p>
          <p className="brand-description">Join queues, track wait times, and get notified when it's your turn.</p>

        </div>

        {/* Quick Links Section */}
        <div className="footer-links">
          <h4>Quick Links</h4>
          <ul>
            <li><Link to="/about">About Us</Link></li>
            <li><Link to="/how-to-use">How to Use</Link></li>
            <li><Link to="/documentation">Documentation</Link></li>
            <li><Link to="/legal">Privacy Policy</Link></li>
          </ul>
        </div>

        {/* Contact & Follow Us Section */}
        <div className="footer-contact">
          <h4>Contact Us</h4>
          <div className="contact-info">
            <a href="mailto:support@queueless.com" className="contact-link">
              <i className="fas fa-envelope"></i> harmandeep02315@gmail.com
            </a>
            {/* <span className="contact-phone">
              <i className="fas fa-phone-alt"></i> +1 (555) 123-4567
            </span> */}
          </div>
          <h4 className="mt-3">Follow Us</h4>
          <div className="social-icons">
            <a href="https://github.com/harman-04" className="social-icon" target="_blank"><i className="fab fa-github"></i></a>
            <a href="https://www.linkedin.com/in/harmandeep700" target="_blank" className="social-icon"><i className="fab fa-linkedin-in"></i></a>

          </div>
        </div>
      </div>

      <div className="footer-bottom">
        <p>&copy; {new Date().getFullYear()} <strong>QueueLess</strong>. All rights reserved.</p>
        <small>Crafted with ❤️ for a smoother queuing experience.</small>
      </div>

      {showBackToTop && (
        <button className="back-to-top" onClick={scrollToTop} aria-label="Back to top">
          ↑
        </button>
      )}
    </footer>
  );
};

export default Footer;