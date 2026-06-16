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

    @Query("SELECT e FROM Event e WHERE e.eventDateTime >= :now")
    Page<Event> findUpcomingEvents(@Param("now") LocalDateTime now, Pageable pageable);

    @Query("""
            SELECT DISTINCT e FROM Event e
            LEFT JOIN e.tags t
            WHERE (
                LOWER(e.title) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(e.organizerName,'')) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(e.city,'')) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(e.state,'')) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(t.name,'')) LIKE LOWER(CONCAT('%', :q, '%'))
            )
            """)
    Page<Event> searchEvents(@Param("q") String query, Pageable pageable);

    @Query("""
            SELECT DISTINCT e FROM Event e
            JOIN e.tags t
            WHERE e.eventDateTime >= :now
            AND LOWER(t.name) = LOWER(:tagName)
            """)
    Page<Event> findByTagName(@Param("tagName") String tagName,
                              @Param("now") LocalDateTime now,
                              Pageable pageable);

    @Query("""
            SELECT DISTINCT e FROM Event e
            JOIN e.tags t
            WHERE LOWER(t.name) = LOWER(:tagName)
            AND (
                LOWER(e.title) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(e.organizerName,'')) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(e.city,'')) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(e.state,'')) LIKE LOWER(CONCAT('%', :q, '%'))
            )
            """)
    Page<Event> searchEventsWithTag(@Param("q") String query,
                                    @Param("tagName") String tagName,
                                    Pageable pageable);

    @Query("SELECT e FROM Event e WHERE e.eventType = :type AND e.eventDateTime >= :now")
    Page<Event> findByEventType(@Param("type") EventType type,
                                @Param("now") LocalDateTime now,
                                Pageable pageable);

    @Query("""
            SELECT e FROM Event e
            WHERE e.eventType = :type
            AND e.eventDateTime >= :now
            AND (
                LOWER(e.title) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(e.organizerName,'')) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(e.city,'')) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(e.state,'')) LIKE LOWER(CONCAT('%', :q, '%'))
            )
            """)
    Page<Event> searchEventsByType(@Param("q") String query,
                                   @Param("type") EventType type,
                                   @Param("now") LocalDateTime now,
                                   Pageable pageable);

    @Query("SELECT e FROM Event e WHERE LOWER(COALESCE(e.state,'')) = LOWER(:state) AND e.eventDateTime >= :now")
    Page<Event> findByState(@Param("state") String state,
                            @Param("now") LocalDateTime now,
                            Pageable pageable);

    Page<Event> findAllByOrderByCreatedAtDesc(Pageable pageable);
    long countByStatus(EventStatus status);
    long countByCreator(User creator);

    @Query("SELECT e FROM Event e WHERE e.status = :status AND e.eventDateTime >= :now ORDER BY e.eventDateTime ASC")
    Page<Event> findUpcomingByStatus(@Param("status") EventStatus status,
                                     @Param("now") LocalDateTime now,
                                     Pageable pageable);
}
