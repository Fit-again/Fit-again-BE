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

    private static final String BRAND_PROTECTION_NOTE = """
            이 가방은 MCM 브랜드 제품입니다. 원본 사진에 있는 MCM 고유 패턴/로고/디테일 외에
            다른 브랜드의 로고, 워터마크, 패턴을 절대 새로 추가하지 마세요.
            브랜드 요소를 변경해야 한다면 무늬 없는 단색 가죽으로 대체하거나 기존 MCM 디자인 요소를
            그대로 유지하는 방향으로만 처리하세요.
            """;

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
                %s
                리폼 작업 내용:
                %s
                """.formatted(diagnosisNote, BRAND_PROTECTION_NOTE, worksSummary);

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
        이 가방과 동일한 소재, 색상, 질감을 사용해서 "%s"(으)로 업사이클링한 결과물을 만들어 주세요.
        설명: %s

        중요한 규칙:
        - 이미지에는 오직 완성된 "%s" 하나만 보여주세요.
        - 원본 가방이나 다른 형태의 제품을 함께 보여주지 마세요. 배경에 원본 가방을 두거나, 원본과 결과물을 나란히 보여주는 비교 구도로 만들지 마세요.
        - "%s"의 실제 형태와 용도에 맞는 완성품 단독 제품 사진(product shot)으로만 생성하세요.

        원본 가방의 가죽/패브릭 질감과 색상이 그대로 느껴지도록 생성하세요.

        이 가방은 MCM 브랜드 제품입니다. 원본 사진에 있는 MCM 고유 패턴/로고/디테일 외에
        다른 브랜드의 로고, 워터마크, 패턴을 절대 새로 추가하지 마세요.

        %s
        """.formatted(candidate.getItemName(), candidate.getDescription(), candidate.getItemName(), candidate.getItemName(), diagnosisNote);
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
                "contents", List.of(Map.of("parts", parts)),
                "generationConfig", Map.of(
                        "responseModalities", List.of("IMAGE"),
                        "imageConfig", Map.of(
                                "imageSize", "1K",
                                "aspectRatio", "1:1"
                        )
                )
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
