package com.whatiread.recommendation.domain;

import com.whatiread.catalog.domain.Book;
import com.whatiread.identity.domain.User;
import com.whatiread.shared.persistence.CreatedEntity;
import com.whatiread.shelf.domain.Shelf;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "recommendations")
public class Recommendation extends CreatedEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_user_id")
    private User fromUser;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_user_id", nullable = false)
    private User toUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private RecommendationTargetType targetType = RecommendationTargetType.BOOK;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id")
    private Book book;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shelf_id")
    private Shelf shelf;

    @Column(length = 500)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RecommendationSource source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RecommendationStatus status = RecommendationStatus.PENDING;

    public Recommendation() {
    }

    public Recommendation(
            User fromUser,
            User toUser,
            Book book,
            String message,
            RecommendationSource source
    ) {
        this.fromUser = fromUser;
        this.toUser = toUser;
        this.targetType = RecommendationTargetType.BOOK;
        this.book = book;
        this.message = message;
        this.source = source;
    }

    public Recommendation(
            User fromUser,
            User toUser,
            Shelf shelf,
            String message,
            RecommendationSource source
    ) {
        this.fromUser = fromUser;
        this.toUser = toUser;
        this.targetType = RecommendationTargetType.SHELF;
        this.shelf = shelf;
        this.message = message;
        this.source = source;
    }

    public User getFromUser() {
        return fromUser;
    }

    public User getToUser() {
        return toUser;
    }

    public RecommendationTargetType getTargetType() {
        return targetType;
    }

    public Book getBook() {
        return book;
    }

    public Shelf getShelf() {
        return shelf;
    }

    public String getMessage() {
        return message;
    }

    public RecommendationSource getSource() {
        return source;
    }

    public RecommendationStatus getStatus() {
        return status;
    }

    public void setStatus(RecommendationStatus status) {
        this.status = status;
    }
}
