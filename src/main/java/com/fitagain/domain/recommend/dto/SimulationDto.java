package com.fitagain.domain.recommend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "REFORM 시뮬레이션 데이터 (해체~완성 4단계, Before/After 비교 포함)")
public class SimulationDto {

    @Schema(description = "단계별 리폼 데이터 (해체 → 교체 → 보강 → 완성, 정확히 4개)")
    private List<StepDto> steps;

    @Schema(description = "Before/After 비교 데이터")
    private BeforeAfterDto beforeAfter;

    @Schema(description = "사용자가 촬영한 손상 부위 이미지 URL 목록 (DiagnosisTask.damageImageUrls 재사용, 없으면 빈 리스트)")
    private List<String> damageImageUrls;

    @Schema(description = "STEP1 \"해체\" 이미지에 합성된 손상/변경 부위 원형 마커의 원본 좌표 목록 (0~100 비율, 없으면 빈 리스트). " +
            "프론트에서 자체적으로 마커를 다시 그리고 싶을 때 참고용으로 사용합니다")
    private List<DamageMarkerDto> damageMarkers;
}