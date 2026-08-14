package com.fitagain.domain.recommend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "리폼의 Before/After 이미지 및 포인트 비교 데이터")
public class BeforeAfterDto {

    @Schema(description = "Before(원본) 이미지 및 불편 포인트 불릿")
    private ImagePointsDto before;

    @Schema(description = "After(Gemini 생성) 이미지 및 개선 포인트 불릿")
    private ImagePointsDto after;
}