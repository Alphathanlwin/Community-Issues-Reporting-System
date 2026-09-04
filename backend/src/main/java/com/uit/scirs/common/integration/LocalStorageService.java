package com.uit.scirs.common.integration;

import com.uit.scirs.common.exception.FileStorageException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@ConditionalOnProperty(prefix = "app.storage", name = "provider", havingValue = "local", matchIfMissing = true)
public class LocalStorageService implements FileStorageService {

    private final Path storageRoot;
    private final ImageValidator imageValidator;

    public LocalStorageService(@Value("${app.storage.local-path}") String localPath,
                                ImageValidator imageValidator) {
        this.storageRoot = Paths.get(localPath).toAbsolutePath().normalize();
        this.imageValidator = imageValidator;
    }

    @Override
    public String store(MultipartFile file, String folder) {
        String extension = imageValidator.validateAndGetExtension(file);
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
}
