package com.queueless.backend.dto;

import com.queueless.backend.model.QueueToken;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to add a group token")
public class AddGroupTokenRequest {
    @NotEmpty(message = "Group members list cannot be empty")
    @Schema(description = "List of group members", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<QueueToken.GroupMember> groupMembers;
}