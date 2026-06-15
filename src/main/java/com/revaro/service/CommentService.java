package com.revaro.service;

import com.revaro.entity.Comment;
import com.revaro.entity.CommentLike;
import com.revaro.entity.Event;
import com.revaro.entity.Report;
import com.revaro.entity.User;
import com.revaro.enums.ReportStatus;
import com.revaro.enums.ReportType;
import com.revaro.repository.CommentLikeRepository;
import com.revaro.repository.CommentRepository;
import com.revaro.repository.ReportRepository;
import com.revaro.util.ProfanityFilter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CommentService {

    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final ReportRepository reportRepository;
    private final ProfanityFilter profanityFilter;

    public CommentService(CommentRepository commentRepository,
                          CommentLikeRepository commentLikeRepository,
                          ReportRepository reportRepository,
                          ProfanityFilter profanityFilter) {
        this.commentRepository = commentRepository;
        this.commentLikeRepository = commentLikeRepository;
        this.reportRepository = reportRepository;
        this.profanityFilter = profanityFilter;
    }

    // ── Comments ──────────────────────────────────────────────────────────────

    public Comment addComment(User user, Event event, String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Comment cannot be empty.");
        }
        if (content.length() > 2000) {
            throw new IllegalArgumentException("Comment must be under 2000 characters.");
        }

        // Filter profanity — replace bad words with asterisks
        ProfanityFilter.FilterResult result = profanityFilter.filterAndFlag(content.trim());
        Comment comment = new Comment(user, event, result.filtered());
        comment = commentRepository.save(comment);

        // Auto-report if profanity was detected
        if (result.wasFlagged()) {
            Report report = new Report();
            report.setReporter(user); // system reports on behalf of poster
            report.setReportType(ReportType.COMMENT);
            report.setReportedComment(comment);
            report.setReason("Auto-flagged: comment contained filtered language");
            report.setStatus(ReportStatus.PENDING);
            reportRepository.save(report);
        }

        return comment;
    }

    public void deleteComment(Long commentId, User requestingUser) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found."));

        if (!comment.getUser().getId().equals(requestingUser.getId())
                && !requestingUser.isAdmin()) {
            throw new SecurityException("You cannot delete this comment.");
        }
        commentRepository.delete(comment);
    }

    @Transactional(readOnly = true)
    public List<Comment> getCommentsForEvent(Event event) {
        return commentRepository.findByEventOrderByCreatedAtDesc(event);
    }

    @Transactional(readOnly = true)
    public Optional<Comment> getTopComment(Event event) {
        return commentRepository.findTopCommentForEvent(event);
    }

    // ── Likes ─────────────────────────────────────────────────────────────────

    public boolean toggleLike(User user, Comment comment) {
        Optional<CommentLike> existing =
                commentLikeRepository.findByUserAndComment(user, comment);

        if (existing.isPresent()) {
            commentLikeRepository.delete(existing.get());
            return false;
        } else {
            commentLikeRepository.save(new CommentLike(user, comment));
            return true;
        }
    }

    @Transactional(readOnly = true)
    public boolean hasUserLiked(User user, Comment comment) {
        return commentLikeRepository.existsByUserAndComment(user, comment);
    }

    @Transactional(readOnly = true)
    public long getLikeCount(Comment comment) {
        return commentLikeRepository.countByComment(comment);
    }

    @Transactional(readOnly = true)
    public Optional<Comment> findById(Long id) {
        return commentRepository.findById(id);
    }
}
