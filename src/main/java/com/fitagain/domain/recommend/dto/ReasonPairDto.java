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
@Schema(description = "업사이클링 후보 품목 하나에 대한 불편(문제)-수정(해결) 쌍. \"왜 이 방향을 제안했을까요?\" 섹션에 사용됩니다")
public class ReasonPairDto {

    @Schema(description = "사용자가 느끼는 불편", example = "무게가 부담스러워요")
    private String problem;

    @Schema(description = "이 업사이클링으로 해결되는 방식", example = "전체 크기를 필요한 소지품만 휴대할 수 있는 형태로 전환해요.")
    private String solution;
}
