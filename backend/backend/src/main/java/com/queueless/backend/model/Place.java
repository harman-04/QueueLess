package com.queueless.backend.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;
import java.util.Map;

@Document(collection = "places")
@CompoundIndexes({
        @CompoundIndex(name = "place_admin_location", def = "{'adminId': 1, 'location': '2dsphere'}")
})
@Data
@NoArgsConstructor
public class Place {
    @Id
    private String id;

    @Field("name")
    private String name;

    @Field("type")
    private String type;

    @Field("address")
    private String address;

    @Field("location")
    @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE)   // <-- added
    private GeoJsonPoint location;

    @Field("adminId")
    @Indexed   // <-- added
    private String adminId;

    @Field("imageUrls")
    private List<String> imageUrls;

    @Field("description")
    private String description;

    @Field("rating")
    private Double rating = 0.0;

    @Field("totalRatings")
    private Integer totalRatings = 0;

    @Field("contactInfo")
    private Map<String, String> contactInfo;

    @Field("businessHours")
    private List<BusinessHours> businessHours;

    @Field("isActive")
    private Boolean isActive = true;

    @Data
    @NoArgsConstructor
    public static class BusinessHours {
        @Field("day")
        private DayOfWeek day;

        @Field("openTime")
        private String openTime;

        @Field("closeTime")
        private String closeTime;

        @Field("isOpen")
        private Boolean isOpen;
    }

    public enum DayOfWeek {
        MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
    }
}