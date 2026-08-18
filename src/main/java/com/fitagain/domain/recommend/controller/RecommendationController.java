package com.fitagain.domain.recommend.controller;

import com.fitagain.domain.recommend.dto.RecommendationRequestResultDto;
import com.fitagain.domain.recommend.dto.RecommendationResultDto;
import com.fitagain.domain.recommend.service.RecommendationService;
import com.fitagain.global.common.CustomResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "추천 API", description = "AI 추천(리폼/리셀/업사이클링) 및 시뮬레이션 결과 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/tasks/{taskId}/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    @Operation(
            summary = "AI 추천 결과 요청",
            description = "진단(AI 분석)이 완료(DIAGNOSED)된 작업에 대해 REFORM/RESELL/UPCYCLING 추천 및 이미지 생성 작업을 큐에 등록합니다. " +
                    "작업은 비동기로 처리되며, 요청이 접수되는 즉시 응답합니다. " +
                    "완료 여부는 GET API로 폴링해서 확인해주세요.\n\n" +
                    "**멱등(idempotent) API입니다.** 진단이 DIAGNOSED로 끝나면 서버가 백그라운드 스케줄러로 추천 생성을 미리 시작해둘 수 있어서, " +
                    "이 API를 호출한 시점에 이미 RECOMMENDING 또는 RECOMMENDED 상태일 수 있습니다. 이 경우 작업을 재시작하지 않고 taskId만 그대로 반환하므로, " +
                    "프론트에서는 '추천 결과 보기' 클릭 시 항상 이 API를 먼저 호출한 뒤 GET으로 폴링하면 됩니다 (이미 끝나 있어도 안전)."
    )
    @PostMapping
    public CustomResponse<RecommendationRequestResultDto> requestRecommendation(
            @Parameter(description = "진단이 완료된 작업 ID", example = "1")
            @PathVariable("taskId") Long taskId
    ) {
        Long resultTaskId = recommendationService.requestRecommendation(taskId);
        return CustomResponse.onSuccess(
                "COMMON200",
                "AI 추천 및 시뮬레이션 생성 작업이 성공적으로 요청되었습니다.",
                new RecommendationRequestResultDto(resultTaskId)
        );
    }

    @Operation(
            summary = "AI 추천 결과 조회 (폴링)",
            description = "해당 작업의 AI 추천 작업 현재 상태(RECOMMENDING, RECOMMENDED, FAILED 등)를 조회합니다. " +
                    "상태가 RECOMMENDED가 되면 3개 순위(REFORM/RESELL/UPCYCLING) 전체의 상세 데이터가 rankings에 담겨 반환됩니다. " +
                    "그 외 상태에서는 rankings가 null이므로, 프론트엔드는 RECOMMENDED가 될 때까지 계속 폴링해야 합니다."
    )
    @GetMapping
    public CustomResponse<RecommendationResultDto> getRecommendation(
            @Parameter(description = "추천을 요청한 작업 ID", example = "1")
            @PathVariable("taskId") Long taskId
    ) {
        RecommendationResultDto result = recommendationService.getRecommendation(taskId);
        String message = "RECOMMENDED".equals(result.getStatus())
                ? "AI 추천 결과 및 시뮬레이션 데이터 조회 성공"
                : "현재 AI 추천 및 이미지 생성이 진행 중입니다.";
        return CustomResponse.onSuccess("COMMON200", message, result);
    }
}