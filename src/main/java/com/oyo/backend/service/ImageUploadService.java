package com.oyo.backend.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageUploadService {

    private final Cloudinary cloudinary;

    public String uploadImage(MultipartFile file, String folder) throws IOException {
        Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(),
                ObjectUtils.asMap(
                        "folder", "oyo/" + folder,
                        "resource_type", "image",
                        "quality", "auto",
                        "fetch_format", "auto"));
        return (String) result.get("secure_url");
    }

    public String uploadImageFromUrl(String url, String folder) throws IOException {
        Map<?, ?> result = cloudinary.uploader().upload(url,
                ObjectUtils.asMap("folder", "oyo/" + folder));
        return (String) result.get("secure_url");
    }

    public void deleteImage(String publicId) {
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (IOException e) {
            log.warn("Failed to delete image {}: {}", publicId, e.getMessage());
        }
    }
}
