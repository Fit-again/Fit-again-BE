package com.fitagain.domain.task.service.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@Transactional
public class DiagnosisAiService {

    @Value("${openai.api-key}")
    private String openAiApiKey;

    @Value("${openai.model:gpt-4o}")
    private String model;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public Map<String, Object> analyzeDamage(String productType, List<String> keywords, String description,
                                             String frontImageUrl, List<String> detailImageUrls, List<String> damageImageUrls) {

        try {
            log.info("OpenAI GPT-4o 진단 분석(S3 URL 활용) API 호출 시작...");

            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", model);

            ArrayNode messagesArray = requestBody.putArray("messages");
            ObjectNode userMessage = messagesArray.addObject();
            userMessage.put("role", "user");

            ArrayNode contentArray = userMessage.putArray("content");

            // 1. 텍스트 프롬프트 추가
            ObjectNode textContent = contentArray.addObject();
            textContent.put("type", "text");
            String prompt = String.format(
                    "당신은 명품 %s 분석 전문가입니다.\n" +
                    "사용자가 입력한 불편 키워드: %s\n" +
                    "사용자 설명: %s\n\n" +
                    "당신에게 제공된 사진들은 다음 순서와 역할을 가집니다:\n" +
                    "1. 첫 번째 사진: 제품의 전체적인 형태를 보여주는 '대표 정면 사진'입니다. (필수)\n" +
                    "2. 그 다음 사진들: 제품의 이해를 돕기위한 추가적인 '디테일 사진'들입니다. (생략되었을 수 있음)\n" +
                    "3. 마지막 사진들: 사용자가 직접 촬영한 '손상 부위 사진'들입니다. (생략되었을 수 있음)\n\n" +
                    "제공된 사진들을 분석하되, '손상 상태(damageState)'를 진단할 때는 반드시 마지막에 첨부된 '손상 부위 사진'들을 중점적으로 확인하여 결과를 도출해 주세요.\n" +
                    "손상 부위 사진이 생략되어 있다면 대표 정면 사진을 참고하여 손상 부위가 있는지 확인해주시고, 크게 티나는 정도가 아닌 사소한 정도는 손상 부위 없음으로 인지해주세요.\n\n" +
                    "이러한 분석을 바탕으로 아래 JSON 형식에 맞게 진단 결과를 응답해 주세요.\n" +
                    "형식: {\n" +
                    "  \"externalStructure\": [\"외부 구조 특징 최대 3개 (반드시 한국어로 작성. 예: 더블 핸들, 탈부착 스트랩, 사이드 포켓)\"],\n" +
                    "  \"damageState\": [\"손상 상태 최대 3개 (반드시 한국어로 작성. 예: 모서리 마모, 스트랩 사용감)\"],\n" +
                    "  \"currentPurpose\": \"사용자의 설명란이나 가방 형태에 의해 알 수 있다면 '출퇴근용'과 같이 사용 목적을 응답하고, 그렇지 않다면 '확인할 수 없음' 반환 (반드시 한국어로 작성)\",\n" +
                    "  \"mainInconvenience\": [\"주요 불편 원인 최대 3개 요약 (반드시 한국어로 작성. 예: 어깨에 부담이 감, 스트랩이 자주 흘러내림)\"],\n" +
                    "  \"areasForImprovement\": [\"개선 필요 부분 최대 3개 요약 (반드시 한국어로 작성. 예: 경량 스트랩 교체, 어깨 패드 추가, 모서리 보강)\"],\n" +
                    "  \"color\": \"대표 정면 사진에 보이는 가방의 주된 색상을 영어로 간결하게 (예: pink, dark brown, cream)\",\n" +
                    "  \"size\": \"가방의 대략적인 크기 (예: small, medium, large)\",\n" +
                    "  \"pattern\": \"가방 표면의 패턴/재질 특징 (예: solid, monogram, quilted, no distinct pattern)\"\n" +
                    "}\n" +
                    "중요 규칙: color, size, pattern 세 가지 필드는 이후 이미지 생성에 쓰이므로 반드시 '영어'로 반환하고, 나머지 모든 텍스트(externalStructure, damageState, currentPurpose, mainInconvenience, areasForImprovement)는 절대 영어를 쓰지 말고 오직 '한국어'로만 반환해 주세요.",
                    productType, keywords.toString(), description != null ? description : "없음"
            );
            textContent.put("text", prompt);

            // 2. 이미지 URL 일괄 추가 로직
            addImageUrl(contentArray, frontImageUrl);
            if (detailImageUrls != null) detailImageUrls.forEach(url -> addImageUrl(contentArray, url));
            if (damageImageUrls != null) damageImageUrls.forEach(url -> addImageUrl(contentArray, url));

            // 3. HTTP 요청 전송
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + openAiApiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                    .timeout(Duration.ofSeconds(60))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("======================================================");
                log.error("❌ [DiagnosisAiService] OpenAI API 진단 요청 실패!");
                log.error("상태 코드: {}", response.statusCode());
                log.error("응답 바디(에러 상세): {}", response.body());
                log.error("======================================================");
                throw new RuntimeException("AI 서버 에러 발생 (" + response.statusCode() + ")");
            }

            // 4. 응답 파싱
            JsonNode rootNode = objectMapper.readTree(response.body());
            String responseText = rootNode.path("choices").get(0).path("message").path("content").asText();
            
            if (responseText.startsWith("```json")) {
                responseText = responseText.replace("```json", "").replace("```", "").trim();
            }

            log.info("진단 분석 결과: {}", responseText);
            
            return objectMapper.readValue(responseText, new TypeReference<Map<String, Object>>() {});

        } catch (Exception e) {
            log.error("진단 AI 호출 중 오류 발생", e);
            throw new RuntimeException("진단 AI 호출 실패: " + e.getMessage());
        }
    }

    private void addImageUrl(ArrayNode contentArray, String url) {
        if (url == null || url.isBlank()) return;
        ObjectNode imageContent = contentArray.addObject();
        imageContent.put("type", "image_url");
        imageContent.putObject("image_url").put("url", url);
    }
}
