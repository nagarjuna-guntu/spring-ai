package com.example.chatmemory;

import org.springframework.boot.SpringApplication;

public class TestChatMemoryApplication {

	public static void main(String[] args) {
		SpringApplication.from(ChatMemoryApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
