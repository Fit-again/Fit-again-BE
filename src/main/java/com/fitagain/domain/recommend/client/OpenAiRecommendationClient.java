package com.fitagain.domain.recommend.client;

import com.fitagain.domain.recommend.dto.RankedRecommendationDto;
import com.fitagain.domain.recommend.dto.RecommendationJudgmentDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

@Component
public class OpenAiRecommendationClient {

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
            Map<String, Object> diagnosisResult
    ) {
        String systemPrompt = """
        당신은 중고 명품 가방 리폼/리셀/업사이클 상담사입니다.
        제품 정보와 사용자가 느끼는 불편, AI 진단 결과를 보고, REFORM(리폼) / RESELL(리셀) / UPCYCLING(업사이클링)
        세 가지 방향 모두에 대해 1순위(가장 추천)부터 3순위(가장 비추천)까지 순위를 매기세요.
        각 방향마다 그 순위인 이유를 정확히 3개의 짧은 한국어 불릿 문장으로 작성하세요.

        AI 진단 결과 JSON은 다음 키를 가집니다:
        - externalStructure: 외부 구조 특징 목록
        - damageState: 손상 상태 목록
        - currentPurpose: 현재 사용 목적
        - mainInconvenience: 주요 불편 원인 목록
        - areasForImprovement: 개선이 필요한 부분 목록
        이 진단 결과, 특히 damageState와 areasForImprovement를 근거로 판단하세요.

        REFORM 방향에는 recommendedWorks 필드를 반드시 포함하세요: 정확히 3개의 {"title": "...", "description": "..."}
        객체 배열이며, damageState/areasForImprovement를 바탕으로 한 구체적인 리폼 작업이어야 합니다.
        UPCYCLING 방향에는 upcyclingCandidates 필드를 반드시 포함하세요: 정확히 3개의 {"itemName": "...", "description": "..."}
        객체 배열이며, itemName은 이 가방의 소재/구조로 실제로 만들 수 있는 새로운 제품 이름입니다
        (예: "카드지갑", "미니 크로스백"). 제품마다 다르게, 그 제품에 실제로 잘 어울리는 품목을 골라주세요.
        RESELL 방향에는 추가 필드가 필요 없습니다.

        반드시 아래 JSON 형식으로만 답하세요. 다른 텍스트나 코드블록은 절대 포함하지 마세요.
        rankings 배열은 정확히 3개 항목이어야 하고, REFORM/RESELL/UPCYCLING이 각각 한 번씩만 나와야 하며,
        각 항목의 reasons 배열은 정확히 3개의 문자열이어야 합니다.
        {
          "rankings": [
            {
              "rank": 1,
              "recommendationType": "REFORM 또는 RESELL 또는 UPCYCLING",
              "reasons": ["이유1", "이유2", "이유3"],
              "recommendedWorks": [{"title": "...", "description": "..."}, ...] (REFORM일 때만 포함),
              "upcyclingCandidates": [{"itemName": "...", "description": "..."}, ...] (UPCYCLING일 때만 포함)
            },
            {"rank": 2, "recommendationType": "...", "reasons": ["...", "...", "..."]},
            {"rank": 3, "recommendationType": "...", "reasons": ["...", "...", "..."]}
          ]
        }
        """;

        String userPrompt = """
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

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "response_format", Map.of("type", "json_object")
        );

        Map<String, Object> response = openAiRestClient.post()
                .uri("/chat/completions")
                .body(requestBody)
                .retrieve()
                .body(Map.class);

        String content = extractContent(response);
        return parseJudgment(content);
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
                if (r.getReasons() == null || r.getReasons().size() != 3) {
                    throw new IllegalStateException("이유가 3개가 아닙니다: " + json);
                }
                if ("REFORM".equals(r.getRecommendationType())
                        && (r.getRecommendedWorks() == null || r.getRecommendedWorks().size() != 3)) {
                    throw new IllegalStateException("REFORM의 recommendedWorks가 3개가 아닙니다: " + json);
                }
                if ("UPCYCLING".equals(r.getRecommendationType())
                        && (r.getUpcyclingCandidates() == null || r.getUpcyclingCandidates().size() != 3)) {
                    throw new IllegalStateException("UPCYCLING의 upcyclingCandidates가 3개가 아닙니다: " + json);
                }
            }
            judgment.getRankings().sort((a, b) -> Integer.compare(a.getRank(), b.getRank()));
            return judgment;
        } catch (Exception e) {
            throw new IllegalStateException("OpenAI 응답 파싱 실패: " + json, e);
        }
    }
}
