package com.revaro.service;

import com.revaro.entity.Comment;
import com.revaro.entity.Event;
import com.revaro.entity.Report;
import com.revaro.entity.User;
import com.revaro.enums.ReportStatus;
import com.revaro.enums.Role;
import com.revaro.exception.NotFoundException;
import com.revaro.repository.ClaimRequestRepository;
import com.revaro.repository.CommentLikeRepository;
import com.revaro.repository.CommentRepository;
import com.revaro.repository.NotificationRepository;
import com.revaro.repository.PasswordResetTokenRepository;
import com.revaro.repository.ReportRepository;
import com.revaro.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
public class AdminService {

    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final ReportRepository reportRepository;
    private final NotificationRepository notificationRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final ClaimRequestRepository claimRequestRepository;
    private final EventService eventService;

    public AdminService(UserRepository userRepository,
                        CommentRepository commentRepository,
                        CommentLikeRepository commentLikeRepository,
                        ReportRepository reportRepository,
                        NotificationRepository notificationRepository,
                        PasswordResetTokenRepository tokenRepository,
                        ClaimRequestRepository claimRequestRepository,
                        EventService eventService) {
        this.userRepository = userRepository;
        this.commentRepository = commentRepository;
        this.commentLikeRepository = commentLikeRepository;
        this.reportRepository = reportRepository;
        this.notificationRepository = notificationRepository;
        this.tokenRepository = tokenRepository;
        this.claimRequestRepository = claimRequestRepository;
        this.eventService = eventService;
    }

    public User toggleAdmin(Long userId, User admin) {
        if (userId.equals(admin.getId())) {
            throw new IllegalArgumentException("You can't change your own admin status.");
        }
        User user = getUser(userId);
        user.setRole(user.isAdmin() ? Role.USER : Role.ADMIN);
        return userRepository.save(user);
    }

    public void deleteUser(Long userId, User admin) {
        if (userId.equals(admin.getId())) {
            throw new IllegalArgumentException("You can't delete your own account.");
        }
        deleteUser(getUser(userId));
    }

    public void deleteComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Comment not found"));
        commentRepository.delete(comment);
    }

    public void dismissReport(Long reportId, User admin) {
        markReviewed(getReport(reportId), ReportStatus.DISMISSED, admin);
    }

    // Deleting a user takes their events and comments with them, so the
    // "user and event" and "user and comment" buttons both just delete the user
    public void resolveReport(Long reportId, String action, User admin) {
        Report report = getReport(reportId);
        Event event = report.getReportedEvent();
        Comment comment = report.getReportedComment();
        User offender = report.getReportedUser() != null ? report.getReportedUser()
                : comment != null ? comment.getUser()
                : event != null ? event.getCreator()
                : null;

        // Close the report out before deleting anything it points at
        report.setReportedEvent(null);
        report.setReportedComment(null);
        report.setReportedUser(null);
        markReviewed(report, ReportStatus.REVIEWED, admin);

        switch (action) {
            case "delete-event" -> {
                if (event != null) {
                    eventService.deleteEvent(event.getId(), admin);
                }
            }
            case "delete-comment" -> {
                if (comment != null) {
                    commentRepository.delete(comment);
                }
            }
            case "delete-user", "delete-user-and-event", "delete-user-and-comment" -> {
                if (offender != null && !offender.getId().equals(admin.getId())) {
                    deleteUser(offender);
                }
            }
            default -> throw new IllegalArgumentException("Unknown action: " + action);
        }
    }

    // Notifications, reset tokens and reports reference users without a cascade, so they
    // have to go first. Likes are deleted in bulk because comments eagerly load their likes,
    // which would otherwise stop Hibernate from removing them.
    private void deleteUser(User user) {
        notificationRepository.deleteAllInvolving(user);
        tokenRepository.deleteAllByUser(user);
        reportRepository.deleteByReporter(user);
        reportRepository.clearReviewer(user);
        claimRequestRepository.clearReviewer(user);
        commentLikeRepository.deleteByUser(user);
        userRepository.delete(user);
    }

    private void markReviewed(Report report, ReportStatus status, User admin) {
        report.setStatus(status);
        report.setReviewedBy(admin);
        report.setReviewedAt(LocalDateTime.now());
        reportRepository.saveAndFlush(report);
    }

    private User getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    private Report getReport(Long id) {
        return reportRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Report not found"));
    }
}
