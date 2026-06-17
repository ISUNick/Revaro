package com.revaro.controller;

import com.revaro.entity.Event;
import com.revaro.repository.EventRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
public class SeoController {

    private final EventRepository eventRepository;
    private static final String BASE_URL = "https://revaromeet.com";
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public SeoController(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public String robots() {
        return """
                User-agent: *
                Allow: /
                Disallow: /admin
                Disallow: /profile/edit
                Disallow: /auth/reset-password
                
                Sitemap: %s/sitemap.xml
                """.formatted(BASE_URL);
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    @ResponseBody
    public String sitemap() {
        List<Event> events = eventRepository.findUpcomingEvents(
                LocalDateTime.now(),
                PageRequest.of(0, 500, Sort.by("eventDateTime").ascending())
        ).getContent();

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        // Static pages
        for (String path : List.of("/", "/about", "/contact")) {
            xml.append("  <url><loc>").append(BASE_URL).append(path).append("</loc></url>\n");
        }

        // Event pages
        for (Event event : events) {
            xml.append("  <url>\n");
            xml.append("    <loc>").append(BASE_URL).append("/events/").append(event.getId()).append("</loc>\n");
            if (event.getUpdatedAt() != null) {
                xml.append("    <lastmod>").append(event.getUpdatedAt().format(ISO)).append("</lastmod>\n");
            }
            xml.append("    <changefreq>weekly</changefreq>\n");
            xml.append("    <priority>0.8</priority>\n");
            xml.append("  </url>\n");
        }

        xml.append("</urlset>");
        return xml.toString();
    }
}
