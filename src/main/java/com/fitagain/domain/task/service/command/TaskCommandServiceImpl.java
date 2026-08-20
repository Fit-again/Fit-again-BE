package com.fitagain.domain.task.service.command;

import com.fitagain.domain.task.dto.request.TaskCreateReqDTO;
import com.fitagain.domain.task.entity.DiagnosisTask;
import com.fitagain.domain.task.enums.TaskStatus;
import com.fitagain.domain.task.repository.DiagnosisTaskRepository;
import com.fitagain.domain.task.service.ai.DiagnosisAiService;
import com.fitagain.global.util.S3Uploader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskCommandServiceImpl implements TaskCommandService {

    private final DiagnosisTaskRepository taskRepository;
    private final S3Uploader s3Uploader;
    private final DiagnosisAiService diagnosisAiService;
    
    @org.springframework.beans.factory.annotation.Autowired
    @Lazy
    private TaskCommandService self;

    @Override
    public Long createTask(TaskCreateReqDTO.CreateReqDTO reqDTO) {

        try {
            // 1. 손상 부위 사진만 S3 업로드 (정면/디테일은 프론트에서 받은 URL 사용)
            List<String> damageImageUrls = s3Uploader.uploadList(reqDTO.damageImages(), "tasks/damage");

            // 2. DB 엔티티 생성 및 저장 (PENDING 상태)
            DiagnosisTask task = DiagnosisTask.builder()
                    .productType(reqDTO.productType())
                    .frontImageUrl(reqDTO.frontImageUrl())
                    .detailImageUrls(reqDTO.detailImageUrls())
                    .damageImageUrls(damageImageUrls)
                    .keywords(reqDTO.keywords())
                    .description(reqDTO.description())
                    .status(TaskStatus.PENDING)
                    .build();

            DiagnosisTask savedTask = taskRepository.save(task);

            // 3. 비동기 AI 진단 작업 큐잉
            self.processDiagnosisAsync(savedTask.getId());

            return savedTask.getId();

        } catch (IOException e) {
            log.error("S3 파일 다중 업로드 실패", e);
            throw new RuntimeException("사진 업로드 중 오류가 발생했습니다. 다시 시도해주세요.");
        }
    }

    @Override
    @Async
    public void processDiagnosisAsync(Long taskId) {
        log.info("비동기 AI 진단 분석 백그라운드 처리 시작 - taskId: {}", taskId);
        
        DiagnosisTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task를 찾을 수 없습니다."));
                
        try {
            task.startDiagnosis(); // 상태 DIAGNOSING 변경
            taskRepository.save(task); // 트랜잭션 전파 확인 필요
            
            // AI 호출
            Map<String, Object> result = diagnosisAiService.analyzeDamage(
                    task.getProductType(),
                    task.getKeywords(),
                    task.getDescription(),
                    task.getFrontImageUrl(),
                    task.getDetailImageUrls(),
                    task.getDamageImageUrls()
            );
            
            // 완료 상태로 업데이트
            task.completeDiagnosis(result);
            taskRepository.save(task);
            log.info("비동기 AI 진단 분석 완료 - taskId: {}", taskId);
            
        } catch (Exception e) {
            log.error("======================================================");
            log.error("❌ [TaskCommandService] 비동기 AI 진단 분석 실패!");
            log.error("Task ID: {}", taskId);
            log.error("에러 내용: {}", e.getMessage(), e);
            log.error("======================================================");
            task.failDiagnosis(e.getMessage());
            taskRepository.save(task);
        }
    }
}
