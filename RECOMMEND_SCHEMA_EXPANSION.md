# 추천 API 스키마 확장 + 이미지 색상 문제 수정 가이드

> 이 문서 보고 아래 내용 그대로 프로젝트에 반영해줘.
> 지금 recommend 도메인 API(POST/GET /api/v1/tasks/{taskId}/recommendations)의
> 응답 스키마가 너무 단순해서, 프론트 3개 페이지(추천 결과 / 시뮬레이션·미리보기 / 결과 활용)에
> 필요한 필드를 다 못 채우고 있음. 스키마를 확장하고, 이미지 생성 색상 오류도 같이 고쳐야 함.

## 문제 1: 이미지 색상이 원본과 다르게 생성됨

**증상**: 사용자가 핑크색 가방을 넣었는데 AI가 갈색 가방 이미지를 생성함.

**원인 추정**: 지금 REFORM/UPCYCLING 이미지 생성 프롬프트(`RecommendationService`의
`buildReformImagePrompt`/`buildUpcyclingImagePrompt`)에 색상/사이즈/패턴 정보가
전혀 안 들어가고 있음. `task.getProductType()`(예: "토트백")만 넣고 있어서,
AI가 임의로 색을 정해버림.

**해결**:
1. AI 분석(진단) 담당자의 코드/DTO를 확인해서, `diagnosisResult`(Map<String,Object>)
   안에 색상/사이즈/패턴 필드가 있는지 확인. 없다면 진단 담당자에게 색상/사이즈/패턴도
   분석해서 `diagnosisResult`에 포함시켜달라고 요청 필요 (예: `color`, `size`, `pattern` 키).
2. `RecommendationService`의 프롬프트 조합 메서드에서 `task.getDiagnosisResult()`를
   파싱해서 색상/사이즈/패턴 값을 프롬프트 문자열에 명시적으로 포함시킬 것.

```java
// 예시
Map<String, Object> diagnosis = task.getDiagnosisResult();
String color = diagnosis != null ? String.valueOf(diagnosis.getOrDefault("color", "")) : "";
String size = diagnosis != null ? String.valueOf(diagnosis.getOrDefault("size", "")) : "";
String pattern = diagnosis != null ? String.valueOf(diagnosis.getOrDefault("pattern", "")) : "";

String prompt = """
        A high-quality product photo of a %s %s bag, %s size, %s pattern,
        after the following repairs/reform have been applied: %s.
        The bag color and material must closely match the original: %s colored leather.
        Studio lighting, clean beige background, realistic leather texture, no text, no watermark.
        """.formatted(color, task.getProductType(), size, pattern, workSummary, color);
```

색상 관련 문구를 프롬프트 안에서 **두 번 반복 강조**하는 게 실제로 색상 정확도에 도움됨
(모델이 앞부분 지시를 놓치는 경우가 있어서).

3. `diagnosisResult`의 정확한 키 이름은 진단 담당자 코드 확인 후 맞춰서 수정할 것
   (지금은 `color`/`size`/`pattern`으로 가정한 것뿐, 실제 스키마 확인 필수).

## 문제 2: 응답 스키마 확장 (타입별 필요 필드 전체 목록)

아래 표의 `+` 항목은 새로 추가해야 하는 필드, `-` 항목은 이미 있는 데이터를
그대로 재사용하면 되는 필드(원본 이미지 URL 등, DiagnosisTask에서 가져오면 됨).

### REFORM

**AI 추천 결과 페이지**
- 추천 순위 (`rank`) — 기존 유지
- 결과: 리폼 (`recommendationType`) — 기존 유지
- **[신규] 리폼 AI 생성 이미지** (사용자 업로드 정면 이미지 기반 수정본) — REFORM 레벨에 독립 필드로 추가. 지금은 `simulation.beforeAfter.after.imageUrl`에만 있음 → `resultImageUrl` 같은 필드로 최상위에도 노출
- 추천 이유 (`reasons`) — 기존 유지
- 추천 리폼 작업 (`recommendedWorks`) — 기존 유지

**리폼 시뮬레이션 페이지**
- 해체/현재상태 이미지 = 사용자 업로드 정면 이미지 — 기존 `steps[0,1].imageUrl` 재사용 유지
- **[신규] 교체 아이템 AI 생성 이미지** — 지금 `steps[1]`("교체")도 원본 이미지 그대로 쓰고 있음. 실제로 별도 생성해야 함 (예: 새 스트랩/패드 단독 이미지, 또는 교체 후 부분 클로즈업)
- **[신규] 손상 부위 이미지 (여러 장)** — `DiagnosisTask.damageImageUrls`를 응답에 그대로 노출 필요 (지금 응답에 아예 안 나감)
- 완성/after 이미지 — 기존 재사용 유지
- 교체/보강 코멘트 — 기존 `steps[].description` 유지
- 현재 문제점 / 리폼 기대효과 — 기존 `beforeAfter.before/after.points` 유지

