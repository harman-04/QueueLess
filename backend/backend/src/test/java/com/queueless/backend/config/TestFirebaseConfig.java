package com.queueless.backend.config;

import com.google.firebase.FirebaseApp;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import static org.mockito.Mockito.mock;

@TestConfiguration
public class TestFirebaseConfig {

    @Bean
    @Primary
    public FirebaseApp firebaseApp() {
        return mock(FirebaseApp.class);
    }
}