//// Enhanced DatabaseMigration to handle field standardization
//package com.queueless.backend.config;
//
//import com.queueless.backend.model.Place;
//import com.queueless.backend.model.Queue;
//import com.queueless.backend.model.Service;
//import com.queueless.backend.model.User;
//import com.queueless.backend.repository.PlaceRepository;
//import com.queueless.backend.repository.QueueRepository;
//import com.queueless.backend.repository.ServiceRepository;
//import com.queueless.backend.repository.UserRepository;
//import jakarta.annotation.PostConstruct;
//import lombok.extern.slf4j.Slf4j;
//import org.bson.Document;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.data.mongodb.core.MongoTemplate;
//import org.springframework.data.mongodb.core.index.GeospatialIndex;
//import org.springframework.data.mongodb.core.query.Criteria;
//import org.springframework.data.mongodb.core.query.Query;
//import org.springframework.data.mongodb.core.query.Update;
//import org.springframework.stereotype.Component;
//
//import java.util.List;
//
//@Slf4j
//@Component
//public class DatabaseMigration {
//
//    @Autowired
//    private MongoTemplate mongoTemplate;
//
//    @Autowired
//    private PlaceRepository placeRepository;
//
//    @Autowired
//    private ServiceRepository serviceRepository;
//
//    @Autowired
//    private QueueRepository queueRepository;
//
//    @Autowired
//    private UserRepository userRepository;
//
//    @PostConstruct
//    public void init() {
//        // Create geospatial index for location-based queries
//        mongoTemplate.indexOps(Place.class).createIndex(new GeospatialIndex("location"));
//        // Migrate existing queues to include placeId and serviceId
//        migrateExistingQueues();
//
//        // Migrate existing users to include profile fields
//        migrateExistingUsers();
//
//        // Standardize field names in queues
//        standardizeQueueFields();
//
//        // Standardize field names in services
//        standardizeServiceFields();
//    }
//
//    private void migrateExistingQueues() {
//        queueRepository.findAll().forEach(queue -> {
//            if (queue.getPlaceId() == null) {
//                queue.setPlaceId("default_place_id");
//                queueRepository.save(queue);
//            }
//            if (queue.getServiceId() == null) {
//                queue.setServiceId("default_service_id");
//                queueRepository.save(queue);
//            }
//        });
//    }
//
//    private void migrateExistingUsers() {
//        userRepository.findAll().forEach(user -> {
//            if (user.getProfileImageUrl() == null) {
//                user.setProfileImageUrl("/images/default-profile.png");
//                userRepository.save(user);
//            }
//            if (user.getIsVerified() == null) {
//                user.setIsVerified(false);
//                userRepository.save(user);
//            }
//        });
//    }
//
//    private void standardizeQueueFields() {
//        // Migrate 'active' field to 'isActive'
//        Query query = new Query(Criteria.where("active").exists(true));
//        List<Document> queuesWithActive = mongoTemplate.find(query, Document.class, "queues");
//
//        for (Document doc : queuesWithActive) {
//            Boolean activeValue = doc.getBoolean("active");
//            Update update = new Update();
//            update.set("isActive", activeValue);
//            update.unset("active");
//            mongoTemplate.updateFirst(
//                    new Query(Criteria.where("_id").is(doc.getObjectId("_id"))),
//                    update,
//                    "queues"
//            );
//            log.info("Migrated queue {} from 'active' to 'isActive'", doc.getObjectId("_id"));
//        }
//    }
//
//    private void standardizeServiceFields() {
//        // Migrate service fields if needed
//        Query query = new Query(Criteria.where("supportsGroup").exists(true));
//        List<Document> servicesWithOldFields = mongoTemplate.find(query, Document.class, "services");
//
//        for (Document doc : servicesWithOldFields) {
//            Boolean supportsGroupValue = doc.getBoolean("supportsGroup");
//            Update update = new Update();
//            update.set("supportsGroupToken", supportsGroupValue);
//            update.unset("supportsGroup");
//            mongoTemplate.updateFirst(
//                    new Query(Criteria.where("_id").is(doc.getObjectId("_id"))),
//                    update,
//                    "services"
//            );
//            log.info("Migrated service {} from 'supportsGroup' to 'supportsGroupToken'", doc.getObjectId("_id"));
//        }
//    }
//
//}