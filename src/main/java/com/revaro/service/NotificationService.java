package com.revaro.service;

import com.revaro.entity.Comment;
import com.revaro.entity.Event;
import com.revaro.entity.Notification;
import com.revaro.entity.User;
import com.revaro.enums.NotificationType;
import com.revaro.repository.NotificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    // ── Create ────────────────────────────────────────────────────────────────

    public void notifyComment(User actor, Event event) {
        // Don't notify yourself
        if (event.getCreator().getId().equals(actor.getId())) return;
        Notification n = new Notification(event.getCreator(), actor,
                NotificationType.COMMENT_ON_EVENT, event, null);
        notificationRepository.save(n);
    }

    public void notifyRsvpGoing(User actor, Event event) {
        if (event.getCreator().getId().equals(actor.getId())) return;
        Notification n = new Notification(event.getCreator(), actor,
                NotificationType.RSVP_GOING, event, null);
        notificationRepository.save(n);
    }

    public void notifyRsvpInterested(User actor, Event event) {
        if (event.getCreator().getId().equals(actor.getId())) return;
        Notification n = new Notification(event.getCreator(), actor,
                NotificationType.RSVP_INTERESTED, event, null);
        notificationRepository.save(n);
    }

    public void notifyCommentLiked(User actor, Comment comment) {
        if (comment.getUser().getId().equals(actor.getId())) return;
        Notification n = new Notification(comment.getUser(), actor,
                NotificationType.COMMENT_LIKED, comment.getEvent(), comment);
        notificationRepository.save(n);
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public long getUnreadCount(User user) {
        return notificationRepository.countByRecipientAndIsReadFalse(user);
    }

    @Transactional(readOnly = true)
    public List<Notification> getRecentUnread(User user) {
        return notificationRepository
                .findTop10ByRecipientAndIsReadFalseOrderByCreatedAtDesc(user);
    }

    @Transactional(readOnly = true)
    public Page<Notification> getAllNotifications(User user, int page) {
        return notificationRepository.findByRecipientOrderByCreatedAtDesc(
                user, PageRequest.of(page, 20));
    }

    public void markAllAsRead(User user) {
        notificationRepository.markAllAsRead(user);
    }

    public void markAsRead(Long notificationId, User user) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            if (n.getRecipient().getId().equals(user.getId())) {
                n.setRead(true);
                notificationRepository.save(n);
            }
        });
    }
}
