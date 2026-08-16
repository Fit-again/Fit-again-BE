package com.fitagain.domain.task.service.command;

import com.fitagain.domain.task.dto.request.TaskCreateReqDTO;

public interface TaskCommandService {

    Long createTask(TaskCreateReqDTO.CreateReqDTO reqDTO);

    void processDiagnosisAsync(Long taskId);
}
