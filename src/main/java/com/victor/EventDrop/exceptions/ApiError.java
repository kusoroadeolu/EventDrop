package com.victor.EventDrop.exceptions;

import java.time.LocalDateTime;

public record ApiError(
        int statusCode,
        String message,
        LocalDateTime occurredAt
) {
}
