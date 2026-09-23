package com.revaro.util;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

@Component
public class FileUploadUtil {

    private static final Logger log = LoggerFactory.getLogger(FileUploadUtil.class);
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp", "image/gif");
    private static final long MAX_SIZE_BYTES = 10 * 1024 * 1024;

    private final Cloudinary cloudinary;

    public FileUploadUtil(@Value("${cloudinary.cloud-name}") String cloudName,
                          @Value("${cloudinary.api-key}") String apiKey,
                          @Value("${cloudinary.api-secret}") String apiSecret) {
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true));
    }

    // Uploads to Cloudinary and returns the image URL
    public String saveImage(MultipartFile file) throws IOException {
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Images need to be JPEG, PNG, WebP, or GIF.");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new IllegalArgumentException("Images can be up to 10 MB.");
        }
        Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(),
                ObjectUtils.asMap("folder", "revaro", "resource_type", "image"));
        return (String) result.get("secure_url");
    }

    public void deleteImage(String imageUrl) {
        String publicId = publicIdOf(imageUrl);
        if (publicId == null) {
            return;
        }
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (IOException | RuntimeException e) {
            // Cleanup is best effort, a leftover image shouldn't fail the request
            log.warn("Could not delete Cloudinary image {}", imageUrl, e);
        }
    }

    // https://res.cloudinary.com/<cloud>/image/upload/v123/revaro/abc.jpg -> revaro/abc
    private String publicIdOf(String url) {
        if (url == null) {
            return null;
        }
        int start = url.indexOf("/upload/");
        if (start == -1) {
            return null;
        }
        String path = url.substring(start + "/upload/".length());
        if (path.matches("v\\d+/.*")) {
            path = path.substring(path.indexOf('/') + 1);
        }
        int dot = path.lastIndexOf('.');
        return dot == -1 ? path : path.substring(0, dot);
    }
}
