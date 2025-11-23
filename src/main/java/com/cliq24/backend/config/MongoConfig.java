package com.cliq24.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(basePackages = "com.cliq24.backend.repository")
@EnableMongoAuditing
public class MongoConfig {
    // MongoDB configuration is handled via application.properties
    // This class enables MongoDB repositories and auditing
}
