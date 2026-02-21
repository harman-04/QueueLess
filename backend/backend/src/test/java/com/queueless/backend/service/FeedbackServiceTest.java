package com.queueless.backend.service;

import com.queueless.backend.model.Feedback;
import com.queueless.backend.model.Place;
import com.queueless.backend.model.Queue;
import com.queueless.backend.model.User;
import com.queueless.backend.repository.FeedbackRepository;
import com.queueless.backend.repository.PlaceRepository;
import com.queueless.backend.repository.QueueRepository;
import com.queueless.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {

    @Mock
    private FeedbackRepository feedbackRepository;

    @Mock
    private PlaceRepository placeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private QueueRepository queueRepository;

    @InjectMocks
    private FeedbackService feedbackService;

    private Feedback testFeedback;
    private final String feedbackId = "fb123";
    private final String tokenId = "token123";
    private final String queueId = "queue123";
    private final String userId = "user123";
    private final String providerId = "provider123";
    private final String placeId = "place123";
    private final String serviceId = "service123";

    @BeforeEach
    void setUp() {
        testFeedback = new Feedback();
        testFeedback.setId(feedbackId);
        testFeedback.setTokenId(tokenId);
        testFeedback.setQueueId(queueId);
        testFeedback.setUserId(userId);
        testFeedback.setProviderId(providerId);
        testFeedback.setPlaceId(placeId);
        testFeedback.setServiceId(serviceId);
        testFeedback.setRating(4);
        testFeedback.setStaffRating(5);
        testFeedback.setServiceRating(4);
        testFeedback.setWaitTimeRating(3);
        testFeedback.setComment("Great service");
        testFeedback.setCreatedAt(LocalDateTime.now());
    }

    // ================= SUBMIT FEEDBACK =================

    @Test
    void submitFeedbackSuccess() {
        Queue queue = new Queue();
        queue.setProviderId(providerId);
        queue.setPlaceId(placeId);
        queue.setServiceId(serviceId);

        // Mock queue retrieval
        when(queueRepository.findById(queueId)).thenReturn(Optional.of(queue));

        // Mock feedback save – return the input
        when(feedbackRepository.save(any(Feedback.class))).thenAnswer(inv -> inv.getArgument(0));

        // Mock place lookup – return a place so updatePlaceRatings proceeds
        Place place = new Place();
        place.setId(placeId);
        when(placeRepository.findById(placeId)).thenReturn(Optional.of(place));

        // Mock feedbacks for this place – return at least one so updatePlaceRatings actually updates
        List<Feedback> feedbacks = List.of(testFeedback);
        when(feedbackRepository.findByPlaceId(placeId)).thenReturn(feedbacks);

        // Mock place save
        when(placeRepository.save(any(Place.class))).thenAnswer(inv -> inv.getArgument(0));

        Feedback saved = feedbackService.submitFeedback(testFeedback);

        assertNotNull(saved);
        assertEquals(providerId, saved.getProviderId());
        assertEquals(placeId, saved.getPlaceId());
        assertEquals(serviceId, saved.getServiceId());

        verify(queueRepository).findById(queueId);
        verify(feedbackRepository).save(testFeedback);
        verify(placeRepository).save(any(Place.class)); // now invoked
    }


    @Test
    void submitFeedbackQueueNotFound() {
        when(queueRepository.findById(queueId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> feedbackService.submitFeedback(testFeedback));

        // The service prepends "Failed to submit feedback: " to the original message
        assertEquals("Failed to submit feedback: Queue not found", exception.getMessage());
    }


    // ================= GET FEEDBACK BY TOKEN ID =================

    @Test
    void getFeedbackByTokenIdFound() {
        when(feedbackRepository.findByTokenId(tokenId)).thenReturn(Optional.of(testFeedback));

        Optional<Feedback> result = feedbackService.getFeedbackByTokenId(tokenId);

        assertTrue(result.isPresent());
        assertEquals(feedbackId, result.get().getId());
    }

    @Test
    void getFeedbackByTokenIdNotFound() {
        when(feedbackRepository.findByTokenId(tokenId)).thenReturn(Optional.empty());

        Optional<Feedback> result = feedbackService.getFeedbackByTokenId(tokenId);

        assertFalse(result.isPresent());
    }

    // ================= GET FEEDBACK BY PLACE ID =================

    @Test
    void getFeedbackByPlaceId() {
        List<Feedback> feedbacks = List.of(testFeedback);
        when(feedbackRepository.findByPlaceId(placeId)).thenReturn(feedbacks);

        List<Feedback> result = feedbackService.getFeedbackByPlaceId(placeId);

        assertEquals(1, result.size());
        assertEquals(feedbackId, result.get(0).getId());
    }

    // ================= GET FEEDBACK BY PROVIDER ID =================

    @Test
    void getFeedbackByProviderId() {
        List<Feedback> feedbacks = List.of(testFeedback);
        when(feedbackRepository.findByProviderId(providerId)).thenReturn(feedbacks);

        List<Feedback> result = feedbackService.getFeedbackByProviderId(providerId);

        assertEquals(1, result.size());
        assertEquals(feedbackId, result.get(0).getId());
    }

    // ================= AVERAGE RATING FOR PLACE =================

    @Test
    void getAverageRatingForPlaceWithFeedbacks() {
        List<Feedback> feedbacks = List.of(
                createFeedbackWithRating(5),
                createFeedbackWithRating(4),
                createFeedbackWithRating(3)
        );
        when(feedbackRepository.findByPlaceId(placeId)).thenReturn(feedbacks);

        Double avg = feedbackService.getAverageRatingForPlace(placeId);

        assertEquals(4.0, avg, 0.01);
    }

    @Test
    void getAverageRatingForPlaceNoFeedbacks() {
        when(feedbackRepository.findByPlaceId(placeId)).thenReturn(List.of());

        Double avg = feedbackService.getAverageRatingForPlace(placeId);

        assertEquals(0.0, avg);
    }

    private Feedback createFeedbackWithRating(int rating) {
        Feedback f = new Feedback();
        f.setRating(rating);
        return f;
    }

    // ================= AVERAGE RATING FOR PROVIDER =================

    @Test
    void getAverageRatingForProviderWithFeedbacks() {
        List<Feedback> feedbacks = List.of(
                createFeedbackWithRating(5),
                createFeedbackWithRating(4)
        );
        when(feedbackRepository.findByProviderId(providerId)).thenReturn(feedbacks);

        Double avg = feedbackService.getAverageRatingForProvider(providerId);

        assertEquals(4.5, avg, 0.01);
    }

    @Test
    void getAverageRatingForProviderNoFeedbacks() {
        when(feedbackRepository.findByProviderId(providerId)).thenReturn(List.of());

        Double avg = feedbackService.getAverageRatingForProvider(providerId);

        assertEquals(0.0, avg);
    }

    // ================= HAS USER PROVIDED FEEDBACK =================

    @Test
    void hasUserProvidedFeedbackForTokenTrue() {
        when(feedbackRepository.findByTokenId(tokenId)).thenReturn(Optional.of(testFeedback));

        boolean result = feedbackService.hasUserProvidedFeedbackForToken(userId, tokenId);

        assertTrue(result);
    }

    @Test
    void hasUserProvidedFeedbackForTokenFalseDifferentUser() {
        when(feedbackRepository.findByTokenId(tokenId)).thenReturn(Optional.of(testFeedback));

        boolean result = feedbackService.hasUserProvidedFeedbackForToken("otherUser", tokenId);

        assertFalse(result);
    }

    @Test
    void hasUserProvidedFeedbackForTokenNotFound() {
        when(feedbackRepository.findByTokenId(tokenId)).thenReturn(Optional.empty());

        boolean result = feedbackService.hasUserProvidedFeedbackForToken(userId, tokenId);

        assertFalse(result);
    }

    // ================= GET ALL AVERAGE RATINGS FOR PLACE =================

    @Test
    void getAllAverageRatingsForPlaceWithData() {
        List<Feedback> feedbacks = List.of(
                createFullFeedback(5, 4, 3, 2),
                createFullFeedback(4, 5, 4, 3),
                createFullFeedback(3, 4, 5, 4)
        );
        when(feedbackRepository.findByPlaceId(placeId)).thenReturn(feedbacks);

        Map<String, Double> ratings = feedbackService.getAllAverageRatingsForPlace(placeId);

        assertEquals(4.0, ratings.get("overall"), 0.01);   // (5+4+3)/3
        assertEquals(4.33, ratings.get("staff"), 0.01);   // (4+5+4)/3
        assertEquals(4.0, ratings.get("service"), 0.01);  // (3+4+5)/3
        assertEquals(3.0, ratings.get("waitTime"), 0.01); // (2+3+4)/3
    }

    @Test
    void getAllAverageRatingsForPlaceNoData() {
        when(feedbackRepository.findByPlaceId(placeId)).thenReturn(List.of());

        Map<String, Double> ratings = feedbackService.getAllAverageRatingsForPlace(placeId);

        assertEquals(0.0, ratings.get("overall"));
        assertEquals(0.0, ratings.get("staff"));
        assertEquals(0.0, ratings.get("service"));
        assertEquals(0.0, ratings.get("waitTime"));
    }

    private Feedback createFullFeedback(int overall, int staff, int service, int waitTime) {
        Feedback f = new Feedback();
        f.setRating(overall);
        f.setStaffRating(staff);
        f.setServiceRating(service);
        f.setWaitTimeRating(waitTime);
        return f;
    }

    // ================= UPDATE PLACE RATINGS (tested via submit) =================

    @Test
    void submitFeedbackTriggersPlaceRatingUpdate() {
        Queue queue = new Queue();
        queue.setProviderId(providerId);
        queue.setPlaceId(placeId);
        queue.setServiceId(serviceId);

        Place place = new Place();
        place.setId(placeId);

        // Existing feedbacks: ratings 5 and 4
        Feedback existing1 = createFullFeedback(5, 5, 5, 5);
        Feedback existing2 = createFullFeedback(4, 4, 4, 4);
        List<Feedback> existingFeedbacks = new ArrayList<>(List.of(existing1, existing2));

        // The new feedback has rating 4 (testFeedback already has rating 4)
        testFeedback.setRating(4);

        when(queueRepository.findById(queueId)).thenReturn(Optional.of(queue));
        when(feedbackRepository.save(any(Feedback.class))).thenAnswer(inv -> {
            Feedback saved = inv.getArgument(0);
            // When save is called, we need to update the list returned by findByPlaceId
            // to include the new feedback. So we modify the existingFeedbacks list.
            existingFeedbacks.add(saved);
            return saved;
        });

        when(placeRepository.findById(placeId)).thenReturn(Optional.of(place));
        // Stub findByPlaceId to return the dynamically updated list
        when(feedbackRepository.findByPlaceId(placeId)).thenAnswer(inv -> existingFeedbacks);
        when(placeRepository.save(any(Place.class))).thenAnswer(inv -> inv.getArgument(0));

        feedbackService.submitFeedback(testFeedback);

        verify(placeRepository).save(argThat(p -> {
            assertEquals(4.33, p.getRating(), 0.01); // (5+4+4)/3 = 4.33
            assertEquals(3, p.getTotalRatings());
            return true;
        }));
    }
}