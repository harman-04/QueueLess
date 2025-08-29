package com.queueless.backend.scheduler;

import com.queueless.backend.repository.OtpRepository;
import com.queueless.backend.model.OtpDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OtpCleanupScheduler {

    private final OtpRepository otpRepository;

    @Scheduled(fixedRate = 300000) // every 5 mins
    public void removeExpiredOtps() {
        List<OtpDocument> allOtps = otpRepository.findAll();
        for (OtpDocument otp : allOtps) {
            if (otp.getExpiryTime().isBefore(LocalDateTime.now())) {
                otpRepository.delete(otp);
            }
        }
    }
}

