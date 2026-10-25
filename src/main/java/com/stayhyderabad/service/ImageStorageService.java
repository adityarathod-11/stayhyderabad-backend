package com.stayhyderabad.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.stayhyderabad.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ImageStorageService {

    private static final Logger log = LoggerFactory.getLogger(ImageStorageService.class);

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "webp");
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    private final Cloudinary cloudinary;
    private final boolean isCloudinaryConfigured;

    public ImageStorageService(
            @Value("${app.cloudinary.cloud-name:}") String cloudName,
            @Value("${app.cloudinary.api-key:}") String apiKey,
            @Value("${app.cloudinary.api-secret:}") String apiSecret) {

        if (StringUtils.hasText(cloudName) && StringUtils.hasText(apiKey) && StringUtils.hasText(apiSecret)) {
            this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                    "cloud_name", cloudName,
                    "api_key", apiKey,
                    "api_secret", apiSecret
            ));
            this.isCloudinaryConfigured = true;
            log.info("Cloudinary service configured with cloud: {}", cloudName);
        } else {
            this.cloudinary = null;
            this.isCloudinaryConfigured = false;
            log.info("Cloudinary credentials not provided; using high-res CDN demonstration fallback");
        }
    }

    public UploadResult uploadImage(MultipartFile file, String folder) {
        validateFile(file);

        if (isCloudinaryConfigured) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                        "folder", "stayhyderabad/" + folder,
                        "resource_type", "image"
                ));
                String secureUrl = (String) uploadResult.get("secure_url");
                String publicId = (String) uploadResult.get("public_id");
                return new UploadResult(secureUrl, secureUrl, publicId);
            } catch (IOException e) {
                log.error("Cloudinary upload failed", e);
                throw new BadRequestException("Image upload failed: " + e.getMessage());
            }
        } else {
            // Fallback for development without active Cloudinary account
            String mockPublicId = "mock_" + UUID.randomUUID().toString().substring(0, 8);
            // Realistic Unsplash high quality room photo
            String sampleUrl = "https://images.unsplash.com/photo-1555854877-bab0e564b8d5?auto=format&fit=crop&w=1200&q=80";
            return new UploadResult(sampleUrl, sampleUrl, mockPublicId);
        }
    }

    public void deleteImage(String publicId) {
        if (isCloudinaryConfigured && StringUtils.hasText(publicId) && !publicId.startsWith("mock_")) {
            try {
                cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            } catch (IOException e) {
                log.warn("Cloudinary image deletion failed for publicId: {}", publicId, e);
            }
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Uploaded file cannot be empty");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("File size exceeds 10MB limit");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new BadRequestException("Invalid file name format");
        }
        String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BadRequestException("Unsupported image format. Allowed formats: JPG, PNG, WEBP");
        }
    }

    public record UploadResult(String imageUrl, String thumbnailUrl, String publicId) {}
}
