package com.whatiread.library.domain;

import com.whatiread.catalog.domain.Book;
import com.whatiread.identity.domain.User;
import com.whatiread.shared.persistence.AuditableEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "user_books",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_books_user_book", columnNames = {"user_id", "book_id"})
)
public class UserBook extends AuditableEntity {

    @OneToMany(mappedBy = "userBook", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private final List<UserBookNote> notes = new ArrayList<>();
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReadingStatus status;
    @Column(precision = 2, scale = 1)
    private BigDecimal rating;
    @Column(name = "progress_pages")
    private Integer progressPages;
    @Column(name = "progress_percent")
    private Short progressPercent;
    @Column(name = "started_at")
    private LocalDate startedAt;
    @Column(name = "finished_at")
    private LocalDate finishedAt;
    @Version
    private long version;

    public UserBook() {
    }

    public UserBook(User user, Book book, ReadingStatus status) {
        this.user = user;
        this.book = book;
        this.status = status;
    }

    public User getUser() {
        return user;
    }

    public Book getBook() {
        return book;
    }

    public ReadingStatus getStatus() {
        return status;
    }

    public void setStatus(ReadingStatus status) {
        this.status = status;
    }

    public BigDecimal getRating() {
        return rating;
    }

    public void setRating(BigDecimal rating) {
        this.rating = rating;
    }

    public Integer getProgressPages() {
        return progressPages;
    }

    public void setProgressPages(Integer progressPages) {
        this.progressPages = progressPages;
    }

    public Short getProgressPercent() {
        return progressPercent;
    }

    public void setProgressPercent(Short progressPercent) {
        this.progressPercent = progressPercent;
    }

    public LocalDate getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDate startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDate getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(LocalDate finishedAt) {
        this.finishedAt = finishedAt;
    }

    public List<UserBookNote> getNotes() {
        return notes;
    }

    public void addNote(UserBookNote note) {
        notes.add(note);
        note.setUserBook(this);
    }
}
