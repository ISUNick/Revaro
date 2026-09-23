package com.revaro.controller;

import com.revaro.entity.Event;
import com.revaro.repository.EventRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
public class SeoController {

    private static final int MAX_SITEMAP_EVENTS = 500;
    private static final List<String> STATIC_PAGES = List.of("/", "/leaderboard", "/about", "/contact");

    private final EventRepository eventRepository;
    private final String baseUrl;

    public SeoController(EventRepository eventRepository, @Value("${app.base-url}") String baseUrl) {
        this.eventRepository = eventRepository;
        this.baseUrl = baseUrl;
    }

    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    public String robots() {
        return """
                User-agent: *
                Allow: /
                Disallow: /admin
                Disallow: /profile/edit
                Disallow: /auth/reset-password
                Sitemap: %s/sitemap.xml
                """.formatted(baseUrl);
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public String sitemap() {
        List<Event> events = eventRepository.findUpcoming(LocalDateTime.now(),
                PageRequest.of(0, MAX_SITEMAP_EVENTS, Sort.by("eventDateTime"))).getContent();

        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        for (String page : STATIC_PAGES) {
            xml.append("  <url><loc>").append(baseUrl).append(page).append("</loc></url>\n");
        }
        for (Event event : events) {
            xml.append("  <url><loc>").append(baseUrl).append("/events/").append(event.getId()).append("</loc>");
            if (event.getUpdatedAt() != null) {
                xml.append("<lastmod>")
                        .append(event.getUpdatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE))
                        .append("</lastmod>");
            }
            xml.append("</url>\n");
        }
        return xml.append("</urlset>").toString();
    }
}
