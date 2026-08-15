package com.fitagain.domain.consultation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public class ConsultationResDTO {

    @Builder
    @Schema(name = "ConsultationCreateResDTO", description = "상담 신청 결과 응답 DTO")
    public record CreateResDTO(
            @Schema(description = "생성된 상담 신청 ID", example = "1")
            Long consultationId
    ) {
    }
}
