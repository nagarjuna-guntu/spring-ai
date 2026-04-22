package com.example.chatmemory;

import org.springframework.boot.SpringApplication;

public class TestChatMemoryVectorStoreApplication {

	public static void main(String[] args) {
		SpringApplication.from(ChatMemoryVectorStoreApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
