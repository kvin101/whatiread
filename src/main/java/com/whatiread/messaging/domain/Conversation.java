package com.whatiread.messaging.domain;

import com.whatiread.identity.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(
        name = "conversations",
        uniqueConstraints = @UniqueConstraint(name = "uk_conversations_users", columnNames = {"user_a_id", "user_b_id"})
)
public class Conversation {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConversationType type = ConversationType.DIRECT;

    @Column(length = 255)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_a_id")
    private User userA;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_b_id")
    private User userB;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Conversation() {
    }

    public Conversation(User userA, User userB) {
        this.type = ConversationType.DIRECT;
        this.userA = userA;
        this.userB = userB;
    }

    public static Conversation createGroup(User creator, String name) {
        Conversation conversation = new Conversation();
        conversation.type = ConversationType.GROUP;
        conversation.name = name.trim();
        conversation.createdBy = creator;
        return conversation;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public ConversationType getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name.trim();
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public User getUserA() {
        return userA;
    }

    public User getUserB() {
        return userB;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isDirect() {
        return type == ConversationType.DIRECT;
    }

    public boolean isGroup() {
        return type == ConversationType.GROUP;
    }

    public boolean involves(UUID userId) {
        if (isDirect()) {
            return userA.getId().equals(userId) || userB.getId().equals(userId);
        }
        return false;
    }

    public UUID otherParticipantId(UUID userId) {
        if (!isDirect()) {
            throw new IllegalStateException("Not a direct conversation");
        }
        if (userA.getId().equals(userId)) {
            return userB.getId();
        }
        if (userB.getId().equals(userId)) {
            return userA.getId();
        }
        throw new IllegalArgumentException("User is not a participant");
    }
}
