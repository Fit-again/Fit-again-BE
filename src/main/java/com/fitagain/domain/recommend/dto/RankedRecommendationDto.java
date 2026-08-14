package com.fitagain.domain.recommend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "추천 방향(REFORM/RESELL/UPCYCLING) 하나에 대한 순위 및 상세 데이터. " +
        "3개 항목 각각이 전체 데이터를 다 갖고 있어서, 사용자가 2순위나 3순위를 클릭하면 프론트에서 그걸 바로 메인 화면으로 승격시킬 수 있습니다.")
public class RankedRecommendationDto {

    @Schema(description = "순위 (1~3)", example = "1")
    private int rank;

    @Schema(description = "추천 방향", example = "REFORM", allowableValues = {"REFORM", "RESELL", "UPCYCLING"})
    private String recommendationType;

    @Schema(description = "이 순위에 대한 이유 (정확히 3개 불릿)")
    private List<String> reasons;

    @Schema(description = "추천 리폼 작업 (정확히 3개). recommendationType이 REFORM일 때만 존재하고, 그 외에는 null입니다")
    private List<RecommendedWorkDto> recommendedWorks;

    @Schema(description = "업사이클링 후보 품목 (정확히 3개). recommendationType이 UPCYCLING일 때만 존재하고, 그 외에는 null입니다")
    private List<UpcyclingCandidateDto> upcyclingCandidates;

    @Schema(description = "리폼 시뮬레이션 데이터 (Before/After 및 단계별 이미지). " +
            "recommendationType이 REFORM일 때만 존재하며, Gemini 이미지 생성이 완료되면 채워집니다")
    private SimulationDto simulation;

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
    public List<RecommendedWorkDto> getRecommendedWorks() { return recommendedWorks; }
    public void setRecommendedWorks(List<RecommendedWorkDto> recommendedWorks) { this.recommendedWorks = recommendedWorks; }
    public List<UpcyclingCandidateDto> getUpcyclingCandidates() { return upcyclingCandidates; }
    public void setUpcyclingCandidates(List<UpcyclingCandidateDto> upcyclingCandidates) { this.upcyclingCandidates = upcyclingCandidates; }
    public SimulationDto getSimulation() { return simulation; }
    public void setSimulation(SimulationDto simulation) { this.simulation = simulation; }
}