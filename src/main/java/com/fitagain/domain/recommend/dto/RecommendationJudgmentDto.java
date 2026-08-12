package com.fitagain.domain.recommend.dto;

public class RecommendationJudgmentDto {

    private String recommendationType; // REFORM, RESELL, UPCYCLING
    private String reason;

    public RecommendationJudgmentDto() {}

    public RecommendationJudgmentDto(String recommendationType, String reason) {
        this.recommendationType = recommendationType;
        this.reason = reason;
    }

    public String getRecommendationType() { return recommendationType; }
    public void setRecommendationType(String recommendationType) { this.recommendationType = recommendationType; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}