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
@Schema(description = "RESELL 추천에서, 현재 니즈에 맞는 대안 제품 추천")
public class AlternativeProductSuggestionDto {

    @Schema(description = "대안 제품 유형", example = "크로스백", allowableValues = {"토트백", "숄더백", "크로스백", "백팩", "파우치"})
    private String productType;

    @Schema(description = "이 제품을 대안으로 추천하는 짧은 이유")
    private String reason;
}