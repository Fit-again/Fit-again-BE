package com.fitagain.domain.task.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public class TaskCreateResDTO {

    @Builder
    @Schema(description = "AI 진단 분석 요청 결과 응답 DTO")
    public record CreateResDTO(
            @Schema(description = "생성된 작업 ID", example = "1")
            Long taskId
    ) {
    }

}