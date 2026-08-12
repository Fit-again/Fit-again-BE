package com.fitagain.domain.image.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

public class ImageAnalysisResDTO {

    @Builder
    @Schema(description = "이미지 분석 결과 응답 DTO")
    public record ImageAnalysisDTO(
            @Schema(description = "S3에 업로드된 제품 정면 사진 퍼블릭 URL", example = "https://fitagain-images.s3.ap-northeast-2.amazonaws.com/tasks/front/uuid_bag.jpg")
            String frontImageUrl,

            @Schema(description = "S3에 업로드된 제품 디테일 사진 퍼블릭 URL 목록", example = "[\"https://fitagain-images.s3.ap-northeast-2.amazonaws.com/tasks/detail/uuid_detail.jpg\"]")
            List<String> detailImageUrls,

            @Schema(description = "정상적인 가방 사진인지 여부", example = "true")
            boolean isValid,

            @Schema(description = "AI 분석 결과 메시지", example = "정상적인 가방 사진")
            String message
    ) {
    }

}