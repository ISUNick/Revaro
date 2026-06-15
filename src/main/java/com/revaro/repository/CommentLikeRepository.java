package com.revaro.repository;

import com.revaro.entity.Comment;
import com.revaro.entity.CommentLike;
import com.revaro.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {

    Optional<CommentLike> findByUserAndComment(User user, Comment comment);

    boolean existsByUserAndComment(User user, Comment comment);

    long countByComment(Comment comment);

    @Query("SELECT COUNT(cl) FROM CommentLike cl WHERE cl.comment.user = :user")
    long countLikesReceivedByUser(@Param("user") User user);
}
