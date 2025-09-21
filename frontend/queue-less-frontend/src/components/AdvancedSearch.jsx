import React, { useState, useEffect, useRef } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import {
  Container,
  Row,
  Col,
  Card,
  Form,
  Button,
  InputGroup,
  Spinner,
  Alert,
  Collapse
} from 'react-bootstrap';
import {
  FaSearch,
  FaFilter,
  FaStar,
  FaClock,
  FaUsers,
  FaAmbulance,
  FaSort,
  FaAngleDown,
  FaMapMarkerAlt,
  FaTimesCircle,
  FaAngleRight
} from 'react-icons/fa';
import { performSearch, setFilters, setSorting, fetchFilterOptions, setPage } from '../redux/searchSlice';
import SearchResults from './SearchResults';
import './AdvancedSearch.css';

const AdvancedSearch = () => {
  const dispatch = useDispatch();
  const searchState = useSelector((state) => state.search);
  const {
    filters,
    filterOptions,
    loading,
    error,
    currentPage,
    sortBy,
    sortDirection
  } = searchState || {};

  const [localFilters, setLocalFilters] = useState(filters || {
    supportsGroupToken: null,
    emergencySupport: null,
    // ...other default filters from your Redux state
  });
  const [showFilters, setShowFilters] = useState(false);
  const searchTimeoutRef = useRef(null);
  const isInitialMount = useRef(true);

  useEffect(() => {
    dispatch(fetchFilterOptions());
  }, [dispatch]);

  useEffect(() => {
    setLocalFilters(filters);
  }, [filters]);

  useEffect(() => {
    if (isInitialMount.current) {
      isInitialMount.current = false;
      return;
    }

    if (searchTimeoutRef.current) {
      clearTimeout(searchTimeoutRef.current);
    }
    
    if (localFilters.query !== filters.query) {
      searchTimeoutRef.current = setTimeout(() => {
        dispatch(setFilters(localFilters));
        dispatch(performSearch({
          filters: localFilters,
          page: 0,
          sortBy: sortBy,
          sortDirection: sortDirection
        }));
      }, 500);
    }

    return () => {
      if (searchTimeoutRef.current) {
        clearTimeout(searchTimeoutRef.current);
      }
    };
  }, [localFilters.query, dispatch, sortBy, sortDirection]);

  const handleFilterChange = (key, value) => {
    setLocalFilters(prev => ({
      ...prev,
      [key]: value
    }));
  };

  const handleSearch = () => {
    if (searchTimeoutRef.current) {
      clearTimeout(searchTimeoutRef.current);
    }
    dispatch(setFilters(localFilters));
    dispatch(performSearch({
      filters: localFilters,
      page: 0,
      sortBy: sortBy,
      sortDirection: sortDirection
    }));
  };

  const handleSortChange = (newSortBy) => {
    const newSortDirection = sortBy === newSortBy && sortDirection === 'asc' ? 'desc' : 'asc';
    dispatch(setSorting({ sortBy: newSortBy, sortDirection: newSortDirection }));
    dispatch(performSearch({
      filters: localFilters,
      page: currentPage,
      sortBy: newSortBy,
      sortDirection: newSortDirection
    }));
  };

  const clearFilters = () => {
    const defaultFilters = {
      query: '',
      placeType: '',
      minRating: 0,
      maxWaitTime: null,
      supportsGroupToken: null,
      emergencySupport: null,
      isActive: true,
      searchPlaces: true,
      searchServices: true,
      searchQueues: true
    };
    setLocalFilters(defaultFilters);
    dispatch(setFilters(defaultFilters));
    dispatch(performSearch({
      filters: defaultFilters,
      page: 0,
      sortBy: sortBy,
      sortDirection: sortDirection
    }));
  };

  const handlePageChange = (page) => {
    dispatch(setPage(page));
    dispatch(performSearch({
      filters: localFilters,
      page: page,
      sortBy: sortBy || 'name',
      sortDirection: sortDirection || 'asc'
    }));
  };

  return (
    <Container className="advanced-search-container">
      <div className="sticky-search-bar animate__animated animate__fadeInDown">
        <Row className="align-items-center">
          <Col md={8}>
            <InputGroup>
              <Form.Control
                type="text"
                placeholder="Search for places, services, or queues..."
                value={localFilters.query || ''}
                onChange={(e) => handleFilterChange('query', e.target.value)}
                onKeyPress={(e) => e.key === 'Enter' && handleSearch()}
                className="search-input"
              />
              <Button variant="primary" onClick={handleSearch} disabled={loading} className="search-btn">
                {loading ? <Spinner animation="border" size="sm" /> : <FaSearch className="me-2" />}
                Search
              </Button>
            </InputGroup>
          </Col>
          <Col md={4} className="text-end mt-3 mt-md-0 d-flex justify-content-end align-items-center">
            <Button
              variant="outline-primary"
              onClick={() => setShowFilters(!showFilters)}
              aria-expanded={showFilters}
              className="filter-toggle-btn me-2"
            >
              <FaFilter className="me-1" />
              <span className="d-none d-sm-inline">{showFilters ? 'Hide' : 'Show'} Filters</span>
            </Button>
            <Button variant="outline-danger" onClick={clearFilters} className="clear-btn">
              <FaTimesCircle className="me-1" />
              <span className="d-none d-sm-inline">Clear</span>
            </Button>
          </Col>
        </Row>
      </div>

      <Collapse in={showFilters}>
        <div className="filters-panel mt-3 animate__animated animate__fadeInUp">
          <Card.Body>
            <Row>
              <Col md={4}>
                <Form.Group className="mb-4">
                  <Form.Label className="filter-label">
                    <FaStar className="text-warning me-1" />
                    Minimum Rating: **{localFilters.minRating || 0}**
                  </Form.Label>
                  <Form.Range
                    min={0}
                    max={5}
                    step={0.5}
                    value={localFilters.minRating || 0}
                    onChange={(e) => handleFilterChange('minRating', parseFloat(e.target.value))}
                  />
                </Form.Group>
              </Col>
              <Col md={4}>
                <Form.Group className="mb-4">
                  <Form.Label className="filter-label">
                    <FaClock className="text-info me-1" />
                    Max Wait Time
                  </Form.Label>
                  <Form.Select
                    value={localFilters.maxWaitTime || ''}
                    onChange={(e) => handleFilterChange('maxWaitTime', e.target.value ? parseInt(e.target.value) : null)}
                    className="filter-select"
                  >
                    <option value="">Any wait time</option>
                    {filterOptions?.waitTimeRanges?.map((range) => (
                      <option key={range.value} value={range.value}>
                        {range.label}
                      </option>
                    ))}
                  </Form.Select>
                </Form.Group>
              </Col>
              <Col md={4}>
                <Form.Group className="mb-4">
                  <Form.Label className="filter-label">
                    <FaMapMarkerAlt className="text-success me-1" />
                    Place Type
                  </Form.Label>
                  <Form.Select
                    value={localFilters.placeType || ''}
                    onChange={(e) => handleFilterChange('placeType', e.target.value)}
                    className="filter-select"
                  >
                    <option value="">All types</option>
                    {filterOptions?.placeTypes?.map((type) => (
                      <option key={type} value={type}>
                        {type}
                      </option>
                    ))}
                  </Form.Select>
                </Form.Group>
              </Col>
            </Row>

            <div className="filter-group-heading mt-4 mb-3">
                <h6 className="mb-0"><FaFilter className="me-2" />Refine your search</h6>
            </div>
            <Row className="gx-5 gy-3">
              <Col md={4}>
                <Form.Check
                  type="checkbox"
                  label="Supports Group Tokens"
                  checked={localFilters.supportsGroupToken === true}
                  onChange={(e) => handleFilterChange('supportsGroupToken', e.target.checked || null)}
                  className="filter-checkbox"
                />
              </Col>
              <Col md={4}>
                <Form.Check
                  type="checkbox"
                  label="Emergency Support"
                  checked={localFilters.emergencySupport === true}
                  onChange={(e) => handleFilterChange('emergencySupport', e.target.checked || null)}
                  className="filter-checkbox"
                />
              </Col>
              <Col md={4}>
                <Form.Check
                  type="checkbox"
                  label="Active Only"
                  checked={localFilters.isActive !== false}
                  onChange={(e) => handleFilterChange('isActive', e.target.checked)}
                  className="filter-checkbox"
                />
              </Col>
              <Col md={4}>
                <Form.Check
                  type="checkbox"
                  label="Search Places"
                  checked={localFilters.searchPlaces !== false}
                  onChange={(e) => handleFilterChange('searchPlaces', e.target.checked)}
                  className="filter-checkbox"
                />
              </Col>
              <Col md={4}>
                <Form.Check
                  type="checkbox"
                  label="Search Services"
                  checked={localFilters.searchServices !== false}
                  onChange={(e) => handleFilterChange('searchServices', e.target.checked)}
                  className="filter-checkbox"
                />
              </Col>
              <Col md={4}>
                <Form.Check
                  type="checkbox"
                  label="Search Queues"
                  checked={localFilters.searchQueues !== false}
                  onChange={(e) => handleFilterChange('searchQueues', e.target.checked)}
                  className="filter-checkbox"
                />
              </Col>
            </Row>
          </Card.Body>
        </div>
      </Collapse>

      {error && (
        <Alert variant="danger" className="mt-3 animate__animated animate__shakeX">
          {error.message || 'An error occurred during search'}
        </Alert>
      )}

      <div className="sort-controls mt-4">
        <span className="me-3 fw-bold text-muted">Sort by:</span>
        <Button
          variant="outline-primary"
          size="sm"
          className={`sort-btn me-2 ${sortBy === 'name' ? 'active' : ''}`}
          onClick={() => handleSortChange('name')}
        >
          Name {sortBy === 'name' && <FaSort className={`sort-icon ${sortDirection === 'desc' ? 'flipped' : ''}`} />}
        </Button>
        <Button
          variant="outline-primary"
          size="sm"
          className={`sort-btn me-2 ${sortBy === 'rating' ? 'active' : ''}`}
          onClick={() => handleSortChange('rating')}
        >
          Rating {sortBy === 'rating' && <FaSort className={`sort-icon ${sortDirection === 'desc' ? 'flipped' : ''}`} />}
        </Button>
        <Button
          variant="outline-primary"
          size="sm"
          className={`sort-btn ${sortBy === 'waitTime' ? 'active' : ''}`}
          onClick={() => handleSortChange('waitTime')}
        >
          Wait Time {sortBy === 'waitTime' && <FaSort className={`sort-icon ${sortDirection === 'desc' ? 'flipped' : ''}`} />}
        </Button>
      </div>

      <SearchResults onPageChange={handlePageChange} />
    </Container>
  );
};

export default AdvancedSearch;