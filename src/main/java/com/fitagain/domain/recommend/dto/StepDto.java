package com.fitagain.domain.recommend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "리폼 시뮬레이션의 단계 하나 (해체/교체/보강/완성 중 하나)")
public class StepDto {

    @Schema(description = "단계 번호 (1~4)", example = "1")
    private int step;

    @Schema(description = "단계 제목", example = "해체")
    private String title;

    @Schema(description = "이 단계에 대한 불릿 설명")
    private List<String> description;

    @Schema(description = "이 단계를 나타내는 이미지 URL (1~2단계는 정면 원본 사진, 3~4단계는 Gemini가 생성한 완성 이미지 사용)",
            example = "https://fitagain-images.s3.ap-northeast-2.amazonaws.com/recommendations/reform/uuid.png")
    private String imageUrl;
}