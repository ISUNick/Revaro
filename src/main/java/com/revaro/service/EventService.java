package com.revaro.service;

import com.revaro.dto.EventDto;
import java.util.List;
import com.revaro.entity.Event;
import com.revaro.entity.User;
import com.revaro.enums.EventStatus;
import com.revaro.enums.EventType;
import com.revaro.enums.RsvpStatus;
import com.revaro.repository.EventRepository;
import com.revaro.repository.RsvpRepository;
import com.revaro.util.FileUploadUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Transactional
public class EventService {

    private static final int PAGE_SIZE = 10;

    private final EventRepository eventRepository;
    private final RsvpRepository rsvpRepository;
    private final FileUploadUtil fileUploadUtil;

    public EventService(EventRepository eventRepository,
                        RsvpRepository rsvpRepository,
                        FileUploadUtil fileUploadUtil) {
        this.eventRepository = eventRepository;
        this.rsvpRepository = rsvpRepository;
        this.fileUploadUtil = fileUploadUtil;
    }

    // ── Create ────────────────────────────────────────────────────────────────

    public Event createEvent(EventDto dto, User creator) throws IOException {
        Event event = new Event();
        applyDto(event, dto);
        event.setCreator(creator);
        event.setStatus(EventStatus.ACTIVE);

        if (dto.getImageFile() != null && !dto.getImageFile().isEmpty()) {
            String filename = fileUploadUtil.saveImage(dto.getImageFile());
            event.setFeaturedImage(filename);
        }

        if (dto.isPostedByOrganizer()) {
            event.setOrganizerName(creator.getUsername());
        } else {
            event.setOrganizerName(dto.getOrganizerName());
        }

        return eventRepository.save(event);
    }

    // ── Update ────────────────────────────────────────────────────────────────

