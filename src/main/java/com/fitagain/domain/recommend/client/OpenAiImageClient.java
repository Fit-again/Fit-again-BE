package com.fitagain.domain.recommend.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Base64;
import java.util.Map;

@Slf4j
@Component
public class OpenAiImageClient {

    private final RestClient openAiRestClient;
    private final ObjectMapper objectMapper;

    public OpenAiImageClient(
            @Qualifier("openAiRestClient") RestClient openAiRestClient,
            ObjectMapper objectMapper
    ) {
        this.openAiRestClient = openAiRestClient;
        this.objectMapper = objectMapper;
    }

    public byte[] generateImage(String prompt) {

        log.info("========== OpenAI 이미지 생성 시작 ==========");
        log.info("프롬프트: {}", prompt);

        Map<String, Object> requestBody = Map.of(
                "model", "gpt-image-2",
                "prompt", prompt,
                "size", "1024x1024",
                "quality", "medium",
                "n", 1
        );

        try {
            String rawResponse = openAiRestClient.post()
                    .uri("/images/generations")
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(rawResponse);
            String base64Data = root.path("data").get(0).path("b64_json").asText();

            byte[] imageBytes = Base64.getDecoder().decode(base64Data);

            log.info("========== OpenAI 이미지 생성 완료 ==========");
            log.info("생성 이미지 크기: {} bytes", imageBytes.length);

            return imageBytes;

        } catch (Exception e) {
            log.error("========== OpenAI 이미지 생성 실패 ==========");
            log.error("에러 메시지: {}", e.getMessage(), e);
            throw e;
        }
    }
}