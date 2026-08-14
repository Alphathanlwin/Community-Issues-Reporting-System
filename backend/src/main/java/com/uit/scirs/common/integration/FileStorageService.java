package com.uit.scirs.common.integration;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    String store(MultipartFile file, String folder);

    void delete(String url);
}
