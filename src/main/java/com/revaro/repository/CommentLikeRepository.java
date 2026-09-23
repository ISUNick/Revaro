package com.revaro.repository;

import com.revaro.entity.Comment;
import com.revaro.entity.CommentLike;
import com.revaro.entity.Event;
import com.revaro.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {

    Optional<CommentLike> findByUserAndComment(User user, Comment comment);

    // One query for the whole comment section instead of one per comment
    @Query("SELECT cl.comment.id FROM CommentLike cl WHERE cl.user = :user AND cl.comment.event = :event")
    List<Long> findLikedCommentIds(@Param("user") User user, @Param("event") Event event);

    @Query("""
            SELECT cl.comment.user.id AS userId, COUNT(cl) AS total FROM CommentLike cl
            WHERE cl.user <> cl.comment.user AND cl.createdAt >= :since
            GROUP BY cl.comment.user.id
            """)
    List<UserCount> countReceivedByUser(@Param("since") LocalDateTime since);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM CommentLike cl WHERE cl.user = :user")
    void deleteByUser(@Param("user") User user);
}
