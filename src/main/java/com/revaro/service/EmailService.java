package com.revaro.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/**
 * Sends transactional emails via the Resend API.
 */
@Service
public class EmailService {

    @Value("${resend.api-key}")
    private String apiKey;

    @Value("${resend.from-address:noreply@revaromeet.com}")
    private String fromAddress;

    @Value("${app.base-url:https://revaromeet.com}")
    private String baseUrl;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    /**
     * Sends a password reset email with a secure link.
     */
    public void sendPasswordReset(String toEmail, String username, String token) {
        String resetUrl = baseUrl + "/auth/reset-password?token=" + token;

        String html = """
            <div style="font-family:Inter,sans-serif;max-width:520px;margin:0 auto;padding:2rem;">
                <h2 style="font-size:1.5rem;font-weight:800;margin-bottom:0.5rem;">Reset your password</h2>
                <p style="color:#6b7280;">Hi %s, we received a request to reset your Revaro password.</p>
                <a href="%s"
                   style="display:inline-block;margin:1.5rem 0;padding:0.75rem 1.5rem;
                          background:#2a6fea;color:#fff;border-radius:8px;text-decoration:none;
                          font-weight:700;font-size:0.95rem;">
                    Reset Password
                </a>
                <p style="color:#6b7280;font-size:0.85rem;">
                    This link expires in 1 hour. If you didn't request a password reset, you can safely ignore this email.
                </p>
                <hr style="border:none;border-top:1px solid #e5e7eb;margin:1.5rem 0;"/>
                <p style="color:#9ca3af;font-size:0.75rem;">
                    REVaro · <a href="%s" style="color:#9ca3af;">revaromeet.com</a>
                </p>
            </div>
            """.formatted(username, resetUrl, baseUrl);

        send(toEmail, "Reset your Revaro password", html);
    }

    /**
     * Core send method — calls Resend API directly (no SDK needed).
     */
    private void send(String to, String subject, String html) {
        try {
            String body = """
                {
                    "from": "%s",
                    "to": ["%s"],
                    "subject": "%s",
                    "html": %s
                }
                """.formatted(fromAddress, to, subject, jsonString(html));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.resend.com/emails"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                System.err.println("Resend API error " + response.statusCode() + ": " + response.body());
            }
        } catch (Exception e) {
            System.err.println("Failed to send email to " + to + ": " + e.getMessage());
        }
    }

    /** Escapes a string for safe embedding in a JSON string value. */
    private String jsonString(String s) {
        return "\"" + s
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                + "\"";
    }
}
