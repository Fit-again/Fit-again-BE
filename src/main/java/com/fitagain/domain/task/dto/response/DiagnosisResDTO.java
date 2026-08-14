package com.fitagain.domain.task.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.Map;

public class DiagnosisResDTO {

    @Builder
    @Schema(description = "진단 분석 작업 상태 및 결과 응답 DTO")
    public record DiagnosisResultDTO(
            @Schema(description = "작업 상태 (PENDING, DIAGNOSING, DIAGNOSED, FAILED)", example = "DIAGNOSED")
            String status,

            @Schema(description = "AI 진단 결과 데이터 (이미지, 제품유형, AI 분석 결과 등 포함)")
            Map<String, Object> diagnosisResult,

            @Schema(description = "에러 메시지 (상태가 FAILED 일 때만 존재)")
            String errorMessage
    ) {
    }
}
