package com.uit.scirs.common.integration;

import com.uit.scirs.common.exception.FileStorageException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class LocalStorageService implements FileStorageService {

    private static final long MAX_IMAGE_SIZE_BYTES = 5L * 1024 * 1024;

    private static final Map<String, String> ALLOWED_CONTENT_TYPES = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp");

    private final Path storageRoot;

    public LocalStorageService(@Value("${app.storage.local-path}") String localPath) {
        this.storageRoot = Paths.get(localPath).toAbsolutePath().normalize();
    }

    @Override
    public String store(MultipartFile file, String folder) {
        validate(file);

        String extension = ALLOWED_CONTENT_TYPES.get(file.getContentType());
        String filename = UUID.randomUUID() + "." + extension;

        try {
            Path targetDir = storageRoot.resolve(folder).normalize();
            Files.createDirectories(targetDir);
            Path targetFile = targetDir.resolve(filename);
            file.transferTo(targetFile);
        } catch (IOException ex) {
            throw new FileStorageException("Failed to store the uploaded image");
        }

        return "/uploads/" + folder + "/" + filename;
    }

    @Override
    public void delete(String url) {
        if (url == null || !url.startsWith("/uploads/")) {
            return;
        }

        Path target = storageRoot.resolve(url.substring("/uploads/".length())).normalize();
        try {
            Files.deleteIfExists(target);
        } catch (IOException ex) {
            throw new FileStorageException("Failed to delete the stored image");
        }
    }

    private void validate(MultipartFile file) {
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
