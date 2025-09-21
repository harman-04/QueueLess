import React, { useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { addFavoritePlace, removeFavoritePlace, fetchFavoritePlaces } from '../redux/userSlice';
import { FaHeart, FaRegHeart } from 'react-icons/fa';
import { Button, Spinner } from 'react-bootstrap';
import { toast } from 'react-toastify'; // Correct import for toast notifications

const FavoriteButton = ({ placeId, size = 'sm' }) => {
    const dispatch = useDispatch();
    const { favoritePlaceIds, loading } = useSelector((state) => state.user);
    const { token } = useSelector((state) => state.auth);
    const [isFavorite, setIsFavorite] = useState(false);
    const [isProcessing, setIsProcessing] = useState(false);

    useEffect(() => {
        if (token) {
            dispatch(fetchFavoritePlaces());
        }
    }, [dispatch, token]);

    useEffect(() => {
        setIsFavorite(favoritePlaceIds.includes(placeId));
    }, [favoritePlaceIds, placeId]);

    const handleToggleFavorite = async () => {
        if (!token) {
            toast.error('Please login to add favorites');
            return;
        }

        setIsProcessing(true);
        try {
            if (isFavorite) {
                await dispatch(removeFavoritePlace(placeId)).unwrap();
            } else {
                await dispatch(addFavoritePlace(placeId)).unwrap();
            }
        } catch (error) {
            console.error('Failed to toggle favorite:', error);
        } finally {
            setIsProcessing(false);
        }
    };

    if (!token) return null;

    return (
        <Button
            variant={isFavorite ? 'danger' : 'outline-danger'}
            size={size}
            onClick={handleToggleFavorite}
            disabled={isProcessing || loading}
            className="d-flex align-items-center"
        >
            {isProcessing ? (
                <Spinner animation="border" size="sm" />
            ) : isFavorite ? (
                <FaHeart className="me-1" />
            ) : (
                <FaRegHeart className="me-1" />
            )}
            {isFavorite ? 'Remove Favorite' : 'Add to Favorites'}
        </Button>
    );
};

export default FavoriteButton;