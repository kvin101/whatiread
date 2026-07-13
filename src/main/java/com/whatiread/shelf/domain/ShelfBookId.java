package com.whatiread.shelf.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class ShelfBookId implements Serializable {

    @Column(name = "shelf_id")
    private UUID shelfId;

    @Column(name = "user_book_id")
    private UUID userBookId;

    public ShelfBookId() {
    }

    public ShelfBookId(UUID shelfId, UUID userBookId) {
        this.shelfId = shelfId;
        this.userBookId = userBookId;
    }

    public UUID getShelfId() {
        return shelfId;
    }

    public UUID getUserBookId() {
        return userBookId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ShelfBookId that)) {
            return false;
        }
        return Objects.equals(shelfId, that.shelfId) && Objects.equals(userBookId, that.userBookId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(shelfId, userBookId);
    }
}
