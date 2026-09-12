package com.myfave.api.global.util;

import com.myfave.api.global.error.CustomException;
import com.myfave.api.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3UploadService {

    private final S3Client s3Client;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${spring.cloud.aws.region.static}")
    private String region;

    private static final List<String> IMAGE_EXTENSIONS = List.of("jpg", "jpeg", "png", "webp");
    private static final List<String> GIF_EXTENSIONS = List.of("gif");
    private static final List<String> VIDEO_EXTENSIONS = List.of("mp4", "mov", "avi");
    //파일 크기 제한
    private static final long IMAGE_MAX_SIZE = 10 * 1024 * 1024;   // 10MB
    private static final long VIDEO_MAX_SIZE = 100 * 1024 * 1024;  // 100MB

    /**
     * S3에 파일 업로드 후 URL 반환
     *
     * @param file      업로드할 파일
     * @param directory S3 버킷 내 경로 (예: "products", "contents/shortforms")
     * @return 업로드된 파일의 S3 URL
     */
    public String upload(MultipartFile file, String directory) {
        // step1. 검증 (확장자, 파일타입, 파일크기)
        String extension = extractExtension(file.getOriginalFilename());
        validateFileType(extension);
        validateFileSize(file.getSize(), extension);
        //step2. S3 업로드
        String key = directory + "/" + UUID.randomUUID() + "." + extension;

        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromBytes(file.getBytes()));
        } catch (IOException e) {
            throw new CustomException(ErrorCode.FILE_UPLOAD_FAILED);
        }
        //step3. URL 반환
        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, region, key);
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new CustomException(ErrorCode.FILE_INVALID_TYPE);
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    private void validateFileType(String extension) {
        boolean allowed = IMAGE_EXTENSIONS.contains(extension)
                || GIF_EXTENSIONS.contains(extension)
                || VIDEO_EXTENSIONS.contains(extension);

        if (!allowed) {
            throw new CustomException(ErrorCode.FILE_INVALID_TYPE);
        }
    }

    private void validateFileSize(long size, String extension) {
        if (VIDEO_EXTENSIONS.contains(extension)) {
            if (size > VIDEO_MAX_SIZE) {
                throw new CustomException(ErrorCode.FILE_SIZE_EXCEEDED);
            }
        } else {
            if (size > IMAGE_MAX_SIZE) {
                throw new CustomException(ErrorCode.FILE_SIZE_EXCEEDED);
            }
        }
    }
}
