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
@Schema(description = "RESELL 추천에서, 이 제품과 잘 맞는 사용자군 하나")
public class SuitableUserDto {

    @Schema(description = "사용자군 제목", example = "가볍게 외출하는 간결한 스타일의 사용자")
    private String title;

    @Schema(description = "이 사용자군에게 어울리는 이유")
    private String description;

    @Schema(description = "관련 해시태그 목록", example = "[\"#간결한 소지품\", \"#짧은 외출\"]")
    private List<String> hashtags;
}