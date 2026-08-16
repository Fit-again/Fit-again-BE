package com.fitagain.domain.image.service.command;

import com.fitagain.domain.image.dto.response.ImageAnalysisResDTO;
import com.fitagain.global.util.S3Uploader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ImageCommandServiceImpl implements ImageCommandService {

    private final S3Uploader s3Uploader;
    private final com.fitagain.domain.image.service.ai.VisionAiService visionAiService;

    @Override
    public ImageAnalysisResDTO.ImageAnalysisDTO analyzeAndUploadImages(MultipartFile frontImage, java.util.List<MultipartFile> detailImages) throws IOException {

        // 1. 정면 사진 S3 업로드
        String frontImageUrl = s3Uploader.upload(frontImage, "tasks/front");

        // 2. 디테일 사진 S3 업로드 (Optional)
        java.util.List<String> detailImageUrls = new java.util.ArrayList<>();
        if (detailImages != null && !detailImages.isEmpty()) {
            for (MultipartFile detailImage : detailImages) {
                if (!detailImage.isEmpty()) {
                    detailImageUrls.add(s3Uploader.upload(detailImage, "tasks/detail"));
                }
            }
        }

        // 3. 비전 AI 검증 (OpenAI GPT-4o 실제 연동 - Base64 방식)
        com.fitagain.domain.image.service.ai.VisionAiService.VisionResult aiResult = 
                visionAiService.verifyBagImages(frontImage, detailImages);

        return ImageAnalysisResDTO.ImageAnalysisDTO.builder()
                .frontImageUrl(frontImageUrl)
                .detailImageUrls(detailImageUrls)
                .isValid(aiResult.isValid())
                .message(aiResult.message())
                .build();
    }
}
