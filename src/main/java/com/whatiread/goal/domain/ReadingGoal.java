package com.whatiread.goal.domain;

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
        uniqueConstraints = @UniqueConstraint(
                name = "uk_reading_goals_user_year",
                columnNames = {"user_id", "year"}
        )
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
    private short goalYear;

    @Column(name = "target_books", nullable = false)
    private int targetBooks;

    protected ReadingGoal() {
    }

    public ReadingGoal(User user, short goalYear, int targetBooks) {
        this.user = user;
        this.goalYear = goalYear;
        this.targetBooks = targetBooks;
    }

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public short getGoalYear() {
        return goalYear;
    }

    public int getTargetBooks() {
        return targetBooks;
    }

    public void setTargetBooks(int targetBooks) {
        this.targetBooks = targetBooks;
    }
}
