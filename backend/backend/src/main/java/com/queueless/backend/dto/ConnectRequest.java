package com.queueless.backend.dto;

import lombok.Data;

@Data
public  class ConnectRequest {
    private String queueId;
    public String getQueueId() { return queueId; }
    public void setQueueId(String queueId) { this.queueId = queueId; }
}