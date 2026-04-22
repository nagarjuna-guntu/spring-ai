package com.example.tools;

import org.springframework.boot.SpringApplication;

public class TestToolsMethodsAsToolsApplication {

	public static void main(String[] args) {
		SpringApplication.from(ToolsMethodsAsToolsApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
