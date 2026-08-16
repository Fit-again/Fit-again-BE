package com.fitagain.domain.recommend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AI 추천 결과 조회 응답 DTO")
public class RecommendationResultDto {

    @Schema(description = "작업 상태 (예: RECOMMENDING, RECOMMENDED, FAILED)", example = "RECOMMENDED")
    private String status;

    @Schema(description = "세 가지 방향(REFORM/RESELL/UPCYCLING) 전체의 순위별 데이터. " +
            "status가 RECOMMENDED가 아니면 null입니다.")
    private List<RankedRecommendationDto> rankings;
}