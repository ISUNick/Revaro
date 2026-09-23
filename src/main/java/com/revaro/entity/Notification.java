package com.revaro.entity;

import com.revaro.enums.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notification_recipient", columnList = "recipient_id"),
        @Index(name = "idx_notification_read", columnList = "is_read")
})
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "actor_id")
    private User actor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private NotificationType type;

    // ON DELETE CASCADE so notifications disappear with the event or comment they point to
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

    public Notification() {
    }

    public Notification(User recipient, User actor, NotificationType type, Event event, Comment comment) {
        this.recipient = recipient;
        this.actor = actor;
        this.type = type;
        this.event = event;
        this.comment = comment;
    }

    public String getMessage() {
        String actorName = actor != null ? actor.getUsername() : "Someone";
        String eventTitle = event != null ? "\"" + event.getTitle() + "\"" : "your event";
        return switch (type) {
            case COMMENT_ON_EVENT -> actorName + " commented on " + eventTitle;
            case MENTION -> actorName + " mentioned you in a comment on " + eventTitle;
            case RSVP_GOING -> actorName + " is going to " + eventTitle;
            case RSVP_INTERESTED -> actorName + " is interested in " + eventTitle;
            case COMMENT_LIKED -> actorName + " liked your comment";
            case EVENT_CANCELLED -> eventTitle + " has been cancelled";
            case EVENT_POSTPONED -> eventTitle + " has been postponed";
        };
    }

    public String getLink() {
        if (event == null) {
            return "/notifications";
        }
        String link = "/events/" + event.getId();
        return comment != null ? link + "#comment-" + comment.getId() : link;
    }

    public Long getId() { return id; }
    public User getRecipient() { return recipient; }
    public User getActor() { return actor; }
    public NotificationType getType() { return type; }
    public Event getEvent() { return event; }
    public Comment getComment() { return comment; }
    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
