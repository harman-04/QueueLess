package com.queueless.backend.dto;

import lombok.Data;

@Data
public class QueueResetResponseDTO {
    private Boolean success;
    private String message;
    private String exportFileUrl; // If data was preserved
    private Integer tokensReset;
}