// src/pages/UserProfile.jsx
import React, { useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { Card, Form, Button, Alert, Container, Row, Col, Modal } from 'react-bootstrap';
import { useNavigate } from 'react-router-dom';
import { logout } from '../redux/authSlice';
import { authService } from '../services/authService';
import { toast } from 'react-toastify';
import './UserProfile.css';

const UserProfile = () => {
    const dispatch = useDispatch();
    const navigate = useNavigate();
    const { name: currentName, phoneNumber: currentPhoneNumber, profileImageUrl: currentProfileImageUrl } = useSelector((state) => state.auth);

    const [name, setName] = useState(currentName || '');
    const [phoneNumber, setPhoneNumber] = useState(currentPhoneNumber || '');
    const [profileImageUrl, setProfileImageUrl] = useState(currentProfileImageUrl || '');
    const [currentPassword, setCurrentPassword] = useState('');
    const [newPassword, setNewPassword] = useState('');
    const [confirmNewPassword, setConfirmNewPassword] = useState('');
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [passwordError, setPasswordError] = useState('');
    const [deleteWarning, setDeleteWarning] = useState(false);
    const [showAvatarModal, setShowAvatarModal] = useState(false);

    // Premium avatar collection
    const premiumAvatars = [
        'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?ixlib=rb-4.0.3&auto=format&fit=crop&w=200&q=80',
        'https://images.unsplash.com/photo-1639149888905-fb39731f2e6c?ixlib=rb-4.0.3&auto=format&fit=crop&w=200&q=80',
        'https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?ixlib=rb-4.0.3&auto=format&fit=crop&w=200&q=80',
        'https://images.unsplash.com/photo-1527980965255-d3b416303d12?ixlib=rb-4.0.3&auto=format&fit=crop&w=200&q=80',
        'https://images.unsplash.com/photo-1640951613773-54706e06851d?ixlib=rb-4.0.3&auto=format&fit=crop&w=200&q=80',
        'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?ixlib=rb-4.0.3&auto=format&fit=crop&w=200&q=80',
        'https://images.unsplash.com/photo-1633332755192-727a05c4013d?ixlib=rb-4.0.3&auto=format&fit=crop&w=200&q=80',
        'https://images.unsplash.com/photo-1580489944761-15a19d654956?ixlib=rb-4.0.3&auto=format&fit=crop&w=200&q=80',
        'https://images.unsplash.com/photo-1544005313-94ddf0286df2?ixlib=rb-4.0.3&auto=format&fit=crop&w=200&q=80',
        'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?ixlib=rb-4.0.3&auto=format&fit=crop&w=200&q=80',
        'https://images.unsplash.com/photo-1552058544-f2b08422138a?ixlib=rb-4.0.3&auto=format&fit=crop&w=200&q=80',
        'https://images.unsplash.com/photo-1599566150163-29194dcaad36?ixlib=rb-4.0.3&auto=format&fit=crop&w=200&q=80'
    ];

    const handleProfileUpdate = async (e) => {
        e.preventDefault();
        setIsSubmitting(true);
        try {
            await authService.updateProfile({ name, phoneNumber, profileImageUrl });
            toast.success('Profile updated successfully!');
            dispatch({ type: 'auth/updateProfile', payload: { name, phoneNumber, profileImageUrl } });
        } catch (error) {
            toast.error('Failed to update profile.');
            console.error('Profile update error:', error);
        } finally {
            setIsSubmitting(false);
        }
    };

    const handleChangePassword = async (e) => {
        e.preventDefault();
        setPasswordError('');

        if (newPassword !== confirmNewPassword) {
            setPasswordError('New passwords do not match.');
            return;
        }

        if (newPassword.length < 8) {
            setPasswordError('New password must be at least 8 characters.');
            return;
        }
        
        setIsSubmitting(true);
        try {
            await authService.changePassword({ currentPassword, newPassword });
            toast.success('Password changed successfully!');
            setCurrentPassword('');
            setNewPassword('');
            setConfirmNewPassword('');
        } catch (error) {
            setPasswordError(error.message || 'Failed to change password.');
            console.error('Password change error:', error);
        } finally {
            setIsSubmitting(false);
        }
    };

    const handleDeleteAccount = async () => {
        if (!deleteWarning) {
            setDeleteWarning(true);
            return;
        }
        setIsSubmitting(true);
        try {
            // New: Call the backend API to delete the account
            await authService.deleteAccount();
            toast.success('Account deleted successfully. You will be logged out.');
            dispatch(logout());
            navigate('/');
        } catch (error) {
            toast.error('Failed to delete account.');
            console.error('Account deletion error:', error);
        } finally {
            setIsSubmitting(false);
        }
    };

    const selectAvatar = (avatarUrl) => {
        setProfileImageUrl(avatarUrl);
        setShowAvatarModal(false);
    };

    return (
        <Container className="my-5 user-profile-container">
            <h1 className="text-center mb-4 profile-title">My Profile</h1>
            <Row className="justify-content-center">
                <Col md={8} lg={6}>
                    {/* Profile Image Section */}
                    <Card className="shadow-lg mb-4 profile-card">
                        <Card.Body className="p-4">
                            <h4 className="card-title text-center mb-4 section-title">Profile Picture</h4>
                            <div className="text-center mb-4">
                                <div className="avatar-preview-wrapper">
                                    <img
                                        src={profileImageUrl || 'https://via.placeholder.com/150'}
                                        alt="Profile"
                                        className="profile-img-preview"
                                    />
                                    <div className="avatar-overlay" onClick={() => setShowAvatarModal(true)}>
                                        <i className="fas fa-camera"></i>
                                    </div>
                                </div>
                            </div>
                            
                            <div className="d-grid gap-2">
                                <Button 
                                    variant="outline-primary" 
                                    className="avatar-select-btn"
                                    onClick={() => setShowAvatarModal(true)}
                                >
                                    <i className="fas fa-user-circle me-2"></i>Choose from Premium Avatars
                                </Button>
                            </div>
                            
                            <div className="divider">
                                <span>OR</span>
                            </div>
                            
                            <Form.Group controlId="formProfileImageUrl" className="mb-3">
                                <Form.Label>Enter Custom Image URL</Form.Label>
                                <div className="input-with-icon">
                                    <i className="fas fa-link"></i>
                                    <Form.Control 
                                        type="url" 
                                        placeholder="https://example.com/your-image.jpg"
                                        value={profileImageUrl} 
                                        onChange={(e) => setProfileImageUrl(e.target.value)} 
                                    />
                                </div>
                            </Form.Group>
                        </Card.Body>
                    </Card>

                    {/* Basic Info Section */}
                    <Card className="shadow-lg mb-4 profile-card">
                        <Card.Body className="p-4">
                            <h4 className="card-title section-title">Personal Information</h4>
                            <Form onSubmit={handleProfileUpdate}>
                                <Form.Group className="mb-3" controlId="formName">
                                    <Form.Label>Full Name</Form.Label>
                                    <div className="input-with-icon">
                                        <i className="fas fa-user"></i>
                                        <Form.Control 
                                            type="text" 
                                            value={name} 
                                            onChange={(e) => setName(e.target.value)} 
                                        />
                                    </div>
                                </Form.Group>
                                <Form.Group className="mb-3" controlId="formPhoneNumber">
                                    <Form.Label>Phone Number</Form.Label>
                                    <div className="input-with-icon">
                                        <i className="fas fa-phone"></i>
                                        <Form.Control 
                                            type="text" 
                                            value={phoneNumber} 
                                            onChange={(e) => setPhoneNumber(e.target.value)} 
                                        />
                                    </div>
                                </Form.Group>
                                <div className="d-grid">
                                    <Button 
                                        variant="primary" 
                                        type="submit" 
                                        disabled={isSubmitting}
                                        className="update-btn"
                                    >
                                        {isSubmitting ? 'Saving...' : 'Save Changes'}
                                    </Button>
                                </div>
                            </Form>
                        </Card.Body>
                    </Card>

                    {/* Password Change Section */}
                    <Card className="shadow-lg mb-4 profile-card">
                        <Card.Body className="p-4">
                            <h4 className="card-title section-title">Change Password</h4>
                            <Form onSubmit={handleChangePassword}>
                                <Form.Group className="mb-3" controlId="formCurrentPassword">
                                    <Form.Label>Current Password</Form.Label>
                                    <div className="input-with-icon">
                                        <i className="fas fa-lock"></i>
                                        <Form.Control 
                                            type="password" 
                                            value={currentPassword} 
                                            onChange={(e) => setCurrentPassword(e.target.value)} 
                                            required 
                                        />
                                    </div>
                                </Form.Group>
                                <Form.Group className="mb-3" controlId="formNewPassword">
                                    <Form.Label>New Password</Form.Label>
                                    <div className="input-with-icon">
                                        <i className="fas fa-key"></i>
                                        <Form.Control 
                                            type="password" 
                                            value={newPassword} 
                                            onChange={(e) => setNewPassword(e.target.value)} 
                                            required 
                                        />
                                    </div>
                                </Form.Group>
                                <Form.Group className="mb-3" controlId="formConfirmPassword">
                                    <Form.Label>Confirm New Password</Form.Label>
                                    <div className="input-with-icon">
                                        <i className="fas fa-check-circle"></i>
                                        <Form.Control 
                                            type="password" 
                                            value={confirmNewPassword} 
                                            onChange={(e) => setConfirmNewPassword(e.target.value)} 
                                            required 
                                        />
                                    </div>
                                </Form.Group>
                                {passwordError && <Alert variant="danger" className="mt-3">{passwordError}</Alert>}
                                <div className="d-grid">
                                    <Button 
                                        variant="warning" 
                                        type="submit" 
                                        disabled={isSubmitting}
                                        className="password-btn"
                                    >
                                        {isSubmitting ? 'Changing...' : 'Change Password'}
                                    </Button>
                                </div>
                            </Form>
                        </Card.Body>
                    </Card>

                    {/* Account Management Section */}
                    <Card className="shadow-lg mb-4 profile-card account-management">
                        <Card.Body className="p-4">
                            <h4 className="card-title section-title text-danger">Account Management</h4>
                            <p className="text-muted">
                                Deleting your account is permanent and cannot be undone. All your data will be erased.
                            </p>
                            <div className="d-grid">
                                <Button
                                    variant={deleteWarning ? 'danger' : 'outline-danger'}
                                    onClick={handleDeleteAccount}
                                    disabled={isSubmitting}
                                    className="delete-btn"
                                >
                                    <i className="fas fa-trash-alt me-2"></i>
                                    {deleteWarning ? 'Click again to confirm deletion' : 'Delete Account'}
                                </Button>
                            </div>
                        </Card.Body>
                    </Card>
                </Col>
            </Row>

            {/* Avatar Selection Modal */}
            <Modal show={showAvatarModal} onHide={() => setShowAvatarModal(false)} size="lg" centered>
                <Modal.Header closeButton>
                    <Modal.Title>Choose Your Avatar</Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    <Row>
                        {premiumAvatars.map((avatar, index) => (
                            <Col xs={4} md={3} key={index} className="mb-3 text-center">
                                <div 
                                    className={`avatar-option ${profileImageUrl === avatar ? 'selected' : ''}`}
                                    onClick={() => selectAvatar(avatar)}
                                >
                                    <img src={avatar} alt={`Avatar ${index + 1}`} />
                                </div>
                            </Col>
                        ))}
                    </Row>
                </Modal.Body>
                <Modal.Footer>
                    <Button variant="secondary" onClick={() => setShowAvatarModal(false)}>
                        Cancel
                    </Button>
                </Modal.Footer>
            </Modal>
        </Container>
    );
};

export default UserProfile;