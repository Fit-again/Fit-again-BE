package com.fitagain.domain.recommend.controller;

import com.fitagain.domain.recommend.dto.RecommendationRequestResultDto;
import com.fitagain.domain.recommend.dto.RecommendationResultDto;
import com.fitagain.domain.recommend.service.RecommendationService;
import com.fitagain.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/tasks/{taskId}/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    @PostMapping
    public ApiResponse<RecommendationRequestResultDto> requestRecommendation(
            @PathVariable("taskId") Long taskId
    ) {
        Long resultTaskId = recommendationService.requestRecommendation(taskId);
        return ApiResponse.success(
                "COMMON200",
                "AI 추천 및 시뮬레이션 생성 작업이 성공적으로 요청되었습니다.",
                new RecommendationRequestResultDto(resultTaskId)
        );
    }

    @GetMapping
    public ApiResponse<RecommendationResultDto> getRecommendation(
            @PathVariable("taskId") Long taskId
    ) {
        RecommendationResultDto result = recommendationService.getRecommendation(taskId);
        String message = "RECOMMENDED".equals(result.getStatus())
                ? "AI 추천 결과 및 시뮬레이션 데이터 조회 성공"
                : "현재 AI 추천 및 이미지 생성이 진행 중입니다.";
        return ApiResponse.success("COMMON200", message, result);
    }
}