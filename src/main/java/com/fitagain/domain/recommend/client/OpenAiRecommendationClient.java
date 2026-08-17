package com.fitagain.domain.recommend.client;

import com.fitagain.domain.recommend.dto.AlternativeProductDto;
import com.fitagain.domain.recommend.dto.RankedRecommendationDto;
import com.fitagain.domain.recommend.dto.RecommendationJudgmentDto;
import com.fitagain.domain.recommend.dto.RecommendedWorkDto;
import com.fitagain.domain.recommend.dto.UpcyclingCandidateDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class OpenAiRecommendationClient {

    private static final Set<String> ALLOWED_ALTERNATIVE_PRODUCT_TYPES =
            Set.of("토트백", "숄더백", "크로스백", "백팩", "파우치", "기타");

    private final RestClient openAiRestClient;
    private final ObjectMapper objectMapper;

    @Value("${openai.model}")
    private String model;

    public OpenAiRecommendationClient(RestClient openAiRestClient, ObjectMapper objectMapper) {
        this.openAiRestClient = openAiRestClient;
        this.objectMapper = objectMapper;
    }

    public RecommendationJudgmentDto judge(
            String productType,
            List<String> keywords,
            String description,
            Map<String, Object> diagnosisResult,
            String frontImageUrl,
            List<String> damageImageUrls
    ) {
        String systemPrompt = """
        당신은 중고 명품 가방 리폼/리셀/업사이클 상담사입니다.
        제품 정보와 사용자가 느끼는 불편, AI 진단 결과, 그리고 첨부된 사진(정면 사진 + 손상 부위 사진)을 보고,
        REFORM(리폼) / RESELL(리셀) / UPCYCLING(업사이클링) 세 가지 방향 모두에 대해
        1순위(가장 추천)부터 3순위(가장 비추천)까지 순위를 매기세요.
        각 방향마다 그 순위인 이유를 정확히 3개의 짧은 한국어 불릿 문장으로 작성하세요 (단, RESELL은 아래 별도 규칙을 따르세요).

        AI 진단 결과 JSON은 다음 키를 가집니다:
        - externalStructure: 외부 구조 특징 목록
        - damageState: 손상 상태 목록
        - currentPurpose: 현재 사용 목적
        - mainInconvenience: 주요 불편 원인 목록
        - areasForImprovement: 개선이 필요한 부분 목록
        이 진단 결과, 특히 damageState와 areasForImprovement를 근거로 판단하세요.

        REFORM 방향에는 아래 필드를 반드시 포함하세요:
        - recommendedWorks: 정확히 3개의 {"title": "...", "description": "...", "category": "REPLACE 또는 REINFORCE"} 객체 배열.
          damageState/areasForImprovement를 바탕으로 한 구체적인 리폼 작업이어야 합니다.
          category는 그 작업이 부품을 교체하는 성격이면 "REPLACE", 보강/보수하는 성격이면 "REINFORCE"로 판단하세요.
        - summaryComment: 이 리폼을 한 문장으로 요약하는 한국어 코멘트.
        - resolvedPains: 이 리폼으로 해결되는 불편을 나열한 한국어 문자열 배열 (mainInconvenience 근거).
        - difficulty: "쉬움", "보통", "어려움" 중 하나로 예상 난이도.
        - damageMarkers: 첨부된 손상 부위 사진(있는 경우)과 정면 사진을 참고해서, 정면 사진 기준으로 손상/변경이 필요한
          부위마다 원형 마커 좌표를 추정한 배열 {"number": 1부터 순번, "xPercent": 정면 사진 가로 기준 0~100 비율,
          "yPercent": 정면 사진 세로 기준 0~100 비율, "label": "짧은 설명"}. 손상 부위 사진이 없거나 특정할 수 없으면 빈 배열로 답하세요.

        UPCYCLING 방향에는 아래 필드를 반드시 포함하세요:
        - upcyclingCandidates: 정확히 3개의 객체 배열, 각 객체는 다음 필드를 가집니다:
          - itemName: 이 가방의 소재/구조로 실제로 만들 수 있는 새로운 제품 이름 (예: "카드지갑", "미니 크로스백"). 제품마다 다르게, 그 제품에 실제로 잘 어울리는 품목을 골라주세요.
          - description: 후보 품목 목록 카드에 쓰일 한 줄 부가 설명.
          - reasonPairs: "왜 이 방향을 제안했을까요?" 섹션에 쓸 불편-해결 쌍 3개 정도, 각각 {"problem": "...", "solution": "..."} 형태. 품목마다 다르게 작성하세요.
          - expectedChanges: "Before -> After" 형태의 한국어 문자열 배열 (예: "큰 토트백 -> 미니 크로스백")로 4~5개 정도. 품목마다 다르게 작성하세요.
        - existingFeatureTags: 원본 가방에서 새 품목으로 이어지는 특징을 나타내는 짧은 한국어 태그 배열 (예: "가죽 소재", "금속 하드웨어").

        RESELL 방향에는 아래 필드를 반드시 포함하세요 (RESELL의 reasons는 일반 불릿 문장이 아니라 해시태그 형식입니다):
        - reasons: 이 순위인 이유를 해시태그 형식 문자열로 최대 4개 (예: ["#무게부담", "#수납공간부족"]). 3개 불릿 문장이 아닙니다.
        - alternativeProducts: 다음 구매로 추천하는 대안 제품 정확히 3개, 각각 {"productType": "...", "hashtags": ["#...", ...]}
          형태이며, productType은 반드시 "토트백", "숄더백", "크로스백", "백팩", "파우치", "기타" 중 하나여야 하고,
          hashtags는 제품당 최대 4개.

        반드시 아래 JSON 형식으로만 답하세요. 다른 텍스트나 코드블록은 절대 포함하지 마세요.
        rankings 배열은 정확히 3개 항목이어야 하고, REFORM/RESELL/UPCYCLING이 각각 한 번씩만 나와야 하며,
        REFORM/UPCYCLING의 reasons 배열은 정확히 3개의 문자열, RESELL의 reasons 배열은 해시태그 문자열 최대 4개여야 합니다.
        {
          "rankings": [
            {
              "rank": 1,
              "recommendationType": "REFORM 또는 RESELL 또는 UPCYCLING",
              "reasons": ["..."],
              "recommendedWorks": [{"title": "...", "description": "...", "category": "REPLACE 또는 REINFORCE"}, ...] (REFORM일 때만 포함),
              "summaryComment": "..." (REFORM일 때만 포함),
              "resolvedPains": ["...", "..."] (REFORM일 때만 포함),
              "difficulty": "쉬움 또는 보통 또는 어려움" (REFORM일 때만 포함),
              "damageMarkers": [{"number": 1, "xPercent": 15.0, "yPercent": 75.0, "label": "..."}, ...] (REFORM일 때만 포함, 없으면 빈 배열),
              "upcyclingCandidates": [{"itemName": "...", "description": "...", "reasonPairs": [{"problem": "...", "solution": "..."}, ...], "expectedChanges": ["...", "..."]}, ...] (UPCYCLING일 때만 포함),
              "existingFeatureTags": ["...", "..."] (UPCYCLING일 때만 포함),
              "alternativeProducts": [{"productType": "...", "hashtags": ["...", "..."]}, ...] (RESELL일 때만 포함)
            },
            {"rank": 2, "recommendationType": "...", "reasons": ["..."], ...위와 동일하게 해당 타입 필드 포함},
            {"rank": 3, "recommendationType": "...", "reasons": ["..."], ...위와 동일하게 해당 타입 필드 포함}
          ]
        }
        """;

        String userPromptText = """
                제품 유형: %s
                불편 키워드: %s
                추가 설명: %s
                AI 분석(진단) 결과: %s
                """.formatted(
                productType,
                keywords == null || keywords.isEmpty() ? "없음" : String.join(", ", keywords),
                (description == null || description.isBlank()) ? "없음" : description,
                diagnosisResult == null ? "없음" : objectMapper.writeValueAsString(diagnosisResult)
        );

        List<Map<String, Object>> userContent = new ArrayList<>();
        userContent.add(Map.of("type", "text", "text", userPromptText));
        addImageContent(userContent, frontImageUrl);
        if (damageImageUrls != null) {
            damageImageUrls.forEach(url -> addImageContent(userContent, url));
        }

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userContent)
                ),
                "response_format", Map.of("type", "json_object")
        );

        Map<String, Object> response = openAiRestClient.post()
                .uri("/chat/completions")
                .body(requestBody)
                .retrieve()
                .body(Map.class);

        String responseContent = extractContent(response);
        return parseJudgment(responseContent);
    }

    private void addImageContent(List<Map<String, Object>> content, String url) {
        if (url == null || url.isBlank()) return;
        content.add(Map.of("type", "image_url", "image_url", Map.of("url", url)));
    }

    @SuppressWarnings("unchecked")
    private String extractContent(Map<String, Object> response) {
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        return (String) message.get("content");
    }

    private RecommendationJudgmentDto parseJudgment(String json) {
        try {
            RecommendationJudgmentDto judgment = objectMapper.readValue(json, RecommendationJudgmentDto.class);
            if (judgment.getRankings() == null || judgment.getRankings().size() != 3) {
                throw new IllegalStateException("OpenAI가 3개 순위를 모두 반환하지 않았습니다: " + json);
            }
            for (RankedRecommendationDto r : judgment.getRankings()) {
                validateReasons(r, json);
                if ("REFORM".equals(r.getRecommendationType())) {
                    validateReform(r, json);
                }
                if ("UPCYCLING".equals(r.getRecommendationType())) {
                    validateUpcycling(r, json);
                }
                if ("RESELL".equals(r.getRecommendationType())) {
                    validateResell(r, json);
                }
            }
            judgment.getRankings().sort((a, b) -> Integer.compare(a.getRank(), b.getRank()));
            return judgment;
        } catch (Exception e) {
            throw new IllegalStateException("OpenAI 응답 파싱 실패: " + json, e);
        }
    }

    private void validateReasons(RankedRecommendationDto r, String json) {
        if (r.getReasons() == null || r.getReasons().isEmpty()) {
            throw new IllegalStateException("reasons가 비어있습니다: " + json);
        }
        if ("RESELL".equals(r.getRecommendationType())) {
            if (r.getReasons().size() > 4) {
                throw new IllegalStateException("RESELL의 reasons(해시태그)는 4개 이하여야 합니다: " + json);
            }
        } else if (r.getReasons().size() != 3) {
            throw new IllegalStateException("이유가 3개가 아닙니다: " + json);
        }
    }

    private void validateReform(RankedRecommendationDto r, String json) {
        if (r.getRecommendedWorks() == null || r.getRecommendedWorks().size() != 3) {
            throw new IllegalStateException("REFORM의 recommendedWorks가 3개가 아닙니다: " + json);
        }
        for (RecommendedWorkDto work : r.getRecommendedWorks()) {
            if (!"REPLACE".equals(work.getCategory()) && !"REINFORCE".equals(work.getCategory())) {
                throw new IllegalStateException("REFORM 작업의 category가 REPLACE/REINFORCE가 아닙니다: " + json);
            }
        }
        if (r.getSummaryComment() == null || r.getSummaryComment().isBlank()
                || r.getResolvedPains() == null || r.getResolvedPains().isEmpty()
                || r.getDifficulty() == null || r.getDifficulty().isBlank()) {
            throw new IllegalStateException("REFORM의 summaryComment/resolvedPains/difficulty가 누락되었습니다: " + json);
        }
    }

    private void validateUpcycling(RankedRecommendationDto r, String json) {
        if (r.getUpcyclingCandidates() == null || r.getUpcyclingCandidates().size() != 3) {
            throw new IllegalStateException("UPCYCLING의 upcyclingCandidates가 3개가 아닙니다: " + json);
        }
        if (r.getExistingFeatureTags() == null || r.getExistingFeatureTags().isEmpty()) {
            throw new IllegalStateException("UPCYCLING의 existingFeatureTags가 누락되었습니다: " + json);
        }
        for (UpcyclingCandidateDto candidate : r.getUpcyclingCandidates()) {
            if (candidate.getReasonPairs() == null || candidate.getReasonPairs().isEmpty()) {
                throw new IllegalStateException("UPCYCLING 후보의 reasonPairs가 비어있습니다: " + json);
            }
            if (candidate.getExpectedChanges() == null || candidate.getExpectedChanges().isEmpty()) {
                throw new IllegalStateException("UPCYCLING 후보의 expectedChanges가 비어있습니다: " + json);
            }
        }
    }

    private void validateResell(RankedRecommendationDto r, String json) {
        if (r.getAlternativeProducts() == null || r.getAlternativeProducts().size() != 3) {
            throw new IllegalStateException("RESELL의 alternativeProducts가 3개가 아닙니다: " + json);
        }
        for (AlternativeProductDto product : r.getAlternativeProducts()) {
            if (product.getProductType() == null || !ALLOWED_ALTERNATIVE_PRODUCT_TYPES.contains(product.getProductType())) {
                throw new IllegalStateException("RESELL alternativeProducts의 productType이 허용된 값이 아닙니다: " + json);
            }
            if (product.getHashtags() == null || product.getHashtags().isEmpty() || product.getHashtags().size() > 4) {
                throw new IllegalStateException("RESELL alternativeProducts의 hashtags는 1~4개여야 합니다: " + json);
            }
        }
    }
}
