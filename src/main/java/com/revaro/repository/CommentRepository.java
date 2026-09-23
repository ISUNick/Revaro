package com.revaro.repository;

import com.revaro.entity.Comment;
import com.revaro.entity.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByEventOrderByCreatedAtDesc(Event event);

    Page<Comment> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("""
            SELECT c.event.creator.id AS userId, COUNT(c) AS total FROM Comment c
            WHERE c.user <> c.event.creator AND c.createdAt >= :since
            GROUP BY c.event.creator.id
            """)
    List<UserCount> countReceivedByUser(@Param("since") LocalDateTime since);

    @Query("""
            SELECT c.user.id AS userId, COUNT(c) AS total FROM Comment c
            WHERE c.createdAt >= :since
            GROUP BY c.user.id
            """)
    List<UserCount> countMadeByUser(@Param("since") LocalDateTime since);
}
