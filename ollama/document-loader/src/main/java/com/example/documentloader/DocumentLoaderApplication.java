package com.example.documentloader;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DocumentLoaderApplication {

	public static void main(String[] args) {
		SpringApplication.run(DocumentLoaderApplication.class, args);
	}

}
