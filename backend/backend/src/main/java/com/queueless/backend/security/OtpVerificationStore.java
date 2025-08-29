package com.queueless.backend.security;

import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class OtpVerificationStore {
    private final Set<String> verifiedEmails = new HashSet<>();

    public void markVerified(String email) {
        verifiedEmails.add(email);
    }

    public boolean isVerified(String email) {
        return verifiedEmails.contains(email);
    }

    public void remove(String email) {
        verifiedEmails.remove(email);
    }
}
