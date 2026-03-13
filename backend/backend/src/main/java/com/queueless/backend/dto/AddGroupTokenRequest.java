package com.queueless.backend.dto;

import com.queueless.backend.model.QueueToken;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public  class AddGroupTokenRequest {
    @NotEmpty(message = "Group members list cannot be empty")
    private List<QueueToken.GroupMember> groupMembers;

}