package com.whatiread.shelf.domain;

import com.whatiread.identity.domain.User;
import com.whatiread.shared.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "shelf_share_links")
public class ShelfShareLink extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shelf_id", nullable = false)
    private Shelf shelf;

    @Column(nullable = false, unique = true)
    private UUID token;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    protected ShelfShareLink() {
    }

    public ShelfShareLink(Shelf shelf, UUID token, User createdBy, Instant expiresAt) {
        this.shelf = shelf;
        this.token = token;
        this.createdBy = createdBy;
        this.expiresAt = expiresAt;
    }

    public Shelf getShelf() {
        return shelf;
    }

    public UUID getToken() {
        return token;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public boolean isActive() {
        if (revokedAt != null) {
            return false;
        }
        return expiresAt == null || !expiresAt.isBefore(Instant.now());
    }

    public void revoke() {
        revokedAt = Instant.now();
    }
}
