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
@Schema(description = "이미지 하나와 그에 대한 불릿 설명")
public class ImagePointsDto {

    @Schema(description = "이미지 URL",
            example = "https://fitagain-images.s3.ap-northeast-2.amazonaws.com/tasks/front/uuid.jpg")
    private String imageUrl;

    @Schema(description = "이미지를 설명하는 불릿 포인트 (예: 불편 포인트 또는 개선 포인트)")
    private List<String> points;
}