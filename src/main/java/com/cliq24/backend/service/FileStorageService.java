package com.cliq24.backend.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Logger logger = LogManager.getLogger(FileStorageService.class);

    @Value("${file.upload.dir:uploads/profile-pictures}")
    private String uploadDir;

    @Value("${file.storage.mode:filesystem}")
    private String storageMode; // "filesystem" or "dataurl"

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final String[] ALLOWED_EXTENSIONS = {".jpg", ".jpeg", ".png", ".gif"};

    public String storeProfilePicture(MultipartFile file, String userId) throws IOException {
        // Validate file
        validateFile(file);

        // Use data URL storage for cloud environments (Railway, Heroku, etc.)
        // These platforms have ephemeral filesystems that don't persist uploads
        if ("dataurl".equals(storageMode)) {
            return storeAsDataUrl(file);
        }

        // Try filesystem storage first (for local development)
        try {
            return storeAsFile(file, userId);
        } catch (Exception e) {
            // Fallback to data URL if filesystem storage fails
            logger.warn("Filesystem storage failed, falling back to data URL: {}", e.getMessage());
            return storeAsDataUrl(file);
        }
    }

    private String storeAsFile(MultipartFile file, String userId) throws IOException {
        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
            logger.info("Created upload directory: {}", uploadPath);
        }

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String filename = userId + "_" + UUID.randomUUID().toString() + extension;

        // Save file
        Path filePath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        logger.info("Stored profile picture as file: {} for user: {}", filename, userId);

        // Return the relative URL path
        return "/uploads/profile-pictures/" + filename;
    }

    private String storeAsDataUrl(MultipartFile file) throws IOException {
        // Convert file to base64 data URL
        byte[] fileBytes = file.getBytes();
        String base64 = Base64.getEncoder().encodeToString(fileBytes);
        String mimeType = file.getContentType();

        if (mimeType == null || mimeType.isEmpty()) {
            // Detect mime type from extension
            String extension = getFileExtension(file.getOriginalFilename()).toLowerCase();
            mimeType = switch (extension) {
                case ".jpg", ".jpeg" -> "image/jpeg";
                case ".png" -> "image/png";
                case ".gif" -> "image/gif";
                default -> "image/jpeg";
            };
        }

        String dataUrl = "data:" + mimeType + ";base64," + base64;
        logger.info("Stored profile picture as data URL (size: {} bytes)", fileBytes.length);

        return dataUrl;
    }

    public void deleteProfilePicture(String pictureUrl) {
        if (pictureUrl == null || !pictureUrl.startsWith("/uploads/profile-pictures/")) {
            return; // Not a local file, skip deletion
        }

        try {
            String filename = pictureUrl.substring(pictureUrl.lastIndexOf("/") + 1);
            Path filePath = Paths.get(uploadDir).resolve(filename);

            if (Files.exists(filePath)) {
                Files.delete(filePath);
                logger.info("Deleted profile picture: {}", filename);
            }
        } catch (IOException e) {
            logger.error("Error deleting profile picture: {}", pictureUrl, e);
        }
    }

    private void validateFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("Cannot upload empty file");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IOException("File size exceeds maximum allowed size of 5MB");
        }

        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new IOException("Filename is null");
        }

        String extension = getFileExtension(filename).toLowerCase();
        boolean isAllowed = false;
        for (String allowedExt : ALLOWED_EXTENSIONS) {
            if (extension.equals(allowedExt)) {
                isAllowed = true;
                break;
            }
        }

        if (!isAllowed) {
            throw new IOException("File type not allowed. Only JPG, JPEG, PNG, and GIF are supported");
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
}
