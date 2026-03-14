package com.queueless.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;

@Data
public class ProviderUpdateRequest {
    @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
    private String name;

    @Email(message = "Email should be valid")
    private String email;

    @Size(min = 10, max = 15, message = "Phone number must be 10-15 characters")
    private String phoneNumber;

    private List<String> managedPlaceIds; // list of place IDs this provider should manage

    private Boolean isActive; // optional, for toggling status separately
}