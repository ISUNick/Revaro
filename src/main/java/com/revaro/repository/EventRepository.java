package com.revaro.repository;

import com.revaro.entity.Event;
import com.revaro.entity.User;
import com.revaro.enums.EventStatus;
import com.revaro.enums.EventType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findByCreator(User creator);
    List<Event> findByCreatorOrderByCreatedAtDesc(User creator);
    Page<Event> findByStatus(EventStatus status, Pageable pageable);

    // ── Homepage feed — all statuses, no date filter ──────────────────────────

    @Query("SELECT e FROM Event e")
    Page<Event> findUpcomingEventsAllStatuses(Pageable pageable);

    @Query("""
            SELECT e FROM Event e
            WHERE (
                LOWER(e.title) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(e.organizerName,'')) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(e.city,'')) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(e.state,'')) LIKE LOWER(CONCAT('%', :q, '%'))
            )
            """)
    Page<Event> searchEvents(@Param("q") String query, Pageable pageable);

    @Query("SELECT e FROM Event e WHERE e.eventType = :type")
    Page<Event> findByEventType(@Param("type") EventType type, Pageable pageable);

    @Query("""
            SELECT e FROM Event e
            WHERE e.eventType = :type
            AND (
                LOWER(e.title) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(e.organizerName,'')) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(e.city,'')) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(e.state,'')) LIKE LOWER(CONCAT('%', :q, '%'))
            )
            """)
    Page<Event> searchEventsByType(@Param("q") String query,
                                   @Param("type") EventType type,
                                   Pageable pageable);

    @Query("SELECT e FROM Event e WHERE LOWER(COALESCE(e.state,'')) = LOWER(:state)")
    Page<Event> findByState(@Param("state") String state, Pageable pageable);

    // ── Keep for backward compat ───────────────────────────────────────────────
    @Query("SELECT e FROM Event e WHERE e.status = :status AND e.eventDateTime >= :now ORDER BY e.eventDateTime ASC")
    Page<Event> findUpcomingEvents(@Param("status") EventStatus status,
                                   @Param("now") LocalDateTime now,
                                   Pageable pageable);

    // ── Admin ──────────────────────────────────────────────────────────────────
    Page<Event> findAllByOrderByCreatedAtDesc(Pageable pageable);
    long countByStatus(EventStatus status);
    long countByCreator(User creator);
}
