package com.example.gamerulesrag;

import org.springframework.boot.SpringApplication;

public class TestGameRulesRagApplication {

	public static void main(String[] args) {
		SpringApplication.from(GameRulesRagApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
