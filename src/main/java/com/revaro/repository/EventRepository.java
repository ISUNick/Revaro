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

    // ── Default feed ──────────────────────────────────────────────────────────

    @Query("SELECT e FROM Event e WHERE e.eventDateTime >= :now")
    Page<Event> findUpcomingEvents(@Param("now") LocalDateTime now, Pageable pageable);

    // ── Fuzzy full-text search using pg_trgm ──────────────────────────────────
    // Searches title, organizer, city, state, tags using trigram similarity
    // so typos and punctuation differences still return results.
    // Threshold: similarity > 0.1 (loose enough to catch most typos)

    @Query(value = """
            SELECT DISTINCT e.* FROM events e
            LEFT JOIN event_tags et ON et.event_id = e.id
            LEFT JOIN tags t ON t.id = et.tag_id
            WHERE (
                e.title % :q
                OR e.organizer_name % :q
                OR e.city % :q
                OR e.state % :q
                OR t.name % :q
                OR e.title ILIKE '%' || :q || '%'
                OR e.organizer_name ILIKE '%' || :q || '%'
                OR e.city ILIKE '%' || :q || '%'
                OR t.name ILIKE '%' || :q || '%'
            )
            """,
            countQuery = """
            SELECT COUNT(DISTINCT e.id) FROM events e
            LEFT JOIN event_tags et ON et.event_id = e.id
            LEFT JOIN tags t ON t.id = et.tag_id
            WHERE (
                e.title % :q OR e.organizer_name % :q OR e.city % :q
                OR e.state % :q OR t.name % :q
                OR e.title ILIKE '%' || :q || '%'
                OR e.organizer_name ILIKE '%' || :q || '%'
                OR e.city ILIKE '%' || :q || '%'
                OR t.name ILIKE '%' || :q || '%'
            )
            """,
            nativeQuery = true)
    Page<Event> fuzzySearchEvents(@Param("q") String query, Pageable pageable);

    // ── Fuzzy search WITH filters ─────────────────────────────────────────────

    @Query(value = """
            SELECT DISTINCT e.* FROM events e
            LEFT JOIN event_tags et ON et.event_id = e.id
            LEFT JOIN tags t ON t.id = et.tag_id
            WHERE (
                e.title % :q OR e.organizer_name % :q OR e.city % :q
                OR e.state % :q OR t.name % :q
                OR e.title ILIKE '%' || :q || '%'
                OR e.organizer_name ILIKE '%' || :q || '%'
                OR e.city ILIKE '%' || :q || '%'
                OR t.name ILIKE '%' || :q || '%'
            )
            AND (:type IS NULL OR e.event_type = :type)
            AND (:state IS NULL OR LOWER(e.state) = LOWER(:state))
            AND (:tag IS NULL OR LOWER(t.name) = LOWER(:tag))
            AND (:organizer IS NULL OR LOWER(e.organizer_name) ILIKE '%' || LOWER(:organizer) || '%')
            """,
            countQuery = """
            SELECT COUNT(DISTINCT e.id) FROM events e
            LEFT JOIN event_tags et ON et.event_id = e.id
            LEFT JOIN tags t ON t.id = et.tag_id
            WHERE (
                e.title % :q OR e.organizer_name % :q OR e.city % :q
                OR e.state % :q OR t.name % :q
                OR e.title ILIKE '%' || :q || '%'
                OR e.organizer_name ILIKE '%' || :q || '%'
                OR e.city ILIKE '%' || :q || '%'
                OR t.name ILIKE '%' || :q || '%'
            )
            AND (:type IS NULL OR e.event_type = :type)
            AND (:state IS NULL OR LOWER(e.state) = LOWER(:state))
            AND (:tag IS NULL OR LOWER(t.name) = LOWER(:tag))
            AND (:organizer IS NULL OR LOWER(e.organizer_name) ILIKE '%' || LOWER(:organizer) || '%')
            """,
            nativeQuery = true)
    Page<Event> fuzzySearchEventsFiltered(@Param("q") String query,
                                           @Param("type") String type,
                                           @Param("state") String state,
                                           @Param("tag") String tag,
                                           @Param("organizer") String organizer,
                                           Pageable pageable);

    // ── Filtered feed (no search query) ──────────────────────────────────────

    @Query(value = """
            SELECT DISTINCT e.* FROM events e
            LEFT JOIN event_tags et ON et.event_id = e.id
            LEFT JOIN tags t ON t.id = et.tag_id
            WHERE e.event_date_time >= :now
            AND (:type IS NULL OR e.event_type = :type)
            AND (:state IS NULL OR LOWER(e.state) = LOWER(:state))
            AND (:tag IS NULL OR LOWER(t.name) = LOWER(:tag))
            AND (:organizer IS NULL OR LOWER(e.organizer_name) ILIKE '%' || LOWER(:organizer) || '%')
            """,
            countQuery = """
            SELECT COUNT(DISTINCT e.id) FROM events e
            LEFT JOIN event_tags et ON et.event_id = e.id
            LEFT JOIN tags t ON t.id = et.tag_id
            WHERE e.event_date_time >= :now
            AND (:type IS NULL OR e.event_type = :type)
            AND (:state IS NULL OR LOWER(e.state) = LOWER(:state))
            AND (:tag IS NULL OR LOWER(t.name) = LOWER(:tag))
            AND (:organizer IS NULL OR LOWER(e.organizer_name) ILIKE '%' || LOWER(:organizer) || '%')
            """,
            nativeQuery = true)
    Page<Event> findUpcomingFiltered(@Param("now") LocalDateTime now,
                                      @Param("type") String type,
                                      @Param("state") String state,
                                      @Param("tag") String tag,
                                      @Param("organizer") String organizer,
                                      Pageable pageable);

    // ── Targeted field searches (for filter chips) ───────────────────────────

    // Search only in tag names
    @Query(value = """
            SELECT DISTINCT e.* FROM events e
            JOIN event_tags et ON et.event_id = e.id
            JOIN tags t ON t.id = et.tag_id
            WHERE (t.name % :q OR t.name ILIKE '%' || :q || '%')
            """,
            countQuery = """
            SELECT COUNT(DISTINCT e.id) FROM events e
            JOIN event_tags et ON et.event_id = e.id
            JOIN tags t ON t.id = et.tag_id
            WHERE (t.name % :q OR t.name ILIKE '%' || :q || '%')
            """,
            nativeQuery = true)
    Page<Event> searchByTagOnly(@Param("q") String query, Pageable pageable);

    // Search only organizer name
    @Query(value = """
            SELECT e.* FROM events e
            WHERE (e.organizer_name % :q OR e.organizer_name ILIKE '%' || :q || '%')
            """,
            countQuery = """
            SELECT COUNT(e.id) FROM events e
            WHERE (e.organizer_name % :q OR e.organizer_name ILIKE '%' || :q || '%')
            """,
            nativeQuery = true)
    Page<Event> searchByOrganizerOnly(@Param("q") String query, Pageable pageable);

    // Search only city/state
    @Query(value = """
            SELECT e.* FROM events e
            WHERE (e.city % :q OR e.city ILIKE '%' || :q || '%'
                OR e.state % :q OR e.state ILIKE '%' || :q || '%')
            """,
            countQuery = """
            SELECT COUNT(e.id) FROM events e
            WHERE (e.city % :q OR e.city ILIKE '%' || :q || '%'
                OR e.state % :q OR e.state ILIKE '%' || :q || '%')
            """,
            nativeQuery = true)
    Page<Event> searchByLocationOnly(@Param("q") String query, Pageable pageable);

    // Search only event title
    @Query(value = """
            SELECT e.* FROM events e
            WHERE (e.title % :q OR e.title ILIKE '%' || :q || '%')
            """,
            countQuery = """
            SELECT COUNT(e.id) FROM events e
            WHERE (e.title % :q OR e.title ILIKE '%' || :q || '%')
            """,
            nativeQuery = true)
    Page<Event> searchByTitleOnly(@Param("q") String query, Pageable pageable);

    // ── Admin / profile ───────────────────────────────────────────────────────

    Page<Event> findAllByOrderByCreatedAtDesc(Pageable pageable);
    long countByStatus(EventStatus status);
    long countByCreator(User creator);

    @Query("SELECT e FROM Event e WHERE e.status = :status AND e.eventDateTime >= :now ORDER BY e.eventDateTime ASC")
    Page<Event> findUpcomingByStatus(@Param("status") EventStatus status,
                                     @Param("now") LocalDateTime now,
                                     Pageable pageable);

    // Keep old JPQL methods for backwards compatibility with admin/profile pages
    @Query("SELECT e FROM Event e WHERE e.eventType = :type AND e.eventDateTime >= :now")
    Page<Event> findByEventType(@Param("type") EventType type,
                                @Param("now") LocalDateTime now,
                                Pageable pageable);

    @Query("SELECT e FROM Event e WHERE LOWER(COALESCE(e.state,'')) = LOWER(:state) AND e.eventDateTime >= :now")
    Page<Event> findByState(@Param("state") String state,
                            @Param("now") LocalDateTime now,
                            Pageable pageable);

    @Query("""
            SELECT DISTINCT e FROM Event e
            JOIN e.tags t
            WHERE e.eventDateTime >= :now
            AND LOWER(t.name) = LOWER(:tagName)
            """)
    Page<Event> findByTagName(@Param("tagName") String tagName,
                              @Param("now") LocalDateTime now,
                              Pageable pageable);
}
