package com.example.boardgamebuddy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.resilience.annotation.EnableResilientMethods;

@SpringBootApplication
@EnableResilientMethods
public class BoardGameBuddyApplication {

	public static void main(String[] args) {
		SpringApplication.run(BoardGameBuddyApplication.class, args);
	}

}
