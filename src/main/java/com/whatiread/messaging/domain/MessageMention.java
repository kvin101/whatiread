package com.whatiread.messaging.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "message_mentions")
public class MessageMention {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;

    @Enumerated(EnumType.STRING)
    @Column(name = "mention_type", nullable = false, length = 20)
    private MentionType mentionType;

    @Column(name = "target_id", nullable = false)
    private UUID targetId;

    @Column(nullable = false, length = 200)
    private String label;

    public MessageMention() {
    }

    public MessageMention(Message message, MentionType mentionType, UUID targetId, String label) {
        this.message = message;
        this.mentionType = mentionType;
        this.targetId = targetId;
        this.label = label;
    }

    public UUID getId() {
        return id;
    }

    public MentionType getMentionType() {
        return mentionType;
    }

    public UUID getTargetId() {
        return targetId;
    }

    public String getLabel() {
        return label;
    }
}
