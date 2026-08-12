package com.fitagain.domain.image.controller;

import com.fitagain.domain.image.dto.response.ImageAnalysisResDTO;
import com.fitagain.domain.image.service.command.ImageCommandService;
import com.fitagain.global.common.CustomResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Tag(name = "Image API", description = "업로드 사진 분석 및 S3 저장 API")
@RestController
@RequestMapping("/api/v1/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageCommandService imageCommandService;

    @Operation(summary = "제품 사진 분석", description = "제품 정면 사진과 디테일 사진(0~4장)을 업로드받아 비전 AI로 검증하고 S3 URL 목록을 반환합니다.")
    @PostMapping(value = "/analyze", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public CustomResponse<ImageAnalysisResDTO.ImageAnalysisDTO> analyzeImages(
            @RequestPart("frontImage") MultipartFile frontImage,
            @RequestPart(value = "detailImages", required = false) java.util.List<MultipartFile> detailImages) throws IOException {
        
        ImageAnalysisResDTO.ImageAnalysisDTO response = imageCommandService.analyzeAndUploadImages(frontImage, detailImages);

        return CustomResponse.onSuccess(response);
    }
}
