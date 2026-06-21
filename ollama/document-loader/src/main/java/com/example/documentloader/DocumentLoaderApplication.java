package com.example.documentloader;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;


@SpringBootApplication
@ConfigurationPropertiesScan("com.example.documentloader")
public class DocumentLoaderApplication {

	public static void main(String[] args) {
		SpringApplication.run(DocumentLoaderApplication.class, args);
	}

}
