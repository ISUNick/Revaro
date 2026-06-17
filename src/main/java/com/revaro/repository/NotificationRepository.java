package com.revaro.repository;

import com.revaro.entity.Notification;
import com.revaro.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // Unread count for navbar badge
    long countByRecipientAndIsReadFalse(User recipient);

    // Recent unread for dropdown preview (max 10)
    List<Notification> findTop10ByRecipientAndIsReadFalseOrderByCreatedAtDesc(User recipient);

    // All notifications for the /notifications page
    Page<Notification> findByRecipientOrderByCreatedAtDesc(User recipient, Pageable pageable);

    // Mark all as read
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.recipient = :recipient AND n.isRead = false")
    void markAllAsRead(@Param("recipient") User recipient);
}
