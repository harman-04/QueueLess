// Pagination.js
import React from 'react';
import { Pagination } from 'react-bootstrap';

const PaginationComponent = ({ currentPage, totalPages, onPageChange }) => {
    if (totalPages <= 1) {
        return null;
    }

    const pages = [];
    for (let i = 0; i < totalPages; i++) {
        pages.push(
            <Pagination.Item 
                key={i} 
                active={i === currentPage}
                onClick={() => onPageChange(i)}
            >
                {i + 1}
            </Pagination.Item>
        );
    }

    return (
        <div className="d-flex justify-content-center mt-4">
            <Pagination>
                <Pagination.Prev onClick={() => onPageChange(currentPage - 1)} disabled={currentPage === 0} />
                {pages}
                <Pagination.Next onClick={() => onPageChange(currentPage + 1)} disabled={currentPage === totalPages - 1} />
            </Pagination>
        </div>
    );
};

export default PaginationComponent;