// Create AdminQueueDTO.java
package com.queueless.backend.dto;

import lombok.Data;
import java.util.List;

@Data
public class AdminQueueDTO {
    private String id;
    private String serviceName;
    private String placeName;
    private String providerName;
    private Boolean isActive;
    private Integer waitingTokens;
    private Integer inServiceTokens;
    private Integer completedTokens;
    private Integer estimatedWaitTime;
}