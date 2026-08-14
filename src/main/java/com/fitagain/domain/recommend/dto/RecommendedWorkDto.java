package com.fitagain.domain.recommend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "REFORM 추천에 포함되는 구체적인 리폼 작업 항목")
public class RecommendedWorkDto {

    @Schema(description = "리폼 작업 제목", example = "경량 스트랩 교체")
    private String title;

    @Schema(description = "리폼 작업 설명", example = "무겁고 흘러내리는 기존 스트랩을 가벼운 소재로 교체해 어깨 부담을 줄여줍니다.")
    private String description;

    public RecommendedWorkDto() {}

    public RecommendedWorkDto(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}