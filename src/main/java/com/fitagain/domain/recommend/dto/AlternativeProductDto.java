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
@Schema(description = "RESELL 추천에서, 다음 구매로 추천하는 대안 제품 하나")
public class AlternativeProductDto {

    @Schema(description = "대안 제품 유형", example = "크로스백",
            allowableValues = {"토트백", "숄더백", "크로스백", "백팩", "파우치", "기타"})
    private String productType;

    @Schema(description = "이 제품을 추천하는 이유 (해시태그 형식, 최대 4개)", example = "[\"#가벼움\", \"#안정적인착용\"]")
    private List<String> hashtags;
}
