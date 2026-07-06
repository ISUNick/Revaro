package com.revaro.repository;

import com.revaro.entity.Event;
import com.revaro.entity.Rsvp;
import com.revaro.entity.User;
import com.revaro.enums.RsvpStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RsvpRepository extends JpaRepository<Rsvp, Long> {

    Optional<Rsvp> findByUserAndEvent(User user, Event event);

    long countByEventAndStatus(Event event, RsvpStatus status);

    @Query("SELECT COUNT(r) FROM Rsvp r WHERE r.event = :event AND r.status = 'GOING'")
    long countGoingByEvent(@Param("event") Event event);

    @Query("SELECT COUNT(r) FROM Rsvp r WHERE r.event.creator = :user AND r.status = 'GOING'")
    long countGoingRsvpsForUserEvents(@Param("user") User user);

    @org.springframework.data.jpa.repository.Query("""
        SELECT COUNT(r) FROM Rsvp r
        WHERE r.event.creator = :user AND r.status = 'INTERESTED'
        """)
    long countInterestedRsvpsForUserEvents(@Param("user") User user);

    @org.springframework.data.jpa.repository.Query("""
        SELECT COUNT(r) FROM Rsvp r
        WHERE r.user = :user AND r.status = 'GOING'
        """)
    long countGoingRsvpsByUser(@Param("user") User user);

    void deleteByUserAndEvent(User user, Event event);
}
