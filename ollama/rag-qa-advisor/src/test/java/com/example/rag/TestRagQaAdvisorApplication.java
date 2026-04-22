package com.example.rag;

import org.springframework.boot.SpringApplication;

public class TestRagQaAdvisorApplication {

	public static void main(String[] args) {
		SpringApplication.from(RagQaAdvisorApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
