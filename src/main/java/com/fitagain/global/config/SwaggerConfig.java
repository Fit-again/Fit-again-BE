package com.fitagain.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI fitagainAPI() {
        return new OpenAPI()
                // 프론트엔드가 EC2 주소로 들어와서 테스트할 때 로컬호스트로 날아가지 않도록 Base URL을 상대경로로 고정!
                .addServersItem(new Server().url("/")) 
                .info(new Info()
                        .title("Fit-again API 명세서")
                        .description("Fit-again 백엔드 프로젝트 API 명세서입니다.")
                        .version("1.0.0"));
    }
}
