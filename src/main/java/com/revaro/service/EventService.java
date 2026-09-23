package com.revaro.service;

import com.revaro.dto.EventDto;
import com.revaro.entity.Event;
import com.revaro.entity.Tag;
import com.revaro.entity.User;
import com.revaro.enums.EventStatus;
import com.revaro.enums.RsvpStatus;
import com.revaro.exception.NotFoundException;
import com.revaro.repository.EventRepository;
import com.revaro.repository.RsvpRepository;
import com.revaro.repository.TagRepository;
import com.revaro.util.FileUploadUtil;
import com.revaro.util.GeocodingUtil;
import com.revaro.util.ProfanityFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

@Service
@Transactional
public class EventService {

    private static final int PAGE_SIZE = 12;
    private static final int MAX_SERIES_SIZE = 52;

    private final EventRepository eventRepository;
    private final RsvpRepository rsvpRepository;
    private final TagRepository tagRepository;
    private final ReportService reportService;
    private final NotificationService notificationService;
    private final FileUploadUtil fileUploadUtil;
    private final GeocodingUtil geocodingUtil;
    private final ProfanityFilter profanityFilter;

    public EventService(EventRepository eventRepository,
                        RsvpRepository rsvpRepository,
                        TagRepository tagRepository,
                        ReportService reportService,
                        NotificationService notificationService,
                        FileUploadUtil fileUploadUtil,
                        GeocodingUtil geocodingUtil,
                        ProfanityFilter profanityFilter) {
        this.eventRepository = eventRepository;
        this.rsvpRepository = rsvpRepository;
        this.tagRepository = tagRepository;
        this.reportService = reportService;
        this.notificationService = notificationService;
        this.fileUploadUtil = fileUploadUtil;
        this.geocodingUtil = geocodingUtil;
        this.profanityFilter = profanityFilter;
    }

    // Creates one event per selected date (the main date plus anything picked on the calendar)
    // and returns the earliest one
    public Event createEvent(EventDto dto, User creator) throws IOException {
        LocalDateTime mainDate = dto.getEventDateTime();
        Set<LocalDateTime> dates = new TreeSet<>(parseDates(dto.getSpecificDates(), mainDate.toLocalTime()));
        dates.add(mainDate);
        checkSeriesSize(dates.size());

        String imageUrl = uploadImage(dto.getImageFile());
        Set<Tag> tags = findTags(dto.getTagIds());
        double[] coords = geocodingUtil.geocode(dto.getCity(), dto.getState());
        boolean flagged = profanityFilter.containsProfanity(dto.getTitle())
                || profanityFilter.containsProfanity(dto.getDescription());

        Event first = null;
        for (LocalDateTime date : dates) {
            Event event = new Event();
            event.setCreator(creator);
            event.setFeaturedImage(imageUrl);
            event.setRecurring(dates.size() > 1);
            applyDto(event, dto, tags, coords);
            event.setEventDateTime(date);

            Event saved = eventRepository.save(event);
            if (flagged) {
                reportService.flagEvent(saved, creator);
            }
            if (first == null) {
                first = saved;
            }
        }
        return first;
    }

    public Event updateEvent(Long id, EventDto dto, User user) throws IOException {
        Event event = getById(id);
        checkCanEdit(event, user);
        EventStatus oldStatus = event.getStatus();
        String oldImage = event.getFeaturedImage();
        String newImage = uploadImage(dto.getImageFile());

        applyDto(event, dto, findTags(dto.getTagIds()), geocodingUtil.geocode(dto.getCity(), dto.getState()));
        if (newImage != null) {
            event.setFeaturedImage(newImage);
        }
        Event saved = eventRepository.save(event);

        if (newImage != null) {
            deleteImageIfUnused(oldImage);
        }
        if (saved.getStatus() != oldStatus) {
            notificationService.notifyStatusChange(saved);
        }
        return saved;
    }

    // Dates picked on the calendar while editing become copies of this event.
    // Returns how many were added.
    public int addDates(Long id, List<String> selectedDates, User user) {
        Event original = getById(id);
        checkCanEdit(original, user);

        Set<LocalDateTime> dates = new TreeSet<>(parseDates(selectedDates, original.getEventDateTime().toLocalTime()));
        dates.remove(original.getEventDateTime());
        if (dates.isEmpty()) {
            return 0;
        }
        checkSeriesSize(dates.size() + 1);

        original.setRecurring(true);
        for (LocalDateTime date : dates) {
            eventRepository.save(copyOf(original, date));
        }
        return dates.size();
    }

    public void deleteEvent(Long id, User user) {
        Event event = getById(id);
        checkCanEdit(event, user);
        String image = event.getFeaturedImage();
        eventRepository.delete(event);
        deleteImageIfUnused(image);
    }

    @Transactional(readOnly = true)
    public Event getById(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Event not found"));
        return withRsvpCounts(event);
    }

    @Transactional(readOnly = true)
    public List<Event> getEventsByCreator(User creator) {
        List<Event> events = eventRepository.findByCreatorOrderByCreatedAtDesc(creator);
        events.forEach(this::withRsvpCounts);
        return events;
    }

