// Documentation.jsx
import React, { useState, useEffect, useRef } from 'react';
import { Container, Row, Col, Card, ListGroup, Badge } from 'react-bootstrap';
import { Link } from 'react-router-dom';
import { FaCopy, FaCheck } from 'react-icons/fa';
import './Documentation.css';

const Documentation = () => {
    const [activeId, setActiveId] = useState('');
    const [copied, setCopied] = useState(null);

    // All section IDs
    const sectionIds = [
        'introduction', 'getting-started', 'authentication', 'api-reference',
        'websocket', 'notification-preferences', 'feedback', 'payments',
        'admin-provider', 'export', 'error-handling', 'rate-limiting',
        'deployment', 'troubleshooting', 'faq'
    ];

    // Scroll spy: highlight active TOC item
    useEffect(() => {
        const observer = new IntersectionObserver(
            (entries) => {
                entries.forEach(entry => {
                    if (entry.isIntersecting) {
                        setActiveId(entry.target.id);
                    }
                });
            },
            { threshold: 0.5, rootMargin: '-80px 0px -80px 0px' }
        );

        sectionIds.forEach(id => {
            const el = document.getElementById(id);
            if (el) observer.observe(el);
        });

        return () => observer.disconnect();
    }, []);

    // Smooth scroll to section
    const scrollToSection = (id) => {
        const element = document.getElementById(id);
        if (element) {
            element.scrollIntoView({ behavior: 'smooth', block: 'start' });
            history.pushState(null, '', `#${id}`);
            setActiveId(id);
        }
    };

    // Copy code to clipboard
    const copyToClipboard = (text, index) => {
        navigator.clipboard.writeText(text);
        setCopied(index);
        setTimeout(() => setCopied(null), 2000);
    };

    // Code block component
    const CodeBlock = ({ code, index }) => (
        <div className="code-block">
            <pre><code>{code}</code></pre>
            <button
                className="copy-btn"
                onClick={() => copyToClipboard(code, index)}
                aria-label="Copy code"
            >
                {copied === index ? <FaCheck /> : <FaCopy />}
            </button>
        </div>
    );

    return (
        <div className="documentation-page">
            <Container className="py-5">
                <div className="doc-grid">
                    <aside className="doc-sidebar">
                        <Card className="toc-card sticky-toc">
                            <Card.Body>
                                <h5 className="toc-title">Table of Contents</h5>
                                <ListGroup variant="flush" className="toc-list">
                                    {sectionIds.map(id => (
                                        <ListGroup.Item
                                            key={id}
                                            action
                                            onClick={() => scrollToSection(id)}
                                            className={activeId === id ? 'active' : ''}
                                        >
                                            {id === 'getting-started' ? '2. Getting Started' :
                                                id === 'authentication' ? '3. Authentication' :
                                                    id === 'api-reference' ? '4. API Reference' :
                                                        id === 'websocket' ? '5. WebSocket Real‑Time Updates' :
                                                            id === 'notification-preferences' ? '6. Notification Preferences' :
                                                                id === 'feedback' ? '7. Feedback System' :
                                                                    id === 'payments' ? '8. Payments & Tokens' :
                                                                        id === 'admin-provider' ? '9. Admin & Provider Management' :
                                                                            id === 'export' ? '10. Export Reports' :
                                                                                id === 'error-handling' ? '11. Error Handling' :
                                                                                    id === 'rate-limiting' ? '12. Rate Limiting' :
                                                                                        id === 'deployment' ? '13. Deployment' :
                                                                                            id === 'troubleshooting' ? '14. Troubleshooting' :
                                                                                                id === 'faq' ? '15. Frequently Asked Questions' :
                                                                                                    '1. Introduction'}
                                        </ListGroup.Item>
                                    ))}
                                </ListGroup>
                            </Card.Body>
                        </Card>
                        {/* </div> */}
                    </aside>


                    {/* Main Content */}
                    <main className="doc-main">
                        {/* Introduction */}
                        <section id="introduction" className="doc-section">
                            <h1>QueueLess - Complete Documentation</h1>
                            <p className="lead">
                                Welcome to the QueueLess documentation. This guide covers everything you need to know to integrate,
                                use, and extend the QueueLess queue management system.
                            </p>
                            <div className="doc-card">
                                <h2>1. Introduction</h2>
                                <p>
                                    QueueLess is a comprehensive queue management platform designed for businesses, hospitals,
                                    banks, and any service that requires managing customer flow. It offers real‑time queue
                                    updates, role‑based access, notifications, analytics, and much more.
                                </p>
                                <p>
                                    The system is built with a Spring Boot backend and a React frontend, communicating via REST
                                    APIs and WebSocket for live updates. It supports three user roles: <strong>USER</strong>,
                                    <strong>PROVIDER</strong>, and <strong>ADMIN</strong>.
                                </p>
                                <ul>
                                    <li><strong>USER</strong> – can search places, join queues, receive notifications, provide feedback.</li>
                                    <li><strong>PROVIDER</strong> – manages queues, serves tokens, handles emergency approvals.</li>
                                    <li><strong>ADMIN</strong> – oversees places, services, providers, and views analytics.</li>
                                </ul>
                            </div>
                        </section>

                        {/* Getting Started */}
                        <section id="getting-started" className="doc-section">
                            <div className="doc-card">
                                <h2>2. Getting Started</h2>
                                <h5>Prerequisites</h5>
                                <ul>
                                    <li>Java 25+</li>
                                    <li>Node.js 20+</li>
                                    <li>MongoDB (local or Atlas)</li>
                                    <li>Redis (for caching)</li>
                                    <li>Razorpay account (for payments)</li>
                                    <li>Firebase project (for push notifications)</li>
                                    <li>SMTP server (e.g., Gmail)</li>
                                </ul>
                                <h5>Installation</h5>
                                <CodeBlock
                                    code={`git clone https://github.com/harman-04/QueueLess.git
cd QueueLess/backend/backend
./mvnw clean install
cd ..
cd ../frontend/queue-less-frontend
npm install`}
                                    index={0}
                                />
                                <p>Configure environment variables as described in the README, then run both services.</p>
                            </div>
                        </section>

                        {/* Authentication */}
                        <section id="authentication" className="doc-section">
                            <div className="doc-card">
                                <h2>3. Authentication</h2>
                                <p>
                                    QueueLess uses JWT (JSON Web Tokens) for authentication. After a successful login, the server
                                    returns a token that must be included in the <code>Authorization</code> header of subsequent requests.
                                </p>
                                <h5>Register</h5>
                                <CodeBlock
                                    code={`POST /api/auth/register
Content-Type: application/json

{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "Password123",
  "phoneNumber": "+1234567890",
  "role": "USER"
}`}
                                    index={1}
                                />
                                <p>For <strong>ADMIN</strong> or <strong>PROVIDER</strong> roles, a valid purchase token is required (see Payments section).</p>
                                <h5>Login</h5>
                                <CodeBlock
                                    code={`POST /api/auth/login
{
  "email": "john@example.com",
  "password": "Password123"
}`}
                                    index={2}
                                />
                                <p>Response includes the JWT token and user details.</p>
                                <h5>Email Verification</h5>
                                <p>After registration, an OTP is sent to the user’s email. Verify with:</p>
                                <CodeBlock
                                    code={`POST /api/auth/verify-email
{
  "email": "john@example.com",
  "otp": "123456"
}`}
                                    index={3}
                                />
                                <p>All endpoints except public ones require the JWT token.</p>
                            </div>
                        </section>

                        {/* API Reference */}
                        <section id="api-reference" className="doc-section">
                            <div className="doc-card">
                                <h2>4. API Reference</h2>
                                <p>Full API documentation is available via Swagger UI at <code>/swagger-ui.html</code> when the backend is running.</p>
                                <p>Below are key endpoints grouped by functionality.</p>

                                <h5>Places</h5>
                                <ul>
                                    <li><code>GET /api/places</code> – get all places (public).</li>
                                    <li><code>POST /api/places</code> – create a place (admin only).</li>
                                    <li><code>GET /api/places/{'{id}'}</code> – get place details.</li>
                                    <li><code>PUT /api/places/{'{id}'}</code> – update place (admin).</li>
                                    <li><code>DELETE /api/places/{'{id}'}</code> – delete place (admin).</li>
                                    <li><code>GET /api/places/nearby?longitude=&latitude=&radius=</code> – find places within radius.</li>
                                </ul>

                                <h5>Queues</h5>
                                <ul>
                                    <li><code>POST /api/queues/create</code> – create a queue (provider).</li>
                                    <li><code>{'/api/queues/{queueId}/add-token'}</code> – join a queue (user).</li>
                                    <li><code>{'/api/queues/{queueId}/add-group-token'}</code> – join with group (user).</li>
                                    <li><code>{'/api/queues/{queueId}/add-emergency-token'}</code> – request emergency token (user).</li>
                                    <li><code>{'/api/queues/{queueId}/serve-next'}</code> – serve next token (provider).</li>
                                    <li><code>{'/api/queues/{queueId}/complete-token'}</code> – mark token as completed (provider).</li>
                                    <li><code>{'/api/queues/{queueId}/cancel-token/{tokenId}'}</code> – cancel token (user/provider).</li>
                                    <li><code>{'/api/queues/{queueId}/activate'}</code> – activate queue (provider).</li>
                                    <li><code>{'/api/queues/{queueId}/deactivate'}</code> – deactivate queue (provider).</li>
                                </ul>

                                <h5>Search</h5>
                                <ul>
                                    <li><code>POST /api/search/comprehensive</code> – advanced search with filters.</li>
                                    <li><code>POST /api/search/nearby</code> – search by location.</li>
                                    <li><code>{'/api/search/quick/{query}'}</code> – quick search with limit.</li>
                                </ul>

                                <h5>Admin</h5>
                                <ul>
                                    <li><code>GET /api/admin/stats</code> – dashboard statistics.</li>
                                    <li><code>GET /api/admin/providers</code> – list of providers with performance data.</li>
                                    <li><code>{'/api/admin/providers/{providerId}'}</code> – provider details.</li>
                                    <li><code>{'/api/admin/providers/{providerId}'}</code> – update provider.</li>
                                    <li><code>{'/api/admin/providers/{providerId}/status?active='}</code> – enable/disable provider.</li>
                                    <li><code>{'/api/admin/providers/{providerId}/reset-password'}</code> – send password reset link.</li>
                                </ul>
                            </div>
                        </section>

                        {/* WebSocket */}
                        <section id="websocket" className="doc-section">
                            <div className="doc-card">
                                <h2>5. WebSocket Real‑Time Updates</h2>
                                <p>
                                    QueueLess uses STOMP over SockJS to push live updates to clients. The WebSocket endpoint is <code>/ws</code>.
                                    The connection requires a valid JWT token passed in the <code>Authorization</code> header during the initial handshake.
                                </p>
                                <h5>Connection Example (JavaScript)</h5>
                                <CodeBlock
                                    code={`import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const client = new Client({
  webSocketFactory: () => new SockJS('https://localhost:8443/ws'),
  connectHeaders: { Authorization: \`Bearer \${token}\` },
  onConnect: () => {
    console.log('Connected');
    // Subscribe to a queue
    client.subscribe('/topic/queues/' + queueId, (message) => {
      const queue = JSON.parse(message.body);
      // Update UI
    });
  }
});
client.activate();`}
                                    index={4}
                                />

                                <h5>Available Destinations</h5>
                                <ul>
                                    <li><code>{'/topic/queues/{queueId}'}</code> – queue updates (token added, served, etc.)</li>
                                    <li><code>/user/queue/emergency-approved</code> – emergency token approval/rejection (private).</li>
                                    <li><code>/user/queue/token-cancelled</code> – token cancellation notification (private).</li>
                                    <li><code>/user/queue/provider-updates</code> – provider‑specific updates.</li>
                                </ul>

                                <h5>Sending Commands</h5>
                                <ul>
                                    <li><code>/app/queue/serve-next</code> – serve next token (payload: <code>{'{"queueId":"..."}'}</code>).</li>
                                    <li><code>/app/queue/add-token</code> – add a token (payload: <code>{'{"queueId":"..."}'}</code>).</li>
                                    <li><code>/app/queue/status</code> – toggle queue active status (payload: queueId string).</li>
                                </ul>
                            </div>
                        </section>

                        {/* Notification Preferences */}
                        <section id="notification-preferences" className="doc-section">
                            <div className="doc-card">
                                <h2>6. Notification Preferences</h2>
                                <p>Users can override global notification settings per queue.</p>
                                <h5>Endpoints</h5>
                                <ul>
                                    <li><code>GET /api/notifications/preferences/my</code> – get all user preferences.</li>
                                    <li><code>{'/api/notifications/preferences/queue/{queueId}'}</code> – get preference for a specific queue.</li>
                                    <li><code>{'/api/notifications/preferences/queue/{queueId}'}</code> – create or update a preference.</li>
                                    <li><code>{'/api/notifications/preferences/queue/{queueId}'}</code> – delete a preference (revert to global).</li>
                                </ul>
                                <h5>Preference Fields</h5>
                                <ul>
                                    <li><code>notifyBeforeMinutes</code> – minutes before turn to be notified (default 5).</li>
                                    <li><code>notifyOnStatusChange</code> – notify when token status changes (e.g., to IN_SERVICE).</li>
                                    <li><code>notifyOnEmergencyApproval</code> – notify when an emergency token is approved/rejected.</li>
                                    <li><code>notifyOnBestTime</code> – receive a daily notification when the queue is short (waiting &lt; 3).</li>
                                    <li><code>enabled</code> – disable all notifications for this queue.</li>
                                </ul>
                            </div>
                        </section>

                        {/* Feedback */}
                        <section id="feedback" className="doc-section">
                            <div className="doc-card">
                                <h2>7. Feedback System</h2>
                                <p>After a token is completed, the user can submit feedback with ratings and a comment.</p>
                                <h5>Submit Feedback</h5>
                                <CodeBlock
                                    code={`POST /api/feedback
{
  "tokenId": "queue123-T-001",
  "queueId": "queue123",
  "rating": 4,
  "comment": "Great service",
  "staffRating": 5,
  "serviceRating": 4,
  "waitTimeRating": 3
}`}
                                    index={5}
                                />
                                <h5>Retrieve Feedback</h5>
                                <ul>
                                    <li><code>{'/api/feedback/place/{placeId}'}</code> – all feedback for a place (public).</li>
                                    <li><code>{'/api/feedback/provider/{providerId}'}</code> – feedback for a provider (provider/admin).</li>
                                    <li><code>{'/api/feedback/place/{placeId}/detailed-ratings'}</code> – average ratings per category.</li>
                                    <li><code>GET /api/feedback/recent?limit=5</code> – recent feedback entries.</li>
                                </ul>
                            </div>
                        </section>

                        {/* Payments */}
                        <section id="payments" className="doc-section">
                            <div className="doc-card">
                                <h2>8. Payments & Tokens</h2>
                                <p>
                                    Admins and providers must purchase tokens via Razorpay. After a successful payment, a unique token
                                    is generated and sent to the user's email. This token is required during registration for the corresponding role.
                                </p>
                                <h5>Create Order</h5>
                                <CodeBlock
                                    code={`POST /api/payment/create-order?email=admin@example.com&role=ADMIN&tokenType=1_MONTH`}
                                    index={6}
                                />
                                <h5>Confirm Payment (Admin)</h5>
                                <CodeBlock
                                    code={`POST /api/payment/confirm?orderId={orderId}&paymentId={paymentId}&email=admin@example.com&tokenType=1_MONTH`}
                                    index={7}
                                />
                                <h5>Confirm Provider Token (Admin)</h5>
                                <CodeBlock
                                    code={`POST /api/payment/confirm-provider?orderId={orderId}&paymentId={paymentId}&providerEmail=provider@example.com&tokenType=1_MONTH`}
                                    index={8}
                                />
                                <p>Tokens are one‑time use and expire after the purchased period.</p>
                            </div>
                        </section>

                        {/* Admin & Provider Management */}
                        <section id="admin-provider" className="doc-section">
                            <div className="doc-card">
                                <h2>9. Admin & Provider Management</h2>
                                <p>Admins have full control over providers and their assigned places.</p>
                                <ul>
                                    <li><strong>List Providers</strong> – <code>GET /api/admin/providers</code> (returns performance data).</li>
                                    <li><strong>View Details</strong> – <code>{'/api/admin/providers/{providerId}'}</code> (includes statistics and assigned places).</li>
                                    <li><strong>Update Provider</strong> – <code>{'/api/admin/providers/{providerId}'}</code> (edit name, email, phone, managed places, status).</li>
                                    <li><strong>Enable/Disable</strong> – <code>{'/api/admin/providers/{providerId}/status?active=false'}</code> (prevents login).</li>
                                    <li><strong>Reset Password</strong> – <code>{'/api/admin/providers/{providerId}/reset-password'}</code> (sends a direct reset link).</li>
                                </ul>
                                <p>Admins can also manage places and services via the respective endpoints.</p>
                            </div>
                        </section>

                        {/* Export Reports */}
                        <section id="export" className="doc-section">
                            <div className="doc-card">
                                <h2>10. Export Reports</h2>
                                <p>Providers and admins can export queue data as PDF or Excel.</p>
                                <ul>
                                    <li><code>{'/api/export/queue/{queueId}/pdf?reportType=tokens&includeUserDetails=false'}</code></li>
                                    <li><code>{'/api/export/queue/{queueId}/excel?reportType=statistics'}</code></li>
                                    <li><code>{'/api/export/exports'}</code> – list previously generated exports (cached).</li>
                                    <li><code>{'/api/export/exports/{exportId}'}</code> – download a cached export.</li>
                                </ul>
                                <p>Report types: <code>tokens</code> (list of tokens), <code>statistics</code> (aggregated metrics), <code>full</code> (both).</p>
                            </div>
                        </section>

                        {/* Error Handling */}
                        <section id="error-handling" className="doc-section">
                            <div className="doc-card">
                                <h2>11. Error Handling</h2>
                                <p>The API returns consistent error responses with the following structure:</p>
                                <CodeBlock
                                    code={`{
  "status": 400,
  "error": "Bad Request",
  "message": "Human‑readable error description",
  "path": "/api/...",
  "timestamp": "2025-01-01T12:00:00"
}`}
                                    index={9}
                                />
                                <p>Common HTTP status codes:</p>
                                <ul>
                                    <li><code>200</code> – OK</li>
                                    <li><code>201</code> – Created</li>
                                    <li><code>400</code> – Bad Request (validation error, business logic)</li>
                                    <li><code>401</code> – Unauthorized (missing or invalid token)</li>
                                    <li><code>403</code> – Forbidden (insufficient role)</li>
                                    <li><code>404</code> – Not Found</li>
                                    <li><code>409</code> – Conflict (e.g., user already in queue)</li>
                                    <li><code>429</code> – Too Many Requests (rate limiting)</li>
                                    <li><code>500</code> – Internal Server Error</li>
                                </ul>
                            </div>
                        </section>

                        {/* Rate Limiting */}
                        <section id="rate-limiting" className="doc-section">
                            <div className="doc-card">
                                <h2>12. Rate Limiting</h2>
                                <p>
                                    To prevent abuse, certain endpoints are rate‑limited per user (or IP if unauthenticated).
                                    Limits are configurable via <code>application.properties</code>.
                                </p>
                                <ul>
                                    <li>Default: 100 requests per minute.</li>
                                    <li>Token creation endpoints: 10 per minute.</li>
                                    <li>Search endpoints: 50 per minute.</li>
                                </ul>
                                <p>When a limit is exceeded, the server responds with <code>429 Too Many Requests</code> and includes a <code>Retry‑After</code> header.</p>
                            </div>
                        </section>

                        {/* Deployment */}
                        <section id="deployment" className="doc-section">
                            <div className="doc-card">
                                <h2>13. Deployment</h2>
                                <p>QueueLess can be deployed using Docker Compose for a full stack or manually.</p>
                                <h5>Docker Compose</h5>
                                <CodeBlock
                                    code={`docker-compose up -d`}
                                    index={10}
                                />
                                <p>This starts the backend, frontend, MongoDB, Redis, Prometheus, Grafana, Loki, and Promtail.</p>
                                <h5>Manual Deployment</h5>
                                <ul>
                                    <li>Build backend JAR: <code>mvn clean package</code></li>
                                    <li>Run with Java: <code>java -jar target/backend-*.jar</code></li>
                                    <li>Build frontend: <code>npm run build</code></li>
                                    <li>Serve static files with Nginx or similar.</li>
                                </ul>
                                <p>Ensure environment variables are set appropriately for production (use secrets, secure keys).</p>
                            </div>
                        </section>

                        {/* Troubleshooting */}
                        <section id="troubleshooting" className="doc-section">
                            <div className="doc-card">
                                <h2>14. Troubleshooting</h2>
                                <h5>Backend won't start – MongoDB connection refused</h5>
                                <p>Check that MongoDB is running and the URI is correct. Use <code>mongod</code> to start locally.</p>
                                <h5>Redis connection errors</h5>
                                <p>Install Redis and start the server. If not needed, disable caching by adding <code>spring.cache.type=none</code> to properties.</p>
                                <h5>Emails not sending</h5>
                                <p>For Gmail, use an App Password. Ensure the SMTP settings are correct in <code>application.properties</code>.</p>
                                <h5>Push notifications not working</h5>
                                <p>Verify Firebase project configuration, VAPID key, and that browser permissions are granted.</p>
                                <h5>File upload fails with 403</h5>
                                <p>Ensure the upload directory exists and is writable. The backend serves static files under <code>/uploads/**</code>.</p>
                                <h5>WebSocket connection fails</h5>
                                <p>Check that the token is valid and passed in the <code>Authorization</code> header. If using self‑signed certificates, browser may block; use a valid certificate or disable SSL in development.</p>
                            </div>
                        </section>

                        {/* FAQ */}
                        <section id="faq" className="doc-section">
                            <div className="doc-card">
                                <h2>15. Frequently Asked Questions</h2>
                                <div className="faq-item">
                                    <h5>How do I become an admin?</h5>
                                    <p>Purchase an admin token from the <Link to="/pricing">Pricing page</Link>, then register using that token.</p>
                                </div>
                                <div className="faq-item">
                                    <h5>Can I join multiple queues at the same time?</h5>
                                    <p>No, users can have only one active token at a time across all queues. This prevents abuse and ensures fair usage.</p>
                                </div>
                                <div className="faq-item">
                                    <h5>What is an emergency token?</h5>
                                    <p>Emergency tokens allow users to request priority service. Providers must approve or reject the request based on the details provided.</p>
                                </div>
                                <div className="faq-item">
                                    <h5>How are wait times calculated?</h5>
                                    <p>The estimated wait time is (number of waiting tokens) × (average service time of the service). Average service time is set by the admin when creating the service.</p>
                                </div>
                                <div className="faq-item">
                                    <h5>Can I reset a queue?</h5>
                                    <p>Yes, providers can reset a queue. They have the option to preserve data by exporting it before resetting.</p>
                                </div>
                                <div className="faq-item">
                                    <h5>Is there an audit trail?</h5>
                                    <p>Yes, important actions (registration, login, queue creation, token operations, payments, etc.) are logged in the <code>audit_logs</code> collection.</p>
                                </div>
                                <div className="faq-item">
                                    <h5>How do I get support?</h5>
                                    <p>For bugs or feature requests, please open an issue on GitHub. For urgent matters, contact support@queueless.com.</p>
                                </div>
                            </div>
                        </section>
                    </main>
                </div>
            </Container>
        </div>
    );
};

export default Documentation;