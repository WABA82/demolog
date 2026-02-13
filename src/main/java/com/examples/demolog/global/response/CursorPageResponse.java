package com.examples.demolog.global.response;

import java.util.List;
import java.util.UUID;

public record CursorPageResponse<T>(
        List<T> content,
        UUID nextCursor,
        boolean hasNext,
        int size
) {
    public static <T> CursorPageResponse<T> of(List<T> content, int size, UUID nextCursor) {
        boolean hasNext = content.size() > size;
        List<T> slicedContent = hasNext ? content.subList(0, size) : content;
        UUID cursor = hasNext ? nextCursor : null;
        return new CursorPageResponse<>(slicedContent, cursor, hasNext, slicedContent.size());
    }
}
