package com.whatiread.shelf.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "explore_shelf_read_model")
public class ExploreShelfReadModel {

    @Id
    @Column(name = "shelf_id")
    private UUID shelfId;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ShelfVisibility visibility;

    @Column(length = 500)
    private String description;

    @Column(length = 32)
    private String icon;

    @Column(name = "book_count", nullable = false)
    private int bookCount;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "cursor_token", nullable = false, length = 100)
    private String cursorToken;

    public UUID getShelfId() {
        return shelfId;
    }

    public void setShelfId(UUID shelfId) {
        this.shelfId = shelfId;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public ShelfVisibility getVisibility() {
        return visibility;
    }

    public void setVisibility(ShelfVisibility visibility) {
        this.visibility = visibility;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public int getBookCount() {
        return bookCount;
    }

    public void setBookCount(int bookCount) {
        this.bookCount = bookCount;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCursorToken() {
        return cursorToken;
    }

    public void setCursorToken(String cursorToken) {
        this.cursorToken = cursorToken;
    }
}
