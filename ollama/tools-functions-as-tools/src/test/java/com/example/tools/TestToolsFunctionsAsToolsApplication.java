package com.example.tools;

import org.springframework.boot.SpringApplication;

public class TestToolsFunctionsAsToolsApplication {

	public static void main(String[] args) {
		SpringApplication.from(ToolsFunctionsAsToolsApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