    public Event updateEvent(Long eventId, EventDto dto, User requestingUser) throws IOException {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found."));

        if (!event.getCreator().getId().equals(requestingUser.getId())
                && !requestingUser.isAdmin()) {
            throw new SecurityException("You do not have permission to edit this event.");
        }

        applyDto(event, dto);

        if (dto.isPostedByOrganizer()) {
            event.setOrganizerName(event.getCreator().getUsername());
        } else {
            event.setOrganizerName(dto.getOrganizerName());
        }

        MultipartFile newImage = dto.getImageFile();
        if (newImage != null && !newImage.isEmpty()) {
            if (event.getFeaturedImage() != null) {
                fileUploadUtil.deleteImage(event.getFeaturedImage());
            }
            String filename = fileUploadUtil.saveImage(newImage);
            event.setFeaturedImage(filename);
        }

        return eventRepository.save(event);
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    public void deleteEvent(Long eventId, User requestingUser) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found."));

        if (!event.getCreator().getId().equals(requestingUser.getId())
                && !requestingUser.isAdmin()) {
            throw new SecurityException("You do not have permission to delete this event.");
        }

        if (event.getFeaturedImage() != null) {
            fileUploadUtil.deleteImage(event.getFeaturedImage());
        }

        eventRepository.delete(event);
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Optional<Event> findById(Long id) {
        return eventRepository.findById(id).map(this::hydrateEvent);
    }

    /**
     * Public method to hydrate a list of events with RSVP counts.
     */
    @Transactional(readOnly = true)
    public List<Event> hydrateEvents(List<Event> events) {
        events.forEach(this::hydrateEvent);
        return events;
    }

    /**
     * Load and hydrate events for a user in a single transaction.
     */
    @Transactional(readOnly = true)
    public List<Event> findByCreatorHydrated(com.revaro.entity.User user) {
        List<Event> events = eventRepository.findByCreatorOrderByCreatedAtDesc(user);
        events.forEach(this::hydrateEvent);
        return events;
    }

    /**
     * Hydrate transient count fields while still inside the transaction.
     * This avoids LazyInitializationException after the session closes.
     */
    private Event hydrateEvent(Event event) {
        long going = rsvpRepository.countByEventAndStatus(event, RsvpStatus.GOING);
        long interested = rsvpRepository.countByEventAndStatus(event, RsvpStatus.INTERESTED);
        event.setGoingCountCache(going);
        event.setInterestedCountCache(interested);
        return event;
    }

    @Transactional(readOnly = true)
    public Page<Event> findEvents(String query, String state, String type, String sort, int page) {
        LocalDateTime now = LocalDateTime.now();
        Pageable pageable = buildPageable(sort, page);

        boolean hasQuery = query != null && !query.isBlank();
        boolean hasType  = type  != null && !type.isBlank();
        boolean hasState = state != null && !state.isBlank();

        Page<Event> events;

        if (hasType) {
            EventType eventType;
            try {
                eventType = EventType.valueOf(type);
            } catch (IllegalArgumentException e) {
                events = eventRepository.findUpcomingEventsAllStatuses(now, pageable);
                return events.map(this::hydrateEvent);
            }
            if (hasQuery) {
                events = eventRepository.searchEventsByType(query, eventType, now, pageable);
            } else {
                events = eventRepository.findByEventType(eventType, now, pageable);
            }
        } else if (hasState && !hasQuery) {
            events = eventRepository.findByState(state, now, pageable);
        } else if (hasQuery) {
            events = eventRepository.searchEvents(query, now, pageable);
        } else {
            events = eventRepository.findUpcomingEventsAllStatuses(now, pageable);
        }

        return events.map(this::hydrateEvent);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void applyDto(Event event, EventDto dto) {
        event.setTitle(dto.getTitle());
        event.setDescription(dto.getDescription());
        event.setEventType(dto.getEventType());
        event.setEventDateTime(dto.getEventDateTime());
        event.setCity(dto.getCity());
        event.setState(dto.getState());
        event.setAddress(dto.getAddress());
        event.setPostedByOrganizer(dto.isPostedByOrganizer());
        event.setOfficialSourceLink(dto.getOfficialSourceLink());
        event.setSourceType(dto.getSourceType());
        if (dto.getStatus() != null) {
            event.setStatus(dto.getStatus());
        }
        // Geocode city+state to lat/lng for distance sorting
        if (dto.getCity() != null && !dto.getCity().isBlank()) {
            geocodeLocation(event, dto.getCity(), dto.getState());
        }
    }

    private void geocodeLocation(Event event, String city, String state) {
        try {
            String query = city + (state != null ? ", " + state : "") + ", USA";
            String encoded = java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8);
            String url = "https://nominatim.openstreetmap.org/search?q=" + encoded + "&format=json&limit=1";
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection)
                    new java.net.URL(url).openConnection();
            conn.setRequestProperty("User-Agent", "Revaro/1.0");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            if (conn.getResponseCode() == 200) {
                String body = new String(conn.getInputStream().readAllBytes());
                if (body.contains(""lat"")) {
                    double lat = Double.parseDouble(body.split(""lat":"")[1].split(""")[0]);
                    double lon = Double.parseDouble(body.split(""lon":"")[1].split(""")[0]);
                    event.setLatitude(lat);
                    event.setLongitude(lon);
                }
            }
        } catch (Exception e) {
            System.err.println("Geocoding failed for " + city + ": " + e.getMessage());
        }
    }

    private Pageable buildPageable(String sort, int page) {
        // For "date" sort we want upcoming first (ascending by date),
        // for "newest" we want most recently created first.
        // Past events will naturally appear at the end with ascending date sort.
        Sort sortOrder = switch (sort != null ? sort : "date") {
            case "newest"  -> Sort.by("createdAt").descending();
            default        -> Sort.by("eventDateTime").ascending();
        };
        return PageRequest.of(Math.max(0, page), PAGE_SIZE, sortOrder);
    }

    public EventDto toDto(Event event) {
        EventDto dto = new EventDto();
        dto.setTitle(event.getTitle());
        dto.setDescription(event.getDescription());
        dto.setEventType(event.getEventType());
        dto.setEventDateTime(event.getEventDateTime());
        dto.setCity(event.getCity());
        dto.setState(event.getState());
        dto.setAddress(event.getAddress());
        dto.setPostedByOrganizer(event.isPostedByOrganizer());
        dto.setOrganizerName(event.getOrganizerName());
        dto.setOfficialSourceLink(event.getOfficialSourceLink());
        dto.setSourceType(event.getSourceType());
        dto.setStatus(event.getStatus());
        dto.setExistingImage(event.getFeaturedImage());
        return dto;
    }
}
