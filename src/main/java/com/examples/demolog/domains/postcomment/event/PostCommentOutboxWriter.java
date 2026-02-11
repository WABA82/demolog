package com.examples.demolog.domains.postcomment.event;

import com.examples.demolog.domains.common.outbox.model.Outbox;
import com.examples.demolog.domains.common.outbox.repository.OutboxRepository;
import com.examples.demolog.domains.post.model.Post;
import com.examples.demolog.global.utils.JsonUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PostCommentOutboxWriter {

    private final OutboxRepository outboxRepository;

    /**
     * 게시물 댓글 생성 이벤트를 Outbox에 저장
     */
    public void savePostCommentCreatedEvent(Post post, UUID actorId) {
        PostCommentEvent event = PostCommentEvent.created(post.getId(), post.getAuthorId(), actorId);
        Outbox outbox = Outbox.create(event, JsonUtil.toJsonStr(event));
        outboxRepository.save(outbox);
    }

}
