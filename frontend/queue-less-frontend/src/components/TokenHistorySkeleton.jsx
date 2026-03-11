import React from 'react';
import { Placeholder, Card } from 'react-bootstrap';
import { FaHistory } from 'react-icons/fa';
import './UserTokenHistoryList.css'; // reuse existing styles for consistency

const TokenHistorySkeleton = () => {
  return (
    <Card className="user-token-history-card skeleton">
      <Card.Header>
        <h5 className="mb-0">
          <FaHistory className="me-2" />
          <Placeholder animation="glow" as="span">
            <Placeholder xs={4} />
          </Placeholder>
        </h5>
      </Card.Header>
      <Card.Body className="p-0">
        <div className="table-responsive">
          <table className="table token-history-table mb-0">
            <thead>
              <tr>
                {[...Array(8)].map((_, i) => (
                  <th key={i}>
                    <Placeholder animation="glow">
                      <Placeholder xs={8} />
                    </Placeholder>
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {[...Array(5)].map((_, row) => (
                <tr key={row}>
                  {[...Array(8)].map((_, col) => (
                    <td key={col}>
                      <Placeholder animation="glow">
                        <Placeholder xs={10} />
                      </Placeholder>
                    </td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Card.Body>
    </Card>
  );
};

export default TokenHistorySkeleton;