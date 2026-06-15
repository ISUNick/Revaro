package com.revaro.service;

import com.revaro.entity.Comment;
import com.revaro.entity.CommentLike;
import com.revaro.entity.Event;
import com.revaro.entity.User;
import com.revaro.repository.CommentLikeRepository;
import com.revaro.repository.CommentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CommentService {

    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;

    public CommentService(CommentRepository commentRepository,
                          CommentLikeRepository commentLikeRepository) {
        this.commentRepository = commentRepository;
        this.commentLikeRepository = commentLikeRepository;
    }

    // ── Comments ──────────────────────────────────────────────────────────────

    public Comment addComment(User user, Event event, String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Comment cannot be empty.");
        }
        if (content.length() > 2000) {
            throw new IllegalArgumentException("Comment must be under 2000 characters.");
        }
        Comment comment = new Comment(user, event, content.trim());
        return commentRepository.save(comment);
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

    /**
     * Toggle like on a comment. Returns true if now liked, false if unliked.
     */
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
