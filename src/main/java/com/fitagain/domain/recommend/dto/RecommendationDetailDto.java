package com.fitagain.domain.recommend.dto;

import java.util.List;

public class RecommendationDetailDto {

    private String recommendationType;
    private List<String> reasons; // 1순위 이유 불릿 3개
    private List<RankedRecommendationDto> alternatives;
    private SimulationDto simulation;

    public RecommendationDetailDto() {}

    public RecommendationDetailDto(
            String recommendationType,
            List<String> reasons,
            List<RankedRecommendationDto> alternatives,
            SimulationDto simulation
    ) {
        this.recommendationType = recommendationType;
        this.reasons = reasons;
        this.alternatives = alternatives;
        this.simulation = simulation;
    }

    public String getRecommendationType() { return recommendationType; }
    public void setRecommendationType(String recommendationType) { this.recommendationType = recommendationType; }
    public List<String> getReasons() { return reasons; }
    public void setReasons(List<String> reasons) { this.reasons = reasons; }
    public List<RankedRecommendationDto> getAlternatives() { return alternatives; }
    public void setAlternatives(List<RankedRecommendationDto> alternatives) { this.alternatives = alternatives; }
    public SimulationDto getSimulation() { return simulation; }
    public void setSimulation(SimulationDto simulation) { this.simulation = simulation; }
}