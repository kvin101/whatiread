package com.whatiread.shelf.domain;

import com.whatiread.identity.domain.User;
import com.whatiread.shared.persistence.AuditActorEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(
        name = "shelves",
        uniqueConstraints = @UniqueConstraint(name = "uk_shelves_user_slug", columnNames = {"user_id", "slug"})
)
@SQLDelete(sql = "UPDATE shelves SET deleted = true WHERE id = ?")
@SQLRestriction("deleted = false")
public class Shelf extends AuditActorEntity {

    @Column(nullable = false)
    private final boolean deleted = false;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User owner;
    @Column(nullable = false, length = 100)
    private String name;
    @Column(nullable = false, length = 100)
    private String slug;
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ShelfVisibility visibility = ShelfVisibility.PRIVATE;
    @Column(length = 500)
    private String description;
    @Column(length = 32)
    private String icon;
    @Column(name = "pin_hash", length = 100)
    private String pinHash;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cloned_from_shelf_id")
    private Shelf clonedFromShelf;
    @Version
    private long version;

    public Shelf() {
    }

    public Shelf(User owner, String name, String slug) {
        this.owner = owner;
        this.name = name;
        this.slug = slug;
    }

    public User getOwner() {
        return owner;
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

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
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

    public String getPinHash() {
        return pinHash;
    }

    public void setPinHash(String pinHash) {
        this.pinHash = pinHash;
    }

    public Shelf getClonedFromShelf() {
        return clonedFromShelf;
    }

    public void setClonedFromShelf(Shelf clonedFromShelf) {
        this.clonedFromShelf = clonedFromShelf;
    }
}
