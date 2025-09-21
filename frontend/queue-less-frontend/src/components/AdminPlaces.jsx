import React, { useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { useNavigate } from 'react-router-dom';
import { fetchMyPlaces, deletePlace } from '../redux/placeSlice';
import { Card, Button, Spinner, Alert, Row, Col, Modal, FormControl, InputGroup, Form } from 'react-bootstrap';
import { FaPlus, FaEdit, FaTrash, FaBuilding, FaInfoCircle, FaSearch, FaStar } from 'react-icons/fa';
import { toast } from 'react-toastify';
import './AdminPlaces.css'; // The new CSS file for the enhanced design

const AdminPlaces = () => {
    const dispatch = useDispatch();
    const navigate = useNavigate();
    const { items: places, loading, error } = useSelector((state) => state.places);
    const { token, role } = useSelector((state) => state.auth);

    const [showDeleteModal, setShowDeleteModal] = useState(false);
    const [placeToDelete, setPlaceToDelete] = useState(null);
    const [searchQuery, setSearchQuery] = useState('');

    useEffect(() => {
        if (token && role === 'ADMIN') {
            dispatch(fetchMyPlaces());
        }
    }, [dispatch, token, role]);

    const handleEditPlace = (placeId) => {
        navigate(`/places/edit/${placeId}`);
    };

    const handleDeletePlace = (place) => {
        setPlaceToDelete(place);
        setShowDeleteModal(true);
    };

    const confirmDeletePlace = async () => {
        try {
            await dispatch(deletePlace(placeToDelete.id)).unwrap();
            toast.success('Place deleted successfully!');
            setShowDeleteModal(false);
            setPlaceToDelete(null);
        } catch (error) {
            toast.error(`Failed to delete place: ${error.message}`);
        }
    };

    const handleManageServices = (placeId) => {
        navigate(`/admin/places/${placeId}/services`);
    };

    const filteredPlaces = places.filter(place =>
        place.name.toLowerCase().includes(searchQuery.toLowerCase())
    );

    if (loading) {
        return (
            <div className="d-flex justify-content-center align-items-center vh-100">
                <Spinner animation="border" role="status" className="text-primary">
                    <span className="visually-hidden">Loading places...</span>
                </Spinner>
            </div>
        );
    }

    if (error) {
        return (
            <div className="container mt-5">
                <Alert variant="danger" className="text-center">
                    <FaInfoCircle className="me-2" />
                    Error loading places: {error.message}
                </Alert>
            </div>
        );
    }

    return (
        <div className="container-fluid py-5 ql-ap-container">
            <header className="mb-5 d-flex flex-column flex-md-row justify-content-between align-items-center">
                <h1 className="mb-3 mb-md-0 ql-ap-heading">My Places</h1>
                <div className="d-flex flex-column flex-sm-row gap-3 w-100 w-md-auto">
                    <InputGroup className="flex-grow-1">
                        <InputGroup.Text className="ql-ap-search-icon-bg">
                            <FaSearch className="ql-ap-search-icon" />
                        </InputGroup.Text>
                        <FormControl
                            placeholder="Search places..."
                            value={searchQuery}
                            onChange={(e) => setSearchQuery(e.target.value)}
                            className="ql-ap-search-input"
                        />
                    </InputGroup>
                    <Button onClick={() => navigate('/places/new')} className="ql-ap-btn-add-place">
                        <FaPlus className="me-2" /> Add New Place
                    </Button>
                </div>
            </header>

            {places.length === 0 ? (
                <div className="ql-ap-empty-state-card text-center py-5 shadow-sm">
                    <FaInfoCircle size={40} className="mb-3 ql-ap-empty-state-icon" />
                    <h4 className="fw-bold">No Places Found</h4>
                    <p className="text-muted">You haven't added any places yet. Get started by creating your first one!</p>
                    <Button onClick={() => navigate('/places/new')} variant="primary" className="mt-3">
                        <FaPlus className="me-2" /> Create First Place
                    </Button>
                </div>
            ) : (
                <Row xs={1} md={2} lg={3} className="g-4">
                    {filteredPlaces.map((place) => (
                        <Col key={place.id}>
                            <Card className="ql-ap-place-card h-100 shadow-lg">
                                <div className="ql-ap-card-img-container">
                                    <Card.Img
                                        variant="top"
                                        src={place.imageUrls?.[0] || 'https://via.placeholder.com/400x250?text=No+Image'}
                                        alt={`Image of ${place.name}`}
                                        className="ql-ap-card-img-top"
                                    />
                                    <div className={`ql-ap-place-status-badge ${place.isActive ? 'active' : 'inactive'}`}>
                                        {place.isActive ? 'Active' : 'Inactive'}
                                    </div>
                                </div>
                                <Card.Body className="d-flex flex-column">
                                    <div className="d-flex justify-content-between align-items-start mb-2">
                                        <Card.Title className="mb-0">{place.name}</Card.Title>
                                        {place.rating !== undefined && (
                                            <span className="ql-ap-place-rating d-flex align-items-center">
                                                <FaStar className="me-1 text-warning" /> {place.rating.toFixed(1)}
                                            </span>
                                        )}
                                    </div>
                                    <Card.Text className="text-muted small">
                                        <FaBuilding className="me-1" />
                                        {place.address}
                                    </Card.Text>
                                    <Card.Text className="flex-grow-1 mb-3 ql-ap-card-description">
                                        {place.description}
                                    </Card.Text>
                                    <div className="mt-auto d-grid gap-2">
                                        <Button
                                            variant="dark"
                                            onClick={() => handleManageServices(place.id)}
                                            className="ql-ap-btn-manage-services"
                                        >
                                            Manage Services
                                        </Button>
                                        <div className="d-flex gap-2">
                                            <Button
                                                variant="outline-secondary"
                                                onClick={() => handleEditPlace(place.id)}
                                                className="flex-fill ql-ap-btn-action"
                                            >
                                                <FaEdit className="me-1" /> Edit
                                            </Button>
                                            <Button
                                                variant="outline-danger"
                                                onClick={() => handleDeletePlace(place)}
                                                className="flex-fill ql-ap-btn-action"
                                            >
                                                <FaTrash className="me-1" /> Delete
                                            </Button>
                                        </div>
                                    </div>
                                </Card.Body>
                            </Card>
                        </Col>
                    ))}
                </Row>
            )}

            <Modal show={showDeleteModal} onHide={() => setShowDeleteModal(false)} centered>
                <Modal.Header closeButton>
                    <Modal.Title className="text-danger">Confirm Deletion</Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    <p>Are you sure you want to delete **{placeToDelete?.name}**?</p>
                    <p className="text-danger fw-bold">This action is permanent and cannot be undone.</p>
                </Modal.Body>
                <Modal.Footer>
                    <Button variant="secondary" onClick={() => setShowDeleteModal(false)}>
                        Cancel
                    </Button>
                    <Button variant="danger" onClick={confirmDeletePlace}>
                        Delete Permanently
                    </Button>
                </Modal.Footer>
            </Modal>
        </div>
    );
};

export default AdminPlaces;