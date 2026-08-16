# Hugging Face FLUX.1-schnell 이미지 생성 적용 가이드

> `.env`(HF_API_KEY)와 `application.yml`(huggingface.api-key, huggingface.model)은 이미 세팅 완료.
> 이 문서 보고 아래 파일들을 프로젝트에 만들거나 수정해줘.
>
> ⚠️ 중요: Gemini 관련 파일(GeminiConfig, GeminiImageClient 등)은 **절대 삭제하지 말 것**.
> 다른 팀원이 다른 기능에서 Gemini를 그대로 쓰고 있음. 이 작업은 `recommend` 도메인의
> 이미지 생성 부분만 Hugging Face로 새로 추가하는 것이고, 기존 Gemini 코드와 공존시킴.

## 배경 / 결정사항
- REFORM/UPCYCLING 추천에 필요한 이미지는 **원본 사진을 넣는 편집 방식이 아니라, 텍스트 프롬프트만으로 생성**하는 방식으로 확정함.
- 이유: `recommend` 도메인에서 이미지 생성을 시도할 때 Gemini(`gemini-3.1-flash-image`), Imagen(`imagen-3.0-generate-001`) 둘 다 API 경로는 무료 티어가 없어서(둘 다 429/RESOURCE_EXHAUSTED 확인함) 제외하고, `recommend` 쪽만 Hugging Face로 감.
- 모델: `black-forest-labs/FLUX.1-schnell` (텍스트→이미지, 무료, Apache 2.0 라이선스)
- 프롬프트는 AI 분석(진단) 결과 + 추천 내용(리폼 작업/업사이클링 후보 설명)을 조합해서 만듦.

## 1. `global/config/HuggingFaceConfig.java` (신규 — 기존 GeminiConfig와 별개로 추가)

```java
package com.fitagain.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class HuggingFaceConfig {

    @Value("${huggingface.api-key}")
    private String apiKey;

    @Bean(name = "huggingFaceRestClient")
    public RestClient huggingFaceRestClient() {
        return RestClient.builder()
                .baseUrl("https://api-inference.huggingface.co/models")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
```

빈 이름을 `huggingFaceRestClient`로 명시했음. 만약 `GeminiConfig`도 `RestClient` 타입 빈을 등록하고 있다면, 그쪽도 이름이 겹치지 않는지 확인하고 주입받는 곳에서 `@Qualifier`로 명확히 구분할 것 (Gemini용 RestClient 빈이랑 충돌 방지).

## 2. `domain/recommend/client/HuggingFaceImageClient.java` (신규)

`recommend` 도메인 안에 새로 추가하는 것이고, 기존 `GeminiImageClient`(다른 도메인/다른 기능에서 쓰는 것)는 손대지 않음.

```java
package com.fitagain.domain.recommend.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Slf4j
@Component
public class HuggingFaceImageClient {

    private final RestClient huggingFaceRestClient;

    @Value("${huggingface.model}")
    private String model;

    public HuggingFaceImageClient(@Qualifier("huggingFaceRestClient") RestClient huggingFaceRestClient) {
        this.huggingFaceRestClient = huggingFaceRestClient;
    }

    /**
     * 텍스트 프롬프트만으로 이미지를 생성한다. (원본 사진 입력 없음)
     * @return 생성된 이미지의 raw bytes (PNG/JPEG)
     */
    public byte[] generateImage(String prompt) {
        Map<String, Object> requestBody = Map.of("inputs", prompt);

        byte[] resultImageBytes = huggingFaceRestClient.post()
                .uri("/{model}", model)
                .header("X-Wait-For-Model", "true") // 무료 서버리스 콜드 스타트 대비, 모델 깨어날 때까지 대기
                .body(requestBody)
                .retrieve()
                .body(byte[].class);

        if (resultImageBytes == null || resultImageBytes.length == 0) {
            throw new IllegalStateException("Hugging Face 응답에서 이미지 데이터를 받지 못했습니다.");
        }
        return resultImageBytes;
    }
}
```

**주의**: 첫 호출에서 400/422/503 에러가 나면 요청 바디 형식이 이 모델 버전과 안 맞을 수 있음. 에러 바디 그대로 로그 확인해서 `inputs` 필드명/구조 조정 필요할 수 있음.

