package com.revaro.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final URI RESEND_URL = URI.create("https://api.resend.com/emails");

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String fromAddress;
    private final String baseUrl;

    public EmailService(ObjectMapper objectMapper,
                        @Value("${resend.api-key}") String apiKey,
                        @Value("${resend.from-address}") String fromAddress,
                        @Value("${app.base-url}") String baseUrl) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.fromAddress = fromAddress;
        this.baseUrl = baseUrl;
    }

    public void sendPasswordReset(String to, String username, String token) {
        String resetUrl = baseUrl + "/auth/reset-password?token=" + token;
        String html = """
                <div style="font-family:Inter,Arial,sans-serif;max-width:520px;margin:0 auto;padding:2rem;">
                    <h2 style="font-size:1.5rem;font-weight:800;margin-bottom:0.5rem;">Reset your password</h2>
                    <p style="color:#6b7280;">Hi %s, we got a request to reset your Revaro password.</p>
                    <a href="%s" style="display:inline-block;margin:1.5rem 0;padding:0.75rem 1.5rem;background:#2a6fea;
                       color:#fff;border-radius:8px;text-decoration:none;font-weight:700;">Reset Password</a>
                    <p style="color:#6b7280;font-size:0.85rem;">
                        This link expires in 1 hour. If you didn't ask for a reset, you can ignore this email.
                    </p>
                    <p style="color:#9ca3af;font-size:0.75rem;">Revaro, <a href="%s" style="color:#9ca3af;">revaromeet.com</a></p>
                </div>
                """.formatted(username, resetUrl, baseUrl);
        send(to, "Reset your Revaro password", html);
    }

    private void send(String to, String subject, String html) {
        try {
            String body = objectMapper.writeValueAsString(Map.of(
                    "from", fromAddress,
                    "to", List.of(to),
                    "subject", subject,
                    "html", html));
            HttpRequest request = HttpRequest.newBuilder(RESEND_URL)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                log.error("Resend returned {}: {}", response.statusCode(), response.body());
            }
        } catch (IOException e) {
            log.error("Could not send email to {}", to, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while sending email to {}", to);
        }
    }
}
