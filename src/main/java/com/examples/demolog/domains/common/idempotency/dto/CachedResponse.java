package com.examples.demolog.domains.common.idempotency.dto;

import java.io.Serializable;

public record CachedResponse(
        String responseBody,
        Integer httpStatusCode,
        boolean inProgress
) implements Serializable {

    public CachedResponse(String responseBody, Integer httpStatusCode) {
        this(responseBody, httpStatusCode, false);
    }

    public boolean isInProgress() {
        return inProgress;
    }
}
