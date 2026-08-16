package com.fitagain;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class FitagainApplication {

	public static void main(String[] args) {
		SpringApplication.run(FitagainApplication.class, args);
	}

}
