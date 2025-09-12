//// Feedback DTOs
//package com.queueless.backend.dto;
//
//import lombok.Data;
//import jakarta.validation.constraints.Max;
//import jakarta.validation.constraints.Min;
//import jakarta.validation.constraints.NotNull;
//
//import java.time.LocalDateTime;
//import java.util.List;
//
//@Data
//public class FeedbackRequest {
//    @NotNull
//    private String queueId;
//
//    @NotNull
//    private String tokenId;
//
//    @NotNull
//    @Min(1)
//    @Max(5)
//    private Integer rating;
//
//    private String comment;
//
//    @Min(1)
//    @Max(5)
//    private Integer waitTimeRating;
//
//    @Min(1)
//    @Max(5)
//    private Integer serviceQualityRating;
//
//    @Min(1)
//    @Max(5)
//    private Integer staffFriendlinessRating;
//
//    private Boolean isAnonymous = false;
//    private List<String> categories;
//    private Boolean wouldRecommend;
//}
//
//
//
