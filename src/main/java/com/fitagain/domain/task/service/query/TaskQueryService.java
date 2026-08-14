package com.fitagain.domain.task.service.query;

import com.fitagain.domain.task.dto.response.DiagnosisResDTO;

public interface TaskQueryService {

    DiagnosisResDTO.DiagnosisResultDTO getDiagnosisStatus(Long taskId);

}
