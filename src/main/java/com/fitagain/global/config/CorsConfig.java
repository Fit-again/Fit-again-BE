package com.fitagain.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 모든 API 주소에 대해
                // 데이터 교환이 가능한 URL 지정
                .allowedOrigins(
                        "http://localhost:8080",
                        "http://localhost:3000",
                        "http://localhost:5173",
                        "https://fit-again-fe.vercel.app"
                )
                // 허용하는 HTTP METHOD 지정
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                // 허용 헤더 설정
                .allowedHeaders("*")
                // 프론트엔드가 응답 헤더에서 토큰을 읽을 수 있도록 노출
                .exposedHeaders("Authorization")
                // 쿠키 및 인증 정보 전송 허용
                .allowCredentials(true);
    }
}