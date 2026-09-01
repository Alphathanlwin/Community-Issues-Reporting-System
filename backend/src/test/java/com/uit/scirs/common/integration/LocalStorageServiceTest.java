package com.uit.scirs.common.integration;

import com.uit.scirs.common.exception.FileStorageException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Priority test cases #25-27 from testing-standards.md: non-image upload
 * rejection, oversized upload rejection, and stored filename divergence.
 */
class LocalStorageServiceTest {

    @TempDir
    Path tempDir;

    private LocalStorageService storageService(Path root) {
        return new LocalStorageService(root.toString());
    }

    @Test
    void store_withNonImageContent_throwsFileStorageException() {
        LocalStorageService service = storageService(tempDir);
        MultipartFile file = new MockMultipartFile("images", "report.pdf", "application/pdf", new byte[]{1, 2, 3});

        assertThatThrownBy(() -> service.store(file, "reports"))
                .isInstanceOf(FileStorageException.class)
                .hasMessageContaining("JPEG, PNG, and WEBP");
    }

    @Test
    void store_withFileOverFiveMegabytes_throwsFileStorageException() {
        LocalStorageService service = storageService(tempDir);
        byte[] oversized = new byte[6 * 1024 * 1024];
        MultipartFile file = new MockMultipartFile("images", "big.jpg", "image/jpeg", oversized);

        assertThatThrownBy(() -> service.store(file, "reports"))
                .isInstanceOf(FileStorageException.class)
                .hasMessageContaining("5 MB");
    }

    @Test
    void store_withValidJpeg_generatesStoredFilenameDifferentFromUploadedFilename() {
        LocalStorageService service = storageService(tempDir);
        byte[] jpegHeader = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00, 0x01, 0x02};
        MultipartFile file = new MockMultipartFile("images", "my-pothole-photo.jpg", "image/jpeg", jpegHeader);

        String url = service.store(file, "reports");

        assertThat(url).startsWith("/uploads/reports/");
        String storedFilename = url.substring(url.lastIndexOf('/') + 1);
        assertThat(storedFilename).isNotEqualTo("my-pothole-photo.jpg");
        assertThat(storedFilename).endsWith(".jpg");
    }
}
