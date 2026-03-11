package com.queueless.backend.integration;

import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.mongo.transitions.Mongod;
import de.flapdoodle.embed.mongo.transitions.RunningMongodProcess;
import de.flapdoodle.reverse.TransitionWalker;
import de.flapdoodle.reverse.transitions.Start;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class BaseIntegrationTest {

    private static TransitionWalker.ReachedState<RunningMongodProcess> mongod;

    @BeforeAll
    static void startMongo() {
        mongod = Mongod.builder()
                .net(Start.to(Net.class).initializedWith(Net.defaults().withPort(27017)))
                .build()
                .start(Version.Main.V6_0);
    }

    @AfterAll
    static void stopMongo() {
        if (mongod != null) {
            mongod.close();
        }
    }

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        // Use the embedded MongoDB (no auth, localhost)
        registry.add("spring.data.mongodb.uri", () -> "mongodb://localhost:27017/queueless");

        // Disable SSL for tests – use plain HTTP
        registry.add("server.ssl.enabled", () -> "false");
        registry.add("spring.data.redis.host", () -> "localhost");
        registry.add("spring.data.redis.port", () -> "6379");
    }



}