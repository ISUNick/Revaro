package com.revaro.entity;

import com.revaro.enums.NotificationType;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * A notification for a user — comment on their event, RSVP, comment like, etc.
 */
@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_notification_recipient", columnList = "recipient_id"),
    @Index(name = "idx_notification_read", columnList = "is_read")
})
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Who receives this notification
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    // Who triggered it
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "actor_id")
    private User actor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private NotificationType type;

    // Optional references to what triggered it
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "event_id", foreignKey = @ForeignKey(name = "fk_notif_event",
        foreignKeyDefinition = "FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE"))
    private Event event;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "comment_id", foreignKey = @ForeignKey(name = "fk_notif_comment",
        foreignKeyDefinition = "FOREIGN KEY (comment_id) REFERENCES comments(id) ON DELETE CASCADE"))
    private Comment comment;

    @Column(nullable = false)
    private boolean isRead = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Notification() {}

    // Convenience constructor
    public Notification(User recipient, User actor, NotificationType type, Event event, Comment comment) {
        this.recipient = recipient;
        this.actor = actor;
        this.type = type;
        this.event = event;
        this.comment = comment;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public Long getId() { return id; }
    public User getRecipient() { return recipient; }
    public void setRecipient(User recipient) { this.recipient = recipient; }
    public User getActor() { return actor; }
    public void setActor(User actor) { this.actor = actor; }
    public NotificationType getType() { return type; }
    public void setType(NotificationType type) { this.type = type; }
    public Event getEvent() { return event; }
    public void setEvent(Event event) { this.event = event; }
    public Comment getComment() { return comment; }
    public void setComment(Comment comment) { this.comment = comment; }
    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    /**
     * Human-readable message for the notification.
     */
    public String getMessage() {
        String actorName = actor != null ? actor.getUsername() : "Someone";
        String eventTitle = event != null ? "\"" + event.getTitle() + "\"" : "your event";
        return switch (type) {
            case COMMENT_ON_EVENT  -> actorName + " commented on " + eventTitle;
            case RSVP_GOING        -> actorName + " is going to " + eventTitle;
            case RSVP_INTERESTED   -> actorName + " is interested in " + eventTitle;
            case COMMENT_LIKED     -> actorName + " liked your comment";
            case EVENT_CANCELLED   -> eventTitle + " has been cancelled";
            case EVENT_POSTPONED   -> eventTitle + " has been postponed";
        };
    }

    /**
     * URL to navigate to when the notification is clicked.
     */
    public String getLink() {
        if (event != null) return "/events/" + event.getId();
        return "/notifications";
    }
}
