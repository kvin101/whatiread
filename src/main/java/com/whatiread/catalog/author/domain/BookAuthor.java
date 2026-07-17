package com.whatiread.catalog.author.domain;

import com.whatiread.catalog.domain.Book;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "book_authors")
public class BookAuthor {

    @EmbeddedId
    private BookAuthorId id;

    @MapsId("bookId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @MapsId("authorId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private Author author;

    @Column(nullable = false)
    private int position;

    public BookAuthor() {
    }

    public BookAuthor(Book book, Author author, int position) {
        this.book = book;
        this.author = author;
        this.position = position;
        this.id = new BookAuthorId(book.getId(), author.getId());
    }

    public BookAuthorId getId() {
        return id;
    }

    public Book getBook() {
        return book;
    }

    public Author getAuthor() {
        return author;
    }

    public int getPosition() {
        return position;
    }

    @Embeddable
    public static class BookAuthorId implements Serializable {

        @Column(name = "book_id")
        private UUID bookId;

        @Column(name = "author_id")
        private UUID authorId;

        public BookAuthorId() {
        }

        public BookAuthorId(UUID bookId, UUID authorId) {
            this.bookId = bookId;
            this.authorId = authorId;
        }

        public UUID getBookId() {
            return bookId;
        }

        public UUID getAuthorId() {
            return authorId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof BookAuthorId that)) {
                return false;
            }
            return Objects.equals(bookId, that.bookId) && Objects.equals(authorId, that.authorId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(bookId, authorId);
        }
    }
}
