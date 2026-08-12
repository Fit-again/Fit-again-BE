package com.fitagain.domain.recommend.dto;

import java.util.List;

public class RankedRecommendationDto {

    private int rank;
    private String recommendationType;
    private List<String> reasons; // 불릿 3개

    public RankedRecommendationDto() {}

    public RankedRecommendationDto(int rank, String recommendationType, List<String> reasons) {
        this.rank = rank;
        this.recommendationType = recommendationType;
        this.reasons = reasons;
    }

    public int getRank() { return rank; }
    public void setRank(int rank) { this.rank = rank; }
    public String getRecommendationType() { return recommendationType; }
    public void setRecommendationType(String recommendationType) { this.recommendationType = recommendationType; }
    public List<String> getReasons() { return reasons; }
    public void setReasons(List<String> reasons) { this.reasons = reasons; }
}