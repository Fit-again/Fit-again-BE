package com.fitagain.domain.recommend.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Slf4j
@Component
public class HuggingFaceImageClient {

    private final RestClient huggingFaceRestClient;

    @Value("${huggingface.model}")
    private String model;

    public HuggingFaceImageClient(
            @Qualifier("huggingFaceRestClient")
            RestClient huggingFaceRestClient
    ) {
        this.huggingFaceRestClient = huggingFaceRestClient;
    }

    public byte[] generateImage(String prompt) {

        log.info("========== Hugging Face 이미지 생성 시작 ==========");
        log.info("모델: {}", model);
        log.info("프롬프트: {}", prompt);

        Map<String, Object> requestBody = Map.of(
                "inputs", prompt,
                "parameters", Map.of(
                        "negative_prompt",
                        "text, watermark, characters, writing, logo, letters, calligraphy, chinese characters, korean text, japanese text, signature, caption"
                )
        );

        try {
            byte[] resultImageBytes = huggingFaceRestClient.post()
                    .uri("/{model}", model)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.IMAGE_PNG)
                    .body(requestBody)
                    .retrieve()
                    .body(byte[].class);

            if (resultImageBytes == null || resultImageBytes.length == 0) {
                throw new IllegalStateException(
                        "Hugging Face 응답에서 이미지 데이터를 받지 못했습니다."
                );
            }

            log.info("========== Hugging Face 이미지 생성 완료 ==========");
            log.info("모델: {}", model);
            log.info("생성 이미지 크기: {} bytes", resultImageBytes.length);

            return resultImageBytes;

        } catch (Exception e) {
            log.error("========== Hugging Face 이미지 생성 실패 ==========");
            log.error("모델: {}", model);
            log.error("에러 메시지: {}", e.getMessage(), e);

            throw e;
        }
    }
}