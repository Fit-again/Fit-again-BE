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

    @Schema(description = "사용자가 업로드한 정면 원본 이미지 URL. 모든 recommendationType 공통으로 채워집니다 (DiagnosisTask.frontImageUrl 재사용)")
    private String frontImageUrl;

    @Schema(description = "추천 리폼 작업 (정확히 3개). recommendationType이 REFORM일 때만 존재하고, 그 외에는 null입니다")
    private List<RecommendedWorkDto> recommendedWorks;

    @Schema(description = "업사이클링 후보 품목 (정확히 3개). recommendationType이 UPCYCLING일 때만 존재하고, 그 외에는 null입니다")
    private List<UpcyclingCandidateDto> upcyclingCandidates;

    @Schema(description = "리폼 시뮬레이션 데이터 (Before/After 및 단계별 이미지). " +
            "recommendationType이 REFORM일 때만 존재하며, Gemini 이미지 생성이 완료되면 채워집니다")
    private SimulationDto simulation;

    @Schema(description = "리폼 AI 생성 결과 이미지 URL (simulation.beforeAfter.after.imageUrl과 동일값). " +
            "recommendationType이 REFORM일 때만 존재하며, 추천 결과 페이지에서 바로 노출할 때 사용합니다")
    private String resultImageUrl;

    @Schema(description = "AI 추천 한줄 요약 코멘트. recommendationType이 REFORM일 때만 존재합니다", example = "스트랩 교체와 어깨 패드 보강으로 착용감이 크게 개선됩니다.")
    private String summaryComment;

    @Schema(description = "이 리폼으로 해결되는 불편 목록. recommendationType이 REFORM일 때만 존재합니다")
    private List<String> resolvedPains;

    @Schema(description = "예상 난이도. recommendationType이 REFORM일 때만 존재합니다", example = "보통", allowableValues = {"쉬움", "보통", "어려움"})
    private String difficulty;

    @Schema(description = "이 제품과 잘 맞는 사용자군 목록. recommendationType이 RESELL일 때만 존재합니다")
    private List<SuitableUserDto> suitableUsers;

    @Schema(description = "재판매 가치에 부정적 영향을 줄 수 있는 요소 목록. recommendationType이 RESELL일 때만 존재합니다", example = "[\"핸들/가죽 마모\", \"수납 공간 부족\"]")
    private List<String> negativeFactors;

    @Schema(description = "재판매 가치를 유지하는 긍정적 요소 목록. recommendationType이 RESELL일 때만 존재합니다", example = "[\"더블 핸들 디자인\", \"탈부착 스트랩\"]")
    private List<String> positiveFactors;

    @Schema(description = "현재 니즈에 맞는 대안 제품 추천. recommendationType이 RESELL일 때만 존재합니다")
    private AlternativeProductSuggestionDto alternativeProductSuggestion;

    @Schema(description = "기존 제품에서 이어지는 특징 태그 목록. recommendationType이 UPCYCLING일 때만 존재합니다", example = "[\"MCM 시그니처 패턴\", \"가죽 소재\", \"금속 하드웨어\", \"브랜드 아이덴티티\"]")
    private List<String> existingFeatureTags;

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
    public String getFrontImageUrl() { return frontImageUrl; }
    public void setFrontImageUrl(String frontImageUrl) { this.frontImageUrl = frontImageUrl; }
    public List<RecommendedWorkDto> getRecommendedWorks() { return recommendedWorks; }
    public void setRecommendedWorks(List<RecommendedWorkDto> recommendedWorks) { this.recommendedWorks = recommendedWorks; }
    public List<UpcyclingCandidateDto> getUpcyclingCandidates() { return upcyclingCandidates; }
    public void setUpcyclingCandidates(List<UpcyclingCandidateDto> upcyclingCandidates) { this.upcyclingCandidates = upcyclingCandidates; }
    public SimulationDto getSimulation() { return simulation; }
    public void setSimulation(SimulationDto simulation) { this.simulation = simulation; }
    public String getResultImageUrl() { return resultImageUrl; }
    public void setResultImageUrl(String resultImageUrl) { this.resultImageUrl = resultImageUrl; }
    public String getSummaryComment() { return summaryComment; }
    public void setSummaryComment(String summaryComment) { this.summaryComment = summaryComment; }
    public List<String> getResolvedPains() { return resolvedPains; }
    public void setResolvedPains(List<String> resolvedPains) { this.resolvedPains = resolvedPains; }
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    public List<SuitableUserDto> getSuitableUsers() { return suitableUsers; }
    public void setSuitableUsers(List<SuitableUserDto> suitableUsers) { this.suitableUsers = suitableUsers; }
    public List<String> getNegativeFactors() { return negativeFactors; }
    public void setNegativeFactors(List<String> negativeFactors) { this.negativeFactors = negativeFactors; }
    public List<String> getPositiveFactors() { return positiveFactors; }
    public void setPositiveFactors(List<String> positiveFactors) { this.positiveFactors = positiveFactors; }
    public AlternativeProductSuggestionDto getAlternativeProductSuggestion() { return alternativeProductSuggestion; }
    public void setAlternativeProductSuggestion(AlternativeProductSuggestionDto alternativeProductSuggestion) { this.alternativeProductSuggestion = alternativeProductSuggestion; }
    public List<String> getExistingFeatureTags() { return existingFeatureTags; }
    public void setExistingFeatureTags(List<String> existingFeatureTags) { this.existingFeatureTags = existingFeatureTags; }
}