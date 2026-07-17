package com.whatiread.reading.domain;

import com.whatiread.identity.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(
        name = "reading_goals",
        uniqueConstraints = @UniqueConstraint(name = "uk_reading_goals_user_year", columnNames = {"user_id", "goal_year"})
)
public class ReadingGoal {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "goal_year", nullable = false)
    private short year;

    @Column(name = "target_books", nullable = false)
    private int targetBooks;

    @Column(name = "target_pages")
    private Integer targetPages;

    public ReadingGoal() {
    }

    public ReadingGoal(User user, short year, int targetBooks, Integer targetPages) {
        this.user = user;
        this.year = year;
        this.targetBooks = targetBooks;
        this.targetPages = targetPages;
    }

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public short getYear() {
        return year;
    }

    public void setYear(short year) {
        this.year = year;
    }

    public int getTargetBooks() {
        return targetBooks;
    }

    public void setTargetBooks(int targetBooks) {
        this.targetBooks = targetBooks;
    }

    public Integer getTargetPages() {
        return targetPages;
    }

    public void setTargetPages(Integer targetPages) {
        this.targetPages = targetPages;
    }
}
