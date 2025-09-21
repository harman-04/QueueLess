import React, { useState, useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { useNavigate, useParams } from 'react-router-dom';
import { createPlace, updatePlace, fetchPlaceById } from '../redux/placeSlice';
import { Form, Button, Card, Spinner, Alert, Row, Col, InputGroup } from 'react-bootstrap';
import { toast } from 'react-toastify';
import { FaMapMarkerAlt, FaGlobe, FaPhoneAlt, FaEnvelope, FaImage, FaPlus, FaTrash, FaCheckCircle, FaTimesCircle, FaClock, FaCalendarAlt,FaBuilding } from 'react-icons/fa';
import './PlaceForm.css'; // The new, conflict-free CSS file

const DAYS_OF_WEEK = [
    'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY',
    'FRIDAY', 'SATURDAY', 'SUNDAY'
];

const PlaceForm = () => {
    const dispatch = useDispatch();
    const navigate = useNavigate();
    const { id } = useParams();
    const { currentPlace, loading, error } = useSelector((state) => state.places);
    const { token, role, id: userId } = useSelector((state) => state.auth);

    const isEdit = Boolean(id);
    const [formData, setFormData] = useState({
        name: '',
        type: 'SHOP',
        address: '',
        location: [null, null], // Use null for empty state
        description: '',
        contactInfo: { phone: '', email: '', website: '' },
        businessHours: DAYS_OF_WEEK.map(day => ({
            day,
            openTime: '09:00',
            closeTime: '17:00',
            isOpen: true
        })),
        imageUrls: [],
        isActive: true,
        adminId: userId
    });

    const [newImageUrl, setNewImageUrl] = useState('');

    useEffect(() => {
        if (isEdit && token) {
            dispatch(fetchPlaceById(id));
        }
    }, [dispatch, id, isEdit, token]);

    useEffect(() => {
        if (isEdit && currentPlace) {
            if (currentPlace.adminId !== userId) {
                toast.error("You don't have permission to edit this place");
                navigate('/admin/places');
                return;
            }

            setFormData({
                name: currentPlace.name || '',
                type: currentPlace.type || 'SHOP',
                address: currentPlace.address || '',
                location: currentPlace.location || [null, null], // Use null
                description: currentPlace.description || '',
                contactInfo: currentPlace.contactInfo || { phone: '', email: '', website: '' },
                businessHours: currentPlace.businessHours || DAYS_OF_WEEK.map(day => ({
                    day,
                    openTime: '09:00',
                    closeTime: '17:00',
                    isOpen: true
                })),
                imageUrls: currentPlace.imageUrls || [],
                isActive: currentPlace.isActive !== undefined ? currentPlace.isActive : true,
                adminId: userId
            });
        }
    }, [currentPlace, isEdit, userId, navigate]);

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: value }));
    };

    const handleContactChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({
            ...prev,
            contactInfo: { ...prev.contactInfo, [name]: value }
        }));
    };

    const handleBusinessHoursChange = (index, field, value) => {
        const updatedBusinessHours = formData.businessHours.map((hours, i) => {
            if (i === index) {
                return { ...hours, [field]: value };
            }
            return hours;
        });
        setFormData(prev => ({ ...prev, businessHours: updatedBusinessHours }));
    };

    const handleLocationChange = (index, value) => {
        const updatedLocation = [...formData.location];
        updatedLocation[index] = value === '' ? null : parseFloat(value) || 0;
        setFormData(prev => ({ ...prev, location: updatedLocation }));
    };

    const addImageUrl = () => {
        if (newImageUrl.trim()) {
            setFormData(prev => ({
                ...prev,
                imageUrls: [...prev.imageUrls, newImageUrl.trim()]
            }));
            setNewImageUrl('');
        }
    };

    const removeImageUrl = (index) => {
        setFormData(prev => ({
            ...prev,
            imageUrls: prev.imageUrls.filter((_, i) => i !== index)
        }));
    };

    const handleSubmit = async (e) => {
        e.preventDefault();

        // Ensure location is formatted correctly for API call
        const finalFormData = {
            ...formData,
            location: formData.location.map(coord => coord === null ? 0 : coord)
        };

        try {
            if (isEdit) {
                await dispatch(updatePlace({ id, placeData: finalFormData })).unwrap();
                toast.success('Place updated successfully!');
            } else {
                await dispatch(createPlace(finalFormData)).unwrap();
                toast.success('Place created successfully!');
            }
            navigate('/admin/places');
        } catch (error) {
            toast.error(`Failed to ${isEdit ? 'update' : 'create'} place: ${error.message}`);
        }
    };

    if (loading) {
        return (
            <div className="ql-pf-loader-container">
                <Spinner animation="border" variant="primary" />
                <p>Loading place data...</p>
            </div>
        );
    }

    if (role !== 'ADMIN') {
        return (
            <Alert variant="danger" className="mt-4 text-center">
                <FaTimesCircle className="me-2" /> You don't have permission to access this page.
            </Alert>
        );
    }

    return (
        <div className="container py-4 ql-pf-container">
            <Card className="ql-pf-card">
                <Card.Header className="ql-pf-card-header">
                    <h3 className="ql-pf-heading">{isEdit ? 'Edit Place' : 'Create New Place'}</h3>
                    <p className="text-muted">{isEdit ? 'Update details for your existing business.' : 'Enter details for your new business or shop.'}</p>
                </Card.Header>
                <Card.Body className="ql-pf-card-body">
                    {error && <Alert variant="danger" className="ql-pf-alert">{error.message}</Alert>}

                    <Form onSubmit={handleSubmit}>
                        {/* Basic Information Section */}
                        <div className="ql-pf-section-heading">
                            <h5><FaBuilding className="me-2 text-primary" /> Basic Information</h5>
                        </div>
                        <Row>
                            <Col md={8}>
                                <Form.Group className="mb-3">
                                    <Form.Label>Name <span className="text-danger">*</span></Form.Label>
                                    <Form.Control
                                        type="text"
                                        name="name"
                                        value={formData.name}
                                        onChange={handleChange}
                                        required
                                        className="ql-pf-form-control"
                                    />
                                </Form.Group>
                            </Col>
                            <Col md={4}>
                                <Form.Group className="mb-3">
                                    <Form.Label>Type <span className="text-danger">*</span></Form.Label>
                                    <Form.Select
                                        name="type"
                                        value={formData.type}
                                        onChange={handleChange}
                                        required
                                        className="ql-pf-form-control"
                                    >
                                        <option value="SHOP">Shop</option>
                                        <option value="HOSPITAL">Hospital</option>
                                        <option value="BANK">Bank</option>
                                        <option value="RESTAURANT">Restaurant</option>
                                        <option value="SALON">Salon</option>
                                        <option value="OTHER">Other</option>
                                    </Form.Select>
                                </Form.Group>
                            </Col>
                        </Row>
                        <Form.Group className="mb-3">
                            <Form.Label>Address <span className="text-danger">*</span></Form.Label>
                            <Form.Control
                                type="text"
                                name="address"
                                value={formData.address}
                                onChange={handleChange}
                                required
                                className="ql-pf-form-control"
                            />
                        </Form.Group>
                        <Form.Group className="mb-4">
                            <Form.Label>Description</Form.Label>
                            <Form.Control
                                as="textarea"
                                rows={3}
                                name="description"
                                value={formData.description}
                                onChange={handleChange}
                                className="ql-pf-form-control"
                            />
                        </Form.Group>

                        {/* Location Section */}
                        <div className="ql-pf-section-heading">
                            <h5><FaMapMarkerAlt className="me-2 text-primary" /> Location</h5>
                        </div>
                        <p className="text-muted small">Enter the exact coordinates for accurate location services. You can find these on Google Maps.</p>
                        <Row className="mb-4">
                            <Col md={6}>
                                <Form.Label>Longitude</Form.Label>
                                <InputGroup>
                                    <InputGroup.Text><FaGlobe /></InputGroup.Text>
                                    <Form.Control
                                        type="number"
                                        step="any"
                                        placeholder="e.g., -74.0060"
                                        value={formData.location[0] === null ? '' : formData.location[0]}
                                        onChange={(e) => handleLocationChange(0, e.target.value)}
                                        className="ql-pf-form-control"
                                    />
                                </InputGroup>
                            </Col>
                            <Col md={6}>
                                <Form.Label>Latitude</Form.Label>
                                <InputGroup>
                                    <InputGroup.Text><FaGlobe /></InputGroup.Text>
                                    <Form.Control
                                        type="number"
                                        step="any"
                                        placeholder="e.g., 40.7128"
                                        value={formData.location[1] === null ? '' : formData.location[1]}
                                        onChange={(e) => handleLocationChange(1, e.target.value)}
                                        className="ql-pf-form-control"
                                    />
                                </InputGroup>
                            </Col>
                        </Row>

                        {/* Contact Information Section */}
                        <div className="ql-pf-section-heading">
                            <h5><FaPhoneAlt className="me-2 text-primary" /> Contact Information</h5>
                        </div>
                        <Row className="mb-4">
                            <Col md={4}>
                                <Form.Group className="mb-3">
                                    <Form.Label>Phone</Form.Label>
                                    <InputGroup>
                                        <InputGroup.Text><FaPhoneAlt /></InputGroup.Text>
                                        <Form.Control
                                            type="tel"
                                            name="phone"
                                            value={formData.contactInfo.phone}
                                            onChange={handleContactChange}
                                            className="ql-pf-form-control"
                                        />
                                    </InputGroup>
                                </Form.Group>
                            </Col>
                            <Col md={4}>
                                <Form.Group className="mb-3">
                                    <Form.Label>Email</Form.Label>
                                    <InputGroup>
                                        <InputGroup.Text><FaEnvelope /></InputGroup.Text>
                                        <Form.Control
                                            type="email"
                                            name="email"
                                            value={formData.contactInfo.email}
                                            onChange={handleContactChange}
                                            className="ql-pf-form-control"
                                        />
                                    </InputGroup>
                                </Form.Group>
                            </Col>
                            <Col md={4}>
                                <Form.Group className="mb-3">
                                    <Form.Label>Website</Form.Label>
                                    <InputGroup>
                                        <InputGroup.Text><FaGlobe /></InputGroup.Text>
                                        <Form.Control
                                            type="url"
                                            name="website"
                                            value={formData.contactInfo.website}
                                            onChange={handleContactChange}
                                            className="ql-pf-form-control"
                                        />
                                    </InputGroup>
                                </Form.Group>
                            </Col>
                        </Row>

                        {/* Business Hours Section */}
                        <div className="ql-pf-section-heading">
                            <h5><FaClock className="me-2 text-primary" /> Business Hours</h5>
                        </div>
                        <p className="text-muted small">Set your business hours for each day of the week.</p>
                        {formData.businessHours.map((hours, index) => (
                            <Row key={hours.day} className="ql-pf-hours-row mb-2">
                                <Col xs={12} sm={3} className="d-flex align-items-center mb-2 mb-sm-0">
                                    <FaCalendarAlt className="me-2 text-muted" />
                                    <Form.Label className="mb-0 fw-bold">{hours.day}</Form.Label>
                                </Col>
                                <Col xs={4} sm={2} className="d-flex align-items-center">
                                    <Form.Check
                                        type="switch"
                                        label="Open"
                                        checked={hours.isOpen}
                                        onChange={(e) => handleBusinessHoursChange(index, 'isOpen', e.target.checked)}
                                        className="ql-pf-switch-control"
                                    />
                                </Col>
                                <Col xs={4} sm={3}>
                                    <Form.Control
                                        type="time"
                                        value={hours.openTime}
                                        onChange={(e) => handleBusinessHoursChange(index, 'openTime', e.target.value)}
                                        disabled={!hours.isOpen}
                                        className="ql-pf-time-control"
                                    />
                                </Col>
                                <Col xs={4} sm={3}>
                                    <Form.Control
                                        type="time"
                                        value={hours.closeTime}
                                        onChange={(e) => handleBusinessHoursChange(index, 'closeTime', e.target.value)}
                                        disabled={!hours.isOpen}
                                        className="ql-pf-time-control"
                                    />
                                </Col>
                            </Row>
                        ))}

                        {/* Images Section */}
                        <div className="ql-pf-section-heading mt-4">
                            <h5><FaImage className="me-2 text-primary" /> Images</h5>
                        </div>
                        <p className="text-muted small">Add image URLs to showcase your place.</p>
                        <InputGroup className="mb-3">
                            <Form.Control
                                type="url"
                                placeholder="Enter image URL"
                                value={newImageUrl}
                                onChange={(e) => setNewImageUrl(e.target.value)}
                                className="ql-pf-form-control"
                            />
                            <Button variant="primary" onClick={addImageUrl} className="ql-pf-btn-add-img">
                                <FaPlus className="me-2" /> Add Image
                            </Button>
                        </InputGroup>

                        {formData.imageUrls.length > 0 && (
                            <div className="mb-4 ql-pf-image-preview-container">
                                <Row className="g-3">
                                    {formData.imageUrls.map((url, index) => (
                                        <Col key={index} xs={6} md={4} lg={3}>
                                            <div className="ql-pf-image-card">
                                                <img
                                                    src={url}
                                                    alt={`Place Image ${index + 1}`}
                                                    className="ql-pf-image-preview"
                                                    onError={(e) => { e.target.src = 'https://via.placeholder.com/150?text=Error'; }}
                                                />
                                                <Button
                                                    variant="danger"
                                                    size="sm"
                                                    className="ql-pf-image-remove-btn"
                                                    onClick={() => removeImageUrl(index)}
                                                >
                                                    <FaTrash />
                                                </Button>
                                            </div>
                                        </Col>
                                    ))}
                                </Row>
                            </div>
                        )}

                        {/* Form Submission */}
                        <div className="d-flex gap-3 justify-content-end ql-pf-action-buttons mt-4">
                            <Button type="button" variant="outline-secondary" onClick={() => navigate('/admin/places')} className="ql-pf-btn-cancel">
                                Cancel
                            </Button>
                            <Button type="submit" variant="primary" disabled={loading} className="ql-pf-btn-submit">
                                {loading ? <Spinner animation="border" size="sm" /> : (isEdit ? 'Update Place' : 'Create Place')}
                            </Button>
                        </div>
                    </Form>
                </Card.Body>
            </Card>
        </div>
    );
};

export default PlaceForm;