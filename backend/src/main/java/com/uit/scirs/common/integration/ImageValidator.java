package com.uit.scirs.common.integration;

import com.uit.scirs.common.exception.FileStorageException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

@Component
public class ImageValidator {

    private static final long MAX_IMAGE_SIZE_BYTES = 5L * 1024 * 1024;

    private static final Map<String, String> ALLOWED_CONTENT_TYPES = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp");

    public String validateAndGetExtension(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileStorageException("Uploaded file is empty");
        }

        if (file.getSize() > MAX_IMAGE_SIZE_BYTES) {
            throw new FileStorageException("Image size must not exceed 5 MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.containsKey(contentType)) {
            throw new FileStorageException("Only JPEG, PNG, and WEBP images are supported");
        }

        if (!matchesDeclaredType(file, contentType)) {
            throw new FileStorageException("Uploaded file content does not match its declared image type");
        }

        return ALLOWED_CONTENT_TYPES.get(contentType);
    }

    private boolean matchesDeclaredType(MultipartFile file, String contentType) {
        byte[] header = readHeader(file);

        return switch (contentType) {
            case "image/jpeg" -> header.length >= 3
                    && (header[0] & 0xFF) == 0xFF
                    && (header[1] & 0xFF) == 0xD8
                    && (header[2] & 0xFF) == 0xFF;
            case "image/png" -> header.length >= 8
                    && (header[0] & 0xFF) == 0x89
                    && header[1] == 0x50 && header[2] == 0x4E && header[3] == 0x47
                    && header[4] == 0x0D && header[5] == 0x0A && header[6] == 0x1A && header[7] == 0x0A;
            case "image/webp" -> header.length >= 12
                    && header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                    && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P';
            default -> false;
        };
    }

    private byte[] readHeader(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            return in.readNBytes(12);
        } catch (IOException ex) {
            throw new FileStorageException("Failed to read the uploaded image");
        }
    }
}
