package com.fitagain.domain.image.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

@Slf4j
@Service
public class VisionAiService {

    @Value("${spring.ai.openai.api-key}")
    private String openAiApiKey;

    @Value("${spring.ai.openai.chat.options.model:gpt-4o}")
    private String model;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    // AI 응답 결과를 담을 내부 레코드
    public record VisionResult(boolean isValid, String message) {}

    public VisionResult verifyBagImages(org.springframework.web.multipart.MultipartFile frontImage, List<org.springframework.web.multipart.MultipartFile> detailImages) {
        try {
            log.info("OpenAI GPT-4o 직접 HTTP 호출 (Base64 인코딩) 시작...");

            // 1. 요청 JSON Body 생성
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", model);

            ArrayNode messagesArray = requestBody.putArray("messages");
            ObjectNode userMessage = messagesArray.addObject();
            userMessage.put("role", "user");

            ArrayNode contentArray = userMessage.putArray("content");

            // 텍스트 프롬프트 추가
            ObjectNode textContent = contentArray.addObject();
            textContent.put("type", "text");
            String textPrompt = "당신은 명품 가방 수선 전문가입니다. " +
                    "첨부된 사진들(정면 사진 1장 및 추가 디테일 사진들)을 보고 이것이 진짜 가방 사진이 맞는지 판단해주세요. " +
                    "만약 사람이거나 풍경, 애완동물, 빈 화면 등 가방이 아니라면 isValid를 false로 반환하세요. " +
                    "가방의 정면이 잘 보이고 수선/분석이 가능한 사진이라면 isValid를 true로 반환하세요. " +
                    "응답은 반드시 아래 JSON 형식으로만 반환하세요: {\"isValid\": true, \"message\": \"isValid 값이 true 라면 '정상적인 가방 사진', 아니라면 사유를 1문장으로 작성\"}";
            textContent.put("text", textPrompt);

            // 정면 이미지 추가 (Base64 인코딩)
            ObjectNode frontImageContent = contentArray.addObject();
            frontImageContent.put("type", "image_url");
            String frontBase64 = java.util.Base64.getEncoder().encodeToString(frontImage.getBytes());
            frontImageContent.putObject("image_url").put("url", "data:" + frontImage.getContentType() + ";base64," + frontBase64);

            // 디테일 이미지 추가 (Base64 인코딩)
            if (detailImages != null && !detailImages.isEmpty()) {
                for (org.springframework.web.multipart.MultipartFile detailImage : detailImages) {
                    if (!detailImage.isEmpty()) {
                        ObjectNode detailImageContent = contentArray.addObject();
                        detailImageContent.put("type", "image_url");
                        String detailBase64 = java.util.Base64.getEncoder().encodeToString(detailImage.getBytes());
                        detailImageContent.putObject("image_url").put("url", "data:" + detailImage.getContentType() + ";base64," + detailBase64);
                    }
                }
            }

            // 2. HTTP 요청 전송
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + openAiApiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("OpenAI API 에러: {}", response.body());
                return new VisionResult(false, "AI 서버 에러 발생 (" + response.statusCode() + ")");
            }

            // 3. 응답 JSON 파싱
            JsonNode rootNode = objectMapper.readTree(response.body());
            String responseText = rootNode.path("choices").get(0).path("message").path("content").asText();

            log.info("OpenAI 응답 결과: {}", responseText);

            if (responseText.startsWith("```json")) {
                responseText = responseText.replace("```json", "").replace("```", "").trim();
            }

            JsonNode resultNode = objectMapper.readTree(responseText);
            boolean isValid = resultNode.get("isValid").asBoolean();
            String message = resultNode.get("message").asText();

            return new VisionResult(isValid, message);

        } catch (Exception e) {
            log.error("비전 AI 검증 중 오류 발생", e);
            // 오류 발생 시 프론트엔드 작업이 블로킹되지 않도록 기본값(false) 반환
            return new VisionResult(false, "AI 서버 연동 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}
