package synamyk.service;

import io.minio.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import synamyk.enums.MediaFileType;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioService {

    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucket;

    @Value("${minio.url}")
    private String minioUrl;

    /**
     * On startup: create bucket if missing and set public-read policy
     * so avatars, covers, and thumbnails are directly accessible by URL.
     */
    @PostConstruct
    public void init() {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("Created MinIO bucket: {}", bucket);
            }
            // Public read policy — all objects are readable without auth
            String policy = """
                    {
                      "Version": "2012-10-17",
                      "Statement": [{
                        "Effect": "Allow",
                        "Principal": {"AWS": ["*"]},
                        "Action": ["s3:GetObject"],
                        "Resource": ["arn:aws:s3:::%s/*"]
                      }]
                    }
                    """.formatted(bucket);
            minioClient.setBucketPolicy(SetBucketPolicyArgs.builder().bucket(bucket).config(policy).build());
            log.info("MinIO bucket '{}' is ready with public-read policy", bucket);
        } catch (Exception e) {
            log.error("MinIO init failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Uploads an image file and returns its permanent public URL.
     * entityId is used as the folder name (userId, newsId, videoId, etc.).
     */
    public String upload(MultipartFile file, MediaFileType type, String entityId) {
        validateImage(file);

        String ext = getExtension(file.getOriginalFilename());
        String objectKey = buildKey(type, entityId, ext);

        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
            log.info("Uploaded to MinIO: {}", objectKey);
        } catch (Exception e) {
            log.error("MinIO upload failed for key {}: {}", objectKey, e.getMessage(), e);
            throw new RuntimeException("File upload failed: " + e.getMessage());
        }

        return buildPublicUrl(objectKey);
    }

    /**
     * Deletes an object by its public URL or objectKey.
     */
    public void delete(String urlOrKey) {
        String objectKey = extractObjectKey(urlOrKey);
        try {
            minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(objectKey).build());
            log.info("Deleted from MinIO: {}", objectKey);
        } catch (Exception e) {
            log.warn("MinIO delete failed (maybe already gone): {}", objectKey);
        }
    }

    // ---- helpers ----

    private String buildKey(MediaFileType type, String entityId, String ext) {
        String folder = switch (type) {
            case AVATAR -> "avatars/" + entityId;
            case NEWS_COVER -> "news/" + entityId;
            case VIDEO_THUMBNAIL -> "thumbnails/" + entityId;
        };
        return folder + "/" + UUID.randomUUID() + ext;
    }

    private String buildPublicUrl(String objectKey) {
        String base = minioUrl.endsWith("/") ? minioUrl : minioUrl + "/";
        return base + bucket + "/" + objectKey;
    }

    private String extractObjectKey(String urlOrKey) {
        String marker = "/" + bucket + "/";
        int idx = urlOrKey.indexOf(marker);
        return idx >= 0 ? urlOrKey.substring(idx + marker.length()) : urlOrKey;
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return ".bin";
        return filename.substring(filename.lastIndexOf("."));
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("File is empty.");
        long maxSize = 10L * 1024 * 1024; // 10 MB
        if (file.getSize() > maxSize) throw new IllegalArgumentException("File too large. Max: 10 MB.");
        String ct = file.getContentType();
        if (ct == null || !ct.startsWith("image/")) {
            throw new IllegalArgumentException("Only image files are allowed.");
        }
    }
}