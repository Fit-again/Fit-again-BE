//package com.fitagain.domain.recommend.service;
//
//import tools.jackson.databind.ObjectMapper;
//import com.fitagain.domain.recommend.dto.*;
//import com.fitagain.domain.recommend.exception.TaskException;
//import com.fitagain.domain.task.entity.DiagnosisTask;
//import com.fitagain.domain.task.enums.TaskStatus;
//import com.fitagain.domain.task.repository.DiagnosisTaskRepository;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.scheduling.annotation.Async;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.List;
//import java.util.Map;
//
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class RecommendationService {
//
//    private final DiagnosisTaskRepository diagnosisTaskRepository;
//    private final ObjectMapper objectMapper;
//
//    @Transactional
//    public Long requestRecommendation(Long taskId) {
//        DiagnosisTask task = diagnosisTaskRepository.findById(taskId)
//                .orElseThrow(TaskException::notDiagnosedYet);
//
//        if (task.getStatus() != TaskStatus.DIAGNOSED) {
//            throw TaskException.notDiagnosedYet();
//        }
//
//        task.startRecommending();
//        diagnosisTaskRepository.save(task);
//
//        generateRecommendationAsync(taskId);
//
//        return task.getId();
//    }
//
//    @Transactional(readOnly = true)
//    public RecommendationResultDto getRecommendation(Long taskId) {
//        DiagnosisTask task = diagnosisTaskRepository.findById(taskId)
//                .orElseThrow(TaskException::notFound);
//
//        if (task.getStatus() != TaskStatus.RECOMMENDED) {
//            return new RecommendationResultDto(task.getStatus().name(), null);
//        }
//
//        RecommendationDetailDto detail = objectMapper.convertValue(
//                task.getRecommendationResult(), RecommendationDetailDto.class
//        );
//        return new RecommendationResultDto(TaskStatus.RECOMMENDED.name(), detail);
//    }
//
//
////    @Async
////    @Transactional
////    public void generateRecommendationAsync(Long taskId) {
////        try {
////            // TODO: task.getDiagnosisResult() + keywords/description을 프롬프트로 만들어
////            // Gemini API 호출 -> recommendationType 판단 + step별/after 이미지 생성
////            RecommendationDetailDto detail = buildMockRecommendation();
////
////            Map<String, Object> resultMap = objectMapper.convertValue(detail, Map.class);
////
////            DiagnosisTask task = diagnosisTaskRepository.findById(taskId)
////                    .orElseThrow(TaskException::notFound);
////
////            task.completeRecommendation(resultMap);
////            diagnosisTaskRepository.save(task);
////
////        } catch (Exception e) {
////            log.error("추천/시뮬레이션 생성 실패. taskId={}", taskId, e);
////            diagnosisTaskRepository.findById(taskId).ifPresent(task -> {
////                task.failRecommendation(e.getMessage());
////                diagnosisTaskRepository.save(task);
////            });
////        }
////    }
//
////    private RecommendationDetailDto buildMockRecommendation() {
////        List<StepDto> steps = List.of(
////                new StepDto(1, "해체",
////                        List.of("교체 대상 부위 확인", "기존 부품 분리 준비"),
////                        "https://s3.../step1-original.jpg"),
////                new StepDto(2, "교체",
////                        List.of("경량 스트랩 교체", "어깨 패드 추가"),
////                        "https://s3.../step2-strap.jpg"),
////                new StepDto(3, "보강",
////                        List.of("모서리 보수", "가죽 마감 보강"),
////                        "https://s3.../step3-corner.jpg"),
////                new StepDto(4, "완성",
////                        List.of("최종 리폼 결과 확인", "개선된 사용 모습 미리보기"),
////                        "https://s3.../step4-generated-after.jpg")
////        );
////
////        BeforeAfterDto beforeAfter = new BeforeAfterDto(
////                new ImagePointsDto(
////                        "https://s3.../step1-original.jpg",
////                        List.of("스트랩이 자주 흘러내림", "장시간 착용 시 어깨 부담", "모서리 마모로 외관 손상")
////                ),
////                new ImagePointsDto(
////                        "https://s3.../step4-generated-after.jpg",
////                        List.of("경량 스트랩으로 착용감 개선", "어깨 패드 추가로 압력 분산", "모서리 보강으로 외관 복원")
////                )
////        );
////
////        return new RecommendationDetailDto(
////                "REFORM",
////                "사용자 애착도가 높은 제품입니다. 구조적 손상이 크지 않아 리폼 효과가 높습니다. "
////                        + "스트랩과 마모 부위를 개선하면 더 적합하게 사용할 수 있어요.",
////                new SimulationDto(steps, beforeAfter)
////        );
////    }
//}

package com.fitagain.domain.recommend.service;

import com.fitagain.domain.recommend.client.OpenAiRecommendationClient;
import com.fitagain.domain.recommend.dto.*;
import com.fitagain.domain.recommend.exception.TaskException;
import com.fitagain.domain.task.entity.DiagnosisTask;
import com.fitagain.domain.task.enums.TaskStatus;
import com.fitagain.domain.task.repository.DiagnosisTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final DiagnosisTaskRepository diagnosisTaskRepository;
    private final ObjectMapper objectMapper;
    private final OpenAiRecommendationClient openAiRecommendationClient;

    @Transactional
    public Long requestRecommendation(Long taskId) {
        DiagnosisTask task = diagnosisTaskRepository.findById(taskId)
                .orElseThrow(TaskException::notDiagnosedYet);

        if (task.getStatus() != TaskStatus.DIAGNOSED) {
            throw TaskException.notDiagnosedYet();
        }

        task.startRecommending();
        diagnosisTaskRepository.save(task);

        generateRecommendationAsync(taskId);

        return task.getId();
    }

    @Transactional(readOnly = true)
    public RecommendationResultDto getRecommendation(Long taskId) {
        DiagnosisTask task = diagnosisTaskRepository.findById(taskId)
                .orElseThrow(TaskException::notFound);

        if (task.getStatus() != TaskStatus.RECOMMENDED) {
            return new RecommendationResultDto(task.getStatus().name(), null);
        }

        RecommendationDetailDto detail = objectMapper.convertValue(
                task.getRecommendationResult(), RecommendationDetailDto.class
        );
        return new RecommendationResultDto(TaskStatus.RECOMMENDED.name(), detail);
    }

    @Async
    @Transactional
    public void generateRecommendationAsync(Long taskId) {
        try {
            DiagnosisTask task = diagnosisTaskRepository.findById(taskId)
                    .orElseThrow(TaskException::notFound);

            // 1) OpenAI로 추천 방향/이유 판단
            RecommendationJudgmentDto judgment = openAiRecommendationClient.judge(
                    task.getProductType(),
                    task.getKeywords(),
                    task.getDescription(),
                    task.getDiagnosisResult()
            );

            // 2) 시뮬레이션(steps/beforeAfter)은 아직 Gemini 붙기 전이라 템플릿 유지
            SimulationDto simulation = buildTemplateSimulation();

            RecommendationDetailDto detail = new RecommendationDetailDto(
                    judgment.getRecommendationType(),
                    judgment.getReason(),
                    simulation
            );

            Map<String, Object> resultMap = objectMapper.convertValue(detail, Map.class);

            task.completeRecommendation(resultMap);
            diagnosisTaskRepository.save(task);

        } catch (Exception e) {
            log.error("추천/시뮬레이션 생성 실패. taskId={}", taskId, e);
            diagnosisTaskRepository.findById(taskId).ifPresent(task -> {
                task.failRecommendation(e.getMessage());
                diagnosisTaskRepository.save(task);
            });
        }
    }

    // TODO: Gemini 연동되면 이 템플릿 대신 실제 생성 이미지로 교체
    private SimulationDto buildTemplateSimulation() {
        List<StepDto> steps = List.of(
                new StepDto(1, "해체",
                        List.of("교체 대상 부위 확인", "기존 부품 분리 준비"),
                        "https://s3.../step1-original.jpg"),
                new StepDto(2, "교체",
                        List.of("경량 스트랩 교체", "어깨 패드 추가"),
                        "https://s3.../step2-strap.jpg"),
                new StepDto(3, "보강",
                        List.of("모서리 보수", "가죽 마감 보강"),
                        "https://s3.../step3-corner.jpg"),
                new StepDto(4, "완성",
                        List.of("최종 리폼 결과 확인", "개선된 사용 모습 미리보기"),
                        "https://s3.../step4-generated-after.jpg")
        );

        BeforeAfterDto beforeAfter = new BeforeAfterDto(
                new ImagePointsDto(
                        "https://s3.../step1-original.jpg",
                        List.of("스트랩이 자주 흘러내림", "장시간 착용 시 어깨 부담", "모서리 마모로 외관 손상")
                ),
                new ImagePointsDto(
                        "https://s3.../step4-generated-after.jpg",
                        List.of("경량 스트랩으로 착용감 개선", "어깨 패드 추가로 압력 분산", "모서리 보강으로 외관 복원")
                )
        );

        return new SimulationDto(steps, beforeAfter);
    }
}