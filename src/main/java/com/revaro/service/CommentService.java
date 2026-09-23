package com.revaro.service;

import com.revaro.entity.Comment;
import com.revaro.entity.CommentLike;
import com.revaro.entity.Event;
import com.revaro.entity.User;
import com.revaro.exception.NotFoundException;
import com.revaro.repository.CommentLikeRepository;
import com.revaro.repository.CommentRepository;
import com.revaro.util.ProfanityFilter;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional
public class CommentService {

    private static final int MAX_LENGTH = 2000;

    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final ReportService reportService;
    private final ProfanityFilter profanityFilter;

    public CommentService(CommentRepository commentRepository,
                          CommentLikeRepository commentLikeRepository,
                          ReportService reportService,
                          ProfanityFilter profanityFilter) {
        this.commentRepository = commentRepository;
        this.commentLikeRepository = commentLikeRepository;
        this.reportService = reportService;
        this.profanityFilter = profanityFilter;
    }

    public Comment addComment(User user, Event event, String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Comment can't be empty.");
        }
        if (content.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Comments can be up to " + MAX_LENGTH + " characters.");
        }
        ProfanityFilter.FilterResult result = profanityFilter.filterAndFlag(content.trim());
        Comment comment = commentRepository.save(new Comment(user, event, result.filtered()));
        if (result.wasFlagged()) {
            reportService.flagComment(comment, user);
        }
        return comment;
    }

    public void deleteComment(Long commentId, User user) {
        Comment comment = getById(commentId);
        if (!comment.getUser().getId().equals(user.getId()) && !user.isAdmin()) {
            throw new AccessDeniedException("You can only delete your own comments.");
        }
        commentRepository.delete(comment);
    }

    // Returns true if the comment is liked now, false if the like was removed
    public boolean toggleLike(User user, Comment comment) {
        Optional<CommentLike> existing = commentLikeRepository.findByUserAndComment(user, comment);
        if (existing.isPresent()) {
            CommentLike like = existing.get();
            // The comment eagerly loads its likes with a cascade, so the like has to come out
            // of that list too or Hibernate puts it right back when it flushes
            like.getComment().getLikes().remove(like);
            commentLikeRepository.delete(like);
            return false;
        }
        commentLikeRepository.save(new CommentLike(user, comment));
        return true;
    }

    @Transactional(readOnly = true)
    public Comment getById(Long id) {
        return commentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Comment not found"));
    }

    @Transactional(readOnly = true)
    public List<Comment> getCommentsForEvent(Event event) {
        return commentRepository.findByEventOrderByCreatedAtDesc(event);
    }

    @Transactional(readOnly = true)
    public Set<Long> getLikedCommentIds(User user, Event event) {
        return new HashSet<>(commentLikeRepository.findLikedCommentIds(user, event));
    }
}
