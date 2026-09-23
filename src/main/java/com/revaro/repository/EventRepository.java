package com.revaro.repository;

import com.revaro.entity.Event;
import com.revaro.entity.User;
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

    // Search uses pg_trgm. The % operator matches on trigram similarity so typos still hit,
    // and ILIKE catches plain substrings the similarity check misses.
    String SELECT = "SELECT e.* FROM events e WHERE ";
    String COUNT = "SELECT COUNT(*) FROM events e WHERE ";
    String TITLE = "(e.title % :q OR e.title ILIKE '%' || :q || '%')";
    String ORGANIZER = "(e.organizer_name % :q OR e.organizer_name ILIKE '%' || :q || '%')";
    String LOCATION = "(e.city % :q OR e.city ILIKE '%' || :q || '%' OR e.state ILIKE :q)";
    String TAGS = "EXISTS (SELECT 1 FROM event_tags et JOIN tags t ON t.id = et.tag_id "
            + "WHERE et.event_id = e.id AND (t.name % :q OR t.name ILIKE '%' || :q || '%'))";
    String ANYWHERE = TITLE + " OR " + ORGANIZER + " OR " + LOCATION + " OR " + TAGS;
    // Upcoming events first, then past ones, each sorted by how close they are to today
    String ORDER = " ORDER BY e.event_date_time < LOCALTIMESTAMP, "
            + "ABS(EXTRACT(EPOCH FROM (e.event_date_time - LOCALTIMESTAMP)))";

    @Query(value = SELECT + ANYWHERE + ORDER, countQuery = COUNT + ANYWHERE, nativeQuery = true)
    Page<Event> search(@Param("q") String query, Pageable pageable);

    @Query(value = SELECT + TITLE + ORDER, countQuery = COUNT + TITLE, nativeQuery = true)
    Page<Event> searchTitle(@Param("q") String query, Pageable pageable);

    @Query(value = SELECT + ORGANIZER + ORDER, countQuery = COUNT + ORGANIZER, nativeQuery = true)
    Page<Event> searchOrganizer(@Param("q") String query, Pageable pageable);

    @Query(value = SELECT + LOCATION + ORDER, countQuery = COUNT + LOCATION, nativeQuery = true)
    Page<Event> searchLocation(@Param("q") String query, Pageable pageable);

    @Query(value = SELECT + TAGS + ORDER, countQuery = COUNT + TAGS, nativeQuery = true)
    Page<Event> searchTags(@Param("q") String query, Pageable pageable);

    @Query("SELECT e FROM Event e WHERE e.eventDateTime >= :now")
    Page<Event> findUpcoming(@Param("now") LocalDateTime now, Pageable pageable);

    List<Event> findByCreatorOrderByCreatedAtDesc(User creator);

    Page<Event> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByFeaturedImage(String featuredImage);

    @Query("""
            SELECT e.creator.id AS userId, COUNT(e) AS total FROM Event e
            WHERE e.createdAt >= :since
            GROUP BY e.creator.id
            """)
    List<UserCount> countPostedByUser(@Param("since") LocalDateTime since);
}
