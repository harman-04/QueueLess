package com.queueless.backend.config;

import jakarta.annotation.PostConstruct;
import org.bson.Document;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.index.GeospatialIndex;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.CompoundIndexDefinition;

@Configuration
public class MongoIndexConfig {

    private final MongoTemplate mongoTemplate;

    public MongoIndexConfig(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @PostConstruct
    public void initIndices() {
        // === User Collection ===
        IndexOperations userIndexOps = mongoTemplate.indexOps("users"); // Corrected to "users"
        userIndexOps.createIndex(
                new Index().on("email", Sort.Direction.ASC).unique().named("email_unique_index")
        );

        // === Place Collection ===
        IndexOperations placeIndexOps = mongoTemplate.indexOps("places");
        placeIndexOps.createIndex(
                new GeospatialIndex("location").typed(GeoSpatialIndexType.GEO_2DSPHERE).named("location_2dsphere")
        );
        placeIndexOps.createIndex(
                new Index().on("adminId", Sort.Direction.ASC).named("adminId_index")
        );
        Document compoundPlaceIndex = new Document("adminId", 1).append("location", "2dsphere");
        placeIndexOps.createIndex(
                new CompoundIndexDefinition(compoundPlaceIndex).named("place_admin_location_index")
        );

        // === Queue Collection ===
        IndexOperations queueIndexOps = mongoTemplate.indexOps("queues"); // Corrected to "queues"
        queueIndexOps.createIndex(new Index().on("providerId", Sort.Direction.ASC).named("provider_id_index"));
        queueIndexOps.createIndex(new Index().on("placeId", Sort.Direction.ASC).named("place_id_index"));
        queueIndexOps.createIndex(new Index().on("serviceId", Sort.Direction.ASC).named("service_id_index"));
        queueIndexOps.createIndex(new Index().on("isActive", Sort.Direction.ASC).named("active_status_index"));
        Index compoundQueueIndex = new Index()
                .on("placeId", Sort.Direction.ASC)
                .on("serviceId", Sort.Direction.ASC)
                .on("isActive", Sort.Direction.ASC)
                .named("queue_compound_index");
        queueIndexOps.createIndex(compoundQueueIndex);

        // === Feedback Collection ===
        IndexOperations feedbackIndexOps = mongoTemplate.indexOps("feedbacks"); // Corrected to "feedbacks"
        feedbackIndexOps.createIndex(new Index().on("tokenId", Sort.Direction.ASC).unique().named("token_id_index"));
        feedbackIndexOps.createIndex(new Index().on("placeId", Sort.Direction.ASC).named("feedback_place_id_index"));
        feedbackIndexOps.createIndex(new Index().on("providerId", Sort.Direction.ASC).named("feedback_provider_id_index"));
        feedbackIndexOps.createIndex(
                new Index()
                        .on("providerId", Sort.Direction.ASC)
                        .on("placeId", Sort.Direction.ASC)
                        .named("feedback_provider_place_index")
        );

        // === Service Collection ===
        IndexOperations serviceIndexOps = mongoTemplate.indexOps("services");
        serviceIndexOps.createIndex(new Index().on("placeId", Sort.Direction.ASC).named("service_place_id_index"));

        // === OTP Collection ===
        IndexOperations otpIndexOps = mongoTemplate.indexOps("otp");
        otpIndexOps.createIndex(new Index().on("email", Sort.Direction.ASC).named("otp_email_index"));
    }
}