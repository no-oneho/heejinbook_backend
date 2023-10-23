package com.book.heejinbook.repository.review;

import com.book.heejinbook.dto.review.response.ReviewListResponse;
import com.book.heejinbook.entity.Book;
import com.book.heejinbook.enums.ReviewSortType;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Wildcard;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.book.heejinbook.entity.QBook.book;
import static com.book.heejinbook.entity.QReview.review;
import static com.book.heejinbook.entity.QComment.comment;

@Repository
@RequiredArgsConstructor
public class ReviewCustomRepositoryImpl implements ReviewCustomRepository {

    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public Page<ReviewListResponse> findReviewList(Book book, Pageable pageable, String sort) {

        ReviewSortType reviewSortType = ReviewSortType.fromString(sort);
        List<OrderSpecifier<?>> orderSpecifiers = new ArrayList<>();

        if (Objects.requireNonNull(reviewSortType) == ReviewSortType.COUNT_COMMENT) {
            orderSpecifiers.add(comment.id.count().desc());
        }
        orderSpecifiers.add(review.id.desc());

        List<ReviewListResponse> result = jpaQueryFactory
                .select(Projections.constructor(ReviewListResponse.class,
                        review.id,
                        review.user.nickname,
                        review.user.profileUrl,
                        review.title,
                        review.contents,
                        review.createdAt,
                        review.phrase,
                        comment.id.count()
                ))
                .from(review)
                .leftJoin(comment)
                .on(comment.review.id.eq(review.id),
                        comment.isDeleted.eq(false))
                .where(
                        eqBookId(book.getId()),
                        isDeletedFalse()
                )
                .orderBy(orderSpecifiers.toArray(new OrderSpecifier[0]))
                .groupBy(review)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        int getTotalCount = jpaQueryFactory
                .select(Wildcard.count)
                .from(review)
                .leftJoin(comment)
                .on(comment.review.id.eq(review.id),
                        comment.isDeleted.eq(false))
                .where(
                        eqBookId(book.getId()),
                        isDeletedFalse()
                )
                .orderBy(orderSpecifiers.toArray(new OrderSpecifier[0]))
                .groupBy(review)
                .fetch().size();

        return new PageImpl<>(result, pageable, getTotalCount);
    }

    private BooleanExpression eqBookId(Long bookId) {
            return review.book.id.eq(bookId);
    }

    private BooleanExpression isDeletedFalse() {
        return review.isDeleted.eq(false);
    }
}