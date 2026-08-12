package com.fitagain.domain.recommend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SimulationDto {
    private List<StepDto> steps;
    private BeforeAfterDto beforeAfter;
}