package com.cliq24.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;



@SpringBootApplication
public class Cliq24Application {

	public static void main(String[] args) {
		SpringApplication.run(Cliq24Application.class, args);
	}

}
