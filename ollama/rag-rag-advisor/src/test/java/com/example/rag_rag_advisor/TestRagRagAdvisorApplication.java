package com.example.rag_rag_advisor;

import org.springframework.boot.SpringApplication;

public class TestRagRagAdvisorApplication {

	public static void main(String[] args) {
		SpringApplication.from(RagRagAdvisorApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
