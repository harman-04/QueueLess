//src/components/Footer.jsx
import React from "react";
import "./Footer.css";

const Footer = () => {
  return (
    <footer className="footer-section">
      <div className="footer-container">
        {/* Logo / Branding */}
        <div className="footer-brand">
          <h2 className="brand-name">Queue<span>Less</span></h2>
          <p className="brand-tagline">
            Making your waiting experience simple, smart, and seamless.
          </p>
        </div>

        {/* Quick Links */}
        <div className="footer-links">
          <h4>Quick Links</h4>
          <ul>
            <li><a href="#">About Us</a></li>
            <li><a href="#">Contact</a></li>
            <li><a href="#">Privacy Policy</a></li>
            <li><a href="#">Terms of Service</a></li>
          </ul>
        </div>

        {/* Social Media */}
        <div className="footer-contact">
          <h4>Follow Us</h4>
          <div className="social-icons">
            <a href="#" className="social-icon"><i className="fab fa-twitter"></i></a>
            <a href="#" className="social-icon"><i className="fab fa-linkedin-in"></i></a>
            <a href="#" className="social-icon"><i className="fab fa-instagram"></i></a>
          </div>
        </div>
      </div>

      {/* Bottom Bar */}
      <div className="footer-bottom">
        <p>&copy; {new Date().getFullYear()} <strong>QueueLess</strong>. All rights reserved.</p>
        <small>Crafted with ❤️ for a smoother queuing experience.</small>
      </div>
    </footer>
  );
};

export default Footer;
