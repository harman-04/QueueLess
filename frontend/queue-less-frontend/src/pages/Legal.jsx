import React from 'react';
import { Container, Row, Col, Card, Accordion, Badge } from 'react-bootstrap';
import { FaShieldAlt, FaFileContract, FaUserSecret, FaCookie, FaTrashAlt, FaEnvelope } from 'react-icons/fa';
import './Legal.css';

const Legal = () => {
  return (
    <div className="legal-page">
      <Container className="py-5">
        <div className="text-center mb-5">
          <h1 className="display-4 fw-bold gradient-text">Legal Information</h1>
          <p className="lead text-muted">Privacy Policy & Terms of Service</p>
          <Badge bg="primary" className="mt-2">Last updated: March 2026</Badge>
        </div>

        <Row className="g-4">
          <Col md={6}>
            <Card className="legal-card h-100">
              <Card.Header className="bg-primary text-white">
                <FaShieldAlt className="me-2" /> Privacy Policy
              </Card.Header>
              <Card.Body>
                <Accordion flush className="legal-accordion">
                  <Accordion.Item eventKey="0">
                    <Accordion.Header>1. Information We Collect</Accordion.Header>
                    <Accordion.Body>
                      <p>QueueLess collects the following types of information:</p>
                      <ul>
                        <li><strong>Account Information:</strong> Name, email address, phone number, password (hashed).</li>
                        <li><strong>Profile Information:</strong> Profile picture (optional), preferences (notification settings, language, etc.).</li>
                        <li><strong>Queue Data:</strong> Tokens you join, positions, wait times, feedback you provide.</li>
                        <li><strong>Location Data:</strong> When you search for nearby places, we may use your device’s location (with your consent).</li>
                        <li><strong>Payment Information:</strong> When you purchase admin or provider tokens, Razorpay processes the payment; we do not store credit card details.</li>
                        <li><strong>Device Information:</strong> IP address, browser type, device tokens for push notifications.</li>
                        <li><strong>Cookies & Similar Technologies:</strong> We use cookies to remember your login state and preferences.</li>
                      </ul>
                    </Accordion.Body>
                  </Accordion.Item>

                  <Accordion.Item eventKey="1">
                    <Accordion.Header>2. How We Use Your Information</Accordion.Header>
                    <Accordion.Body>
                      <p>We use the information we collect to:</p>
                      <ul>
                        <li>Create and manage your account.</li>
                        <li>Allow you to join queues, track your position, and receive notifications.</li>
                        <li>Process payments via Razorpay.</li>
                        <li>Send important service emails (verification, password reset, queue updates).</li>
                        <li>Analyze usage patterns to improve our services (e.g., wait time predictions, popular places).</li>
                        <li>Prevent fraud, enforce our terms, and comply with legal obligations.</li>
                      </ul>
                    </Accordion.Body>
                  </Accordion.Item>

                  <Accordion.Item eventKey="2">
                    <Accordion.Header>3. Sharing of Information</Accordion.Header>
                    <Accordion.Body>
                      <p>We do not sell your personal information. We may share data with:</p>
                      <ul>
                        <li><strong>Service Providers:</strong> Razorpay (payments), Firebase (push notifications), SendGrid/SMTP (emails), MongoDB Atlas (database hosting).</li>
                        <li><strong>Business Partners:</strong> When you join a queue, the provider and admin of that place will see your token and any details you provide (purpose, condition) according to your privacy settings.</li>
                        <li><strong>Legal Authorities:</strong> If required by law or to protect our rights.</li>
                      </ul>
                    </Accordion.Body>
                  </Accordion.Item>

                  <Accordion.Item eventKey="3">
                    <Accordion.Header>4. Data Security</Accordion.Header>
                    <Accordion.Body>
                      <p>We implement industry‑standard security measures:</p>
                      <ul>
                        <li>Passwords are hashed using BCrypt.</li>
                        <li>All data is transmitted over HTTPS (TLS).</li>
                        <li>Access to production data is restricted to authorized personnel.</li>
                        <li>Regular security audits and vulnerability scanning.</li>
                      </ul>
                      <p>However, no method of transmission over the internet is 100% secure. We encourage you to use strong, unique passwords and to keep your account credentials confidential.</p>
                    </Accordion.Body>
                  </Accordion.Item>

                  <Accordion.Item eventKey="4">
                    <Accordion.Header>5. Your Rights and Choices</Accordion.Header>
                    <Accordion.Body>
                      <p>Depending on your location, you may have the following rights:</p>
                      <ul>
                        <li><strong>Access:</strong> Request a copy of your personal data.</li>
                        <li><strong>Correction:</strong> Update your profile information.</li>
                        <li><strong>Deletion:</strong> Delete your account and associated data (from your profile page).</li>
                        <li><strong>Opt‑out:</strong> Disable email or push notifications in your preferences.</li>
                        <li><strong>Data Portability:</strong> Export your token history.</li>
                      </ul>
                      <p>To exercise these rights, contact us at <a href="mailto:support@queueless.com">support@queueless.com</a>.</p>
                    </Accordion.Body>
                  </Accordion.Item>

                  <Accordion.Item eventKey="5">
                    <Accordion.Header>6. Data Retention</Accordion.Header>
                    <Accordion.Body>
                      <p>We retain your data as long as your account is active. If you delete your account, your personal information is removed from our active databases within 30 days. Anonymized statistical data (e.g., aggregated wait times) may be kept indefinitely. OTPs and temporary tokens are automatically deleted after expiration.</p>
                    </Accordion.Body>
                  </Accordion.Item>

                  <Accordion.Item eventKey="6">
                    <Accordion.Header>7. Children's Privacy</Accordion.Header>
                    <Accordion.Body>
                      <p>QueueLess is not intended for children under the age of 13. We do not knowingly collect personal information from children. If you believe a child has provided us with data, please contact us to have it removed.</p>
                    </Accordion.Body>
                  </Accordion.Item>

                  <Accordion.Item eventKey="7">
                    <Accordion.Header>8. Changes to This Policy</Accordion.Header>
                    <Accordion.Body>
                      <p>We may update this policy from time to time. We will notify you of material changes via email or a prominent notice on the website. Continued use after changes indicates acceptance of the updated policy.</p>
                    </Accordion.Body>
                  </Accordion.Item>

                  <Accordion.Item eventKey="8">
                    <Accordion.Header>9. Contact Us</Accordion.Header>
                    <Accordion.Body>
                      <p>If you have any questions about this Privacy Policy, please contact us:</p>
                      <ul>
                        <li>Email: <a href="mailto:privacy@queueless.com">privacy@queueless.com</a></li>
                        <li>Address: [Your Business Address]</li>
                      </ul>
                    </Accordion.Body>
                  </Accordion.Item>
                </Accordion>
              </Card.Body>
            </Card>
          </Col>

          <Col md={6}>
            <Card className="legal-card h-100">
              <Card.Header className="bg-primary text-white">
                <FaFileContract className="me-2" /> Terms of Service
              </Card.Header>
              <Card.Body>
                <Accordion flush className="legal-accordion">
                  <Accordion.Item eventKey="0">
                    <Accordion.Header>1. Acceptance of Terms</Accordion.Header>
                    <Accordion.Body>
                      <p>By accessing or using QueueLess, you agree to be bound by these Terms of Service. If you do not agree, please do not use the Service.</p>
                    </Accordion.Body>
                  </Accordion.Item>

                  <Accordion.Item eventKey="1">
                    <Accordion.Header>2. Description of Service</Accordion.Header>
                    <Accordion.Body>
                      <p>QueueLess provides a digital queue management system that allows users to join queues, providers to manage service flow, and administrators to oversee multiple locations. The Service includes a website, mobile‑optimized interface, and APIs.</p>
                    </Accordion.Body>
                  </Accordion.Item>

                  <Accordion.Item eventKey="2">
                    <Accordion.Header>3. User Accounts</Accordion.Header>
                    <Accordion.Body>
                      <p>You must be at least 13 years old to create an account. You are responsible for maintaining the confidentiality of your login credentials. You agree to notify us immediately of any unauthorized use of your account.</p>
                      <p>We reserve the right to suspend or terminate accounts that violate these terms or engage in abusive behavior (e.g., spamming, attempting to manipulate queues).</p>
                    </Accordion.Body>
                  </Accordion.Item>

                  <Accordion.Item eventKey="3">
                    <Accordion.Header>4. Purchases and Payments</Accordion.Header>
                    <Accordion.Body>
                      <p>Admins and providers may purchase tokens through Razorpay. All payments are final and non‑refundable. Tokens expire according to the purchased plan (1 month, 1 year, lifetime).</p>
                      <p>By providing a payment method, you authorize us to charge the applicable fees. Razorpay handles all payment processing; we do not store your credit card information.</p>
                    </Accordion.Body>
                  </Accordion.Item>

                  <Accordion.Item eventKey="4">
                    <Accordion.Header>5. User Conduct</Accordion.Header>
                    <Accordion.Body>
                      <p>You agree not to:</p>
                      <ul>
                        <li>Use the Service for any illegal purpose.</li>
                        <li>Interfere with or disrupt the Service or servers.</li>
                        <li>Attempt to gain unauthorized access to other accounts.</li>
                        <li>Submit false or misleading information.</li>
                        <li>Harass other users or providers.</li>
                      </ul>
                      <p>Violations may result in immediate suspension or termination of your account.</p>
                    </Accordion.Body>
                  </Accordion.Item>

                  <Accordion.Item eventKey="5">
                    <Accordion.Header>6. Intellectual Property</Accordion.Header>
                    <Accordion.Body>
                      <p>All content, logos, and code are the property of QueueLess or its licensors. You may not copy, modify, or distribute any part of the Service without written permission.</p>
                      <p>You retain ownership of the content you submit (e.g., feedback, profile information), but grant QueueLess a worldwide, royalty‑free license to use, store, and display that content in connection with the Service.</p>
                    </Accordion.Body>
                  </Accordion.Item>

                  <Accordion.Item eventKey="6">
                    <Accordion.Header>7. Disclaimer of Warranties</Accordion.Header>
                    <Accordion.Body>
                      <p>The Service is provided "as is" without warranties of any kind, either express or implied. We do not guarantee that the Service will be uninterrupted, error‑free, or secure. Your use is at your own risk.</p>
                    </Accordion.Body>
                  </Accordion.Item>

                  <Accordion.Item eventKey="7">
                    <Accordion.Header>8. Limitation of Liability</Accordion.Header>
                    <Accordion.Body>
                      <p>To the fullest extent permitted by law, QueueLess and its affiliates shall not be liable for any indirect, incidental, or consequential damages arising out of your use of the Service. Our total liability shall not exceed the amount you paid us, if any, in the past 12 months.</p>
                    </Accordion.Body>
                  </Accordion.Item>

                  <Accordion.Item eventKey="8">
                    <Accordion.Header>9. Indemnification</Accordion.Header>
                    <Accordion.Body>
                      <p>You agree to indemnify and hold QueueLess harmless from any claims, damages, or expenses arising from your use of the Service or violation of these Terms.</p>
                    </Accordion.Body>
                  </Accordion.Item>

                  <Accordion.Item eventKey="9">
                    <Accordion.Header>10. Termination</Accordion.Header>
                    <Accordion.Body>
                      <p>We may suspend or terminate your access at any time, with or without cause, with or without notice. Upon termination, your right to use the Service will cease immediately. You may delete your account at any time from your profile page.</p>
                    </Accordion.Body>
                  </Accordion.Item>

                  <Accordion.Item eventKey="10">
                    <Accordion.Header>11. Governing Law</Accordion.Header>
                    <Accordion.Body>
                      <p>These Terms shall be governed by the laws of India, without regard to its conflict of law provisions.</p>
                    </Accordion.Body>
                  </Accordion.Item>

                  <Accordion.Item eventKey="11">
                    <Accordion.Header>12. Changes to Terms</Accordion.Header>
                    <Accordion.Body>
                      <p>We may update these Terms from time to time. We will notify you of material changes via email or a notice on the website. Continued use after changes constitutes acceptance.</p>
                    </Accordion.Body>
                  </Accordion.Item>

                  <Accordion.Item eventKey="12">
                    <Accordion.Header>13. Contact</Accordion.Header>
                    <Accordion.Body>
                      <p>If you have any questions about these Terms, please contact us at <a href="mailto:legal@queueless.com">legal@queueless.com</a>.</p>
                    </Accordion.Body>
                  </Accordion.Item>
                </Accordion>
              </Card.Body>
            </Card>
          </Col>
        </Row>

        <div className="text-center mt-5 text-muted small">
          <p>© {new Date().getFullYear()} QueueLess. All rights reserved.</p>
          <p>This document was last updated on March 20, 2026.</p>
        </div>
      </Container>
    </div>
  );
};

export default Legal;