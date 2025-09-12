package com.queueless.backend.repository;

import com.queueless.backend.model.Payment;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends MongoRepository<Payment, String> {
    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);

    // Add this new method
    List<Payment> findByCreatedForEmail(String email);

    // Update PaymentRepository.java
    List<Payment> findByCreatedByAdminId(String adminId);

}
