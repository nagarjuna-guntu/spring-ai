package com.example.chatmemory;

import org.springframework.boot.SpringApplication;

public class TestChatMemoryCassandraApplication {

	public static void main(String[] args) {
		SpringApplication.from(ChatMemoryCassandraApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
