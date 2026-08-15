package com.fitagain.domain.recommend.dto;

import java.util.List;

public class RecommendationJudgmentDto {

    private List<RankedRecommendationDto> rankings;

    public RecommendationJudgmentDto() {}

    public RecommendationJudgmentDto(List<RankedRecommendationDto> rankings) {
        this.rankings = rankings;
    }

    public List<RankedRecommendationDto> getRankings() { return rankings; }
    public void setRankings(List<RankedRecommendationDto> rankings) { this.rankings = rankings; }
}