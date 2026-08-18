# 추천 결과 선행 생성 (진단 완료 즉시 백그라운드로 미리 시작)

> 이 문서 보고 아래 내용 그대로 적용해줘.
> 지금은 사용자가 3단계에서 "추천 결과 보기" 버튼을 눌러야(POST 호출) 그때부터
> 추천/이미지 생성이 시작됨. 이걸 "사용자가 결국 누를 거다"라고 가정하고,
> 진단(AI 분석)이 DIAGNOSED로 끝나는 즉시 recommend 도메인이 알아서 먼저
> 백그라운드로 생성을 시작해두는 방식으로 바꿔줘. 다른 팀원(진단 담당) 코드는
> 건드리지 않고, recommend 도메인 안에서만 DB를 주기적으로 확인하는 방식으로 구현.

## 방식: 짧은 주기 스케줄러로 DIAGNOSED 작업을 자동으로 감지해서 선점 시작

## 1. `@EnableScheduling` 활성화

메인 애플리케이션 클래스(`FitagainApplication.java`) 또는 별도 `@Configuration` 클래스에
`@EnableScheduling` 추가 (이미 다른 곳에 있으면 중복 추가하지 말 것, 프로젝트 전체 검색해서 확인).

```java
@EnableScheduling
@SpringBootApplication
public class FitagainApplication {
    ...
}
```

## 2. `DiagnosisTaskRepository`에 조회 메서드 추가

```java
List<DiagnosisTask> findByStatus(TaskStatus status);
```
(기존 인터페이스에 메서드 시그니처만 추가하면 Spring Data JPA가 자동 구현함)

## 3. `RecommendationService`에 스케줄러 메서드 추가

```java
@Scheduled(fixedDelay = 3000) // 3초마다 확인
@Transactional
public void autoStartRecommendationForDiagnosedTasks() {
    List<DiagnosisTask> readyTasks = diagnosisTaskRepository.findByStatus(TaskStatus.DIAGNOSED);
    for (DiagnosisTask task : readyTasks) {
        try {
            task.startRecommending(); // 상태를 RECOMMENDING으로 선점 (중복 실행 방지)
            diagnosisTaskRepository.save(task);
            generateRecommendationAsync(task.getId());
            log.info("진단 완료 감지 - 추천 자동 시작. taskId={}", task.getId());
        } catch (Exception e) {
            log.error("추천 자동 시작 실패. taskId={}", task.getId(), e);
        }
    }
}
```

**주의**: `findByStatus` 조회와 `startRecommending()` 저장 사이에 시간차가 있어서, 여러 인스턴스가
동시에 떠 있으면 같은 작업을 중복 시작할 가능성이 이론상 있음. 지금 로컬/단일 인스턴스 배포
환경에서는 문제 없지만, 나중에 서버를 여러 대로 늘리게 되면 낙관적 락(`@Version`)이나
DB 레벨 `UPDATE ... WHERE status = 'DIAGNOSED'` 방식으로 바꿔야 함 (지금은 스킵해도 됨).

## 4. `POST /api/v1/tasks/{taskId}/recommendations`를 멱등(idempotent)하게 수정

이제 사용자가 버튼을 누르기 전에 이미 스케줄러가 추천을 시작해놨을 수 있으므로,
POST가 호출됐을 때 상태를 다시 확인해서:
- 이미 `RECOMMENDING`이거나 `RECOMMENDED`면 → 에러 던지지 말고 그냥 `taskId` 그대로 반환 (재시작하지 않음)
- `DIAGNOSED`면 → 기존처럼 시작 (스케줄러가 아직 못 잡은 경우, 예: 방금 진단이 끝나서 3초 이내인 경우 대비)
- `PENDING`/`DIAGNOSING`(진단 자체가 안 끝남)이면 → 기존처럼 `TASK400`

```java
@Transactional
public Long requestRecommendation(Long taskId) {
    DiagnosisTask task = diagnosisTaskRepository.findById(taskId)
            .orElseThrow(TaskException::notDiagnosedYet);

    if (task.getStatus() == TaskStatus.RECOMMENDING || task.getStatus() == TaskStatus.RECOMMENDED) {
        // 이미 스케줄러가 선행 생성 중이거나 끝났음 - 그대로 반환, 재시작 안 함
        return task.getId();
    }

    if (task.getStatus() != TaskStatus.DIAGNOSED) {
        throw TaskException.notDiagnosedYet();
    }

    task.startRecommending();
    diagnosisTaskRepository.save(task);
    generateRecommendationAsync(task.getId());

    return task.getId();
}
```

## 5. GET 쪽은 변경 없음

`GET /api/v1/tasks/{taskId}/recommendations`는 지금 로직 그대로 유지. 프론트는 여전히
"추천 결과 보기" 클릭 → POST 호출(멱등이라 이미 끝나있어도 안전) → GET 폴링, 이 순서로
쓰면 되고, 다만 이제 대부분의 경우 POST 직후 GET에서 바로 `RECOMMENDED`가 나올 가능성이 높아짐
(진단 끝나고 사용자가 화면 보는 동안 이미 백그라운드에서 다 끝나 있을 것이므로).

## 확인할 것
- 스케줄러가 실제로 3초마다 도는지 로그로 확인 (`autoStartRecommendationForDiagnosedTasks` 안에 로그 찍혀 있음)
- 기존에 수동으로 taskId 상태를 `DIAGNOSED`로 SQL UPDATE 해서 테스트하던 방식이면, 이제 **SQL로 DIAGNOSED로 바꾸자마자 몇 초 안에 자동으로 RECOMMENDING → RECOMMENDED까지 진행**되는 걸 확인할 수 있음 (POST 안 날려도 됨). 테스트 방식이 이렇게 바뀌는 것도 참고.
- 컴파일: `./gradlew compileJava --console=plain`
