package com.fitagain.domain.recommend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "UPCYCLING 추천에 포함되는 업사이클링 후보 품목. 제품마다 다르며, OpenAI가 그때그때 동적으로 결정합니다")
public class UpcyclingCandidateDto {

    @Schema(description = "업사이클링 후보 품목명", example = "미니 크로스백")
    private String itemName;

    @Schema(description = "이 품목에 대한 부가 설명", example = "두꺼운 손잡이 가죽이 작은 크로스백 몸체로 활용하기 좋습니다.")
    private String description;

    @Schema(description = "\"왜 이 방향을 제안했을까요?\" 섹션용 불편-해결 쌍 목록 (품목마다 별도)")
    private List<ReasonPairDto> reasonPairs;

    @Schema(description = "품목별 예상 변화 (Before -> After 형태의 문자열 리스트, 프론트에서 '->' 기준으로 좌/우 분리해 표시)",
            example = "[\"큰 토트백 -> 미니 크로스백\", \"큰 사이즈 -> 컴팩트한 사이즈\", \"숄더 착용 -> 크로스바디 착용\"]")
    private List<String> expectedChanges;

    @Schema(description = "Gemini가 생성한 결과 이미지의 S3 URL",
            example = "https://fitagain-images.s3.ap-northeast-2.amazonaws.com/recommendations/upcycling/uuid.png")
    private String imageUrl;

    public UpcyclingCandidateDto() {}

    public UpcyclingCandidateDto(String itemName, String description, String imageUrl) {
        this.itemName = itemName;
        this.description = description;
        this.imageUrl = imageUrl;
    }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<ReasonPairDto> getReasonPairs() { return reasonPairs; }
    public void setReasonPairs(List<ReasonPairDto> reasonPairs) { this.reasonPairs = reasonPairs; }
    public List<String> getExpectedChanges() { return expectedChanges; }
    public void setExpectedChanges(List<String> expectedChanges) { this.expectedChanges = expectedChanges; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}