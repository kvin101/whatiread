package com.whatiread.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class BusinessMetrics {

    private final MeterRegistry meterRegistry;
    private final Counter recommendationsAccepted;
    private final Counter usersRegistered;
    private final Counter messagesSent;
    private final Counter friendRequestsSent;
    private final Counter friendRequestsAccepted;
    private final Counter friendRequestsDeclined;
    private final Counter shelvesCreated;
    private final Counter booksAddedToLibrary;
    private final Counter commentsCreated;
    private final Counter recommendationsCreated;

    public BusinessMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.recommendationsAccepted = counter("recommendations.accepted", "Recommendations accepted by users");
        this.usersRegistered = counter("users.registered", "Users registered");
        this.messagesSent = counter("messages.sent", "Chat messages sent");
        this.friendRequestsSent = counter("friend.requests.sent", "Friend requests sent");
        this.friendRequestsAccepted = counter("friend.requests.accepted", "Friend requests accepted");
        this.friendRequestsDeclined = counter("friend.requests.declined", "Friend requests declined");
        this.shelvesCreated = counter("shelves.created", "Shelves created");
        this.booksAddedToLibrary = counter("books.added.to.library", "Books added to user library");
        this.commentsCreated = counter("comments.created", "Comments created");
        this.recommendationsCreated = counter("recommendations.created", "Recommendations created");
    }

    public void recordRecommendationAccepted() {
        recommendationsAccepted.increment();
    }

    public void recordUserRegistered() {
        usersRegistered.increment();
    }

    public void recordMessageSent() {
        messagesSent.increment();
    }

    public void recordLoginSuccess() {
        meterRegistry.counter("logins", "status", "success").increment();
    }

    public void recordLoginFailure(String reason) {
        meterRegistry.counter("logins", "status", "failure", "reason", reason).increment();
    }

    public void recordFriendRequestSent() {
        friendRequestsSent.increment();
    }

    public void recordFriendRequestAccepted() {
        friendRequestsAccepted.increment();
    }

    public void recordFriendRequestDeclined() {
        friendRequestsDeclined.increment();
    }

    public void recordShelfCreated() {
        shelvesCreated.increment();
    }

    public void recordBookAddedToLibrary() {
        booksAddedToLibrary.increment();
    }

    public void recordCommentCreated() {
        commentsCreated.increment();
    }

    public void recordRecommendationCreated() {
        recommendationsCreated.increment();
    }

    private Counter counter(String name, String description) {
        return Counter.builder(name)
                .description(description)
                .register(meterRegistry);
    }
}
