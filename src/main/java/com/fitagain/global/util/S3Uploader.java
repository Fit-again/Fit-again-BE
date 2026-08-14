package com.fitagain.global.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class S3Uploader {

    private final S3Client s3Client;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucket;

    /**
     * S3에 파일을 업로드하고 퍼블릭 URL을 반환합니다.
     * @param multipartFile 업로드할 파일
     * @param dirName S3 내부에 저장될 폴더 이름 (예: "tasks/front")
     * @return S3 객체 URL
     */
    public String upload(MultipartFile multipartFile, String dirName) throws IOException {

        String originalFileName = multipartFile.getOriginalFilename();
        String extension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        
        // 한글/공백 URL 인코딩 이슈 방지를 위해 순수 UUID와 확장자만 결합
        String uniqueFileName = dirName + "/" + UUID.randomUUID() + extension;

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(uniqueFileName)
                .contentType(multipartFile.getContentType())
                .build();

        // S3로 파일 전송
        s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(multipartFile.getInputStream(), multipartFile.getSize()));

        // 생성된 퍼블릭 URL 반환
        return "https://" + bucket + ".s3.ap-northeast-2.amazonaws.com/" + uniqueFileName;
    }

    /**
     * S3에 여러 파일을 한 번에 업로드하고 퍼블릭 URL 리스트를 반환합니다.
     * @param multipartFiles 업로드할 파일 리스트
     * @param dirName S3 내부에 저장될 폴더 이름
     * @return S3 객체 URL 리스트
     */
    public List<String> uploadList(List<MultipartFile> multipartFiles, String dirName) throws IOException {
        List<String> fileUrls = new ArrayList<>();
        if (multipartFiles != null && !multipartFiles.isEmpty()) {
            for (MultipartFile file : multipartFiles) {
                if (file != null && !file.isEmpty()) {
                    fileUrls.add(upload(file, dirName));
                }
            }
        }
        return fileUrls;
    }

}
