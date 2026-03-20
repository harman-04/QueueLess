import React from 'react';
import { Container, Card } from 'react-bootstrap';
// import './TermsOfService.css';

const TermsOfService = () => {
  return (
    <Container className="py-5">
      <Card className="shadow-lg">
        <Card.Body className="p-5">
          <h1 className="text-center mb-4">Terms of Service</h1>
          <p className="text-muted text-center">Last updated: {new Date().toLocaleDateString()}</p>
          <hr />

          <h4>1. Acceptance of Terms</h4>
          <p>
            By accessing or using QueueLess, you agree to be bound by these Terms of Service. If you do not agree, you may not use the service.
          </p>

          <h4>2. Description of Service</h4>
          <p>
            QueueLess provides a queue management platform for businesses and users. We reserve the right to modify or discontinue the service at any time.
          </p>

          <h4>3. User Accounts</h4>
          <p>
            You are responsible for maintaining the confidentiality of your account credentials. You agree to notify us immediately of any unauthorized use of your account.
          </p>

          <h4>4. Payments and Refunds</h4>
          <p>
            Purchases of admin/provider tokens are final and non‑refundable, except as required by law. All transactions are processed through Razorpay.
          </p>

          <h4>5. Prohibited Conduct</h4>
          <p>
            You agree not to misuse the service, including attempting to interfere with the service, violating any laws, or impersonating another user.
          </p>

          <h4>6. Intellectual Property</h4>
          <p>
            All content and trademarks on the site are the property of QueueLess. You may not copy, modify, or distribute any content without our prior written consent.
          </p>

          <h4>7. Limitation of Liability</h4>
          <p>
            To the fullest extent permitted by law, QueueLess shall not be liable for any indirect, incidental, or consequential damages arising out of your use of the service.
          </p>

          <h4>8. Governing Law</h4>
          <p>
            These Terms shall be governed by the laws of India, without regard to its conflict of law provisions.
          </p>

          <h4>9. Changes to Terms</h4>
          <p>
            We may revise these Terms at any time. By continuing to use the service after changes are posted, you agree to the updated Terms.
          </p>

          <h4>10. Contact</h4>
          <p>For questions about these Terms, please contact us at legal@queueless.com.</p>
        </Card.Body>
      </Card>
    </Container>
  );
};

export default TermsOfService;