package com.whatiread.social.domain;

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
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "friendships")
public class Friendship {

    @Column(name = "created_at", nullable = false, updatable = false)
    private final Instant createdAt = Instant.now();
    @EmbeddedId
    private FriendshipId id;
    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private com.whatiread.identity.domain.User user;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "friend_id", nullable = false, insertable = false, updatable = false)
    private com.whatiread.identity.domain.User friend;

    public Friendship() {
    }

    public Friendship(com.whatiread.identity.domain.User user, com.whatiread.identity.domain.User friend) {
        this.user = user;
        this.friend = friend;
        this.id = new FriendshipId(user.getId(), friend.getId());
    }

    public FriendshipId getId() {
        return id;
    }

    public com.whatiread.identity.domain.User getUser() {
        return user;
    }

    public com.whatiread.identity.domain.User getFriend() {
        return friend;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Embeddable
    public static class FriendshipId implements Serializable {

        @Column(name = "user_id")
        private UUID userId;

        @Column(name = "friend_id")
        private UUID friendId;

        public FriendshipId() {
        }

        public FriendshipId(UUID userId, UUID friendId) {
            this.userId = userId;
            this.friendId = friendId;
        }

        public UUID getUserId() {
            return userId;
        }

        public UUID getFriendId() {
            return friendId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof FriendshipId that)) {
                return false;
            }
            return Objects.equals(userId, that.userId) && Objects.equals(friendId, that.friendId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, friendId);
        }
    }
}
