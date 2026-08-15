//package com.fitagain.global.config;
//
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.web.client.RestClient;
//
//@Configuration
//public class HuggingFaceConfig {
//
//    @Value("${huggingface.api-key}")
//    private String apiKey;
//
//    @Bean(name = "huggingFaceRestClient")
//    public RestClient huggingFaceRestClient() {
//        return RestClient.builder()
//                .baseUrl("https://router.huggingface.co/hf-inference/models")
//                .defaultHeader("Authorization", "Bearer " + apiKey)
//                .defaultHeader("Content-Type", "application/json")
//                .build();
//    }
//}

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
                .baseUrl("https://router.huggingface.co/hf-inference/models")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
