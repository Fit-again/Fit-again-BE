package com.fitagain.domain.recommend.service;

import com.fitagain.domain.recommend.client.HuggingFaceImageClient;
import com.fitagain.domain.recommend.client.OpenAiRecommendationClient;
import com.fitagain.domain.recommend.dto.*;
import com.fitagain.domain.recommend.exception.TaskException;
import com.fitagain.domain.task.entity.DiagnosisTask;
import com.fitagain.domain.task.enums.TaskStatus;
import com.fitagain.domain.task.repository.DiagnosisTaskRepository;
import com.fitagain.global.util.S3Uploader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final DiagnosisTaskRepository diagnosisTaskRepository;
    private final ObjectMapper objectMapper;
    private final OpenAiRecommendationClient openAiRecommendationClient;
    private final HuggingFaceImageClient huggingFaceImageClient;
    private final S3Uploader s3Uploader;

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

        return objectMapper.convertValue(task.getRecommendationResult(), RecommendationResultDto.class);
    }

    @Async
    @Transactional
    public void generateRecommendationAsync(Long taskId) {
        try {
            DiagnosisTask task = diagnosisTaskRepository.findById(taskId)
                    .orElseThrow(TaskException::notFound);

            RecommendationJudgmentDto judgment = openAiRecommendationClient.judge(
                    task.getProductType(),
                    task.getKeywords(),
                    task.getDescription(),
                    task.getDiagnosisResult()
            );

            List<RankedRecommendationDto> rankings = judgment.getRankings();

            // REFORM 시뮬레이션 이미지 생성과 UPCYCLING 후보 이미지 생성을 병렬로 실행 (순차 대비 5~8초 목표)
            RankedRecommendationDto reform = findByType(rankings, "REFORM");
            RankedRecommendationDto upcycling = findByType(rankings, "UPCYCLING");

            CompletableFuture<Void> reformFuture = reform == null
                    ? CompletableFuture.completedFuture(null)
                    : CompletableFuture.runAsync(() -> fillReformSimulation(task, reform));

            List<CompletableFuture<Void>> upcyclingFutures = upcycling == null
                    ? List.of()
                    : upcycling.getUpcyclingCandidates().stream()
                        .map(candidate -> CompletableFuture.runAsync(() -> fillUpcyclingImage(task, candidate)))
                        .toList();

            CompletableFuture.allOf(
                    java.util.stream.Stream.concat(java.util.stream.Stream.of(reformFuture), upcyclingFutures.stream())
                            .toArray(CompletableFuture[]::new)
            ).join();

            RecommendationResultDto result = new RecommendationResultDto(TaskStatus.RECOMMENDED.name(), rankings);
            Map<String, Object> resultMap = objectMapper.convertValue(result, Map.class);

            task.completeRecommendation(resultMap);
            diagnosisTaskRepository.save(task);

        } catch (Exception e) {
            log.error("추천/시뮬레이션 생성 실패. taskId={}", taskId, e);
            diagnosisTaskRepository.findById(taskId).ifPresent(t -> {
                t.failRecommendation(e.getMessage());
                diagnosisTaskRepository.save(t);
            });
        }
    }

    private RankedRecommendationDto findByType(List<RankedRecommendationDto> rankings, String type) {
        return rankings.stream()
                .filter(r -> type.equals(r.getRecommendationType()))
                .findFirst()
                .orElse(null);
    }

    private void fillReformSimulation(DiagnosisTask task, RankedRecommendationDto reform) {
        String beforeImageUrl = task.getFrontImageUrl();

        String prompt = buildReformImagePrompt(task, reform.getRecommendedWorks());
        byte[] afterImageBytes = huggingFaceImageClient.generateImage(prompt);
        String afterImageUrl = s3Uploader.upload(afterImageBytes, "image/png", "recommendations/reform");

        List<StepDto> steps = List.of(
                new StepDto(1, "해체",
                        List.of("교체 대상 부위 확인", "기존 부품 분리 준비"), beforeImageUrl),
                new StepDto(2, "교체",
                        List.of("경량 스트랩 교체", "어깨 패드 추가"), beforeImageUrl),
                new StepDto(3, "보강",
                        List.of("모서리 보수", "가죽 마감 보강"), afterImageUrl),
                new StepDto(4, "완성",
                        List.of("최종 리폼 결과 확인", "개선된 사용 모습 미리보기"), afterImageUrl)
        );

        BeforeAfterDto beforeAfter = new BeforeAfterDto(
                new ImagePointsDto(beforeImageUrl,
                        List.of("스트랩이 자주 흘러내림", "장시간 착용 시 어깨 부담", "모서리 마모로 외관 손상")),
                new ImagePointsDto(afterImageUrl,
                        List.of("경량 스트랩으로 착용감 개선", "어깨 패드 추가로 압력 분산", "모서리 보강으로 외관 복원"))
        );

        reform.setSimulation(new SimulationDto(steps, beforeAfter));
    }

    private void fillUpcyclingImage(DiagnosisTask task, UpcyclingCandidateDto candidate) {
        String prompt = buildUpcyclingImagePrompt(task, candidate);
        byte[] imageBytes = huggingFaceImageClient.generateImage(prompt);
        candidate.setImageUrl(s3Uploader.upload(imageBytes, "image/png", "recommendations/upcycling"));
    }

    // TODO: diagnosisResult에서 색상/재질 필드 확인되면 프롬프트에 추가
    private String buildReformImagePrompt(DiagnosisTask task, List<RecommendedWorkDto> works) {
        String workSummary = works.stream()
                .map(RecommendedWorkDto::getDescription)
                .collect(java.util.stream.Collectors.joining(" "));

        return """
                A high-quality product photo of a %s bag,
                after the following repairs/reform have been applied: %s.
                Studio lighting, clean beige background, realistic leather texture, no text, no watermark.
                """.formatted(task.getProductType(), workSummary);
    }

    private String buildUpcyclingImagePrompt(DiagnosisTask task, UpcyclingCandidateDto candidate) {
        return """
                A high-quality product photo of a %s,
                upcycled/repurposed from an old %s bag, reusing its leather and hardware details.
                %s
                Studio lighting, clean beige background, realistic leather texture, no text, no watermark.
                """.formatted(candidate.getItemName(), task.getProductType(), candidate.getDescription());
    }
}
