package com.fitagain.domain.task.controller;

import com.fitagain.domain.task.dto.request.TaskCreateReqDTO;
import com.fitagain.domain.task.dto.response.DiagnosisResDTO;
import com.fitagain.domain.task.dto.response.TaskCreateResDTO;
import com.fitagain.domain.task.service.command.TaskCommandService;
import com.fitagain.domain.task.service.query.TaskQueryService;
import com.fitagain.global.common.CustomResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI Task API", description = "AI 진단 분석 및 작업 관리 API")
@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskCommandService taskCommandService;
    private final TaskQueryService taskQueryService;

    @Operation(summary = "AI 진단 분석 요청 (Task 생성)", description = "멀티파트 폼 데이터를 통해 제품 종류, 사진(정면/디테일/손상), 키워드 등을 받아 AI 분석을 큐잉하고 Task ID를 반환합니다.")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CustomResponse<TaskCreateResDTO.CreateResDTO> requestDiagnosisTask(
            @Valid @ModelAttribute TaskCreateReqDTO.CreateReqDTO reqDTO
    ) {
        Long taskId = taskCommandService.createTask(reqDTO);
        return CustomResponse.onSuccess(
                "COMMON200",
                "AI 진단 분석 요청이 성공적으로 접수되었습니다.",
                new TaskCreateResDTO.CreateResDTO(taskId)
        );
    }

    @Operation(summary = "진단 분석 작업 상태 및 결과 조회", description = "Task ID를 통해 AI 진단 작업의 현재 상태(DIAGNOSING, DIAGNOSED 등)와 완료 시 분석 결과 JSON을 반환합니다.")
    @GetMapping("/{taskId}/diagnosis")
    public CustomResponse<DiagnosisResDTO.DiagnosisResultDTO> getDiagnosisStatus(
            @PathVariable Long taskId
    ) {
        DiagnosisResDTO.DiagnosisResultDTO result = taskQueryService.getDiagnosisStatus(taskId);
        return CustomResponse.onSuccess(
                "COMMON200",
                "진단 분석 상태 조회가 완료되었습니다.",
                result
        );
    }
}
