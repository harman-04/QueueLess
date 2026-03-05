// src/components/QueuesTableSkeleton.jsx
import React from 'react';
import { Table, Placeholder } from 'react-bootstrap';
import './QueuesTableSkeleton.css';

const QueuesTableSkeleton = () => {
  return (
    <div className="queues-table-skeleton">
    <div className="table-responsive">
      <Table hover className="queues-table mb-0">
        <thead>
          <tr>
            {[...Array(9)].map((_, i) => (
              <th key={i}><Placeholder animation="glow"><Placeholder xs={6} /></Placeholder></th>
            ))}
          </tr>
        </thead>
        <tbody>
          {[...Array(5)].map((_, rowIdx) => (
            <tr key={rowIdx}>
              {[...Array(9)].map((_, colIdx) => (
                <td key={colIdx}><Placeholder animation="glow"><Placeholder xs={10} /></Placeholder></td>
              ))}
            </tr>
          ))}
        </tbody>
      </Table>
    </div>
    </div>
  );
};

export default QueuesTableSkeleton;