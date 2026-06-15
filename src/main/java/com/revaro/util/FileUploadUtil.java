package com.revaro.util;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * Handles uploading and deleting images via Cloudinary.
 * Images are stored in the cloud — no local filesystem dependency.
 */
@Component
public class FileUploadUtil {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp", "image/gif"
    );

    private static final long MAX_SIZE_BYTES = 10 * 1024 * 1024; // 10 MB

    private final Cloudinary cloudinary;

    public FileUploadUtil(
            @Value("${cloudinary.cloud-name}") String cloudName,
            @Value("${cloudinary.api-key}") String apiKey,
            @Value("${cloudinary.api-secret}") String apiSecret) {
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true
        ));
    }

    /**
     * Uploads an image to Cloudinary and returns the secure URL.
     */
    public String saveImage(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("No file provided.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Invalid file type. Allowed: JPEG, PNG, WebP, GIF.");
        }

        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new IllegalArgumentException("File too large. Maximum size is 10 MB.");
        }

        Map uploadResult = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                        "folder", "revaro",
                        "resource_type", "image"
                )
        );

        return (String) uploadResult.get("secure_url");
    }

    /**
     * Deletes an image from Cloudinary by its URL.
     */
    public void deleteImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) return;
        try {
            // Extract public_id from URL
            // URL format: https://res.cloudinary.com/cloud/image/upload/v123/revaro/filename.jpg
            String publicId = extractPublicId(imageUrl);
            if (publicId != null) {
                cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            }
        } catch (Exception e) {
            System.err.println("Warning: could not delete Cloudinary image: " + imageUrl);
        }
    }

    private String extractPublicId(String url) {
        try {
            // Find "/upload/" and take everything after it, strip version if present
            int uploadIdx = url.indexOf("/upload/");
            if (uploadIdx == -1) return null;
            String after = url.substring(uploadIdx + 8); // skip "/upload/"
            // Remove version segment like "v1234567890/"
            if (after.matches("v\\d+/.*")) {
                after = after.substring(after.indexOf('/') + 1);
            }
            // Remove file extension
            int dotIdx = after.lastIndexOf('.');
            if (dotIdx != -1) after = after.substring(0, dotIdx);
            return after;
        } catch (Exception e) {
            return null;
        }
    }
}
