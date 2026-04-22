package com.example.chatmemory;

import org.springframework.boot.SpringApplication;

public class TestChatMemoryJdbcApplication {

	public static void main(String[] args) {
		SpringApplication.from(ChatMemoryJdbcApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
