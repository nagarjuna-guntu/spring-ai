package com.example.rag;

import org.springframework.boot.SpringApplication;

public class TestRagRagAdvisorQueryTransformerApplication {

	public static void main(String[] args) {
		SpringApplication.from(RagRagAdvisorQueryTransformerApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