## 3. `global/util/S3Uploader.java`에 byte[] 업로드 오버로드 추가 (없다면)

이건 Gemini든 Hugging Face든 공통으로 재사용할 수 있는 유틸이라 그대로 추가하면 됨 (Gemini 쪽 이미지 생성 결과를 올릴 때도 재사용 가능).

```java
public String upload(byte[] data, String contentType, String dirName) {
    String extension = contentType != null && contentType.contains("png") ? ".png" : ".jpg";
    String uniqueFileName = dirName + "/" + java.util.UUID.randomUUID() + extension;

    software.amazon.awssdk.services.s3.model.PutObjectRequest putObjectRequest =
            software.amazon.awssdk.services.s3.model.PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(uniqueFileName)
                    .contentType(contentType)
                    .build();

    s3Client.putObject(putObjectRequest, software.amazon.awssdk.core.sync.RequestBody.fromBytes(data));

    return "https://" + bucket + ".s3.ap-northeast-2.amazonaws.com/" + uniqueFileName;
}
```

## 4. `RecommendationService`에 프롬프트 조합 + 이미지 생성 연결

`HuggingFaceImageClient`와 `S3Uploader`를 주입받아서, REFORM/UPCYCLING 추천 생성 시 호출.
**이 서비스 안에서 Gemini 관련 클라이언트를 쓰고 있었다면 그것도 그대로 두고, Hugging Face 호출을 추가/교체하는 부분만 반영.**

```java
private final HuggingFaceImageClient huggingFaceImageClient;
private final S3Uploader s3Uploader;
```

### REFORM용 프롬프트 예시
```java
private String buildReformImagePrompt(DiagnosisTask task, List<RecommendedWorkDto> works) {
    String workSummary = works.stream()
            .map(RecommendedWorkDto::getDescription)
            .collect(java.util.stream.Collectors.joining(" "));

    return """
            A high-quality product photo of a %s bag,
            after the following repairs/reform have been applied: %s.
            Studio lighting, clean beige background, realistic leather texture, no text, no watermark.
            """.formatted(task.getProductType(), workSummary);
    // TODO: diagnosisResult에서 색상/재질 필드 확인되면 프롬프트에 추가
}
```

### UPCYCLING용 프롬프트 예시 (후보 품목마다 1장씩)
```java
private String buildUpcyclingImagePrompt(DiagnosisTask task, UpcyclingCandidateDto candidate) {
    return """
            A high-quality product photo of a %s,
            upcycled/repurposed from an old %s bag, reusing its leather and hardware details.
            %s
            Studio lighting, clean beige background, realistic leather texture, no text, no watermark.
            """.formatted(candidate.getItemName(), task.getProductType(), candidate.getDescription());
}
```

### 호출 + S3 업로드 흐름
```java
byte[] imageBytes = huggingFaceImageClient.generateImage(prompt);
String imageUrl = s3Uploader.upload(imageBytes, "image/png", "recommendations/reform"); // 또는 "recommendations/upcycling"
```

### 병렬 처리
REFORM 시뮬레이션 이미지(4단계 중 실제 생성 필요한 완성본 1장)와 UPCYCLING 후보 이미지(품목 수만큼, 보통 3장)를 `CompletableFuture.supplyAsync(...)`로 동시에 쏘고 `CompletableFuture.allOf(...)`로 모아서 5~10초 내 완료되게 처리.

## 5. 확인/정리할 것
- **Gemini 관련 파일(GeminiConfig, GeminiImageClient 등)은 삭제 금지** — 다른 팀원 기능에서 사용 중
- `application.yml`의 `gemini:` 블록도 그대로 유지
- `huggingface:` 블록만 새로 추가된 상태인지 확인 (기존 `gemini:` 블록과 나란히 존재해야 함)
- `RestClient` 타입 빈이 여러 개(Gemini용 + Hugging Face용 + 필요시 OpenAI용)라 스프링이 헷갈릴 수 있으니, **각 Config 클래스에서 `@Bean(name = "...")`으로 이름 명시 + 주입받는 곳에서 `@Qualifier`로 명확히 지정**돼 있는지 전체적으로 한번 점검할 것
- `diagnosisResult`의 실제 필드(색상/재질 등)를 아직 못 받았으면, 일단 `task.getProductType()`과 `keywords`만으로 프롬프트 구성하고 TODO로 남겨둘 것
