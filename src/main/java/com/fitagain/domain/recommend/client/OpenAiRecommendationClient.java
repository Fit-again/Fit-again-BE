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
        당신은 중고 가방 리폼/리셀/업사이클 상담사입니다.
        제품 정보와 사용자가 느끼는 불편을 보고, REFORM(리폼) / RESELL(리셀) / UPCYCLING(업사이클링)
        세 가지 방향 모두에 대해 1순위(가장 추천)부터 3순위(가장 비추천)까지 순위를 매기세요.
        각 방향마다 그 순위인 이유를 정확히 3개의 짧은 한국어 불릿 문장으로 작성하세요.

        반드시 아래 JSON 형식으로만 답하세요. 다른 텍스트나 코드블록은 절대 포함하지 마세요.
        rankings 배열은 정확히 3개 항목이어야 하고, REFORM/RESELL/UPCYCLING이 각각 한 번씩만 나와야 하며,
        각 항목의 reasons 배열은 정확히 3개의 문자열이어야 합니다.
        {
          "rankings": [
            {"rank": 1, "recommendationType": "REFORM 또는 RESELL 또는 UPCYCLING", "reasons": ["이유1", "이유2", "이유3"]},
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
                diagnosisResult == null ? "없음" : diagnosisResult.toString()
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
            }
            judgment.getRankings().sort((a, b) -> Integer.compare(a.getRank(), b.getRank()));
            return judgment;
        } catch (Exception e) {
            throw new IllegalStateException("OpenAI 응답 파싱 실패: " + json, e);
        }
    }
}