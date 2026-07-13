package com.whatiread.shelf.domain;

import com.whatiread.identity.domain.User;
import com.whatiread.shared.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;

@Entity
@Table(
        name = "shelf_members",
        uniqueConstraints = @UniqueConstraint(name = "uk_shelf_members_shelf_user", columnNames = {"shelf_id", "user_id"})
)
public class ShelfMember extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shelf_id", nullable = false)
    private Shelf shelf;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ShelfMemberRole role;

    @Column(name = "invited_by")
    private UUID invitedBy;

    public ShelfMember() {
    }

    public ShelfMember(Shelf shelf, User user, ShelfMemberRole role, UUID invitedBy) {
        this.shelf = shelf;
        this.user = user;
        this.role = role;
        this.invitedBy = invitedBy;
    }

    public Shelf getShelf() {
        return shelf;
    }

    public User getUser() {
        return user;
    }

    public ShelfMemberRole getRole() {
        return role;
    }

    public void setRole(ShelfMemberRole role) {
        this.role = role;
    }

    public UUID getInvitedBy() {
        return invitedBy;
    }
}
