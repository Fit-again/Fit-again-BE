package com.fitagain.domain.task.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public class TaskCreateReqDTO {

    @Builder
    @Schema(description = "AI 진단 분석 요청 DTO")
    public record CreateReqDTO(
            
            @Schema(description = "가방 종류", example = "토트백")
            @NotEmpty(message = "가방 종류는 필수입니다.")
            String productType,

            @Schema(description = "이미 업로드된 정면 사진 S3 URL")
            @NotEmpty(message = "정면 사진 URL은 필수입니다.")
            String frontImageUrl,

            @Schema(description = "이미 업로드된 디테일 사진 S3 URL 목록 (최대 4장)")
            List<String> detailImageUrls,

            @Schema(description = "새로 업로드할 마모/손상 부위 사진 리스트 (최대 5장)")
            List<MultipartFile> damageImages,

            @Schema(description = "선택한 불편 키워드 목록", example = "[\"스트랩이 무거움\", \"어깨가 아픔\"]")
            @NotNull(message = "키워드 목록은 필수입니다.")
            List<String> keywords,

            @Schema(description = "추가 설명", example = "가방이 너무 무거워서 어깨가 아파요.")
            String description
    ) {
    }

}