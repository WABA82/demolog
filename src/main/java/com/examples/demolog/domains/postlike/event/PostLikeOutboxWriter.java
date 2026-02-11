package com.examples.demolog.domains.postlike.event;

import com.examples.demolog.domains.common.outbox.model.EventType;
import com.examples.demolog.domains.common.outbox.model.Outbox;
import com.examples.demolog.domains.common.outbox.repository.OutboxRepository;
import com.examples.demolog.domains.post.model.Post;
import com.examples.demolog.global.utils.JsonUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PostLikeOutboxWriter {

    private final OutboxRepository outboxRepository;

    /**
     * 좋아요 생성 이벤트를 Outbox에 저장
     */
    public void savePostLikedEvent(Post post, UUID actorId) {
        PostLikeEvent event = PostLikeEvent.liked(post.getId(), post.getAuthorId(), actorId);

        Outbox outbox = Outbox.create(
                EventType.POST_LIKED.getTopic(),
                EventType.POST_LIKED.getAggregateType(),
                post.getId(),
                EventType.POST_LIKED.name(),
                JsonUtil.toJsonStr(event)
        );

        outboxRepository.save(outbox);
    }

    /**
     * 좋아요 해제 이벤트를 Outbox에 저장
     */
    public void savePostUnlikedEvent(Post post, UUID actorId) {
        PostLikeEvent event = PostLikeEvent.unliked(post.getId(), post.getAuthorId(), actorId);

        Outbox outbox = Outbox.create(
                EventType.POST_UNLIKED.getTopic(),
                EventType.POST_UNLIKED.getAggregateType(),
                post.getId(),
                EventType.POST_UNLIKED.name(),
                JsonUtil.toJsonStr(event)
        );

        outboxRepository.save(outbox);
    }
}
