package com.fitagain.domain.consultation.service.command;

import com.fitagain.domain.consultation.dto.request.ConsultationReqDTO;
import com.fitagain.domain.consultation.dto.response.ConsultationResDTO;
import com.fitagain.domain.consultation.entity.Consultation;
import com.fitagain.domain.consultation.repository.ConsultationRepository;
import com.fitagain.domain.task.entity.DiagnosisTask;
import com.fitagain.domain.task.repository.DiagnosisTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fitagain.domain.consultation.exception.ConsultationErrorCode;
import com.fitagain.global.common.exception.CustomException;

@Service
@RequiredArgsConstructor
public class ConsultationCommandServiceImpl implements ConsultationCommandService {

    private final ConsultationRepository consultationRepository;
    private final DiagnosisTaskRepository diagnosisTaskRepository;

    @Override
    @Transactional
    public ConsultationResDTO.CreateResDTO createConsultation(Long taskId, ConsultationReqDTO.CreateReqDTO reqDTO) {
        if (consultationRepository.existsByDiagnosisTaskId(taskId)) {
            throw new CustomException(ConsultationErrorCode.ALREADY_EXISTS);
        }

        DiagnosisTask task = diagnosisTaskRepository.findById(taskId)
                .orElseThrow(() -> new CustomException(ConsultationErrorCode.TASK_NOT_FOUND));

        Consultation consultation = Consultation.builder()
                .diagnosisTask(task)
                .userName(reqDTO.userName())
                .phoneNumber(reqDTO.phoneNumber())
                .desiredUpcyclingProducts(reqDTO.desiredUpcyclingProducts())
                .importantAspect(reqDTO.importantAspect())
                .additionalRequest(reqDTO.additionalRequest())
                .privacyAgreed(reqDTO.privacyAgreed())
                .build();

        Consultation savedConsultation = consultationRepository.save(consultation);

        return ConsultationResDTO.CreateResDTO.builder()
                .consultationId(savedConsultation.getId())
                .build();
    }
}
