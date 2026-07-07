package com.bonusflowpj.web.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ErrorResponse(
    Instant timestamp,
    int status,
    String error,
    String message,
    Map<String, List<String>> fieldErrors
) {

    public ErrorResponse(Instant timestamp, int status, String error, String message) {
        this(timestamp, status, error, message, Map.of());
    }
}
