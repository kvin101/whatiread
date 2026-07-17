package com.whatiread.catalog.author.domain;

import com.whatiread.shared.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "authors")
public class Author extends AuditableEntity {

    @Column(nullable = false, unique = true, length = 200)
    private String slug;

    @Column(nullable = false, length = 300)
    private String name;

    @Column(columnDefinition = "clob")
    private String bio;

    @Column(name = "photo_url", length = 2048)
    private String photoUrl;

    @Column(name = "open_library_author_id", length = 100)
    private String openLibraryAuthorId;

    public Author() {
    }

    public Author(String slug, String name) {
        this.slug = slug;
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public String getOpenLibraryAuthorId() {
        return openLibraryAuthorId;
    }

    public void setOpenLibraryAuthorId(String openLibraryAuthorId) {
        this.openLibraryAuthorId = openLibraryAuthorId;
    }
}
