# 이미지 생성 최종 전환: OpenAI → Gemini

> 이 문서 보고 아래 내용 그대로 적용해줘.
> OpenAI(gpt-image-2)는 팀 조직의 Organization Verification이 막혀서
> rate limit이 0으로 계속 떠서 이미지 생성이 아예 안 됨 (텍스트 판단용 gpt-4o는 계속 정상 사용).
> 그래서 이미지 생성은 최종적으로 Gemini로 확정함. GeminiConfig/GeminiImageClient는
> 이미 프로젝트에 존재하니 삭제하지 말고 그대로 활용, RecommendationService의
> 호출부만 OpenAiImageClient에서 GeminiImageClient로 교체.

## 1. `GeminiImageClient.java` 전체 교체

기존 파일(`domain/recommend/client/GeminiImageClient.java`)을 아래 내용으로 완전히 덮어써줘.
기존과 달라진 점: `generateReformAfterImage`/`generateUpcyclingImage`에
`Map<String, Object> diagnosisResult` 파라미터가 추가됨 — 진단 결과(색상/사이즈/패턴 등)가
있으면 프롬프트에 참고 문구로 포함시켜서 원본과 색상이 달라지는 문제를 줄임.

```java
package com.fitagain.domain.recommend.client;

import com.fitagain.domain.recommend.dto.RecommendedWorkDto;
import com.fitagain.domain.recommend.dto.UpcyclingCandidateDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class GeminiImageClient {

    private final RestClient geminiRestClient;

    @Value("${gemini.model:gemini-3.1-flash-image}")
    private String model;

    public GeminiImageClient(RestClient geminiRestClient) {
        this.geminiRestClient = geminiRestClient;
    }

    /**
     * REFORM 시뮬레이션의 완성(after) 이미지를 생성합니다.
     * 정면 사진 + 디테일 사진을 참고 이미지로 넣고, 리폼 작업 내용을 편집 지시로 전달합니다.
     */
    public byte[] generateReformAfterImage(
            String frontImageUrl,
            List<String> detailImageUrls,
            List<RecommendedWorkDto> recommendedWorks,
            Map<String, Object> diagnosisResult
    ) {
        String worksSummary = recommendedWorks.stream()
                .map(w -> "- " + w.getTitle() + ": " + w.getDescription())
                .collect(Collectors.joining("\n"));

        String diagnosisNote = buildDiagnosisNote(diagnosisResult);

        String prompt = """
                첨부된 이미지는 리폼 대상 가방의 정면 사진과 디테일 사진입니다.
                아래 리폼 작업 내용을 실제로 적용한 것처럼, 같은 가방을 편집한 완성 이미지 1장을 생성해 주세요.
                가방의 기본 형태, 소재, 색상은 반드시 원본 사진과 동일하게 유지하면서 아래 내용만 적당한 수준으로 반영하고, 과도한 디자인 변경은 하지 마세요.
                %s
                리폼 작업 내용:
                %s
                """.formatted(diagnosisNote, worksSummary);

        return generateImage(prompt, referenceImages(frontImageUrl, detailImageUrls));
    }

    /**
     * UPCYCLING 후보 품목 1개에 대한 결과물 이미지를 생성합니다. (품목마다 각각 호출)
     */
    public byte[] generateUpcyclingImage(
            String frontImageUrl,
            List<String> detailImageUrls,
            UpcyclingCandidateDto candidate,
            Map<String, Object> diagnosisResult
    ) {
        String diagnosisNote = buildDiagnosisNote(diagnosisResult);

        String prompt = """
                첨부된 이미지는 업사이클링 원재료가 될 가방의 정면 사진과 디테일 사진입니다.
                이 가방과 동일한 소재, 색상, 질감을 사용해서 "%s"(으)로 업사이클링한 결과물 이미지 1장을 생성해 주세요.
                설명: %s
                %s
                원본 가방의 가죽/패브릭 질감과 색상이 그대로 느껴지도록 생성하세요.
                """.formatted(candidate.getItemName(), candidate.getDescription(), diagnosisNote);

        return generateImage(prompt, referenceImages(frontImageUrl, detailImageUrls));
    }

    private String buildDiagnosisNote(Map<String, Object> diagnosisResult) {
        if (diagnosisResult == null || diagnosisResult.isEmpty()) {
            return "";
        }
        Object color = diagnosisResult.get("color");
        Object size = diagnosisResult.get("size");
        Object pattern = diagnosisResult.get("pattern");

        StringBuilder note = new StringBuilder("참고 (원본 특징, 반드시 유지):");
        if (color != null) note.append(" 색상=").append(color);
        if (size != null) note.append(", 사이즈=").append(size);
        if (pattern != null) note.append(", 패턴=").append(pattern);
        return note.toString();
    }

    private List<String> referenceImages(String frontImageUrl, List<String> detailImageUrls) {
        List<String> references = new ArrayList<>();
        references.add(frontImageUrl);
        if (detailImageUrls != null) references.addAll(detailImageUrls);
        return references;
    }

    private byte[] generateImage(String prompt, List<String> referenceImageUrls) {
        List<Map<String, Object>> parts = new ArrayList<>();
        parts.add(Map.of("text", prompt));
        for (String url : referenceImageUrls) {
            if (url == null || url.isBlank()) continue;
            parts.add(Map.of(
                    "inline_data", Map.of(
                            "mime_type", resolveMimeType(url),
                            "data", Base64.getEncoder().encodeToString(downloadImage(url))
                    )
            ));
        }

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(Map.of("parts", parts))
        );

        Map<String, Object> response = geminiRestClient.post()
                .uri("/{model}:generateContent", model)
                .body(requestBody)
                .retrieve()
                .body(Map.class);

        return extractImageBytes(response);
    }

    private byte[] downloadImage(String url) {
        try (InputStream in = URI.create(url).toURL().openStream()) {
            return in.readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException("Gemini 참조 이미지 다운로드 실패: " + url, e);
        }
    }

    private String resolveMimeType(String url) {
        String lower = url.toLowerCase();
        if (lower.contains(".png")) return "image/png";
        if (lower.contains(".webp")) return "image/webp";
        return "image/jpeg";
    }

    @SuppressWarnings("unchecked")
    private byte[] extractImageBytes(Map<String, Object> response) {
        try {
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
            for (Map<String, Object> part : parts) {
                Object inline = part.get("inlineData");
                if (inline == null) inline = part.get("inline_data");
                if (inline instanceof Map<?, ?> inlineMap) {
                    Object data = inlineMap.get("data");
                    if (data != null) {
                        return Base64.getDecoder().decode((String) data);
                    }
                }
            }
            throw new IllegalStateException("Gemini 응답에 이미지 데이터가 없습니다: " + response);
        } catch (ClassCastException | NullPointerException | IndexOutOfBoundsException e) {
            throw new IllegalStateException("Gemini 응답 파싱 실패: " + response, e);
        }
    }
}
```

