package com.fitagain.domain.recommend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RecommendationResultDto {
    private String status;
    private RecommendationDetailDto recommendation;
}