package com.examples.demolog.domains.common.idempotency.dto.response;

public record InProgressResponse(
        String idempotencyToken,
        String message,
        String retryAfter
) {
}