## 2. `RecommendationService.java` 수정

- 필드: `OpenAiImageClient openAiImageClient` → `GeminiImageClient geminiImageClient`로 교체 (또는 추가, `OpenAiImageClient`는 지우지 않아도 됨)
- REFORM 이미지 생성 호출부(`fillReformSimulation` 안, 지금 `openAiImageClient.generateImage(prompt)` 형태로 되어 있는 곳)를 아래로 교체:
  ```java
  byte[] imageBytes = geminiImageClient.generateReformAfterImage(
          task.getFrontImageUrl(),
          task.getDetailImageUrls(),
          top.getRecommendedWorks(),
          task.getDiagnosisResult()
  );
  ```
- UPCYCLING 이미지 생성 호출부(`fillUpcyclingImage` 안)를 아래로 교체:
  ```java
  byte[] imageBytes = geminiImageClient.generateUpcyclingImage(
          task.getFrontImageUrl(),
          task.getDetailImageUrls(),
          candidate,
          task.getDiagnosisResult()
  );
  ```
- 기존에 프롬프트를 직접 조합하던 `buildReformImagePrompt(...)`, `buildUpcyclingImagePrompt(...)` private 메서드는 이제 안 쓰니 삭제해도 됨 (프롬프트 조합 로직이 `GeminiImageClient` 내부로 이동함).
- S3 업로드 부분(`s3Uploader.upload(imageBytes, "image/png", "recommendations/reform")` 등)은 그대로 유지.

## 3. 확인할 것

- `GeminiConfig`의 `RestClient` 빈과 `OpenAiConfig`/`HuggingFaceConfig`의 `RestClient` 빈이 동시에 존재해서 주입 모호성 에러가 나면, `GeminiConfig`에 `@Bean(name = "geminiRestClient")` 명시하고 `GeminiImageClient` 생성자 파라미터에 `@Qualifier("geminiRestClient")` 추가.
- `task.getDiagnosisResult()`가 아직 색상/사이즈/패턴 키를 안 담고 있어도 에러 없이 동작함(`buildDiagnosisNote`가 null-safe). 나중에 진단 담당자 스키마 확정되면 `color`/`size`/`pattern` 키 이름만 맞춰주면 됨.
- 컴파일 확인: `./gradlew compileJava --console=plain`
