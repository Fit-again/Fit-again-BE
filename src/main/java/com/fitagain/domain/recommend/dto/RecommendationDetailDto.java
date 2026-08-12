package com.fitagain.domain.recommend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationDetailDto {
    private String recommendationType; // REFORM, RESELL, UPCYCLING
    private String reason;
    private SimulationDto simulation;
}