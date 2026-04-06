package com.example.MyWeb.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
public class FileStorageService {

    // L7: Danh sách MIME type cho phép
    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );
    // L7: Giới hạn kích thước 5MB
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    private final Path fileStorageLocation;

    public FileStorageService() {
        this.fileStorageLocation = Paths.get("uploads").toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    public String storeFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("File is empty");
        }

        // L7 FIX: Validate MIME type — ngăn upload file thực thi (.php, .jsp, .exe)
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new RuntimeException(
                "Invalid file type: " + contentType + ". Only JPEG, PNG, WebP, GIF images are allowed."
            );
        }

        // L7 FIX: Giới hạn kích thước file — ngăn DoS qua upload file khổng lồ
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new RuntimeException(
                "File size exceeds the maximum limit of 5MB. Current size: " + (file.getSize() / 1024 / 1024) + "MB"
            );
        }

        // Lấy extension từ original name (chỉ dùng extension, bỏ toàn bộ path gốc)
        String originalFileName = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFileName != null) {
            int i = originalFileName.lastIndexOf('.');
            if (i > 0) {
                fileExtension = originalFileName.substring(i).toLowerCase();
            }
        }

        // UUID làm tên file — không bao giờ dùng original filename (ngăn path traversal)
        String fileName = UUID.randomUUID().toString() + fileExtension;

        try {
            Path targetLocation = this.fileStorageLocation.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            return fileName;
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + fileName + ". Please try again!", ex);
        }
    }
}
