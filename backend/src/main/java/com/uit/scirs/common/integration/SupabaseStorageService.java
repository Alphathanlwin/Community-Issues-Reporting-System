package com.uit.scirs.common.integration;

import com.uit.scirs.common.exception.FileStorageException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

/**
 * Uploads report images to a public Supabase Storage bucket via its REST API
 * (https://{project}.supabase.co/storage/v1/object/...), using the service
 * role key so uploads bypass bucket RLS policies.
 */
@Service
@ConditionalOnProperty(prefix = "app.storage", name = "provider", havingValue = "supabase")
public class SupabaseStorageService implements FileStorageService {

    private final RestClient restClient;
    private final ImageValidator imageValidator;
    private final String supabaseUrl;
    private final String bucket;

    public SupabaseStorageService(RestClient.Builder restClientBuilder,
                                   ImageValidator imageValidator,
                                   @Value("${app.supabase.url}") String supabaseUrl,
                                   @Value("${app.supabase.bucket}") String bucket,
                                   @Value("${app.supabase.service-key}") String serviceKey) {
        this.imageValidator = imageValidator;
        this.supabaseUrl = supabaseUrl.replaceAll("/+$", "");
        this.bucket = bucket;
        this.restClient = restClientBuilder
                .baseUrl(this.supabaseUrl)
                .defaultHeader("apikey", serviceKey)
                .defaultHeader("Authorization", "Bearer " + serviceKey)
                .build();
    }

    @Override
    public String store(MultipartFile file, String folder) {
        String extension = imageValidator.validateAndGetExtension(file);
        String objectPath = folder + "/" + UUID.randomUUID() + "." + extension;

        try {
            restClient.post()
                    .uri("/storage/v1/object/" + bucket + "/" + objectPath)
                    .contentType(MediaType.parseMediaType(file.getContentType()))
                    .body(file.getBytes())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        throw new FileStorageException("Failed to store the uploaded image");
                    })
                    .toBodilessEntity();
        } catch (IOException ex) {
            throw new FileStorageException("Failed to read the uploaded image");
        }

        return supabaseUrl + "/storage/v1/object/public/" + bucket + "/" + objectPath;
    }

    @Override
    public void delete(String url) {
        String marker = "/storage/v1/object/public/" + bucket + "/";
        if (url == null || !url.contains(marker)) {
            return;
        }
        String objectPath = url.substring(url.indexOf(marker) + marker.length());

        restClient.delete()
                .uri("/storage/v1/object/" + bucket + "/" + objectPath)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw new FileStorageException("Failed to delete the stored image");
                })
                .toBodilessEntity();
    }
}
