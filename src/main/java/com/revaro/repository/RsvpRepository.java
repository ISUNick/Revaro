package com.revaro.repository;

import com.revaro.entity.Event;
import com.revaro.entity.Rsvp;
import com.revaro.entity.User;
import com.revaro.enums.RsvpStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RsvpRepository extends JpaRepository<Rsvp, Long> {

    Optional<Rsvp> findByUserAndEvent(User user, Event event);

    List<Rsvp> findByEvent(Event event);

    long countByEventAndStatus(Event event, RsvpStatus status);

    // RSVPs on a user's events from other people
    @Query("""
            SELECT r.event.creator.id AS userId, COUNT(r) AS total FROM Rsvp r
            WHERE r.status = :status AND r.user <> r.event.creator AND r.createdAt >= :since
            GROUP BY r.event.creator.id
            """)
    List<UserCount> countReceivedByUser(@Param("status") RsvpStatus status,
                                        @Param("since") LocalDateTime since);

    @Query("""
            SELECT r.user.id AS userId, COUNT(r) AS total FROM Rsvp r
            WHERE r.status = :status AND r.user <> r.event.creator AND r.createdAt >= :since
            GROUP BY r.user.id
            """)
    List<UserCount> countMadeByUser(@Param("status") RsvpStatus status,
                                    @Param("since") LocalDateTime since);
}
