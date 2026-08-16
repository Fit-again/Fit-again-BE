package com.fitagain.domain.consultation.controller;

import com.fitagain.domain.consultation.dto.request.ConsultationReqDTO;
import com.fitagain.domain.consultation.dto.response.ConsultationResDTO;
import com.fitagain.domain.consultation.service.command.ConsultationCommandService;
import com.fitagain.global.common.CustomResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/tasks")
@Tag(name = "Consultation API", description = "공식 상담 신청 API")
public class ConsultationController {

    private final ConsultationCommandService consultationCommandService;

    @Operation(summary = "공식 상담 신청", description = "최종 리포트를 확인한 후 공식 상담을 신청합니다.")
    @PostMapping("/{taskId}/consultations")
    public CustomResponse<ConsultationResDTO.CreateResDTO> createConsultation(
            @PathVariable Long taskId,
            @Valid @RequestBody ConsultationReqDTO.CreateReqDTO reqDTO) {
        
        ConsultationResDTO.CreateResDTO resDTO = consultationCommandService.createConsultation(taskId, reqDTO);
        return CustomResponse.onSuccess(resDTO);
    }
}
