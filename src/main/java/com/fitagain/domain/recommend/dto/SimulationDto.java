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
}