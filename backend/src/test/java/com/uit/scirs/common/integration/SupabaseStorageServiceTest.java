package com.uit.scirs.common.integration;

import com.uit.scirs.common.exception.FileStorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class SupabaseStorageServiceTest {

    private static final String SUPABASE_URL = "https://project.supabase.co";
    private static final String BUCKET = "report-images";

    private MockRestServiceServer mockServer;
    private SupabaseStorageService service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        service = new SupabaseStorageService(builder, new ImageValidator(), SUPABASE_URL, BUCKET, "service-key");
    }

    @Test
    void store_withValidJpeg_uploadsAndReturnsPublicUrl() {
        mockServer.expect(method(HttpMethod.POST))
                .andRespond(withSuccess());

        byte[] jpegHeader = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00, 0x01, 0x02};
        MultipartFile file = new MockMultipartFile("images", "photo.jpg", "image/jpeg", jpegHeader);

        String url = service.store(file, "reports");

        assertThat(url).startsWith(SUPABASE_URL + "/storage/v1/object/public/" + BUCKET + "/reports/");
        assertThat(url).endsWith(".jpg");
        mockServer.verify();
    }

    @Test
    void store_whenSupabaseReturnsError_throwsFileStorageException() {
        mockServer.expect(method(HttpMethod.POST))
                .andRespond(withServerError());

        byte[] jpegHeader = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00, 0x01, 0x02};
        MultipartFile file = new MockMultipartFile("images", "photo.jpg", "image/jpeg", jpegHeader);

        assertThatThrownBy(() -> service.store(file, "reports"))
                .isInstanceOf(FileStorageException.class);
    }

    @Test
    void store_withNonImageContent_throwsBeforeCallingSupabase() {
        MultipartFile file = new MockMultipartFile("images", "report.pdf", "application/pdf", new byte[]{1, 2, 3});

        assertThatThrownBy(() -> service.store(file, "reports"))
                .isInstanceOf(FileStorageException.class)
                .hasMessageContaining("JPEG, PNG, and WEBP");

        mockServer.verify();
    }

    @Test
    void delete_withPublicUrl_sendsDeleteToObjectPath() {
        String objectPath = "reports/abc-123.jpg";
        String url = SUPABASE_URL + "/storage/v1/object/public/" + BUCKET + "/" + objectPath;

        mockServer.expect(requestTo(SUPABASE_URL + "/storage/v1/object/" + BUCKET + "/" + objectPath))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withSuccess());

        service.delete(url);

        mockServer.verify();
    }

    @Test
    void delete_withNullOrUnrecognisedUrl_doesNothing() {
        service.delete(null);
        service.delete("/uploads/reports/local-file.jpg");

        mockServer.verify();
    }
}
