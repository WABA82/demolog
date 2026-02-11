package com.examples.demolog.domains.common.outbox.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EventType {

    POST_LIKED("post-like", "PostLike"),
    POST_UNLIKED("post-like", "PostLike"),

    COMMENT_CREATED("post-comment", "PostComment")
    ;

    private final String topic; // 어그리게이트 타입
    private final String aggregateType; // 어그리게이트 타입

}
