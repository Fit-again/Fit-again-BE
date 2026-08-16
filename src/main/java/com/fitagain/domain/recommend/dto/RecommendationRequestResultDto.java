package com.fitagain.domain.recommend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "AI 추천 요청에 대한 응답 DTO")
public class RecommendationRequestResultDto {

    @Schema(description = "추천 요청이 접수된 작업(task) ID", example = "1")
    private Long taskId;
}