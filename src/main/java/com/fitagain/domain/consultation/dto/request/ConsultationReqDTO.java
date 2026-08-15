package com.fitagain.domain.consultation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.List;

public class ConsultationReqDTO {

    @Builder
    @Schema(name = "ConsultationCreateReqDTO", description = "상담 신청 요청 DTO")
    public record CreateReqDTO(
            @NotBlank(message = "이름은 필수 입력값입니다.")
            @Schema(description = "신청자 이름", example = "홍길동")
            String userName,

            @NotBlank(message = "연락처는 필수 입력값입니다.")
            @Schema(description = "연락처", example = "010-1234-5678")
            String phoneNumber,

            @Schema(description = "[업사이클링] 희망 제품 목록", example = "[\"미니 크로스백\", \"카드지갑\"]")
            List<String> desiredUpcyclingProducts,

            @Schema(description = "[업사이클링] 가장 중요하게 생각하는 부분", example = "가벼운 무게")
            String importantAspect,

            @Schema(description = "추가 요청사항", example = "스트랩 길이 조절 가능 여부가 궁금해요.")
            String additionalRequest,

            @NotNull(message = "개인정보 수집 동의 여부는 필수 입력값입니다.")
            @AssertTrue(message = "개인정보 수집 및 이용에 동의해야 합니다.")
            @Schema(description = "개인정보 수집 및 이용 동의 여부", example = "true")
            Boolean privacyAgreed
    ) {
    }
}
