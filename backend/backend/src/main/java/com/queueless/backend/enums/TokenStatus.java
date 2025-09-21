package com.queueless.backend.enums;

public enum TokenStatus {
    WAITING,
    IN_SERVICE,
    COMPLETED, // ✅ Add this new status
    CANCELLED,
     PENDING
}