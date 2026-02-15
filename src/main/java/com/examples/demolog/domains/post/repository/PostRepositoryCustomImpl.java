package com.examples.demolog.domains.post.repository;

import com.examples.demolog.domains.post.dto.response.PostFeedResponse;
import com.examples.demolog.domains.post.dto.response.PostResponse;
import com.examples.demolog.domains.post.model.QPost;
import com.examples.demolog.domains.postlike.model.QPostLike;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PostRepositoryCustomImpl implements PostRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<PostFeedResponse> findFeedOrderByLikeCount(Pageable pageable) {
        QPost post = QPost.post;
        QPostLike postLike = QPostLike.postLike;

        List<PostFeedResponse> content = queryFactory
                .select(Projections.constructor(PostFeedResponse.class,
                        post.id,
                        post.title,
                        post.content,
                        post.authorId,
                        post.createdAt,
                        post.updatedAt,
                        postLike.id.count()
                ))
                .from(post)
                .leftJoin(postLike).on(postLike.postId.eq(post.id))
                .groupBy(post.id, post.title, post.content, post.authorId, post.createdAt, post.updatedAt)
                .orderBy(postLike.id.count().desc(), post.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory.select(post.count()).from(post);
        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    @Override
    public List<PostResponse> findAllByCursor(UUID cursor, int size) {
        QPost post = QPost.post;

        return queryFactory
                .select(Projections.constructor(PostResponse.class,
                        post.id,
                        post.title,
                        post.content,
                        post.authorId,
                        post.createdAt,
                        post.updatedAt
                ))
                .from(post)
                .where(cursorCondition(post, cursor))
                .orderBy(post.createdAt.desc(), post.id.desc())
                .limit(1 + size)
                .fetch();
    }

    @Override
    public Page<PostResponse> findAllByOffset(Pageable pageable) {
        QPost post = QPost.post;

        List<PostResponse> content = queryFactory
                .select(Projections.constructor(PostResponse.class,
                        post.id,
                        post.title,
                        post.content,
                        post.authorId,
                        post.createdAt,
                        post.updatedAt
                ))
                .from(post)
                .orderBy(post.createdAt.desc(), post.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory.select(post.count()).from(post);
        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    private BooleanExpression cursorCondition(QPost post, UUID cursor) {
        if (cursor == null) {
            return null;
        }
        return post.id.lt(cursor);
    }
}
