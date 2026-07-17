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
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(
        name = "reading_activity_days",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_reading_activity_days_user_date",
                columnNames = {"user_id", "activity_date"}
        )
)
public class ReadingActivityDay {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "activity_date", nullable = false)
    private LocalDate activityDate;

    public ReadingActivityDay() {
    }

    public ReadingActivityDay(User user, LocalDate activityDate) {
        this.user = user;
        this.activityDate = activityDate;
    }

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public LocalDate getActivityDate() {
        return activityDate;
    }
}
