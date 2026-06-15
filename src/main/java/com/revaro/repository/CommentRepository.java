package com.revaro.repository;

import com.revaro.entity.Comment;
import com.revaro.entity.Event;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByEventOrderByCreatedAtDesc(Event event);

    long countByEvent(Event event);

    org.springframework.data.domain.Page<Comment> findAllByOrderByCreatedAtDesc(
            org.springframework.data.domain.Pageable pageable);

    @Query("""
            SELECT c FROM Comment c
            LEFT JOIN c.likes l
            WHERE c.event = :event
            GROUP BY c
            ORDER BY COUNT(l) DESC, c.createdAt DESC
            """)
    List<Comment> findTopLikedCommentByEvent(@Param("event") Event event, Pageable pageable);

    default Optional<Comment> findTopCommentForEvent(Event event) {
        List<Comment> results = findTopLikedCommentByEvent(event, PageRequest.of(0, 1));
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }
}