**결과 활용 페이지**
- 리폼 이미지 — 기존 재사용
- **[신규] AI 추천 한줄 코멘트** — `reasons`(3개 불릿)와 별개로, 한 문장 요약 필드 추가 (`summaryComment` 등)
- **[신규] 해결되는 불편** — `beforeAfter.before.points`와 겹칠 수 있으나, "해결됨"을 명시하는 별도 문구로 다시 뽑아야 할 수 있음 (예: `resolvedPains: List<String>`)
- 추천 리폼 작업 — 기존 재사용
- **[신규] 예상 난이도** — enum 또는 문자열 (`difficulty`: "쉬움"/"보통"/"어려움" 등)

### RESELL

**AI 추천 결과 페이지**
- 추천 순위, 결과: 리셀 — 기존 유지
- **[신규] 정면 이미지 재사용** — RESELL도 `frontImageUrl` 노출 필요 (지금 RESELL은 이미지 필드 자체가 없음)
- 추천 이유 — 기존 유지

**리셀 미리보기 페이지**
- 정면 이미지 재사용
- **[신규] 이 제품과 잘 맞는 사용자군 + 이유 + 해시태그** — 리스트 형태, 예:
  ```json
  "suitableUsers": [
    {"title": "가볍게 외출하는 간결한 스타일의 사용자", "description": "...", "hashtags": ["#간결한 소지품", "#짧은 외출"]}
  ]
  ```
- **[신규] 가치에 부정적 영향을 줄 수 있는 요소** — `List<String>` (예: "핸들/가죽 마모", "수납 공간 부족")
- **[신규] 가치를 유지하는 긍정적 요소** — `List<String>` (예: "더블 핸들 디자인", "탈부착 스트랩")

**결과 활용 페이지**
- **[신규] 현재 니즈에 맞는 대안 제품 추천** — 유형(토트백/숄더백/크로스백/백팩/파우치 중 택1) + 짧은 이유. 예:
  ```json
  "alternativeProductSuggestion": {"productType": "크로스백", "reason": "..."}
  ```

### UPCYCLING

**AI 추천 결과 페이지**
- 정면 이미지 재사용 — 지금 UPCYCLING도 이미지 필드 없음, 추가 필요
- 업사이클링 추천 이유 — 기존 `reasons` 유지
- 후보 품목 + 부가설명 — 기존 `upcyclingCandidates[].itemName/description` 유지

**업사이클링 미리보기 페이지**
- 후보 품목/부가설명 재사용
- AI 생성 이미지들 — 기존 `upcyclingCandidates[].imageUrl` 유지
- **[신규] 품목별 추천 이유** — 지금 `description` 하나로 퉁치고 있는데, "추천 이유"를 별도 필드로 분리할지 확인 필요 (`reason` 필드 추가 검토)
- **[신규] 품목별 예상 변화 (before→after 리스트)** — 예:
  ```json
  "expectedChanges": [
    "큰 토트백 -> 미니 크로스백",
    "큰 사이즈 -> 컴팩트한 사이즈",
    "숄더 착용 -> 크로스바디 착용"
  ]
  ```
  프론트가 "->"로 파싱해서 좌/우로 나눠 쓸 수 있게 문자열 리스트로 전달.

**결과 활용 페이지**
- 후보 품목/부가설명/이미지/예상변화 — 재사용
- **[신규] 이어지는 기존 제품의 특징 (태그)** — `List<String>` (예: "MCM 시그니처 패턴", "가죽 소재", "금속 하드웨어", "브랜드 아이덴티티")

## 작업 순서 제안

1. `diagnosisResult` 실제 스키마부터 확인 (색상/사이즈/패턴 키 이름) — 진단 담당자 코드 확인
2. 이미지 생성 프롬프트에 색상/사이즈/패턴 반영 (문제 1 먼저 해결, 임팩트 크고 빠르게 고칠 수 있음)
3. DTO 확장: `RankedRecommendationDto`에 타입 공통 필드(`frontImageUrl` 등) 추가
4. REFORM 전용 DTO 필드 추가: `resultImageUrl`, `summaryComment`, `resolvedPains`, `difficulty`
5. RESELL 전용 DTO 신규 작성: `suitableUsers`, `negativeFactors`, `positiveFactors`, `alternativeProductSuggestion`
6. UPCYCLING 기존 DTO 확장: `expectedChanges`, `existingFeatureTags`, (필요시 candidate별 `reason` 분리)
7. `OpenAiRecommendationClient`의 judge() 프롬프트를 위 신규 필드들 다 포함해서 JSON 반환하도록 대폭 확장 (한 번의 OpenAI 호출로 텍스트성 필드들은 다 받아오는 걸 추천 — 호출 횟수 늘리지 않기 위해)
8. `RecommendationService`에서 이미지 생성이 필요한 부분(REFORM 교체 아이템 이미지 추가 등)도 병렬 처리에 포함시키기