    @Transactional(readOnly = true)
    public Page<Event> findEvents(String query, String searchIn, String sort, int page) {
        if (query == null || query.isBlank()) {
            Sort order = "newest".equals(sort)
                    ? Sort.by("createdAt").descending()
                    : Sort.by("eventDateTime").ascending();
            Pageable pageable = PageRequest.of(Math.max(page, 0), PAGE_SIZE, order);
            return eventRepository.findUpcoming(LocalDateTime.now(), pageable).map(this::withRsvpCounts);
        }

        // The search queries sort themselves in SQL, so the page request stays unsorted
        String q = query.trim();
        Pageable pageable = PageRequest.of(Math.max(page, 0), PAGE_SIZE);
        Page<Event> results = switch (searchIn == null ? "" : searchIn) {
            case "title" -> eventRepository.searchTitle(q, pageable);
            case "organizer" -> eventRepository.searchOrganizer(q, pageable);
            case "location" -> eventRepository.searchLocation(q, pageable);
            case "tags" -> eventRepository.searchTags(q, pageable);
            default -> eventRepository.search(q, pageable);
        };
        return results.map(this::withRsvpCounts);
    }

    public boolean canEdit(Event event, User user) {
        return user != null && (user.isAdmin() || event.getCreator().getId().equals(user.getId()));
    }

    public EventDto toDto(Event event) {
        EventDto dto = new EventDto();
        dto.setTitle(event.getTitle());
        dto.setDescription(event.getDescription());
        dto.setEventType(event.getEventType());
        dto.setEventDateTime(event.getEventDateTime());
        dto.setAddress(event.getAddress());
        dto.setCity(event.getCity());
        dto.setState(event.getState());
        dto.setPostedByOrganizer(event.isPostedByOrganizer());
        dto.setOrganizerName(event.getOrganizerName());
        dto.setOfficialSourceLink(event.getOfficialSourceLink());
        dto.setSourceType(event.getSourceType());
        dto.setStatus(event.getStatus());
        dto.setExistingImage(event.getFeaturedImage());
        dto.setTagIds(event.getTags().stream().map(Tag::getId).toList());
        return dto;
    }

    private void checkCanEdit(Event event, User user) {
        if (!canEdit(event, user)) {
            throw new AccessDeniedException("You don't have permission to change this event.");
        }
    }

    private void checkSeriesSize(int size) {
        if (size > MAX_SERIES_SIZE) {
            throw new IllegalArgumentException("A series can have at most " + MAX_SERIES_SIZE + " dates.");
        }
    }

    private void applyDto(Event event, EventDto dto, Set<Tag> tags, double[] coords) {
        event.setTitle(profanityFilter.filter(dto.getTitle()));
        event.setDescription(profanityFilter.filter(dto.getDescription()));
        event.setEventType(dto.getEventType());
        event.setEventDateTime(dto.getEventDateTime());
        event.setAddress(dto.getAddress());
        event.setCity(dto.getCity());
        event.setState(dto.getState());
        event.setPostedByOrganizer(dto.isPostedByOrganizer());
        event.setOrganizerName(dto.isPostedByOrganizer() ? event.getCreator().getUsername() : dto.getOrganizerName());
        event.setOfficialSourceLink(dto.getOfficialSourceLink());
        event.setSourceType(dto.getSourceType());
        event.setTags(tags);
        if (dto.getStatus() != null) {
            event.setStatus(dto.getStatus());
        }
        if (coords != null) {
            event.setLatitude(coords[0]);
            event.setLongitude(coords[1]);
        }
    }

    private Event copyOf(Event source, LocalDateTime date) {
        Event copy = new Event();
        copy.setCreator(source.getCreator());
        copy.setTitle(source.getTitle());
        copy.setDescription(source.getDescription());
        copy.setEventType(source.getEventType());
        copy.setEventDateTime(date);
        copy.setAddress(source.getAddress());
        copy.setCity(source.getCity());
        copy.setState(source.getState());
        copy.setLatitude(source.getLatitude());
        copy.setLongitude(source.getLongitude());
        copy.setPostedByOrganizer(source.isPostedByOrganizer());
        copy.setOrganizerName(source.getOrganizerName());
        copy.setOfficialSourceLink(source.getOfficialSourceLink());
        copy.setSourceType(source.getSourceType());
        copy.setFeaturedImage(source.getFeaturedImage());
        copy.setTags(new HashSet<>(source.getTags()));
        copy.setRecurring(true);
        return copy;
    }

    private List<LocalDateTime> parseDates(List<String> values, LocalTime time) {
        List<LocalDateTime> dates = new ArrayList<>();
        for (String value : values) {
            try {
                dates.add(LocalDate.parse(value).atTime(time));
            } catch (DateTimeParseException e) {
                // skip anything that isn't a yyyy-MM-dd date
            }
        }
        return dates;
    }

    private Set<Tag> findTags(List<Long> tagIds) {
        return tagIds.isEmpty() ? new HashSet<>() : new HashSet<>(tagRepository.findAllById(tagIds));
    }

    private String uploadImage(MultipartFile file) throws IOException {
        return file == null || file.isEmpty() ? null : fileUploadUtil.saveImage(file);
    }

    // Every event in a series shares one uploaded image, so it only gets deleted
    // from Cloudinary once no event points at it anymore
    private void deleteImageIfUnused(String imageUrl) {
        if (imageUrl != null && eventRepository.countByFeaturedImage(imageUrl) == 0) {
            fileUploadUtil.deleteImage(imageUrl);
        }
    }

    private Event withRsvpCounts(Event event) {
        event.setGoingCount(rsvpRepository.countByEventAndStatus(event, RsvpStatus.GOING));
        event.setInterestedCount(rsvpRepository.countByEventAndStatus(event, RsvpStatus.INTERESTED));
        return event;
    }
}
