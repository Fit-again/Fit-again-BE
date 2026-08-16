package com.fitagain.domain.consultation.service.command;

import com.fitagain.domain.consultation.dto.request.ConsultationReqDTO;
import com.fitagain.domain.consultation.dto.response.ConsultationResDTO;

public interface ConsultationCommandService {
    ConsultationResDTO.CreateResDTO createConsultation(Long taskId, ConsultationReqDTO.CreateReqDTO reqDTO);
}
