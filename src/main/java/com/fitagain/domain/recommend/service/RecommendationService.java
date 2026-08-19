package com.fitagain.domain.recommend.service;

import com.fitagain.domain.recommend.client.GeminiImageClient;
import com.fitagain.domain.recommend.client.OpenAiRecommendationClient;
import com.fitagain.domain.recommend.dto.*;
import com.fitagain.domain.recommend.exception.TaskException;
import com.fitagain.domain.task.entity.DiagnosisTask;
import com.fitagain.domain.task.enums.TaskStatus;
import com.fitagain.domain.task.repository.DiagnosisTaskRepository;
import com.fitagain.global.util.ImageMarkerRenderer;
import com.fitagain.global.util.S3Uploader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
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
    private final GeminiImageClient geminiImageClient;
    private final ImageMarkerRenderer imageMarkerRenderer;
    private final S3Uploader s3Uploader;
    private final ApplicationContext applicationContext;

    @Transactional
    public Long requestRecommendation(Long taskId) {
        DiagnosisTask task = diagnosisTaskRepository.findById(taskId)
                .orElseThrow(TaskException::notDiagnosedYet);

        if (task.getStatus() == TaskStatus.RECOMMENDING || task.getStatus() == TaskStatus.RECOMMENDED) {
            // 선행 생성 스케줄러가 이미 시작했거나 끝났음 - 그대로 반환, 재시작하지 않음
            return task.getId();
        }

        if (task.getStatus() != TaskStatus.DIAGNOSED) {
            throw TaskException.notDiagnosedYet();
        }

        task.startRecommending();
        diagnosisTaskRepository.save(task);

        applicationContext.getBean(RecommendationService.class).generateRecommendationAsync(taskId);

        return task.getId();
    }

    @Scheduled(fixedDelay = 3000)
    @Transactional
    public void autoStartRecommendationForDiagnosedTasks() {
        List<DiagnosisTask> readyTasks = diagnosisTaskRepository.findByStatus(TaskStatus.DIAGNOSED);
        for (DiagnosisTask task : readyTasks) {
            try {
                task.startRecommending(); // 상태를 RECOMMENDING으로 선점 (중복 실행 방지)
                diagnosisTaskRepository.save(task);
                applicationContext.getBean(RecommendationService.class).generateRecommendationAsync(task.getId());
                log.info("진단 완료 감지 - 추천 자동 시작. taskId={}", task.getId());
            } catch (Exception e) {
                log.error("추천 자동 시작 실패. taskId={}", task.getId(), e);
            }
        }
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
                    task.getDiagnosisResult(),
                    task.getFrontImageUrl(),
                    task.getDamageImageUrls()
            );

            List<RankedRecommendationDto> rankings = judgment.getRankings();

            // 정면 원본 이미지는 recommendationType 공통으로 노출 (RESELL/UPCYCLING은 이미지 필드가 아예 없었음)
            rankings.forEach(r -> r.setFrontImageUrl(task.getFrontImageUrl()));

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
        List<RecommendedWorkDto> works = reform.getRecommendedWorks();
        List<DamageMarkerDto> damageMarkers = reform.getDamageMarkers() == null ? List.of() : reform.getDamageMarkers();

        List<RecommendedWorkDto> replaceWorks = works.stream()
                .filter(w -> "REPLACE".equals(w.getCategory()))
                .toList();
        List<RecommendedWorkDto> reinforceWorks = works.stream()
                .filter(w -> "REINFORCE".equals(w.getCategory()))
                .toList();

        CompletableFuture<String> replaceFuture = replaceWorks.isEmpty()
                ? CompletableFuture.completedFuture(null)
                : CompletableFuture.supplyAsync(() -> generateReformStepImage(task, replaceWorks));

        CompletableFuture<String> reinforceFuture = reinforceWorks.isEmpty()
                ? CompletableFuture.completedFuture(null)
                : CompletableFuture.supplyAsync(() -> generateReformStepImage(task, reinforceWorks));

        CompletableFuture<String> finalFuture = CompletableFuture.supplyAsync(() -> generateReformStepImage(task, works));

        CompletableFuture<String> step1Future = damageMarkers.isEmpty()
                ? CompletableFuture.completedFuture(beforeImageUrl)
                : CompletableFuture.supplyAsync(() -> {
                    byte[] markedImageBytes = imageMarkerRenderer.renderMarkers(beforeImageUrl, damageMarkers);
                    return s3Uploader.upload(markedImageBytes, "image/png", "recommendations/reform");
                });

        CompletableFuture.allOf(replaceFuture, reinforceFuture, finalFuture, step1Future).join();

        String step1ImageUrl = step1Future.join();
        String replaceImageUrl = replaceFuture.join();
        String reinforceImageUrl = reinforceFuture.join();
        String afterImageUrl = finalFuture.join();

        List<StepDto> steps = new ArrayList<>();
        int stepNo = 1;
        steps.add(new StepDto(stepNo++, "해체",
                List.of("변경이 필요한 부위 확인", "리폼 작업 준비"), step1ImageUrl));
        if (replaceImageUrl != null) {
            steps.add(new StepDto(stepNo++, "교체",
                    replaceWorks.stream().map(RecommendedWorkDto::getTitle).toList(), replaceImageUrl));
        }
        if (reinforceImageUrl != null) {
            steps.add(new StepDto(stepNo++, "보강",
                    reinforceWorks.stream().map(RecommendedWorkDto::getTitle).toList(), reinforceImageUrl));
        }
        steps.add(new StepDto(stepNo, "완성",
                List.of("최종 리폼 결과 확인", "개선된 사용 모습 미리보기"), afterImageUrl));

        BeforeAfterDto beforeAfter = new BeforeAfterDto(
                new ImagePointsDto(beforeImageUrl,
                        List.of("스트랩이 자주 흘러내림", "장시간 착용 시 어깨 부담", "모서리 마모로 외관 손상")),
                new ImagePointsDto(afterImageUrl,
                        List.of("경량 스트랩으로 착용감 개선", "어깨 패드 추가로 압력 분산", "모서리 보강으로 외관 복원"))
        );

        List<String> damageImageUrls = task.getDamageImageUrls() == null ? List.of() : task.getDamageImageUrls();

        reform.setSimulation(new SimulationDto(steps, beforeAfter, damageImageUrls, damageMarkers));
        reform.setResultImageUrl(afterImageUrl);
        reform.setDamageMarkers(null);
    }

    private String generateReformStepImage(DiagnosisTask task, List<RecommendedWorkDto> works) {
        byte[] imageBytes = geminiImageClient.generateReformAfterImage(
                task.getFrontImageUrl(),
                task.getDetailImageUrls(),
                works,
                task.getDiagnosisResult()
        );
        return s3Uploader.upload(imageBytes, "image/png", "recommendations/reform");
    }

    private void fillUpcyclingImage(DiagnosisTask task, UpcyclingCandidateDto candidate) {
        byte[] imageBytes = geminiImageClient.generateUpcyclingImage(
                task.getFrontImageUrl(),
                task.getDetailImageUrls(),
                candidate,
                task.getDiagnosisResult()
        );
        candidate.setImageUrl(s3Uploader.upload(imageBytes, "image/png", "recommendations/upcycling"));
    }
}
