package com.examples.demolog.domains.postcomment.repository;

import com.examples.demolog.domains.postcomment.dto.response.PostCommentResponse;
import com.examples.demolog.domains.postcomment.model.QPostComment;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PostCommentRepositoryCustomImpl implements PostCommentRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<PostCommentResponse> findAllByPostIdAndCursor(UUID postId, UUID cursor, int size) {
        QPostComment comment = QPostComment.postComment;

        return queryFactory
                .select(Projections.constructor(PostCommentResponse.class,
                        comment.id,
                        comment.content,
                        comment.postId,
                        comment.authorId,
                        comment.createdAt,
                        comment.updatedAt
                ))
                .from(comment)
                .where(
                        comment.postId.eq(postId),
                        cursorCondition(comment, cursor)
                )
                .orderBy(comment.createdAt.desc(), comment.id.desc())
                .limit(size + 1)
                .fetch();
    }

    private BooleanExpression cursorCondition(QPostComment comment, UUID cursor) {
        if (cursor == null) {
            return null;
        }
        return comment.id.lt(cursor);
    }
}
