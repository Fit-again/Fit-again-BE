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
@Schema(description = "REFORM 시뮬레이션 STEP1 \"해체\" 이미지에 표시되는 손상/변경 부위 원형 마커 하나. " +
        "좌표는 원본 정면 이미지 기준 0~100 비율이며, 프론트에서 직접 그릴 때 참고할 수 있도록 원본 데이터로 함께 제공됩니다")
public class DamageMarkerDto {

    @Schema(description = "마커 번호 (1부터)", example = "1")
    private int number;

    @Schema(description = "원본 이미지 가로 기준 x좌표 비율 (0~100)", example = "15.0")
    private double xPercent;

    @Schema(description = "원본 이미지 세로 기준 y좌표 비율 (0~100)", example = "75.0")
    private double yPercent;

    @Schema(description = "이 마커가 가리키는 부위에 대한 짧은 설명", example = "모서리 마모")
    private String label;
}
