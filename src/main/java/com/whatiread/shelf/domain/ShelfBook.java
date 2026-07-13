package com.whatiread.shelf.domain;

import com.whatiread.library.domain.UserBook;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "shelf_books")
public class ShelfBook {

    @EmbeddedId
    private ShelfBookId id;

    @MapsId("shelfId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shelf_id", nullable = false)
    private Shelf shelf;

    @MapsId("userBookId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_book_id", nullable = false)
    private UserBook userBook;

    @Column(nullable = false)
    private int position;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ShelfVisibility visibility;

    @Column(name = "added_by")
    private UUID addedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public ShelfBook() {
    }

    public ShelfBook(Shelf shelf, UserBook userBook, int position, UUID addedBy) {
        this.shelf = shelf;
        this.userBook = userBook;
        this.position = position;
        this.addedBy = addedBy;
        this.id = new ShelfBookId(shelf.getId(), userBook.getId());
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public ShelfBookId getId() {
        return id;
    }

    public Shelf getShelf() {
        return shelf;
    }

    public UserBook getUserBook() {
        return userBook;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public ShelfVisibility getVisibility() {
        return visibility;
    }

    public void setVisibility(ShelfVisibility visibility) {
        this.visibility = visibility;
    }

    public ShelfVisibility effectiveVisibility() {
        return visibility != null ? visibility : shelf.getVisibility();
    }

    public UUID getAddedBy() {
        return addedBy;
    }

    public void setAddedBy(UUID addedBy) {
        this.addedBy = addedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
