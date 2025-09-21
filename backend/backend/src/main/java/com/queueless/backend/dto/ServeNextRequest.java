package com.queueless.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServeNextRequest {
    @NotBlank(message = "Queue ID is required")
    private String queueId;
}