package com.revaro.service;

import com.revaro.entity.Comment;
import com.revaro.entity.Event;
import com.revaro.entity.Notification;
import com.revaro.entity.Rsvp;
import com.revaro.entity.User;
import com.revaro.enums.NotificationType;
import com.revaro.enums.RsvpStatus;
import com.revaro.repository.NotificationRepository;
import com.revaro.repository.RsvpRepository;
import com.revaro.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Transactional
public class NotificationService {

    private static final int PAGE_SIZE = 20;
    // Same rules as usernames: letters, numbers and underscores, 3 to 30 characters
    private static final Pattern MENTION = Pattern.compile("@([A-Za-z0-9_]{3,30})");

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final RsvpRepository rsvpRepository;

    public NotificationService(NotificationRepository notificationRepository,
                               UserRepository userRepository,
                               RsvpRepository rsvpRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.rsvpRepository = rsvpRepository;
    }

    public void notifyCommentPosted(User author, Comment comment) {
        Event event = comment.getEvent();
        Collection<User> mentioned = findMentionedUsers(comment.getContent());
        for (User user : mentioned) {
            send(user, author, NotificationType.MENTION, event, comment);
        }

        // If the owner was mentioned they already got a notification for this comment
        boolean ownerMentioned = mentioned.stream()
                .anyMatch(user -> user.getId().equals(event.getCreator().getId()));
        if (!ownerMentioned) {
            send(event.getCreator(), author, NotificationType.COMMENT_ON_EVENT, event, comment);
        }
    }

    public void notifyRsvp(User user, Event event, RsvpStatus status) {
        NotificationType type = status == RsvpStatus.GOING
                ? NotificationType.RSVP_GOING
                : NotificationType.RSVP_INTERESTED;
        send(event.getCreator(), user, type, event, null);
    }

    public void notifyCommentLiked(User liker, Comment comment) {
        send(comment.getUser(), liker, NotificationType.COMMENT_LIKED, comment.getEvent(), comment);
    }

    // Lets everyone who RSVP'd know when an event gets cancelled or postponed
    public void notifyStatusChange(Event event) {
        NotificationType type = switch (event.getStatus()) {
            case CANCELLED -> NotificationType.EVENT_CANCELLED;
            case POSTPONED -> NotificationType.EVENT_POSTPONED;
            case ACTIVE -> null;
        };
        if (type == null) {
            return;
        }
        for (Rsvp rsvp : rsvpRepository.findByEvent(event)) {
            send(rsvp.getUser(), event.getCreator(), type, event, null);
        }
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(User user) {
        return notificationRepository.countByRecipientAndIsReadFalse(user);
    }

    @Transactional(readOnly = true)
    public Page<Notification> getNotifications(User user, int page) {
        return notificationRepository.findByRecipientOrderByCreatedAtDesc(user,
                PageRequest.of(Math.max(page, 0), PAGE_SIZE));
    }

    public void markAllAsRead(User user) {
        notificationRepository.markAllAsRead(user);
    }

    private Collection<User> findMentionedUsers(String content) {
        Map<Long, User> users = new LinkedHashMap<>();
        Matcher matcher = MENTION.matcher(content);
        while (matcher.find()) {
            userRepository.findByUsernameIgnoreCase(matcher.group(1))
                    .ifPresent(user -> users.put(user.getId(), user));
        }
        return users.values();
    }

    private void send(User recipient, User actor, NotificationType type, Event event, Comment comment) {
        if (recipient.getId().equals(actor.getId())) {
            return;
        }
        notificationRepository.save(new Notification(recipient, actor, type, event, comment));
    }
}
